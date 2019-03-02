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

import java.util.ArrayList;
import java.util.List;

import com.socketanywhere.irc.IRCConnection;
import com.socketanywhere.irc.IRCUtil;
import com.socketanywhere.irc.NodeImpl;
import com.socketanywhere.irc.IRCSocketAddress;
import com.socketanywhere.net.CompletableWait;

public class QueryListeningListener implements IMsgListener {
	long _context;
	String _requesteeUUID;
	NodeImpl _node;
	List<IRCSocketAddress> _response = new ArrayList<IRCSocketAddress>();
	
	CompletableWait _waiter = new CompletableWait(QueryListeningListener.class.getName());
	
	public QueryListeningListener(NodeImpl node, String requesteeUUID, long context) {
		_node = node;
		_context = context;
		_requesteeUUID = requesteeUUID;
	}

	@Override
	public void userMsg(String nickname, String address, String destination, String message, IRCConnection connection) {
		if(message == null || message.indexOf("!query-listening-response") == -1 ||
				message.indexOf("!query-listening-end-response") == -1) {
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
		if(!theirUUID.equalsIgnoreCase(_requesteeUUID)) {
			return;
		}
		

		if(message.indexOf("!query-listening-response") != -1) {
//			!query-listening-response address() port() src-uuid() my-uuid() ctxt()

			String addr = IRCUtil.extractField("address", message);
			int port = Integer.parseInt(IRCUtil.extractField("port", message));
			
			synchronized(_response) {
				_response.add(new IRCSocketAddress(addr, port));
			}			
			
		} else {
//			!query-listening-end-response src-uuid() my-uuid() ctxt()
			_waiter.informComplete();
		}
	}

//	@Override
//	public void userQuit(String nickname, String address, String quitMsg) {
//		_response = null;
//		_waiter.informComplete();
//	}

	public List<IRCSocketAddress> getResponse() {
		return _response;
	}
	
	public CompletableWait getResponseWait() {
		return _waiter;
	}
}
