/*
	Copyright 2012 Jonathan West

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
*/

package com.socketanywhere.irc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.socketanywhere.irc.listener.ConnectionResponseListener;
import com.socketanywhere.irc.listener.IJoinLeaveListener;
import com.socketanywhere.irc.listener.IMsgListener;
import com.socketanywhere.irc.listener.IQuitListener;
import com.socketanywhere.irc.listener.QueryAddressListener;
import com.socketanywhere.irc.listener.QueryListeningListener;
import com.socketanywhere.irc.listener.QueryNickResponseListener;
import com.socketanywhere.net.CompletableWait;
import com.socketanywhere.net.SoAnUtil;


/** This class opens TCP sockets directly to IRC servers, and is the interface between them and the rest of
 * our class code. Implements nickname support, join/leave/quit listeners, monitors text
 * in the current channel, writes a LARGE variety of commands to the channel (and listens for their response),
 * etc.   */
public class IRCConnection {

	String _host;
	int _port;
	Socket _socket;
	SocketReaderThread _thread;
	NodeImpl _node;
	String _currentChannel;

//	int _nextNickID = 1;
	
	// The info part of a host's whois query
	String _userWhoisInfo = "Xn.";
	
	/** The prefix is taken and appended to UUID string to generate nick */
	String _nicknamePrefix = "Xn-";
	
	/** The user name of a whois query on the user */
	String _userNameWhois = "Xn";
	
	List<IJoinLeaveListener> _joinLeaveListeners = new ArrayList<IJoinLeaveListener>();
	List<IMsgListener> _msgListeners = new ArrayList<IMsgListener>();
	List<IQuitListener> _quitListeners = new ArrayList<IQuitListener>();
	
	/** Time at which data was last sent (in milliseconds) */
	long _timeOfLastDataSend = -1;
	Object _lastDataLock = new Object();
	
	public IRCConnection(NodeImpl node) {
		_node = node;
	}
	
	/** Blocks until connected */
	public void connect(String host, int port) throws UnknownHostException, IOException {
		_host = host;
		_port = port;
		_socket = new Socket(host, port);
		_thread = new SocketReaderThread(_socket, this);
		_thread.start();
		
		try {
			synchronized(_thread.getReadyForCommandsMonitor()) {
				if(!_thread.isReadyForCommands()) {
					_thread.getReadyForCommandsMonitor().wait();
				}
			}
		} catch(InterruptedException e) {
			return;
		}
	}
	
	List<IJoinLeaveListener> getJoinLeaveListeners() {
		return _joinLeaveListeners;
	}
	
	List<IMsgListener> getMsgListeners() {
		return _msgListeners;
	}
	
	List<IQuitListener> getQuitListeners() {
		return _quitListeners;
	}
	
	public void addJoinLeaveListener(IJoinLeaveListener l) {
		synchronized(_joinLeaveListeners) {
			_joinLeaveListeners.add(l);
		}
	}
	
	public void addMsgListener(IMsgListener l) {
		synchronized(_msgListeners) {
			_msgListeners.add(l);
		}
	}
	
	public void addQuitListener(IQuitListener l) {
		synchronized(_quitListeners) {
			_quitListeners.add(l);
		}
	}
	
	public void removeMsgListener(IMsgListener l) {
		synchronized(_msgListeners) {
			_msgListeners.remove(l);
		}
		
	}
	
	public void removeQuitListener(IQuitListener l) {
		synchronized(_quitListeners) {
			_msgListeners.remove(l);
		}
	}
	
	public void removeJoinLeaveListener(IJoinLeaveListener l) {
		synchronized(_joinLeaveListeners) {
			_msgListeners.remove(l);
		}
	}
	
	public String getNicknamePrefix() {
		return _nicknamePrefix;
	}
	
	public void setNicknamePrefix(String nickName) {
		_nicknamePrefix = nickName;
	}

