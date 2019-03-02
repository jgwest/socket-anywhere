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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.socketanywhere.irc.listener.ListenRequestListener;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;

/** 
 * Node corresponds to a single instance of IRCSocketFactory, and, consequently, all sockets created
 * by that factory instance will use a single node. 
 * 
 * A single node will connect to many different IRC servers at the same time. This is because the data
 * rate limits of IRC servers are fairly strict, and in order to prevent being booted for excess floods,
 * it is necessary to split the data across multiple servers.
 * 
 * Two connected nodes must both be connected to the same set of servers, in order to communicate with each other,
 * otherwise data will be lost..
 * 
 * 
 * */
public class NodeImpl implements INode {
	
	/** List of addresses we are listening on */
	List<ListenedSocketAddress> _listeningOn = new ArrayList<ListenedSocketAddress>();
	
	/** IRC Connections that are presently active */
	List<IRCConnection> _ircConnections = new ArrayList<IRCConnection>();
	
	List<IRCSocketConnection> _activeConnections = new ArrayList<IRCSocketConnection>();
	
	/** Maps other node's UUID to the addresses they are listening on*/
	Map<String, UUIDAddressList> _uuidList = new HashMap<String, UUIDAddressList>();
	
	/** Maps the UUID of other nodes, to their shortened UUID*/
	Map<String, String> _uuidToShortenedUUIDMap = new HashMap<String, String>();
	
	/** The addresses that other nodes are listening on -> their UUID */
	Map<IRCSocketAddress, String> _addrToUUIDMap = new HashMap<IRCSocketAddress, String>();
	
	IRCPacketManager _packetManagerInst = new IRCPacketManager(this);
	
	IRCOutputStreamDataManager _outputDataManager = new IRCOutputStreamDataManager(this);
	
	/** List of IRC servers to connect to once connetcToServers() is called */
	List<String> _ircHostsToConnect = new ArrayList<String>();
		
	Map<String, GetShortenedUUIDThread> _uuidShortenedThreads = new HashMap<String, GetShortenedUUIDThread>();	
	
	String _uuid = null;
	
	String _shortenedUUID = null;
		
	static int _nextNickId = 1;
	
	long _nextContextId = 1;
	Object _contextLock = new Object();
	
	Object _connectionLock = new Object();
	long _nextConnectionId = 1; // protected by connectionLock
	boolean _isConnected = false; // protected by connectionLock
	boolean _closed = false; // protected by connectionLock
	
	
	public NodeImpl() {
		_uuid = UUID.randomUUID().toString();
	}
	
	public Map<IRCSocketAddress, String> getAddrToUUIDMap() {
		return _addrToUUIDMap;
	}
	
	public Map<String, UUIDAddressList> getUUIDtoAddrListMap() {
		return _uuidList;
	}

	public List<IRCConnection> getConnections() {
		return _ircConnections;
	}
	
	public List<ListenedSocketAddress> getListeningOn() {
		return _listeningOn;
	}
	
	public String getUUID() {
		return _uuid;
	}
	
	protected IRCConnection getCommandConnection() throws IOException {
		synchronized(_ircConnections) {
			if(_ircConnections.size() > 0) {
				
				IRCConnection conn = _ircConnections.get(0);
				return conn;
			} else {
				throw new IOException("Node is not connected");
			}
		}
		
	}
		
