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
import java.util.List;

import com.socketanywhere.irc.IRCConnection;
import com.socketanywhere.irc.IRCUtil;
import com.socketanywhere.irc.NodeImpl;
import com.socketanywhere.net.CompletableWait;

/** Does two things:
 * - Listens for listen requests and responds to them if they match an address we are already listening on 
 *   (thus shutting down their listen request)
 * 
 * - Listens for listen request responses, to detect if anyone else has replied to our listen request
 *   (if they have, it usually means they are shutting us down, as they are already listening on that address)  
 * */
public class ListenRequestListener implements IMsgListener {
	
	NodeImpl _node;
	List<String> _addresses;
	int _port;
	CompletableWait _waiter;
	boolean _listenSuccess = true;
	IRCConnection _conn;
	
	public ListenRequestListener(IRCConnection conn, NodeImpl node, List<String> addresses, int port) {
		_conn = conn;
		_node = node;
		_addresses = addresses;
		_port = port;
		_waiter = new CompletableWait(ListenRequestListener.class.getName());
	}

	@Override
	public void userMsg(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!listen-request-response ") != -1) { 
			handleListenRequestResponse(nickname, address, destination, message, connection);
		}

		if(message.indexOf("!listen-request ") != -1) { 
			handleListenRequest(nickname, address, destination, message, connection);
		}

	}

	/** If we are listening on the given address, send back a conflict response to shut down the remote request.  */
	public void handleListenRequest(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!listen-request ") == -1) {
			return;
		}
		// !listen-request address() port() my-uuid()
		
		String addr = IRCUtil.extractField("address", message);
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		String myUUID = IRCUtil.extractField("my-uuid", message);
		
		// Address must match our addresses for us to care
		if(!_addresses.contains(addr)) return;
		
		// Port must match for us to care
		if(_port != port) return;
		
		// If our UUID is smaller, we take precedence
		if(_node.getUUID().compareTo(myUUID) < 0) {
			String cmd = "!listen-request-response ";
			cmd += "conflict(true) ";
			cmd += "address("+addr+") ";
			cmd += "port("+_port+") ";
			cmd += "src-uuid("+myUUID+") ";
			cmd += "my-uuid("+_node.getUUID()+")";
			
			try {
				_conn.sayInChannel(cmd);
			} catch (IOException e) {
				// Nothing we can do if this is thrown
			}
			
		} else {
			_listenSuccess = false;
			_waiter.informComplete();
		}
		
	}
	
	public void handleListenRequestResponse(String nickname, String address, String destination,
			String message, IRCConnection connection) {
		
		// Ignore other cmds
		if(message.indexOf("!listen-request-response ") == -1) { 
			return;
		}
		
		String conflict = IRCUtil.extractField("conflict", message);
		if(conflict.equalsIgnoreCase("false")) return;
		
		
		String srcUUID = IRCUtil.extractField("srd-uuid", message);
		
		// Ignore the message if it is not for us
		if(!srcUUID.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		// The address specified must match our listener's
		String addr = IRCUtil.extractField("address", message);
		if(!_addresses.contains(addr)) {
			return;
		}
		
		// Port must match
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		if(port != _port) {
			return;
		}
		
//		String myUUID = IRCUtil.extractField("my-uuid", message);
		
		_listenSuccess = false;
		_waiter.informComplete();
	}
	
	public CompletableWait getResponseWait() {
		return _waiter;
	}

	public boolean isListenSuccess() {
		return _listenSuccess;
	}
}
