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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.socketanywhere.nonbreakingnew.NBLog;

/** Log of all the actions that occur during a test; these actions are then used to verify correctness 
 * of the test. Each of the actions is serializable/deserializable, and very simple checksum code is contained
 * here to verify data integrity. */

public class TestLog {

	public static final boolean DEBUG = MultiSendReceiveMain.DEBUG;
	
	public static final int UNKNOWN_REMOTE_ID = -2;
	
	/** Used to retrieve agent list on ser/deser*/
	private TestBrain _brain;
	
	final Map<Integer /* our agent id*/, Map<Integer /* remote agent id*/, TestLogEntries> /* logs for this id*/> _entries 
		= new HashMap<Integer, Map<Integer, TestLogEntries>>();
	
	final List<TestLogEntry> _generalErrors = new ArrayList<TestLogEntry>();
	
	/** Local director id represents the ID of the director from which the test log originated */
	int _localDirectorId = -1;
	
	
	public TestLog() {
	}
	
	public void setBrain(TestBrain _brain) {
		this._brain = _brain;
	}
	
	public void setLocalDirectorId(int directorId) {
		this._localDirectorId = directorId;
	}
	
	public int getLocalDirectorId() {
		return _localDirectorId;
	}
	
	
	public void unexpectedGeneralError(String message) {
		unexpectedGeneralError(message, null);
	}
	
	public void unexpectedGeneralError(String message, Exception e) {
		TLEError entry = new TLEError();
		entry._entryTime = System.currentTimeMillis();	
		entry._errorException = e;
		entry._errorMsg = message;
		synchronized(_generalErrors) {
			_generalErrors.add(entry);
		}
		if(DEBUG) {
			MultiSendReceiveMain.println(entry.serialize());
		}
	}

	/** The remote test director is sending us its results, so process them
	 * from the input stream. */
	public void deserializeTestLog(BufferedReader br) throws IOException {
		
		// Deserialize the entries
		String str = null;
		int remoteMyAgentId = -1; // 'my agent id' from remote perspective
		int remoteRemoteAgentId = -1; // 'remote agent id' from remote perspective
		
		while( (str = br.readLine()) != null ) {
			if(str.startsWith("result - general error - ")) {
				TLEError e = new TLEError();
				e.deserialize(str);
				_generalErrors.add(e);
			}

			if(str.startsWith("result - my-agent-id:")) {
				remoteMyAgentId = Integer.parseInt(TestLogEntry.extractField(str, "my-agent-id"));
			}
			
			if(str.startsWith("result - remote-agent-id:")) {
				remoteRemoteAgentId = Integer.parseInt(TestLogEntry.extractField(str, "remote-agent-id"));
//				for(TestAgent ta :_brain.getAgents()) {
//					if(ta.getAgentId() == remoteMyAgentId ) {
//						getTLE(remoteMyAgentId, remoteRemoteAgentId).setAgentDebugStr(ta.getDebugStr());
//						break;
//					}
//				}
			}
			
			if(str.startsWith("result - my-agent-debug:")) {
				String remoteMyAgentDebug = TestLogEntry.extractField(str, "my-agent-debug");
				getTLE(remoteMyAgentId, remoteRemoteAgentId).setAgentDebugStr(remoteMyAgentDebug);
//				System.out.println("Setting debug: "+remoteMyAgentId+" "+remoteRemoteAgentId+" "+remoteMyAgentDebug);
			}

			if(str.startsWith("result - complete")) {
				break;
			}
			
			// MultiSendReceiveMain.println("["+str+"] "+remoteMyAgentId + " / "+remoteRemoteAgentId);
			
			if(str.startsWith("result entry - ")) {
				if(str.contains("TLEDataSent")) {
					TLEDataSent e = new TLEDataSent();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}
				
				if(str.contains("TLEDataReceived")) {
					TLEDataReceived e = new TLEDataReceived();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}
				
				if(str.contains("TLEError")) {
					TLEError e = new TLEError();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}
				
				if(str.contains("TLELocalInitiatedConnection")) {
					TLELocalInitiatedConnection e = new TLELocalInitiatedConnection();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}
				
				if(str.contains("TLERemoteInitiatedConnection")) {
					TLERemoteInitiatedConnection e = new TLERemoteInitiatedConnection();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}
				
				if(str.contains("TLELocalInitiatedDisconnect")) {
					TLELocalInitiatedDisconnect e = new TLELocalInitiatedDisconnect();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}
				
				if(str.contains("TLERemoteInitiatedDisconnect")) {
					TLERemoteInitiatedDisconnect e = new TLERemoteInitiatedDisconnect();
					e.deserialize(str);
					getTLE(remoteMyAgentId, remoteRemoteAgentId).addTestLogEntry(e);
				}

			}
		}
		
		// Deserialize the log
	}
	