	/** Any calls after the first will return without error. */
	public void connectToServers() throws IOException {
		synchronized(_connectionLock) {
			if(_isConnected) {
				IRCSocketLogger.logDebug(this, "NodeImpl connect called when already connected.");
				return;
			} else {
				_isConnected = true;
			}
		}
		
		// The following code will run once per instance, as the above ensures thread-safety
		
		List<IRCServerConnectionThread> connectionThreads = new ArrayList<IRCServerConnectionThread>(); 
		for(String s : _ircHostsToConnect) {
			
			IRCServerConnectionThread t 
				= new IRCServerConnectionThread(this, s, 6667, "#HalC", "test12", "HalCXn-");
			t.start();
			connectionThreads.add(t);
			
//			IRCConnection ic = new IRCConnection(this);
//			
//			// Add the node state command response listener
//			SingleRespCommandListener l1 = new SingleRespCommandListener(this);
//			ic.addMsgListener(l1);
//			
//			ConnectionListener l2 = new ConnectionListener(this);
//			ic.addMsgListener(l2);
//			
//			DataConnectionListener l3 = new DataConnectionListener(this);
//			ic.addMsgListener(l3);
//							
//			try {
//				
//				// Move this to IRCConnection, remove ability to change, make XN-(UUID) or something
//				ic.setNicknamePrefix("HalCXn-");
//				
//				ic.connect(s, 6667);
//				
//				ic.joinChannel("#HalC", "test12");
//
//				synchronized(_ircConnections) {
//					_ircConnections.add(ic);
//				}
//			} catch(IOException e) {
//				synchronized(_connectionLock) {
//					_isConnected = false;
//					_closed = true;
//				}
//				IRCSocketLogger.logError(this, "IOException on connection "+SoAnUtil.convertStackTrace(e));
//				throw e;
//			}
		}
		
		long startTime = System.currentTimeMillis();
		
		boolean threadsComplete = false;
		boolean allThreadsSucceeded = false;
		while(System.currentTimeMillis() - startTime < 120 * 1000 && !threadsComplete) {
		
			threadsComplete = true;
			allThreadsSucceeded = true;

			for(IRCServerConnectionThread t : connectionThreads) {
				
				// For each thread, if it has not completed, wait a bit of time
				if(!t.isConnectionComplete()) {
					t.getResponseWait().waitForCompleteTimeout(1000);
					threadsComplete = false;
					allThreadsSucceeded = false;
				} else {
					// If thread is complete...
					
					if(!t.isConnected()) {
						// If one thread failed to connect then fail the whole thing
						allThreadsSucceeded = false;
						threadsComplete = true;
						break;
					}
					
				}
				
			}

		}
		
		if(allThreadsSucceeded) {
			
			// Add the resulting IRCConnections to _ircConnections
			synchronized(_ircConnections) {
				for(IRCServerConnectionThread t : connectionThreads) {
					_ircConnections.add(t.getResult());
				}

				synchronized (_connectionLock) {
					_isConnected = true;
					_closed = false;				
					
				}
			}
			
			startGenerateShortenedUUIDThread();
			
		} else {
			
			synchronized (_connectionLock) {
				_isConnected = false;
				_closed = true;					
			}
			
			IRCSocketLogger.logError(this, "Unable to connect to one of the IRC servers.");
			throw new IOException("Unable to connect to one of the IRC servers.");
		}
		
	}
	
	// TODO: ARCHITECTURE - The idea that the entire channel space is one big field is interesting, but many listens require a response on the initial channel/serve (for instance)
	
	/** Server socket impl calls this method to register itself as a listen
	 * on that port*/
	public boolean listenOnPort(int port, ISocketQueueReceiver receiver) throws IOException {
		boolean result = false;
		
		if(_closed) {
			throw new IOException("Node is closed.");
		}
		
		// Create the list of addresses to listen on
		List<String> listenAddrs = new ArrayList<String>();
		
		List<TLAddress> tlAddrs = SoAnUtil.getNameResolver().getLocalAddresses(); 
		List<String> addrs = SoAnUtil.convertTLAddressList(tlAddrs);
		for(String s : addrs) {
			if(s.equalsIgnoreCase("::1") || s.equalsIgnoreCase("127.0.0.1") 
					|| s.equalsIgnoreCase("localhost")) continue;
			listenAddrs.add(s);
		}
		
		IRCConnection conn = getCommandConnection();
		ListenRequestListener lrl = new ListenRequestListener(conn, this, listenAddrs, port);
		conn.addMsgListener(lrl);
		
		// If the no one blocks our listen, then this variable is added to _listeningOn
		List<ListenedSocketAddress> listListSockAddrs = new ArrayList<ListenedSocketAddress>();
		
		// Go through the names we plan to listen on
		for(String s : listenAddrs) {
	
			ListenedSocketAddress lsa = new ListenedSocketAddress(); 
			lsa.setSocketAddress(new IRCSocketAddress(s, port));
			lsa.setReceiver(receiver);
			
			listListSockAddrs.add(lsa);

			try {
				
				String str = "!listen-request ";
				str += "address("+s+") ";
				str += "port("+port+") ";
				str += "my-uuid("+_uuid+")";
				conn.sayInChannel(str);
				
			} catch (IOException e) {
				IRCSocketLogger.logError(this, "Unable to send listen request to IRC server.");
				conn.removeMsgListener(lrl);

				return false;
			}
		}
		
		lrl.getResponseWait().waitForCompleteTimeout(10000);
		
		// Success! Add our listeners to the list
		if(lrl.isListenSuccess()) {
			
			synchronized(_listeningOn) {
				_listeningOn.addAll(listListSockAddrs);
			}
			result = true;
			
			for(String s : listenAddrs) {
			
				try {
					String str = "!listen-announce ";
					str += "address("+s+") ";
					str += "port("+port+") ";
					str += "my-uuid("+_uuid+")";
					
					conn.sayInChannel(str);
				} catch (IOException e) {
					synchronized(_listeningOn) {
						_listeningOn.removeAll(listListSockAddrs);
					}
					result = false;
					IRCSocketLogger.logError(this, "Unable to write listen announce to channel.");
				}
			}
			
			
			
		} else {
			result = false;
			// TODO: MEDIUM - Create second logger, that logs messages and listens
			// TODO: LOWER - When creating the second log structure, add this into there
		}
		
		conn.removeMsgListener(lrl);
		
		return result;
	}
	
