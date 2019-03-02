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
import java.util.List;

import com.socketanywhere.net.ByteHolder;

/** 
 * Data written into an output stream is routed here; from here, the data is split into packets
 * and distributed among the various IRC server connections that the node is connected to.
 * 
 * Each node instance has a single IRCOutputStreamDataManager instance.*/
public class IRCOutputStreamDataManager {
	
	private static final int TIME_BETWEEN_WRITES = 2000;
	private static final int DATA_TO_SEND_PER_CONN = 150;
	
	NodeImpl _node;
	
	ByteHolder _dataContainer = new ByteHolder();
	long _nextSeqNum = 0;
	
	Object _dataManagerLock = new Object();
	
	IRCOutputStreamDataManager(NodeImpl node) {
		_node = node;
	}
	
	public long getNextSeqNum() {
		synchronized(_dataManagerLock) {
			return _nextSeqNum++;
		}
	}
	
	public void sendData(IRCSocketConnection conn, byte[] data) throws IOException {
		
		/** Check if our node already has the shortened UUID for the remote host */
		_node.checkShortenedUUID(conn.getRemoteUUID());
		
		synchronized(_dataManagerLock) {
			
			_dataContainer.addBytes(data);
			
			List<IRCConnection> l = _node.getConnections();

			while(_dataContainer.getContentsSize() > 0) {
			
				/** The time at which the next IRC connection will be available to write to */
				long lowestTimeToNextWrite = -1;
				
				for(IRCConnection ic : l) {
					
					long timeToNextWrite = 0;
					if(ic.getTimeOfLastDataSent() <= 0) {
						timeToNextWrite = 0;
					} else {
						timeToNextWrite = ic.getTimeOfLastDataSent() + TIME_BETWEEN_WRITES;
					}
					
					// If it's after when we should write for this ic, then use it
					if(timeToNextWrite < System.currentTimeMillis()) {
						
						int dataBytesToSend = Math.min(_dataContainer.getContentsSize(), DATA_TO_SEND_PER_CONN);
			
						byte[] dataToSend = _dataContainer.extractAndRemove(dataBytesToSend);
												
						ic.writeData(conn, getNextSeqNum(), dataToSend);
						
					} else {
						
						// If this connection is not ready, check if it is the next connection 
						// that will become ready
						if(lowestTimeToNextWrite == -1 || timeToNextWrite < lowestTimeToNextWrite) {
							lowestTimeToNextWrite = timeToNextWrite;
						}
						
					}

					// If we're all done, we can stop iterating through connections
					if(_dataContainer.getContentsSize() <= 0) break;
				}

				// Only sleep if there is data left
				if(_dataContainer.getContentsSize() > 0) {
				
					// Sleep until the next IRC connection is ready
					try {
						
						// Sanity check out lowestTimeToNextWrite to prevent bad sleeps
						if(lowestTimeToNextWrite > (System.currentTimeMillis() + TIME_BETWEEN_WRITES)) {
							lowestTimeToNextWrite = (System.currentTimeMillis() + TIME_BETWEEN_WRITES);
							
							IRCSocketLogger.logWarning(this, "This condition should not occur."); 
						}
						
						Thread.sleep(Math.max(15, lowestTimeToNextWrite - System.currentTimeMillis() ));
					} catch (InterruptedException e) {
						return;
					}
				}

			}
		}
		
	}

}
