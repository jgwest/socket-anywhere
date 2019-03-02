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

package com.socketanywhere.passthrough;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.socketanywhere.multisendreceive.MultiSendReceiveMain;

/** Contains all the test log entries (TestLogEntries) for a given socket. */
public class PTLogger {
	
	protected static final String DIRECTORY_TO_WRITE_TO = "c:\\temp\\pt\\";

	
	
	public static Object _lock = new Object();
	
	public static int nextEntryId = 0;
	
	public static Map<PTSocketImpl, TestLogEntries> _logMap = new HashMap<PTSocketImpl, TestLogEntries>();
	
	
	public static TestLogEntries getEntry(PTSocketImpl s) {
		TestLogEntries e;
		synchronized(_lock) {
			e = _logMap.get(s);
			if(e == null) {
				e = new TestLogEntries(nextEntryId++, 0, s); 
				_logMap.put(s, e);
			}
		}
		
		return e;
	}

}


/** Contains all of the recorded data sent and received between two given agents (stored as checksums), as well 
 * as any events/errors that occurred.*/
class TestLogEntries {
	int _agentId;
	int _remoteId;
	
	byte[] _bytesReceived = new byte[16];
	int _bytesReceivedNum = 0;
	byte[] _bytesSent = new byte[16];
	int _bytesSentNum = 0;
	boolean _remainingDataFlushed = false;

	List<TestLogEntry> _entries = new ArrayList<TestLogEntry>();

	PTSocketImpl _socket;
	
	

		
	public TestLogEntries(int agentId, int remoteId, PTSocketImpl socket) {
		_agentId = agentId;
		_remoteId = remoteId;
		_socket = socket;
	}
	
	public int getAgentId() {
		return _agentId;
	}
	
	public int getRemoteId() {
		return _remoteId;
	}
	
