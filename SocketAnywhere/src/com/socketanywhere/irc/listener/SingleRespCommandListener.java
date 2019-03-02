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

package com.socketanywhere.irc.listener;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.socketanywhere.irc.IRCConnection;
import com.socketanywhere.irc.IRCSocketLogger;
import com.socketanywhere.irc.IRCUtil;
import com.socketanywhere.irc.ListenedSocketAddress;
import com.socketanywhere.irc.NodeImpl;
import com.socketanywhere.irc.IRCSocketAddress;
import com.socketanywhere.irc.UUIDAddressList;
import com.socketanywhere.net.SoAnUtil;

/** Listens on a variety of commands that require a response based on NodeImpl state. */
public class SingleRespCommandListener implements IMsgListener {
	NodeImpl _node;
	
	public SingleRespCommandListener(NodeImpl node) {
		_node = node;
	}

	// TODO: MEDIUM - Implement logging of the opening closing of sockets, sending and receiving messages, etc.
	
	public void handleGetShortenedUUID(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!get-shortened-uuid ") == -1) {
			return;
		}

		// !get-shortened-uuid uuid() my-uuid() ctxt()

		String myUUID = IRCUtil.extractField("my-uuid", message);
		String uuid = IRCUtil.extractField("uuid", message);
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		
		// Not for us, return
		if(!uuid.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		// Otherwise, this is a request for our shortened UUID, so respond
		
		String cmd = "!get-shortened-uuid-response ";
		
		String shortID = _node.getShortenedUUID();
		if(shortID == null) shortID = "n/a";
		
		cmd += "shortened-id("+shortID+") ";
		cmd += "my-uuid("+_node.getUUID()+") ";
		cmd += "src-uuid("+myUUID+") ";
		cmd += "ctxt("+ctxt+")";
		
		try {
			connection.sayInChannel(cmd);
		} catch (IOException e) {
			// Not much we can do
		}
		
	}
	
	@SuppressWarnings("unused")
	public void handleShortenedUUIDListen(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!shortened-uuid-listen ") == -1) {
			return;
		}
		
		// !shortened-uuid-listen uuid() shortened-id() my-uuid() ctxt()
		
		String shortenedID = IRCUtil.extractField("shortened-id", message);