	public String queryNickForUUID(String nickname) throws IOException {
		
		long context = _node.getNextContext();
		
		QueryNickResponseListener ql = new QueryNickResponseListener(_node, nickname, context);
		CompletableWait w = ql.getResponseWait();
		addMsgListener(ql);
		
		String cmd = "!query-nick name("+nickname+") src-uuid("+_node.getUUID()+") ctxt("+context+")";

		if(_node.getConnections().size() != 0) {
			IRCConnection c = _node.getConnections().get(0);
			c.sayInChannel(cmd);
		}
		
		w.waitForCompleteTimeout(10000);
		removeMsgListener(ql);
		
		return ql.getResponseUUID();
	}
	
	public List<IRCSocketAddress> queryListening(String uuid) throws IOException {
//		!query-listening uuid() src-uuid() ctxt()

		long context = _node.getNextContext();
		
		String cmd = "!query-listening uuid("+uuid+") src-uuid("+_node.getUUID()+") ctxt("+context+")";
		QueryListeningListener ql = new QueryListeningListener(_node, uuid, context);
		
		addMsgListener(ql);
		
		if(_node.getConnections().size() != 0) {
			IRCConnection c = _node.getConnections().get(0);
			c.sayInChannel(cmd);
		}
		
		ql.getResponseWait().waitForCompleteTimeout(10000);
		
		removeMsgListener(ql);
		
		return ql.getResponse();
		
	}
	
	public String queryAddress(String address, int port) throws IOException {

		long context = _node.getNextContext();

		String cmd = "!query-address address("+address+") port("+port+") my-uuid("
			+_node.getUUID()+") ctxt("+context+")";
		
		QueryAddressListener ql = new QueryAddressListener(_node, context);
		
		addMsgListener(ql);
		
		sayInChannel(cmd);
		
		ql.getResponseWait().waitForCompleteTimeout(10000);
		
		removeMsgListener(ql);
		
		String responseUUID = ql.getResponseUUID();
		
		// Either the uuid from the other node, or null
		return responseUUID;		
	}
	
	public IRCSocketConnection establishConnection(String address, int port) throws IOException {
		
		// Check our address list to see if we know who listens on this address / port 
		String uuid = _node.getAddrToUUIDMap().get(new IRCSocketAddress(address, port));
		if(uuid == null) {
			uuid = queryAddress(address, port);
			
			if(uuid == null) return null;
		}
		
		
		long context = _node.getNextContext();
		ConnectionResponseListener crl = new ConnectionResponseListener(_node, uuid, context);
		
		String cmd = "!connect ";
		cmd += "address("+address+") ";
		cmd += "port("+port+") ";
		cmd += "uuid("+uuid+") ";
		cmd += "my-uuid("+_node.getUUID()+") ";
		cmd += "ctxt("+context+")";
		
		addMsgListener(crl);

		sayInChannel(cmd);
		
		crl.getResponseWait().waitForCompleteTimeout(10000);
		
		removeMsgListener(crl);
		
		if(crl.isConnectionSuccess()) {
			IRCSocketConnection sc = new IRCSocketConnection(_node);
			sc.setAreWeConnOriginator(true);
			sc.setRemoteConnectionID(crl.getConnectionID());
			sc.setRemoteAddress(new IRCSocketAddress(address, port));
			sc.setRemoteUUID(uuid);
			
			return sc;
		}
		
		return null;
		
	}

	public void joinChannel(String channel, String password) throws IOException {
		writeToConnection("join "+channel+ " "+password);
		try { Thread.sleep(4000); } catch (InterruptedException e) {}
//		writeToConnection("MODE "+channel+" +p");
//		try { Thread.sleep(2000); } catch (InterruptedException e) {}
		writeToConnection("MODE "+channel+" +k "+password);
		try { Thread.sleep(1000); } catch (InterruptedException e) {}
		_currentChannel = channel;
	}
	
	public void joinChannel(String channel) throws IOException {
		writeToConnection("join "+channel);
		try { Thread.sleep(4000); } catch (InterruptedException e) {}
//		writeToConnection("MODE "+channel+" +p");
//		try { Thread.sleep(2000); } catch (InterruptedException e) {}
		_currentChannel = channel;
	}