	void serialize() throws IOException {
		
		String fileName= _socket._dbgNodeIdentifier+"-"+_agentId+".txt";
		
		FileWriter fw = new FileWriter(new File(PTLogger.DIRECTORY_TO_WRITE_TO+fileName));
		synchronized(_entries) {
			for(TestLogEntry e : _entries) {
				String s = e.serialize()+"\r\n";
				fw.write(s);
			}
		}
		fw.close();
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
	
	
	private void populateChecksums(PTData e, byte[] data, int off, int len) {
		long[] r = calculateChecksum(data, off, len);
		e._checksumAll = r[0];
		e._checksumEven = r[1];
		e._checksumOdd = r[2];
	}
	
	public void sentData(byte[] data, int off, int len) {
		// Copy only enough to fill our buffer
		int bytesToCopy = Math.min(len, _bytesSent.length - _bytesSentNum);
		
		System.arraycopy(data, off, _bytesSent, _bytesSentNum, bytesToCopy);
		
		_bytesSentNum +=  bytesToCopy;
		
		// Did we fill our buffer?
		if(_bytesSentNum == _bytesSent.length) {
			recordSentData(_bytesSent, 0, _bytesSent.length);
			_bytesSentNum = 0;
		}
		
		// If we have data left
		if(bytesToCopy < len) {
			sentData(data, off+bytesToCopy, len-bytesToCopy);
		}
		
	}
	
	private void recordSentData(byte[] data, int off, int len) {
		PTDataSent e = new PTDataSent();
		e._entryTime = System.currentTimeMillis();
		e.setBytesSentRecv(len);
		
		populateChecksums(e, data, off, len);
		addTestLogEntry(e);
	}
	
	public void receiveData(byte[] data, int off, int len) {
		
		// Copy only enough to fill our buffer
		int bytesToCopy = Math.min(len, _bytesReceived.length - _bytesReceivedNum);
		
		System.arraycopy(data, off, _bytesReceived, _bytesReceivedNum, bytesToCopy);
		
		_bytesReceivedNum +=  bytesToCopy;
		
		
//		dbgMessage("receiveData received - "+_bytesReceivedNum + " / "+_bytesReceived.length);
		
		// Did we fill our buffer?
		if(_bytesReceivedNum == _bytesReceived.length) {
			recordReceivedData(_bytesReceived, 0, _bytesReceived.length);
			_bytesReceivedNum = 0;
		}
		
		// If we have data left
		if(bytesToCopy < len) {
			receiveData(data, off+bytesToCopy, len-bytesToCopy);
		}
		
		
	}
	
	private void recordReceivedData(byte[] data, int off, int len) {
		PTDataReceived e = new PTDataReceived();
		e._entryTime = System.currentTimeMillis();
		e.setBytesSentRecv(len);
		
		populateChecksums(e, data, off, len);
		addTestLogEntry(e);
	}
	
	public void localInitConnect() {
		PTLocalInitiatedConnection e = new PTLocalInitiatedConnection();
		e._entryTime = System.currentTimeMillis();
		addTestLogEntry(e);
	}

	public void remoteInitConnect() {
		PTRemoteInitiatedConnection e = new PTRemoteInitiatedConnection();
		e._entryTime = System.currentTimeMillis();
		addTestLogEntry(e);
	}

	@SuppressWarnings("unused")
	private void dbgMessage(String s) {
		PTDebug d = new PTDebug(s);
		d._entryTime = System.currentTimeMillis();
		addTestLogEntry(d);
	}
	
	private void disconnectSoFlushData() {
		
		if(!_remainingDataFlushed) {
			_remainingDataFlushed = true;
		} else {
			return;
		}
		
//		dbgMessage("disconnectSoFlushData received - "+_bytesReceivedNum + " / "+_bytesSentNum);
		
		// Flush received data
		if(_bytesReceivedNum != 0) {
			recordReceivedData(_bytesReceived, 0, _bytesReceivedNum);
		}
		
		// Flush sent data
		if(_bytesSentNum != 0) {
			recordSentData(_bytesSent, 0, _bytesSentNum);
		}
	}
	
	public void localInitDisconnect(String reason) {
		disconnectSoFlushData();
		PTLocalInitiatedDisconnect e = new PTLocalInitiatedDisconnect();
		e._entryTime = System.currentTimeMillis();
		e._debugMessage = reason;
		addTestLogEntry(e);
		
		try {
			serialize();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public void remoteInitDisconnect(String reason) {
		disconnectSoFlushData();
		PTRemoteInitiatedDisconnect e = new PTRemoteInitiatedDisconnect();
		e._entryTime = System.currentTimeMillis();
		e._debugMessage = reason;
		addTestLogEntry(e);

		try {
			serialize();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}
	

	
	public void unexpectedError(String message) {
		unexpectedError(message, null);
	}
	
	public void unexpectedError(String message, Exception e) {
		PTError entry = new PTError();
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

	String _debugMessage;
	long _entryTime;

	String serialize() {
		String result = "";
		result += "entryTime:["+_entryTime+"]";
		if(_debugMessage != null) {
			result += " debugMessage:["+PTError.sanitizeString(_debugMessage)+"]";
		}
		return result;
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
		if(s.contains("debugMessage:")) {
			_debugMessage = extractField(s, "debugMessage");
		}
	}
}

abstract class SimpleTestLogEntry extends TestLogEntry {
}

abstract class PTData extends TestLogEntry {
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
	
	public void setBytesSentRecv(long _bytesSent) {
		this._bytesSentOrRecv = _bytesSent;
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


class PTDataSent extends PTData {
	@Override
	EntryType getType() {
		return EntryType.SENT;
	}
	
	String serialize() {
		return PTDataSent.class.getName() + " " + super.serialize();
	}
	
	public boolean isEquivalent(PTDataReceived r) {
		if(r.getChecksumAll() != getChecksumAll()) return false;
		if(r.getChecksumOdd() != getChecksumOdd()) return false;
		if(r.getChecksumEven() != getChecksumEven()) return false;
		
		return true;
		
	}
}


class PTDataReceived extends PTData {
	@Override
	EntryType getType() {
		return EntryType.RECEIVED;
	}

	String serialize() {
		return PTDataReceived.class.getName() + " " + super.serialize();
	}

	public boolean isEquivalent(PTDataSent r) {
		if(r.getChecksumAll() != getChecksumAll()) return false;
		if(r.getChecksumOdd() != getChecksumOdd()) return false;
		if(r.getChecksumEven() != getChecksumEven()) return false;
		
		return true;
		
	}

}

class PTError extends TestLogEntry {
	// Error
	Exception _errorException;
	String _errorMsg;
	
	String _errorExceptionString; // populated by deserializing the exception field
	
	@Override
	EntryType getType() {
		return EntryType.ERROR;
	}
	
	String serialize() {
		String result = PTError.class.getName() + " ";
		
		if(_errorException != null) {
			String exc = convertString(_errorException);
			exc = sanitizeString(exc);
			exc = exc.replace("\n", "");
			exc = exc.replace("\r", "");
			
			result += "exception:["+exc+"] ";
		}
		
		if(_errorMsg != null) {
			result += "error:["+sanitizeString(_errorMsg)+"] ";
		}
		
		return result + " " + super.serialize();
	}
	
	public static String sanitizeString(String str) {
//		System.out.println("{"+str+"}");
		str = str.replace("[", "");
		str = str.replace("]", "");
		return str;
	}
	
	private static String convertString(Exception e) {
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

class PTLocalInitiatedConnection extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.LOCAL_INIT_CONN;
	}

	String serialize() {
		return PTLocalInitiatedConnection.class.getName() + " " + super.serialize();
	}

}

class PTRemoteInitiatedConnection extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.REMOTE_INIT_CONN;
	}

	String serialize() {
		return PTRemoteInitiatedConnection.class.getName() + " " + super.serialize();
	}

}

class PTLocalInitiatedDisconnect extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.LOCAL_INIT_DC;
	}
	
	String serialize() {
		return PTLocalInitiatedDisconnect.class.getName() + " " + super.serialize();
	}
	
}

class PTRemoteInitiatedDisconnect extends SimpleTestLogEntry {

	@Override
	EntryType getType() {
		return EntryType.REMOTE_INIT_DC;
	}
	
	String serialize() {
		return PTRemoteInitiatedDisconnect.class.getName() + " " + super.serialize();
	}
	
}


class PTDebug extends SimpleTestLogEntry {

	String _dbgMsg;
	@Override
	EntryType getType() {
		return EntryType.GENERAL;
	}
	
	public PTDebug(String dbgMsg) {
		_dbgMsg = dbgMsg;
	}

	public PTDebug() {
	}
	
	String serialize() {
		return PTDebug.class.getName() + " dbgMsg:["+_dbgMsg+"] "+ super.serialize();
	}

	void deserialize(String s) {
		_dbgMsg = extractField(s, "dbgMsg");
	}
}