	public IRCSocketConnection createConnection(String address, int port) throws IOException {
		IRCConnection ic;
		
		ic = getCommandConnection();
		
		try {			
		
			IRCSocketConnection isc = ic.establishConnection(address, port);
			if(isc == null) {
				return null;
			}
			
			IRCInputStream is = new IRCInputStream(this, isc);
			isc.setInputStream(is);
			
			IRCOutputStream os = new IRCOutputStream(this, isc);
			isc.setOutputStream(os);

			synchronized(_activeConnections) {
				_activeConnections.add(isc);
			}
			return isc;
			
		} catch (IOException e) {
			IRCSocketLogger.logError(this, "Unable to send data on creating connection");
			throw new IOException("Unable to send data on creating connection");
		}
		
	}
	
	public void handleConnectFromClient(String srcUUID, 
			String address, int port, IRCConnection ircConnection, 
				int context, String destination) {
		
		boolean matchFound = false;
		ListenedSocketAddress sa = null;
		
		// The only condition in which we reject the connection is if we are not listening on that port
		synchronized(_listeningOn) {
			 	for(ListenedSocketAddress a : _listeningOn) {
			 		
			 		if(a.getSocketAddress().getPort() == port) {

				 		if(address.equalsIgnoreCase(a.getSocketAddress().getAddress()) || (
				 				a.getSocketAddress().getAddress().startsWith(address+".") && !SoAnUtil.isAddressAnIP(address))) {

				 			sa = a;
				 			matchFound = true;
				 			break;
				 		}
			 			
			 		}
			 	}
		}
		
		if(!matchFound) {
			IRCSocketLogger.logError("Listening address match not found ");
			return;
		}
		
		IRCSocketConnection sc = new IRCSocketConnection(this);
		sc.setAreWeConnOriginator(false);
		sc.setListeningAddress(sa.getSocketAddress());
		sc.setRemoteUUID(srcUUID);
		sc.setOutputStream(new IRCOutputStream(this, sc));
		sc.setInputStream(new IRCInputStream(this, sc));

		IRCServerSocketQueueEntry q = new IRCServerSocketQueueEntry();
		q.context = context;
		q.destination = destination;
		q.ic = sc;
		q.ircConn = ircConnection;

		sa.getReceiver().addToSockAcceptQueue(q);
	}
	
