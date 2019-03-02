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

import com.socketanywhere.irc.listener.ShortUUIDAnnounceRespListener;
import com.socketanywhere.net.SoAnUtil;

/** In order to shorten our UUID to something that takes less bytes, we announce our new UUID in order
 * to make sure no one else is using it.  */
public class GenerateShortenedUUIDThread extends Thread {
	
	NodeImpl _node;
	
	GenerateShortenedUUIDThread(NodeImpl node) {
		_node = node;
	}
	
	@Override
	public void run() {
		try {

			String resultShortenedID = null;
			
			IRCConnection ic = _node.getCommandConnection();
			
			do {
			
				String tempUUID = SoAnUtil.generateUUID().toString().replaceAll("-", "");
				String shortenedID = tempUUID.substring(0, 5);
	
				long context = _node.getNextContext();
				
				// Announce in the channel that we will now be using a shortened UUID
				
				String cmd = "!shortened-uuid-announce uuid("+_node.getUUID()+") ";
				cmd += "shortened-id("+shortenedID+") ";
				cmd += "my-uuid("+_node.getUUID()+") ";
				cmd += "ctxt("+context+")";
				
				ShortUUIDAnnounceRespListener ll = new ShortUUIDAnnounceRespListener(ic, _node, shortenedID, context);
				ic.addMsgListener(ll);
				
				ic.sayInChannel(cmd);
				
				ll.getResponseWait().waitForCompleteTimeout(10000);
				
				if(ll.isAnnounceSuccess()) {
					resultShortenedID = shortenedID;
				}
				ic.removeMsgListener(ll);

			} while(resultShortenedID == null);
						
			_node.setShortenedUUID(resultShortenedID);

			long newContext = _node.getNextContext();
			String cmd = "!shortened-uuid-listen ";
			cmd += "uuid("+_node.getUUID()+") ";
			cmd += "shortened-id("+resultShortenedID+") ";
			cmd += "my-uuid("+_node.getUUID()+") ";
			cmd += "ctxt("+newContext+")";
			
			ic.sayInChannel(cmd);
			
			
		} catch(IOException e) {
			// Not much we can do;
		}
		
	}
	

}