	public void serializeTestLog(TestDirector dir) throws IOException {
		
		dir.writeStringToDirectorSocket("result start\n");
		dir.writeStringToDirectorSocket("result - general errors:\n");
		
//		System.err.println("[stl] Serializing: ");
//		
		List<TestAgent> allAgents = new ArrayList<TestAgent>();
		allAgents.addAll(_brain.getAgents());
		allAgents.addAll(_brain.getRemovedAgents());
		
		// For each of the agents, see if they have a global id and store it in the map
		HashMap<Integer /* agent id*/, String /* debug info */> debugIdMap = new HashMap<Integer, String>();
		for(TestAgent ta : allAgents) {
			
//			TestAgent.getAgentDebugStr(ta.getSocket());
			
			debugIdMap.put(ta.getAgentId(), TestAgent.getAgentDebugStr(ta.getSocket()) );
			
//			if(ta.getSocket() != null && ta.getSocket() instanceof NBSocket) {
//				NBSocket nbs = (NBSocket)ta.getSocket();
//				
////				System.err.println("[stl] - "+ta.getAgentId()+" -> "+ta.getRemoteAgentId()+" "+nbs.getDebugTriplet());
////				
////				ITaggedSocketTL tag = (ITaggedSocketTL)ta.getSocket();
////				Long id = (Long)tag.getTagMap().get("id");
//				if(nbs.getDebugTriplet() != null) {
//					debugIdMap.put(ta.getAgentId(), nbs.getDebugTriplet().toString());
//				} 
////				else {
////					System.err.println("[stl] remove me222: " +nbs.getDebugTriplet() +" "+ta.getAgentId());
////				}
//			} else {
////				System.err.println("[stl] null or not nbsocket: "+ta.getSocket());
//			}
		}
		
		synchronized(_generalErrors) {
			for(TestLogEntry te :_generalErrors) {
				String str = "result - general error - " +te.serialize()+"\n";
				dir.writeStringToDirectorSocket(str);
			}
		}
		
		synchronized(_entries) {
			Set<Entry<Integer,Map<Integer, TestLogEntries>>> entriesSet = _entries.entrySet();
			
			for(Entry<Integer,Map<Integer, TestLogEntries>> ourAgentEntry : entriesSet) {
				
				for(Entry<Integer, TestLogEntries> e2 : ourAgentEntry.getValue().entrySet()) {

					dir.writeStringToDirectorSocket("result - my-agent-id:["+ourAgentEntry.getKey()+"]\n");
					
//					System.err.println("result - my-agent-debug:["+debugIdMap.get(ourAgentEntry.getKey())+"]  "+ourAgentEntry.getKey()+" ");
					
					dir.writeStringToDirectorSocket("result - remote-agent-id:["+e2.getKey()+"]\n");
					
					dir.writeStringToDirectorSocket("result - my-agent-debug:["+debugIdMap.get(ourAgentEntry.getKey())+"]\n");
					
					
					e2.getValue().serialize(dir);
					
				}
				
			}
			
		}
		
		dir.writeStringToDirectorSocket("result - complete\n");
		
	}
	
	/** Return a copy of the test log entries for the given agent, in cases where the
	 * remote agent id isn't known (this would be because the connection process failed
	 * before the remote agent id value was received) */
	public TestLogEntries getUnknownTLE(int agentId) {
		return getTLE(agentId, UNKNOWN_REMOTE_ID);
	}
	
	/** Return a copy of the test log entries for the given local agent id/remote agent id combination.*/
	public TestLogEntries getTLE(int agentId, int remoteId) {

		synchronized(_entries) {
			Map<Integer /* remote agent id*/, TestLogEntries> map = _entries.get((Integer)agentId);
			if(map == null) {
				map = new HashMap<Integer /* remote agent id*/, TestLogEntries>(); 
				_entries.put(agentId, map);
			}
			
			TestLogEntries e = map.get(remoteId);
			if(e == null) {
				TestLogEntries tle = new TestLogEntries(this, agentId, remoteId);	
				map.put(remoteId, tle);
				e = tle;
			}
			
			return e;
		}
	}
	
	/** This should only be called by ResultComparator after test activity has completed. */
	protected Map<Integer , Map<Integer , TestLogEntries> > getEntries() {
		return _entries;
	}
	
}

/** Contains all of the recorded data sent and received between two given agents (stored as checksums), as well 
 * as any events/errors that occured.*/
class TestLogEntries {
	private final int _agentId;
	private final int _remoteId;
	private final List<TestLogEntry> _entries = new ArrayList<TestLogEntry>();

	private String _agentDebugStr;
//	private String _remoteDebugStr;
	
	final TestLog _parent;

	public TestLogEntries(TestLog parent, int agentId, int remoteId) {
		_parent = parent;
		_agentId = agentId;
		_remoteId = remoteId;
	}
	