	public void acceptSocket(IRCSocketConnection c, IRCConnection ircConn, String destination, int context) {
		
		synchronized(_activeConnections) {
			_activeConnections.add(c);
		}
		
		synchronized(_connectionLock) {
			c.setOurConnectionID(_nextConnectionId);
			_nextConnectionId++;
		}
		
		String respCmd = "!connect-response ";
		respCmd += "conn-id("+(c != null ? c.getOurConnectionID() : "-1")+") ";
		respCmd += "result("+(c != null ? "success" : "fail")+") ";
		respCmd += "my-uuid("+getUUID()+") ";
		respCmd += "src-uuid("+c.getRemoteUUID()+") ";
		respCmd += "ctxt("+context+")";
		
		try {
			ircConn.sayInChannel(respCmd);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public long getNextContext() {
		synchronized (_contextLock) {
			long result = _nextContextId;
			_nextContextId++;
			return result;
		}
	}
		
	public void close() {

		synchronized(_connectionLock) {
			if(_closed) {
				return;
			} else {
				_closed = true;
			}

		}
		
		synchronized(_ircConnections) {
			
			for(Iterator<IRCConnection> it = _ircConnections.iterator(); it.hasNext(); ) {
				IRCConnection ic = it.next();
				ic.disconnect();
				
			}
		}				
		
	}

//	protected void writeData(IRCSocketConnection sockConn, long seqNum, byte[] data) throws IOException {
//		
//		IRCConnection c = getCommandConnection();
//		
//		try {
//			
//			c.writeData(sockConn, seqNum, data);
//			
//		} catch (IOException e) {
//
//			if(sockConn != null) {
//				IRCSocketLogger.logWarning("Unable to write data to connection ("+sockConn.toString()+") data-bytes:("+data.length+")");
//				throw(e);
//			}
//		}		
//	}
	
	/** Called by DataConnectionListener whenever data is received for one of this node's sockets */
	public void dataReceived(boolean isSenderID, int connId, long seqNum, String textData) {

		IRCSocketConnection match = null;
		synchronized(_activeConnections) {
			
			for(IRCSocketConnection ic : _activeConnections) {

				// If we are in dataReceived, then we are being sent data, and therefore we 
				// are necessarily the 'receiver' in this case
				
				if(isSenderID) {
					// As per above, we are not the sender, so it is a remote data conn id
					if(ic.areWeConnOriginator() && ic.getRemoteConnectionID() == connId) {
						match = ic;
					}
				} else {
					// As per above, we are the receiver, so it is our data connection
					
					if(!ic.areWeConnOriginator() && ic.getOurConnectionID() == connId) {
						match = ic;
					}
				}

			}
		}
		
		if(match == null) {
			IRCSocketLogger.logError(this, "No data connection match (conn-id:"+connId+" isSender:"+isSenderID+")");
			return;
		}
		
		byte data[] = SoAnUtil.decodeBase64(textData);
		
		_packetManagerInst.dataReceived(match, seqNum, data);
		
		
	}
	
	/** Called (only) by handleCloseSocket of SingleRespCommandListener */
	public void handleRemoteCloseSocket(boolean isSenderId, int connId, String remoteUUID) {
		
		// Because we are receiving the !close-socket command, we are the receiver.
		
		IRCSocketConnection result = null;
		synchronized(_activeConnections) {
			for(IRCSocketConnection s : _activeConnections) {
				if(isSenderId) {
					// It was their ID originally, not ours.					
					if(s.areWeConnOriginator() && s.getRemoteConnectionID() == connId) {
						
						result = s;
						break;
					} 
				} else {
					// It was our ID, originally, not theres.
					
					if(!s.areWeConnOriginator() && s.getOurConnectionID() == connId) {
						
						result = s;
						break;
											
					}
				}
			}
		}
		
		if(result != null) {
			try {
				result.informClose();
			} catch (IOException e) { 
				/* Inform close doesn't write to IRC  connection so it won't throw this (so bury it)*/
			}
		} else {
			IRCSocketLogger.logError(this, "close socket received, but the data connection was not found");
		}
		
		
	}
	
	/** Called by IRCSocketConnection only */
	protected void closeSocketConnection(IRCSocketConnection c, boolean outputCloseToChannel) throws IOException {
		
		// Remove the connection from active connections
		synchronized(_activeConnections) {
			for(Iterator<IRCSocketConnection> it = _activeConnections.iterator(); it.hasNext();) {
				IRCSocketConnection s = it.next();
				if(c.equals(s)) {
					it.remove();
					break;
				}
			}
		}

		// This method is called in the case where we call close() on a socket,
		// AND when the socket is closed by a remote host. We only want to
		// output when WE were the ones closing it, hence outputCloseToChannel.
		if(outputCloseToChannel) {
			// Output to the channel that we have closed the connection
			String cmd = "!close-socket ";
			
			if(c.areWeConnOriginator()) {
				// If we originated the connection, we were the client and they were
				// the server. So it is their connection id.
				//
				// This is our way of saying, we are closing the connection, check
				// the connections that you accept()ed for id x that came from you originally. 
				cmd += "recv-conn-id("+c.getRemoteConnectionID()+") ";	
			} else {
				// If they originated the connection, we were the ServerSocket and they
				// were the client. So this is our connection id.
				//
				// This is our way of say, we are closing the connection, even though
				// you originally opened it. So check your connections for the id that
				// we gave you, and close that.
				cmd += "sender-conn-id("+c.getOurConnectionID()+") ";
			}
			
			cmd += "my-uuid("+getUUID()+") ";
			cmd += "target-uuid("+c.getRemoteUUID()+") ";
			cmd += "ctxt("+getNextContext()+")";
			
			IRCConnection ic = getCommandConnection();
			ic.sayInChannel(cmd);
		}
	}
	
	protected void stopListeningOnPort(int port) throws IOException {
		ArrayList<String> cmds = new ArrayList<String>();
		synchronized(_listeningOn) {
			for(Iterator<ListenedSocketAddress> it = _listeningOn.iterator(); it.hasNext(); ) {
				ListenedSocketAddress a = it.next();
				
				if(a.getSocketAddress().getPort() == port) {
					String cmd = "!stop-listening ";
					cmd += "address("+a.getSocketAddress().getAddress()+") ";
					cmd += "port("+port+") ";
					cmd += "my-uuid("+_uuid+")";
					cmds.add(cmd);
					it.remove();
				}
			}
		}
		
		IRCConnection ic = getCommandConnection();
		
		for(String s : cmds) {
			ic.sayInChannel(s);
		}
	}
	
	public IRCOutputStreamDataManager getOutputDataManager() {
		return _outputDataManager;
	}
	
	/** Get the list of  IRC servers to connect to. */
	public List<String> getIRCHostConnectList() {
		return _ircHostsToConnect;
	}
	
	/** Set the list of IRC hosts to connect to once. This is only used
	 * when  connectToServers() is first called */
	public void setIRCHostConnectList(List<String> _ircHostsToConnect) {
		this._ircHostsToConnect = _ircHostsToConnect;
	}
	
	/** Add an IRC host to the list of hosts to connect to once. This is only used
	 * when connectToServers() is first called */	
	public void addIRCHostToConnectList(String s) {
		_ircHostsToConnect.add(s);
	}
	
	private void startGenerateShortenedUUIDThread() {
		GenerateShortenedUUIDThread t = new GenerateShortenedUUIDThread(this);
		t.start();
	}
	
	public String getShortenedUUID() {
		return _shortenedUUID;
	}
	
	public void setShortenedUUID(String shortenedUUID) {
		_shortenedUUID = shortenedUUID;
	}
	
//	protected CompletableWait getShortenedUUIDWait() {
//		return _shortenedUUIDWait;
//	
//	}
	
	/** Check if we have a shortened UUID, if not, start the thread that
	 * gets it from the remote host*/
	protected void checkShortenedUUID(String uuid) {
		
		// If we already have the UUID, then return 
		if(getRemoteShortenedUUID(uuid) != null) {
			return;
		}
		
		synchronized(_uuidShortenedThreads) {
			
			if(_uuidShortenedThreads.get(uuid) == null) {
				GetShortenedUUIDThread t = new GetShortenedUUIDThread(this, uuid);
				_uuidShortenedThreads.put(uuid, t);
				t.start();
			}
		}
		
	}
	
	/** If available, returns the shortened UUID for the given UUID */
	public String getRemoteShortenedUUID(String uuid) {
		synchronized(_uuidToShortenedUUIDMap) {
			return _uuidToShortenedUUIDMap.get(uuid);
		}
	}
	
	public void addShortenedUUID(String uuid, String shortenedUUID) {
		
		synchronized(_uuidToShortenedUUIDMap) {
			_uuidToShortenedUUIDMap.put(uuid, shortenedUUID);
		}
	}
}
