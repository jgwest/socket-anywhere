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

import com.socketanywhere.irc.listener.IMsgListener;

// TODO: MEDIUM - Log the addition and removal of listeners

public class GetShortenedUUIDThread extends Thread implements IMsgListener {
	
	NodeImpl _node;
	String _targetUUID;
	long _currentContext = -1;

	Object _resultAvailableLock = new Object();
	boolean _resultAvailable = false;
	String _shortenedIDResult = null;
	
	public GetShortenedUUIDThread(NodeImpl node, String targetUUID) {
		_node = node;
		_targetUUID = targetUUID;
	}
	
	@Override
	public void run() {
		
		try {
			
			IRCConnection cmdConn = _node.getCommandConnection();

			long initialStartTime = System.currentTimeMillis();
			
			while(System.currentTimeMillis() - initialStartTime <= 60000 && _shortenedIDResult == null) {

				// If we already have a shortened UUID for this UUID, then end the thread
				if(_node.getRemoteShortenedUUID(_targetUUID) != null) {
					return;
				}
				
				long context = _node.getNextContext();
				
				String cmd = "!get-shortened-uuid ";
				cmd += "uuid("+_targetUUID+") ";
				cmd += "my-uuid("+_node.getUUID()+") ";
				cmd += "ctxt("+context+")";
		
				_currentContext = context;
				_resultAvailable = false;
				_shortenedIDResult = null;
				
				cmdConn.addMsgListener(this);
				
				cmdConn.sayInChannel(cmd);
			
				// Wait for 10 seconds...
				long startTime = System.currentTimeMillis();
				while(!_resultAvailable 
						&& System.currentTimeMillis() - startTime < (10 * 1000) 
						&& _node.getRemoteShortenedUUID(_targetUUID) == null) {
					
					// If we have a result, or 10 seconds has elapsed, or 
					// it has a UUID already (then stop listening)
					
					synchronized(_resultAvailableLock) {
						if(!_resultAvailable) {
							_resultAvailableLock.wait(1000);
						}
					}
				}
				cmdConn.removeMsgListener(this);
				
			}
			
			if(_shortenedIDResult != null) {
				_node.addShortenedUUID(_targetUUID, _shortenedIDResult);
			}
			
		} catch (IOException e) {
			IRCSocketLogger.logWarning(this, "IOException caught");
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void userMsg(String nickname, String address, String destination,
			String message, IRCConnection connection) {
		
		if(message.indexOf("!get-shortened-uuid-response ") == -1) {
			return;
		}
		
		String srcUUID = IRCUtil.extractField("src-uuid", message);
		
		if(!srcUUID.equalsIgnoreCase(_node.getUUID())) {
			// Ignore messages that are not for us
			return;
		}

		long context = Long.parseLong(IRCUtil.extractField("ctxt", message));
		if(context != _currentContext) {
			// Ignore mismatching contexts
			return;
		}
		
		// !get-shortened-uuid-response shortened-id() my-uuid() src-uuid() ctxt()

		String myUUID = IRCUtil.extractField("my-uuid", message);
		if(!myUUID.equalsIgnoreCase(_targetUUID)) {
			// Not the node that we were requesting a shortened UUID from
			return;
		}
		
		String shortenedID = IRCUtil.extractField("shortened-id", message);

		if(shortenedID.equalsIgnoreCase("n/a")) {
			shortenedID = null;
		}
		
		synchronized(_resultAvailableLock) {
			
			_shortenedIDResult = shortenedID;
			_resultAvailable = true;
			_resultAvailableLock.notifyAll();
		}
		
	}

	public String getShortenedIDResult() {
		return _shortenedIDResult;
	}
}
