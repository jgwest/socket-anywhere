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

import com.socketanywhere.irc.IRCConnection;
import com.socketanywhere.irc.IRCUtil;
import com.socketanywhere.irc.NodeImpl;
import com.socketanywhere.net.CompletableWait;

/** Used by GenerateShortenedUUIDThread. Will listen to responses on announce of shortened UUID;
 * specifically, it will listen either for anyone else announcing on the same shortened UUID, 
 * or it will listen for someone explicitly flagging a conflict. */ 
public class ShortUUIDAnnounceRespListener implements IMsgListener {
	
	NodeImpl _node;
	CompletableWait _waiter;
	IRCConnection _conn;
	String _shortenedUUID;
	long _context = -1;
	
	boolean _announceSuccess = true;
	
	public ShortUUIDAnnounceRespListener(IRCConnection conn, NodeImpl node, String shortenedUUID, long context) {
		_conn = conn;
		_node = node;
		_waiter = new CompletableWait(ShortUUIDAnnounceRespListener.class.getName());
		_shortenedUUID = shortenedUUID;
		_context = context;
	}

	@Override
	public void userMsg(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!shortened-uuid-announce-response ") != -1) { 
			handleShortUUIDAnnounceResponse(nickname, address, destination, message, connection);
		}
		
		if(message.indexOf("!shortened-uuid-announce ") != -1) {
			handleShortenedUUIDAnnounce(nickname, address, destination, message, connection);
		}

	}

	private void handleShortenedUUIDAnnounce(String nickname, String address, String destination,
			String message, IRCConnection connection) {

		if(message.indexOf("!shortened-uuid-announce ") == -1) {
			return;
		}
		
		// !shortened-uuid-announce uuid() shortened-id(1a9s) my-uuid() ctxt()

		// We only care if the id they use matches our ID
		String shortenedId = IRCUtil.extractField("shortened-id", message);
		if(!shortenedId.equalsIgnoreCase(_shortenedUUID)) {
			return;
		}
		
		// If the UUIDs match ours (because we sent it) then return;
		String uuid = IRCUtil.extractField("uuid", message);
		String myUUID = IRCUtil.extractField("my-uuid", message);
		if(uuid.equalsIgnoreCase(_node.getUUID()) || myUUID.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
				
		int ctxt = Integer.parseInt(IRCUtil.extractField("ctxt", message));
		
		// If our UUID is smaller, we take precedence
		if(_node.getUUID().compareTo(myUUID) < 0) {
			String cmd = "!shortened-uuid-announce-response conflict(true)";
			cmd += "uuid("+uuid+") ";
			cmd += "src-uuid("+myUUID+") ";
			cmd += "my-uuid("+_node.getUUID()+") ";
			cmd += "shortened-id("+_shortenedUUID+") ";
			cmd += "ctxt("+ctxt+")";
			
			try {
				_conn.sayInChannel(cmd);
			} catch (IOException e) {
				// Nothing we can do if this is thrown
			}
			
		} else {
			// Otherwise, they take precedence over us, so stop listening and try again
			_announceSuccess = false;
			_waiter.informComplete();
		}
		
	}
	
	private void handleShortUUIDAnnounceResponse(String nickname, String address, String destination,
			String message, IRCConnection connection) {
		
		// uuid() conflict(true) src-uuid() my-uuid() shortened-id() ctxt()
		
		// Ignore other cmds
		if(message.indexOf("!shortened-uuid-announce-response ") == -1) { 
			return;
		}
		
		String conflict = IRCUtil.extractField("conflict", message);
		if(conflict.equalsIgnoreCase("false")) return;
				
		String srcUUID = IRCUtil.extractField("srd-uuid", message);
		
		// Ignore the message if it is not for us
		if(!srcUUID.equalsIgnoreCase(_node.getUUID())) {
			return;
		}
		
		// Ignore the message if the context doesn't match
		int ctxt = Integer.parseInt(IRCUtil.extractField("ctxt", message));
		if(ctxt != _context) {
			return;
		}

		// The shortened id they include should match our own (otherwise someone screwed up)
		String shortenedId = IRCUtil.extractField("shortened-id", message);
		if(!shortenedId.equalsIgnoreCase(_shortenedUUID)) {
			return;
		}		
		
		// All relevant fields match, so someone takes precedence over our id
		// (or already owns it)
		_announceSuccess = false;
		_waiter.informComplete();
	}
	
	public CompletableWait getResponseWait() {
		return _waiter;
	}

	public boolean isAnnounceSuccess() {
		return _announceSuccess;
	}
}
