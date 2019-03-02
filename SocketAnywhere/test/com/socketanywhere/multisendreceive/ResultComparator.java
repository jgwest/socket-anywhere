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

package com.socketanywhere.multisendreceive;

import java.io.PrintStream;
import java.util.List;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.socketanywhere.multisendreceive.TestLogEntry.EntryType;

/** Compare the contents of two test logs: the local agent test log and the remote agent test log. */
public class ResultComparator {
	public static final boolean DEBUG = false;
	
	/** The test director on which the comparison is being done */
	private final TestDirector _localTestDirector;
	private final boolean _checkDCs;
	
	
	// UNKNOWN TLE and general errors
	
	public ResultComparator(TestDirector testDirector, boolean checkDCs) {
		_checkDCs = checkDCs;
		_localTestDirector = testDirector;
	}

	/** Compare the contents of two test logs: the local agent test log and the remote agent test log.
	 * In this case these params DO actually correspond to the local (this test director) agent and remote agent (other test director).  */
	public boolean checkResults(TestLog localAgentLog, TestLog remoteAgentLog) {
		// TODO: MEDIUM - I don't think this properly looks through remoteAgent's log
		
		Map<Integer /* agent id*/, Map<Integer /* remote agent id */, TestLogEntries> > allLocalAgentLogEntries = localAgentLog.getEntries();
		
		Set<Entry<Integer /* agent id */ , Map<Integer /* remote agent id */, TestLogEntries> >> localSet = allLocalAgentLogEntries.entrySet();

		boolean totalResult = true;
		int numCompared = 0;
		int numPassed = 0;
		
		/** For each of the local agents log entries in our test log... */
		for(Entry<Integer /* agent id*/ , Map<Integer /* remote agent id*/, TestLogEntries>> ourAgentEntry : localSet ) {
			
			int currAgentId = ourAgentEntry.getKey();
			
			Set<Entry<Integer, TestLogEntries>> localRemoteAgentEntriesSet = ourAgentEntry.getValue().entrySet();
			
			/** For each of the remote agent entries under local in test log....*/
			for(Entry<Integer /* remote agent id*/, TestLogEntries> currEntry : localRemoteAgentEntriesSet) {
				int remoteAgentId = currEntry.getKey();
				
				if(DEBUG) {
					MultiSendReceiveMain.println("------------------------------------------------------------------------------------");
					MultiSendReceiveMain.println("Comparing local agent "+currAgentId + "["+localAgentLog.getLocalDirectorId()+"]  with remote agent "+remoteAgentId+" ["+remoteAgentLog.getLocalDirectorId()+"]");
				}
				
				// Get the test log entries from the local perspective
				TestLogEntries localEntries = currEntry.getValue();
				
				// Get the test log entries from the remote perspective
				TestLogEntries remoteEntries = remoteAgentLog.getTLE(remoteAgentId, currAgentId);
				
//				if(localEntries != null && remoteEntries != null) {
//					System.out.println("compare: " +localEntries.getAgentDebugStr()+" "+remoteEntries.getAgentDebugStr());
//				}
				
				boolean compareResult = compareEntries(localEntries, remoteEntries, false, DEBUG);
				
				if(compareResult) {
					if(DEBUG) {
						MultiSendReceiveMain.println("");
					}
					compareResult = compareEntries(remoteEntries, localEntries, true, DEBUG);
				}

				
				
				
//				if(DEBUG) {
					MultiSendReceiveMain.println("Comparison result: ("+compareResult+")");
//				}
				
				if(!compareResult) {
					totalResult = false;
				} else {
					numPassed++;
				}
				
				numCompared++;
			}
			
		}	
		
//		if(DEBUG) {
			MultiSendReceiveMain.println("\nComparison total - Passed: "+numPassed+"/"+numCompared);
//		}
		
		return totalResult;
	}
	
	
	/** If we have a send or receive entry before a close, we ignore any mismatches, because close may have prevented the sending/receiving of the data.
	 * However, this ignorance does not apply if there are additional entries of the same type (send, followed by another send), or if there is no close. */
	private static boolean isLastOfTypeBeforeClose(List<TestLogEntry> l, int x) {
		boolean passed = false;
		
		TestLogEntry curr = l.get(x);
		
		int closeIndex = -1;
		for(int y = x+1; y < l.size(); y++) {
			TestLogEntry yTLE = l.get(y);
			
			if(yTLE.getType() == curr.getType()) {
				passed = false;
				break;
			}
			
			if(yTLE.getType() == EntryType.LOCAL_INIT_DC || yTLE.getType() == EntryType.REMOTE_INIT_CONN) {
				closeIndex = y;
				passed = true;
				break;
			}
		}
		
		// Shouldn't be more than 5 entries between the mismatch and the close
		if(closeIndex - x > 5) {
			passed = false;
		}

		if(closeIndex == -1) {
			passed = false;
		}
		
		return passed;
	}
	