		String myUUID = IRCUtil.extractField("my-uuid", message);
		String uuid = IRCUtil.extractField("uuid", message);
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		
		_node.addShortenedUUID(uuid, shortenedID);

	}
	
	public void handleShortenedUUIDAnnounce(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!shortened-uuid-announce ") == -1) {
			return;
		}
		
		if(_node.getShortenedUUID() == null) {
			return;
		}

		String shortenedID = IRCUtil.extractField("shortened-id", message);
		
		// The shortened-id must match our own Node's in order for us to generate a response
		if(!_node.getShortenedUUID().equalsIgnoreCase(shortenedID)) {
			return;
		}
		
		String myUUID = IRCUtil.extractField("my-uuid", message);
		String uuid = IRCUtil.extractField("uuid", message);
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		
		String cmd = "!shortened-uuid-announce-response conflict(true) ";
		cmd += "uuid("+uuid+") ";
		cmd += "src-uuid("+myUUID+") ";
		cmd += "my-uuid("+_node.getUUID()+") ";
		cmd += "shortened-id("+shortenedID+") ";
		cmd += "ctxt("+ctxt+")";
		
		try {
			connection.sayInChannel(cmd);
		} catch (IOException e) {
			// Not much we can do
		}
				
	}
	
	public void handleListenRequest(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!listen-request ") == -1) {
			return;
		}
		// !listen-request address() port() my-uuid()
		
		String addr = IRCUtil.extractField("address", message);
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		String myUUID = IRCUtil.extractField("my-uuid", message);

		ListenedSocketAddress match = null;
		List<ListenedSocketAddress> l = _node.getListeningOn();
		synchronized(l) {
			for(ListenedSocketAddress sa : l ) {
				IRCSocketAddress saddr = sa.getSocketAddress();
				if(saddr.getAddress().equalsIgnoreCase(addr) &&
						saddr.getPort() == port) {
					match = sa;
					break;
				}
			}
		}
		
		if(match == null) {
			return;
		}
		
		String cmd = "!listen-request-response ";
		cmd += "conflict(true) ";
		cmd += "address("+addr+") ";
		cmd += "port("+port+") ";
		cmd += "src-uuid("+myUUID+") ";
		cmd += "my-uuid("+_node.getUUID()+")";
		
		try {
			connection.sayInChannel(cmd);
		} catch (IOException e) {
			// Not much we can do
		}
				
	}

	

	// !close-socket (recv/sender)-conn-id() my-uuid() target-uuid() ctxt()	
	@SuppressWarnings("unused")
	public void handleCloseSocket(String nickname, String address, String destination, String message, IRCConnection connection) {
		
		// It must match our own node
		String targetUUID = IRCUtil.extractField("target-uuid", message);
		if(!targetUUID.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		String srcUUID = IRCUtil.extractField("my-uuid", message);
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		
		String recvIdField = IRCUtil.extractField("recv-conn-id", message);
		int recvConnId = -1;
		String senderIdField = IRCUtil.extractField("sender-conn-id", message);
		int senderConnId = -1;
		
		if(recvIdField != null) {
			recvConnId = Integer.parseInt(recvIdField);
			
			_node.handleRemoteCloseSocket(false, recvConnId, srcUUID);
			
		} else {
			senderConnId = Integer.parseInt(senderIdField);
			_node.handleRemoteCloseSocket(true, senderConnId, srcUUID);
		}
		
	}
	
	public void handleStopListening(String nickname, String address, String destination, String message, IRCConnection connection) {
		String addr = IRCUtil.extractField("address", message);
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		String srcUUID = IRCUtil.extractField("my-uuid", message);

		Map<IRCSocketAddress, String> m = _node.getAddrToUUIDMap();
		synchronized(m) {
			String uuid = m.remove(new IRCSocketAddress(addr, port));
			if(uuid != null && !uuid.equalsIgnoreCase(srcUUID)) {
				IRCSocketLogger.logWarning(this, "On stopListening, different UUID matched address and port");
			}
		}
		
		Map<String, UUIDAddressList> m2 = _node.getUUIDtoAddrListMap();
		synchronized(m2) {
			UUIDAddressList ua = m2.get(srcUUID);

			synchronized(ua) {
				if(ua != null) {
					List<IRCSocketAddress> sa = ua.getListeningAddresses();
					synchronized(sa) {
						for(Iterator<IRCSocketAddress> it = sa.iterator(); it.hasNext();) {
							IRCSocketAddress x = it.next();
							if(x.getPort() == port && x.getAddress().equalsIgnoreCase(addr)) {
								it.remove();
							}
						}
					}
				}
			}
		}
		
	}
	
	public void handleQueryAddress(String nickname, String address, String destination, String message, IRCConnection connection) {
		// !query-address address() port() my-uuid() ctxt()

		String addr = IRCUtil.extractField("address", message);
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		String srcUUID = IRCUtil.extractField("my-uuid", message);
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		
		synchronized(_node.getListeningOn()) {
			IRCSocketAddress result = null;	
			
			for(ListenedSocketAddress a : _node.getListeningOn()) {
				if(a.getSocketAddress().getPort() == port) {
					
					if(addr.equalsIgnoreCase(a.getSocketAddress().getAddress())) {
						result = a.getSocketAddress();
					} else {
						
						if(a.getSocketAddress().getAddress().startsWith(addr+".") && !SoAnUtil.isAddressAnIP(addr)) {
							result = a.getSocketAddress();
						}
					}
					
				}
			}
			
			if(result != null) {
				String response = "!query-address-response ";
				response += "uuid("+_node.getUUID()+") ";
				response += "my-uuid("+_node.getUUID()+") ";
				response += "src-uuid("+srcUUID+") ";
				response += "ctxt("+ctxt+")";

				try {
					connection.sayInChannel(response);
				} catch (IOException e) {
				}
			}
		}
		

	}
	
	public void handleListenAnnounce(String nickname, String address, String destination, String message, IRCConnection connection) {
		
		String addr = IRCUtil.extractField("address", message);
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		String srcUUID = IRCUtil.extractField("my-uuid", message);
		
		IRCSocketAddress s = new IRCSocketAddress(addr, port);
		
		_node.getAddrToUUIDMap().put(s, srcUUID);

		UUIDAddressList al = _node.getUUIDtoAddrListMap().get(srcUUID);
		if(al == null) {
			addUUIDToNodeImpl(srcUUID);
			al = _node.getUUIDtoAddrListMap().get(srcUUID);
		}
		
		synchronized(al) {
			al.getListeningAddresses().add(s);
		}
		
	}
	
	private void addUUIDToNodeImpl(String uuidStr) {

		synchronized(_node.getUUIDtoAddrListMap()) {
			
			if(!_node.getUUIDtoAddrListMap().containsKey(uuidStr)) {
				_node.getUUIDtoAddrListMap().put(uuidStr, new UUIDAddressList());
			}
		}
	}
	/**
	 * !register-uuid uuid()
	 */
	public void handleRegisterUUIDString(String nickname, String address, String destination, String message, IRCConnection connection) {
		String uuidStr = IRCUtil.extractField("uuid", message).toLowerCase();

		addUUIDToNodeImpl(uuidStr);
	}
	
	/**
	 * !query-nick name()  src-uuid() ctxt()
	 * !query-nick-response uuid() src-uuid() my-uuid() ctxt()
	 * @throws IOException 
	 */
	public void handleQueryNick(String nickname, String address, String destination, String message, IRCConnection connection) throws IOException {
		String name = IRCUtil.extractField("name", message).toLowerCase();
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		String srcUUID = IRCUtil.extractField("src-uuid", message);

		// Look through all our IRC connections looking to see if this is directed at us
		for(IRCConnection c : _node.getConnections()) {
			if(name.equalsIgnoreCase(c.getNicknamePrefix())) {
				String resp = "!query-nick-response ";
				
				resp += "uuid("+_node.getUUID()+") ";
				resp += "src-uuid("+srcUUID+") ";
				resp += "my-uuid("+_node.getUUID()+") ";
				resp += "ctxt("+ctxt+")";
								
				connection.sayInChannel(resp);
				
				break;
			}
		}
	}

	/**
	 * !unregister-uuid uuid() 
	 */
	public void handleUnregisterUUIDString(String nickname, String address, String destination, String message, IRCConnection connection) {
		String uuidStr = IRCUtil.extractField("uuid", message).toLowerCase();
		UUID u = UUID.fromString(uuidStr);
		
		if(_node.getUUIDtoAddrListMap().containsKey(u)) {
			_node.getUUIDtoAddrListMap().remove(u);
		}
	}
	
	public void handleQueryListening(String nickname, String address, String destination, String message, IRCConnection connection) throws IOException {
		String uuid = IRCUtil.extractField("uuid", message).toLowerCase();
		long ctxt = Long.parseLong(IRCUtil.extractField("ctxt", message));
		String srcUUID = IRCUtil.extractField("src-uuid", message);
		
		if(!uuid.equalsIgnoreCase(_node.getUUID())) {
			// Not for us
			return;
		}
		
		synchronized(_node.getListeningOn()) {
		
			for(ListenedSocketAddress sa : _node.getListeningOn()) {
				String result = new String();
				result += "!query-listening-response ";
				result += "address("+sa.getSocketAddress().getAddress()+") ";
				result += "port("+sa.getSocketAddress().getPort()+") ";
				result += "src-uuid("+srcUUID+") ";
				result += "my-uuid("+_node.getUUID()+") ";
				result += "ctxt("+ctxt+")";
				connection.sayInChannel(result);

			}
		}
		
		String result = new String();
		result += "!query-listening-end-response ";
		result += "src-uuid("+srcUUID+") ";
		result += "my-uuid("+_node.getUUID()+") ";
		result += "ctxt("+ctxt+")\n";
		connection.sayInChannel(result);		
		
	}
	
	@Override
	public void userMsg(String nickname, String address, String destination, String message, IRCConnection connection) {

		try {
			String[] msgs = message.split(" ");
			
			String cmd = msgs[0];
						
			if(cmd.equalsIgnoreCase("!query-nick")) {			
				handleQueryNick(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!register-uuid")) {
				handleRegisterUUIDString(nickname, address, destination, message, connection);
			}

			if(cmd.equalsIgnoreCase("!unregister-uuid")) {
				handleUnregisterUUIDString(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!query-listening")) {
				handleQueryListening(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!listen-announce")) {
				handleListenAnnounce(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!query-address")) {
				handleQueryAddress(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!stop-listening")) {
				handleStopListening(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!close-socket")) {
				handleCloseSocket(nickname, address, destination, message, connection);				
			}
			
			if(cmd.equalsIgnoreCase("!listen-request")) {
				handleListenRequest(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!shortened-uuid-announce")) {
				handleShortenedUUIDAnnounce(nickname, address, destination, message, connection);
			}

			if(cmd.equalsIgnoreCase("!shortened-uuid-listen")) {
				handleShortenedUUIDListen(nickname, address, destination, message, connection);
			}
			
			if(cmd.equalsIgnoreCase("!get-shortened-uuid")) {
				handleGetShortenedUUID(nickname, address, destination, message, connection);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

}