	public void sayInChannel(String message) throws IOException {
		sendPrivMessage(getCurrentChannel(), message);
	}
	
	public void sendPrivMessage(String username, String message) throws IOException {
		writeToConnection("privmsg "+username + " :"+message);
	}
	
	public void writeToConnection(String text) throws IOException {
		synchronized(_socket.getOutputStream()) {
			_socket.getOutputStream().write((text+"\n").getBytes());
		}		
	}
	
	public String getCurrentChannel() {
		return _currentChannel;
	}
	
	public void disconnect() {
		_thread.disconnect();
	}
	
	/** writeData in IRCConnection will not do any splitting of data[],
	 * it is assumed this will be handled by the calling node */
	public void writeData(IRCSocketConnection ic, long seqNum, byte data[]) throws IOException {
		final boolean isShortForm = true;
				
//		(recv/sender)-conn-id() my-uuid() target-uuid() ctxt() data()
		StringBuffer sb = new StringBuffer();
		
		if(isShortForm) {
			sb.append("!p ");
		} else {
			sb.append("!packet ");
		}
		
		
		// The server always determines the connection id
		if(ic.areWeConnOriginator()) {
			
			// If we originated, we are not the server, so it is the receiver's conn id
			if(isShortForm) sb.append("rid"); 
			else sb.append("recv-conn-id");
			sb.append("("+ic.getRemoteConnectionID()+") ");
			
		} else {
			
			// If we did not originate, we are the server, so it is our id
			if(isShortForm) sb.append("sid"); 
			else sb.append("sender-conn-id");
			sb.append("("+ic.getOurConnectionID()+") ");
			
		}

		String shortenedUUID = _node.getRemoteShortenedUUID(ic.getRemoteUUID());
		if(shortenedUUID != null) {
			// We have a shortened UUID, so use it
			if(isShortForm) sb.append("shd"); 
			else sb.append("short-uuid");
			sb.append("("+shortenedUUID+") ");			
			
		} else {
			// We don't have a shortened UUID (yet) for this remote node, so use this
			if(isShortForm) sb.append("tid"); 
			else sb.append("target-uuid");
			sb.append("("+ic.getRemoteUUID()+") ");			
		}
				
		if(isShortForm) sb.append("s"); 
		else sb.append("seq");
		sb.append("("+seqNum+") ");
		
		if(isShortForm) sb.append("d"); 
		else sb.append("data");
		sb.append("("+SoAnUtil.encodeBase64(data)+")");
		
		synchronized(_lastDataLock) {
			_timeOfLastDataSend = System.currentTimeMillis();
		}
		
		sayInChannel(sb.toString());
	}

	public long getTimeOfLastDataSent() {
		synchronized(_lastDataLock) {
			return _timeOfLastDataSend;
		}
	}
	
	public String getUserWhoisInfo() {
		return _userWhoisInfo;
	}
	
	public String getWhoisUserName() {
		return _userNameWhois;
	}
	
	public void setUserNameWhois(String _userNameWhois) {
		this._userNameWhois = _userNameWhois;
	}
	
	public void setUserWhoisInfo(String _userWhoisInfo) {
		this._userWhoisInfo = _userWhoisInfo;
	}
	
}


class SocketReaderThread extends Thread {
	Socket _socket;
	IRCConnection _conn;
	Object _readyForCommandsMonitor = new Object();
	boolean _readyForCommands = false;
	boolean _closed = false;
	
	public SocketReaderThread(Socket socket, IRCConnection conn) {
		setName(SocketReaderThread.class.getName());
		_socket = socket;
		_conn = conn;
		_closed = false;
	}
	
	public void disconnect() {
		
		_closed = true;
		
		try {
			_conn.writeToConnection("quit\n");
		} catch (IOException e1) { }
		
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {}
		
		try {
			_socket.close();
		} catch (IOException e) {
		}
	}
	
