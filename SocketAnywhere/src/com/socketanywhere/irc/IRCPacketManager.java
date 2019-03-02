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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Data is received out of order by the nodes; this class puts it all back together, and transmits
 * it to the InputStream once a complete and consecutive set of data is received.  */
public class IRCPacketManager {
	
	NodeImpl _node;
	Map<IRCSocketConnection, PacketManagerEntry> _connToDataMap = new HashMap<IRCSocketConnection, PacketManagerEntry>();
	
	public IRCPacketManager(NodeImpl node) {
		_node = node;
	}

	Object packetLock = new Object();
	
	private PacketManagerEntry getOrCreateEntry(IRCSocketConnection c) {
		
		synchronized(packetLock) {
			
			PacketManagerEntry pe = _connToDataMap.get(c);
			if(pe == null) {
				pe = new PacketManagerEntry();
				_connToDataMap.put(c, pe);
			}
			
			return pe;
			
		}
	}
	
	/** Nodes let us know when data is received: the seqnum for the data, and the data itself, and the 
	 * connection from which it came. */
	public void dataReceived(IRCSocketConnection conn, long seq, byte[] data) {
		
		synchronized(packetLock) {
			
			PacketManagerEntry pe = getOrCreateEntry(conn);
			
			boolean nextSeqUpdated = false;
			
			// If the data we received matches the next, then send it, else add it to the list
			
			if(pe._nextSeq == seq) {
				conn.getInputStream().informBytes(data);
				pe._nextSeq ++;
				nextSeqUpdated = true;
			} else {
				DataPair dp = new DataPair();
				dp._data = data;
				dp._seqNum = seq;
				pe._dataList.add(dp);
				nextSeqUpdated = false;
			}
			
			if(nextSeqUpdated) {
			
				// We have satisfied one (and inc-ed), then loop through the others to see if we can satisfy them
				boolean matchFound = false;			
				do {
					
					matchFound = false;
					for(Iterator<DataPair> it = pe._dataList.iterator(); it.hasNext();) {
						DataPair dp = it.next();
						
						if(pe._nextSeq == dp._seqNum) {
							it.remove();
							conn.getInputStream().informBytes(dp._data);
							pe._nextSeq++;
							matchFound = true;
							break;
						}					
					}
									
				} while(matchFound);

			}
			
		}
				
	}

}

class PacketManagerEntry {
	long _nextSeq = 0;
	
	List<DataPair> _dataList = new ArrayList<DataPair>();
}

class DataPair {
	long _seqNum;
	byte[] _data;
}