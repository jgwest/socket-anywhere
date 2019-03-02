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

public class DataConnectionListener implements IMsgListener {
	
	NodeImpl _node;
	
	public DataConnectionListener(NodeImpl node) {
		_node = node;
	}
	
	private static String extractEitherField(String field1, String field2, String message) {
		if(message.indexOf(" "+field1+"(") != -1) {
			return IRCUtil.extractField(field1, message);
		} else if(message.indexOf(" "+field2+"(") != -1) {
			return IRCUtil.extractField(field2, message);
			
		}

		return null;
	}
	
	@Override
	public void userMsg(String nickname, String address, String destination,
			String message, IRCConnection connection) {
		
		// !packet conn-id() my-uuid() target-uuid() ctxt() data()

		
		if(!message.startsWith("!packet") && !message.startsWith("!p")) {
			return;
		}

		String targetUUID = null;
		
		// If the shortened UUID was specified, use it
		if(message.indexOf("short-uuid") != -1 || message.indexOf(" shd(") != -1) {
			
			String shortUUID = extractEitherField("short-uuid", "shd", message);
			if(shortUUID.equalsIgnoreCase(_node.getShortenedUUID())) {
				targetUUID = _node.getUUID();
			}
			
		} else {
			// ... Otherwise, extract the proper target UUID
			targetUUID = extractEitherField("target-uuid", "tid", message);
		}
		
		if(!targetUUID.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		boolean isSenderID = false;
		int connId = -1;
		if(message.indexOf("recv-conn-id") != -1 || message.indexOf(" rid(") != -1) {
			connId = Integer.parseInt(extractEitherField("recv-conn-id", "rid", message));
			isSenderID = false;
		}
		
		
		if(message.indexOf("sender-conn-id") != -1 || message.indexOf(" sid(") != -1) {
			connId = Integer.parseInt(extractEitherField("sender-conn-id", "sid", message));
			isSenderID = true;
		}
		
//		String srcUUID = extractEitherField("my-uuid", "myid", message);
		
		long seq = Long.parseLong(extractEitherField("seq", "s", message));
		String data = extractEitherField("data", "d", message);
		
		_node.dataReceived(isSenderID, connId, seq, data);
		
		
	}

}
