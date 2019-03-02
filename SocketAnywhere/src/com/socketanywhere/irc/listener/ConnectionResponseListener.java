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

import com.socketanywhere.irc.IRCConnection;
import com.socketanywhere.irc.IRCUtil;
import com.socketanywhere.irc.NodeImpl;
import com.socketanywhere.net.CompletableWait;

public class ConnectionResponseListener implements IMsgListener {
	
	long _context;
	NodeImpl _node;
	String _destUUID;
	CompletableWait _waiter;
	
	boolean _connectionSuccess = false;
	int _connectionID = -1;
	
	public ConnectionResponseListener(NodeImpl node, String destUUID, long context) {
		
		_node = node;
		_destUUID =  destUUID;
		_context = context;
		_waiter = new CompletableWait(ConnectionResponseListener.class.getName());
		
	}

	@Override
	public void userMsg(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		// !connect-response conn-id() result(success/fail) my-uuid() src-uuid() ctxt()
		
		if(message == null || message.indexOf("!connect-response") == -1) {
			return;
		}
		
		// Must be the original context we specified
		int context = Integer.parseInt(IRCUtil.extractField("ctxt", message));
		if(context != _context) {
			return;
		}
		
		// Must be us they are replying to
		String srcUUID = IRCUtil.extractField("src-uuid", message); 
		if(!srcUUID.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		// Must be the UUID we orginally sent the request to
		String theirUUID = IRCUtil.extractField("my-uuid", message);
		if(!theirUUID.equalsIgnoreCase(_destUUID)) {
			return;
		}

		boolean connSuccess = IRCUtil.extractField("result", message).equalsIgnoreCase("success");

		if(connSuccess) {
			int connId = Integer.parseInt(IRCUtil.extractField("conn-id", message));
			_connectionID = connId;
			_connectionSuccess = true;
		} else {
			_connectionID = -1;
			_connectionSuccess = false;			
		}
		
		_waiter.informComplete();

	}
	
	public int getConnectionID() {
		return _connectionID;
	}
	
	public boolean isConnectionSuccess() {
		return _connectionSuccess;
	}

	public CompletableWait getResponseWait() {
		return _waiter;
	}
}