	/** Do a one sided comparison (local param -> remote param) of the given entries.
	 * isReversed affects the debug values printed.  
	 */
	@SuppressWarnings("unused")
	private boolean compareEntries(TestLogEntries localLog, TestLogEntries remoteLog, boolean isReversedUNUSED, boolean printResults) {
		
		int numberReceived = 0;
		int numberSent = 0;
		
		boolean passed = true;
		
		int validDCPair = 0;
		int DCEventsSeen = 0;
		
		int matchedReceived = 0;
		
		int matchedSent = 0;
		
		List<TestLogEntry> l = localLog.getEntries();
		for(int x = 0; x < l.size(); x++) {
			TestLogEntry currEntry = l.get(x);

			if(currEntry.getType() == EntryType.ERROR) {
				TLEError er = (TLEError)currEntry;
				passed = false;
				if(printResults) {
					MultiSendReceiveMain.println("* error in results, "+getAgentName(localLog)+", error:"+er.serialize());
				}
			}
			
			if(currEntry.getType() == EntryType.RECEIVED) {
				TLEDataReceived currReceived = (TLEDataReceived)currEntry;
				
				numberReceived++;
				TLEDataSent currSentRemote = (TLEDataSent)getNthEntryByType(remoteLog, EntryType.SENT, numberReceived);
				
				boolean matched = currSentRemote == null ? false : currReceived.isEquivalent(currSentRemote);
				
				if(!matched) {
					passed = isLastOfTypeBeforeClose(l, x);
					if(!passed) {
						 isLastOfTypeBeforeClose(l, x);
					}
				}
				
				matchedReceived += (matched ? 1 : 0);
				
				if(printResults) {
					MultiSendReceiveMain.println("* compared "+getAgentName(localLog)+" receive("+numberReceived+") with "+getAgentName(remoteLog) + " send - result:"+matched /*+ "[s:"+(s==null)+"]"*/);
					MultiSendReceiveMain.println("receive: "+currReceived.serialize() + " / send: " +(currSentRemote != null ? currSentRemote.serialize() : " null") + "\r\n");
				}
				
			}
			
			if(currEntry.getType() == EntryType.SENT) {
				numberSent++;

				TLEDataSent currSent = (TLEDataSent)currEntry;

				TLEDataReceived currReceivedRemote = (TLEDataReceived)getNthEntryByType(remoteLog, EntryType.RECEIVED, numberSent);
				
				boolean matched = currReceivedRemote == null ? false : currSent.isEquivalent(currReceivedRemote);
				
				if(!matched) {
					passed = isLastOfTypeBeforeClose(l, x);
					if(!passed) {
						 isLastOfTypeBeforeClose(l, x);
					}
				}
				
				matchedSent += (matched ? 1 : 0);
								
				if(printResults) {
					MultiSendReceiveMain.println("* compared "+getAgentName(localLog)+" send("+numberSent+"), with "+getAgentName(remoteLog) + " receive - result:"+matched);
					MultiSendReceiveMain.println("send: "+currSent.serialize() + " / receive: " +(currReceivedRemote != null ? currReceivedRemote.serialize() : " null") + "\r\n");
				}

			}
			
			if(currEntry.getType() == EntryType.LOCAL_INIT_CONN)  {
				TLELocalInitiatedConnection lic = (TLELocalInitiatedConnection)currEntry;

				TLERemoteInitiatedConnection ric = (TLERemoteInitiatedConnection)getNthEntryByType(remoteLog, EntryType.REMOTE_INIT_CONN, 1);
				boolean matched = ric != null;

				if(!matched) {
					passed = false;
				}
				
				if(printResults) {
					MultiSendReceiveMain.println("* local init connection, "+getAgentName(localLog)+", matched "+matched);
				}				

			}

			if(currEntry.getType() == EntryType.REMOTE_INIT_CONN)  {
				TLERemoteInitiatedConnection ric = (TLERemoteInitiatedConnection)currEntry;
				
				TLELocalInitiatedConnection lic = (TLELocalInitiatedConnection)getNthEntryByType(remoteLog, EntryType.LOCAL_INIT_CONN, 1);

				boolean matched = lic != null;
				
				if(!matched) {
					passed = false;
				}

				if(printResults) {
					MultiSendReceiveMain.println("* remote init connection, initiated by "+getAgentName(remoteLog)+", matched "+matched);
				}				

				
			}
			
			if(currEntry.getType() == EntryType.LOCAL_INIT_DC)  {
				
				DCEventsSeen++;
				
				TLELocalInitiatedDisconnect lid = (TLELocalInitiatedDisconnect)currEntry;

				TLERemoteInitiatedDisconnect rid = (TLERemoteInitiatedDisconnect)getNthEntryByType(remoteLog, EntryType.REMOTE_INIT_DC, 1);

				boolean matched = rid != null;

				if(matched) {
					validDCPair++;
				}
				if(printResults) {
					MultiSendReceiveMain.println("* local init disconnect, initiated by "+getAgentName(localLog)+", matched "+matched);
				}				

			}
			
			if(currEntry.getType() == EntryType.REMOTE_INIT_DC) {
				DCEventsSeen++;
				
				TLERemoteInitiatedDisconnect rid = (TLERemoteInitiatedDisconnect)currEntry;
				TLELocalInitiatedDisconnect lid = (TLELocalInitiatedDisconnect)getNthEntryByType(remoteLog, EntryType.LOCAL_INIT_DC, 1);
				
				boolean matched = lid != null;

				if(matched) {
					validDCPair++;
				}
				
				if(printResults) {
					MultiSendReceiveMain.println("* remote init disconnection, intiated by "+getAgentName(remoteLog)+", matched "+matched);
				}				

				
			}
			
		}
		
		int sentFactor = (int)(100d * matchedSent / numberSent);
		int recvFactor = (int)(100d * matchedReceived / numberReceived);
		
		PrintStream ps = System.out;
		if(matchedSent != numberSent || matchedReceived != numberReceived) {
			ps = System.err;
		}
		
		ps.println(localLog.getAgentId()+"("+localLog.getAgentDebugStr()+") "+remoteLog.getAgentId()+"("+remoteLog.getAgentDebugStr()
			+") - total matched sent: "+matchedSent+"/"+numberSent+"("+sentFactor+"%)   total matched received: "+matchedReceived+"/"+numberReceived+"("+recvFactor+"%)");
		
		if(_checkDCs) {
		
			// There were disconnect events, but we did not pair any events together; this is an error.
			if(DCEventsSeen > 0 && validDCPair == 0) {
				if(printResults) {
					MultiSendReceiveMain.println("* There were disconnect events, but we did not pair any events together; this is an error. "+getAgentName(localLog));
				}
				passed = false;
		
			}
		}

		if(!passed && !printResults) {
			compareEntries(localLog, remoteLog, isReversedUNUSED, true);
		}
		
		return passed;
		
	}
	
	private String getAgentName(TestLogEntries e) {
		return "{agent id: " +e.getAgentId() + " debug-str: "+e.getAgentDebugStr() +" [dirId:"+e._parent.getLocalDirectorId()+"]}";
	}
	
//	private String getAgentName(boolean isReversed, boolean isLocal, int id) {
//
//		boolean result = isLocal;
//		result = isReversed ? !result : result; 
//	
//		if(result) {
//			return "local agent "+id;
//		} else {
//			return "remote agent "+id;
//		}
//		
//	}
	
	/** Look for the nth entry of given type 't' in the entries list. */
	private TestLogEntry getNthEntryByType(TestLogEntries entries, EntryType t, int n) {
		List<TestLogEntry> l = entries.getEntries();
		
		int typeEncountered = 0;
		
		for(int x = 0; x < l.size(); x++) {
			TestLogEntry e = l.get(x);

			if(e.getType() == t) {
				typeEncountered++;
				if(typeEncountered == n) {
					return e;
				}
			}
		}
		
		return null;
	}
	
}