	private String generateRandomSuffix(int length) {
		String str = UUID.randomUUID().toString().replace("-", "");
		
		return str.substring(0, length);
			
	}
	
	private void selectNick() {
		try {
			
			_conn.writeToConnection("NICK "+_conn.getNicknamePrefix()+ generateRandomSuffix(12));
			
			_conn.writeToConnection("USER "+_conn.getWhoisUserName()+" 8 * : "+_conn.getUserWhoisInfo()+" \n");
			
			InputStream is = _socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
		
			String str;
			while(  null != (str = br.readLine())) {
				if(str.indexOf("433") != -1) {
					_conn.writeToConnection("NICK "+_conn.getNicknamePrefix()+ generateRandomSuffix(12));
				} else {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
				
		InputStream is;
		try {

			Thread.sleep(3000);
	
//			_conn.writeToConnection("NICK "+_conn.getNicknamePrefix());
			selectNick();
//			_conn.writeToConnection("USER m2 8 * : xn. \n");
			
			is = _socket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
		
			String str;
			while(  null != (str = br.readLine())) {
//				System.out.println("("+Thread.currentThread().getId()+") ["+str+"]");
				
				try {
					parseLine(str);
				} catch(Exception e) {
					if(!_closed) {
						// This catches any exceptions thrown by parseLine or its listeners, to keep
						// them from taking out the IRC connection listener thread
						e.printStackTrace();
					}
				}
			}
			
		} catch(IOException e) {
			if(!_closed) {
				e.printStackTrace();
				return;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void parseLine(String line) {
		if(line == null || line.trim().length() == 0) return;
		
		System.out.println("["+_conn._host + "] " +line);
		
		String[] strArr = line.split(" ");
		
//		for(int x = 0; x < strArr.length; x++)
//			System.out.println(strArr[x]);
		
		String src = strArr[0];
		String action = strArr[1];

		String nick = null;
		String addr = null;
		
		if(src.contains("!")) {
			nick = src.substring(1);
			String[] tempArr = nick.split("!");
			nick = tempArr[0];
			addr = tempArr[1];
		}
		
		if(src.equalsIgnoreCase("PING")) {
			try {
				_conn.writeToConnection("PONG "+strArr[1] + "\n"); 
			} catch (IOException e) {
				return;
			}
		}
		
		/** When we receive MOTD end, signal that connection is ready for commands */
		if(action.equalsIgnoreCase("376")) {
			
			synchronized(_readyForCommandsMonitor) {
				_readyForCommands = true;
				_readyForCommandsMonitor.notifyAll();
			}
		}
		
		if(action.equalsIgnoreCase("QUIT")) {
			
			for(IQuitListener l : _conn.getQuitListeners()) {
				String tmpLine = line.substring(1);
				
				String quitMsg = tmpLine.substring(tmpLine.indexOf(":"));
				l.userQuit(nick, addr, quitMsg);
				
			}
			
		}
		
		if(action.equalsIgnoreCase("JOIN") || action.equalsIgnoreCase("PART")) {
			
			for(IJoinLeaveListener l : _conn.getJoinLeaveListeners()) {
				
				// Third field, strip out the leading :
				String channel = strArr[2].substring(1);
				
				if(action.equalsIgnoreCase("JOIN")) {
					l.userJoined(channel, nick, addr);	
				} else {
					// PART
					l.userLeft(channel, nick, addr);
				}
				
			}
			
		}
		
		if(action.equalsIgnoreCase("PRIVMSG")) {
			
			synchronized(_conn.getMsgListeners()) {
				for(IMsgListener l : _conn.getMsgListeners()) {
					String tmpLine = line.substring(1);
					
					String textMsg = tmpLine.substring(tmpLine.indexOf(":")+1);
					
					l.userMsg(nick, addr, strArr[2], textMsg, _conn);
					
				}
			}
		}
	}
	
	protected boolean isReadyForCommands() {
		return _readyForCommands;
	}
	protected Object getReadyForCommandsMonitor() {
		return _readyForCommandsMonitor;
	}
	
}
