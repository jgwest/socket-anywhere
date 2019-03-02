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

/** Parses a received connect request, and hands it to the node */
public class ConnectionListener implements IMsgListener {
	
	NodeImpl _node;
	
	public ConnectionListener(NodeImpl node) {
		_node = node;
	}
	
	@Override
	public void userMsg(String nickname, String address, String destination, String message, IRCConnection connection) {

		String[] msgs = message.split(" ");
		
		String cmd = msgs[0];
					
		if(!cmd.equalsIgnoreCase("!connect")) {
			return;
		}
		
		String addr = IRCUtil.extractField("address", message);
		int port = Integer.parseInt(IRCUtil.extractField("port", message));
		String uuid = IRCUtil.extractField("uuid", message);
		String srcUUID = IRCUtil.extractField("my-uuid", message);
		int context = Integer.parseInt(IRCUtil.extractField("ctxt", message));
		
		if(!uuid.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		_node.handleConnectFromClient(srcUUID, addr, port, connection, 
				context, destination);
				
	}

}
