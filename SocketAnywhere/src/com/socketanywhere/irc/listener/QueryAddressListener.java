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

public class QueryAddressListener implements IMsgListener {

	NodeImpl _node;
	long _context;
	String _responseUUID;
	
	CompletableWait _waiter = new CompletableWait(QueryAddressListener.class.getName());
	
	public QueryAddressListener(NodeImpl node, long context) {
		_node = node; 
		_context = context;
	}
	
	@Override
	public void userMsg(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		// !query-address-response uuid() my-uuid() src-uuid() ctxt()
		
		if(message == null || message.indexOf("!query-address-response") == -1) {
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
		
		String uuid  = IRCUtil.extractField("uuid", message);
		_responseUUID = uuid;
	
		_waiter.informComplete();
		
		
	}

//	@Override
//	public void userQuit(String nickname, String address, String quitMsg) {
//		
//	}
	
	public String getResponseUUID() {
		return _responseUUID;
	}
	
	public CompletableWait getResponseWait() {
		return _waiter;
	}
}