	public int getAgentId() {
		return _agentId;
	}
	
	public int getRemoteId() {
		return _remoteId;
	}
	
	public TestLog getParent() {
		return _parent;
	}
	
	public void setAgentDebugStr(String agentDebugStr) {
		_agentDebugStr = agentDebugStr;
	}
//	public void setRemoteDebugStr(String remoteDebugStr) {
//		this._remoteDebugStr = remoteDebugStr;
//	}
	
	public String getAgentDebugStr() {
		return _agentDebugStr;
	}
	
//	public String getRemoteDebugStr() {
//		return _remoteDebugStr;
//	}
	
	
	void serialize(TestDirector dir) throws IOException {
		
		synchronized(_entries) {
			for(TestLogEntry e : _entries) {
				String s = "result entry - " +e.serialize()+"\n";
				dir.writeStringToDirectorSocket(s);
			}
		}
	}
	
	private long[] calculateChecksum(byte[] data, int off, int len) {
		long all = 0;
		long even = 0;
		long odd = 0;
		
		for(int x = off; x < off+len; x++) {
			all += data[x];
			if(x % 2 == 0) even += data[x];
			if(x % 2 == 1) odd += data[x];
		}
		
		long[] result = new long[3];
		result[0] = all;
		result[1] = even;
		result[2] = odd;
		
		return result;
	}
	
	
	private void populateChecksums(TLEData e, byte[] data, int off, int len) {
		long[] r = calculateChecksum(data, off, len);
		e._checksumAll = r[0];
		e._checksumEven = r[1];
		e._checksumOdd = r[2];
	}
	
	public void sentData(byte[] data, int off, int len) {
		TLEDataSent e = new TLEDataSent();
		e._entryTime = System.currentTimeMillis();
		e.setBytesSentRecv(len);
		
		populateChecksums(e, data, off, len);
		addTestLogEntry(e);
	}
	
	public void receiveData(byte[] data, int off, int len) {
		TLEDataReceived e = new TLEDataReceived();
		e._entryTime = System.currentTimeMillis();
		e.setBytesSentRecv(len);
		
		populateChecksums(e, data, off, len);
		addTestLogEntry(e);
	}
	
	public void localInitConnect() {
		TLELocalInitiatedConnection e = new TLELocalInitiatedConnection();
		e._entryTime = System.currentTimeMillis();
		addTestLogEntry(e);
	}

	public void remoteInitConnect() {
		TLERemoteInitiatedConnection e = new TLERemoteInitiatedConnection();
		e._entryTime = System.currentTimeMillis();
		addTestLogEntry(e);
	}

	public void localInitDisconnect() {
		TLELocalInitiatedDisconnect e = new TLELocalInitiatedDisconnect();
		e._entryTime = System.currentTimeMillis();
		addTestLogEntry(e);
	}
	
	public void remoteInitDisconnect() {
		TLERemoteInitiatedDisconnect e = new TLERemoteInitiatedDisconnect();
		e._entryTime = System.currentTimeMillis();
		addTestLogEntry(e);
	}
	

	
	public void unexpectedError(String message) {
		unexpectedError(message, null);
	}
	
	public void unexpectedError(String message, Throwable e) {
		TLEError entry = new TLEError();
		entry._entryTime = System.currentTimeMillis();	
		entry._errorException = e;
		entry._errorMsg = message;
		
		addTestLogEntry(entry);
	}

	void addTestLogEntry(TestLogEntry e) {
		synchronized(_entries) {
			_entries.add(e);
		}
		
		if(MultiSendReceiveMain.DEBUG) {
			// note that this does not discriminate between local and remote
			MultiSendReceiveMain.println("atle - agId:"+_agentId + ", remoteAgId:"+ _remoteId +" - "+ e.serialize());
		}
		
	}
	
	public List<TestLogEntry> getEntries() {
		return _entries;
	}
}

abstract class TestLogEntry {
	
	enum EntryType {SENT, RECEIVED, ERROR, LOCAL_INIT_CONN, REMOTE_INIT_CONN, LOCAL_INIT_DC, REMOTE_INIT_DC, GENERAL };

	abstract EntryType getType();

	long _entryTime;

	String serialize() {
		return "entryTime:["+_entryTime+"]";
	}

	
	public static String extractField(String str, String field) {
		String lookingFor = field +":[";
		int x = str.indexOf(lookingFor);
		if(x == -1) return null;
		int lastCharPos = str.indexOf("]", x);
		if(lastCharPos == -1) return null;
		
		return str.substring(x+lookingFor.length(), lastCharPos);
		
	}
	
	void deserialize(String s) {
		_entryTime = Long.parseLong(extractField(s, "entryTime"));
	}
}

abstract class SimpleTestLogEntry extends TestLogEntry {
}

abstract class TLEData extends TestLogEntry {
//	byte[] dataSent;
	long _checksumAll = -1;
	long _checksumEven = -1;
	long _checksumOdd = -1;
	
	long _bytesSentOrRecv = -1;

	public long getChecksumAll() {
		return _checksumAll;
	}

	public void setChecksumAll(long checksumAll) {
		this._checksumAll = checksumAll;
	}

	public long getChecksumEven() {
		return _checksumEven;
	}

	public void setChecksumEven(long checkSumEven) {
		this._checksumEven = checkSumEven;
	}

	public long getChecksumOdd() {
		return _checksumOdd;
	}

	public void setChecksumOdd(long checksumOdd) {
		this._checksumOdd = checksumOdd;
	}
	
	public long getBytesSentRecv() {
		return _bytesSentOrRecv;
	}
	
	public void setBytesSentRecv(long bytesSent) {
		this._bytesSentOrRecv = bytesSent;
	}
	
	String serialize() {
		String str = "checksumAll:["+_checksumAll+"]";
		str +=  " checksumEven:["+_checksumEven+"]";
		str += " checksumOdd:["+_checksumOdd+"]";
		str += " bytesSentOrRecv:["+_bytesSentOrRecv+"]";
		
		return str;
	}
	
	void deserialize(String s) {
		_checksumAll = Long.parseLong(extractField(s, "checksumAll"));
		_checksumEven = Long.parseLong(extractField(s, "checksumEven"));
		_checksumOdd = Long.parseLong(extractField(s, "checksumOdd"));
		_bytesSentOrRecv = Long.parseLong(extractField(s, "bytesSentOrRecv"));
	}
}


class TLEDataSent extends TLEData {
	@Override
	EntryType getType() {
		return EntryType.SENT;
	}
	
	String serialize() {
		return TLEDataSent.class.getName() + " " + super.serialize();
	}
	
	public boolean isEquivalent(TLEDataReceived r) {
		if(r.getChecksumAll() != getChecksumAll()) return false;
		if(r.getChecksumOdd() != getChecksumOdd()) return false;
		if(r.getChecksumEven() != getChecksumEven()) return false;
		
		return true;
		
	}
}


class TLEDataReceived extends TLEData {
	@Override
	EntryType getType() {
		return EntryType.RECEIVED;
	}

	String serialize() {
		return TLEDataReceived.class.getName() + " " + super.serialize();
	}

	public boolean isEquivalent(TLEDataSent r) {
		if(r.getChecksumAll() != getChecksumAll()) return false;
		if(r.getChecksumOdd() != getChecksumOdd()) return false;
		if(r.getChecksumEven() != getChecksumEven()) return false;
		
		return true;
		
	}

}

class TLEError extends TestLogEntry {
	// Error
	Throwable _errorException;
	String _errorMsg;
	
	String _errorExceptionString; // populated by deserializing the exception field
	
	@Override
	EntryType getType() {
		return EntryType.ERROR;
	}
	
	String serialize() {
		String result = TLEError.class.getName() + " ";
		
		if(_errorException != null) {
			String exc = convertString(_errorException);
			exc = sanitizeString(exc);
			exc = exc.replace("\n", "");
			exc = exc.replace("\r", "");
			
			NBLog.err("TLEError: exception: "+exc);
			
			result += "exception:["+exc+"] ";
		}
		
		if(_errorExceptionString != null) {
			result += "exception-string:["+_errorExceptionString+"] ";
		}
		
		if(_errorMsg != null) {
			result += "error:["+sanitizeString(_errorMsg)+"] ";
		}
		
		return result + " " + super.serialize();
	}
	
	private static String sanitizeString(String str) {
//		System.out.println("{"+str+"}");
		str = str.replace("[", "");
		str = str.replace("]", "");
		return str;
	}
	
	private static String convertString(Throwable e) {
		
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String stackTrace = sw.toString();
		return stackTrace;
	}

	void deserialize(String s) {
		_errorExceptionString = extractField(s, "exception");
		_errorMsg = extractField(s, "error");
	}

}

class TLELocalInitiatedConnection extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.LOCAL_INIT_CONN;
	}

	String serialize() {
		return TLELocalInitiatedConnection.class.getName() + " " + super.serialize();
	}

}

class TLERemoteInitiatedConnection extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.REMOTE_INIT_CONN;
	}

	String serialize() {
		return TLERemoteInitiatedConnection.class.getName() + " " + super.serialize();
	}

}

class TLELocalInitiatedDisconnect extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.LOCAL_INIT_DC;
	}
	
	String serialize() {
		return TLELocalInitiatedDisconnect.class.getName() + " " + super.serialize();
	}
	
}

class TLERemoteInitiatedDisconnect extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.REMOTE_INIT_DC;
	}
	
	String serialize() {
		return TLERemoteInitiatedDisconnect.class.getName() + " " + super.serialize();
	}
	
}

