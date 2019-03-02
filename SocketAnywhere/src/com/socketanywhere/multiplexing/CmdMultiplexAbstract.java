/*
	Copyright 2012, 2016 Jonathan West

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

package com.socketanywhere.multiplexing;

import java.io.UnsupportedEncodingException;

public abstract class CmdMultiplexAbstract {
	
	public static final byte[] MAGIC_NUMBER = { 0x24, 0x35, 0x46, 0x57 };
	
	public final static int BYTE_CMD_ID_FIELD_LENGTH = 2;
	public final static int CMD_LENGTH_HEADER_FIELD_LENGTH = 4;
	
	public final static int CMD_CONN_ID_FIELD_LENGTH = 4;
	
	
	byte[] _magicNumberArr;
	byte[] _parsedCmdIdArr;
	int _parsedCmdLength;
	String _connUUID; 
	int _connId;
	
	
	// ---------------------------------------------------------
	
	
	public abstract byte[] buildCommand();
	
	public abstract void parseCommand(byte[] b);
	
	public abstract int getId();
	
	abstract byte[] getByteId();
	
	abstract CmdMultiplexAbstract instantiateInstance();
	
	// ---------------------------------------------------------
	
	public String getCmdConnUUID() {
		return _connUUID;
	}
	
	public int getCmdConnectionId() {
		return _connId;
	}
	
	public void setConnUUID(String connUUID1) {
		this._connUUID = connUUID1;
	}
	
	public void setConnId(int connId1) {
		this._connId = connId1;
	}
	
	public String toString() {
		return "["+_connUUID+":"+_connId+"]";
	}
	
	
	public static byte[] l2b(long l) {
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
	    return result;
	}

	public static long b2l(byte[] b, int startPos) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (b[i+startPos] & 0xFF);
	    }
	    return result;
	}

	
	/** Convert 8 bytes to long*/
	public static long b2l(byte[] b) {
		return b2l(b, 0);
	}

	
	/** Convert int to 4 bytes */
	public static byte[] i2b(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}
	
	/** Convert 4 bytes to int */
	public static int b2i(byte[] b, int start) {
		return (b[start] << 24) 
				+ ((b[start+1] & 0xFF) << 16) 
				+ ((b[start+2] & 0xFF) << 8) 
				+ (b[start+3] & 0xFF);
	}

	/** Convert 4 bytes to int */
	public static int b2i(byte[] b) {
		return b2i(b, 0);
	}
	
	/**
	 * Returns a byte array that contains the header fields with the correct values for the subclass
	 * @param fullCmdLength Length of the full command as determined by subclass
	 * @return Returns a couple of values inside the param pass class (the byte array, and the post-header position in the byte array)
	 */
	BuildGenericCmdHeaderParamPass buildGenericCmdHeader(int additionalCmdLength) {
		
		int currPos = 0;
		

		// Precalculate the amount of space required to store the connection UUID string
		byte[] connUUIDBytes = convertStringToBytes(_connUUID);
		
		
		int thisLength = MAGIC_NUMBER.length + BYTE_CMD_ID_FIELD_LENGTH + CMD_LENGTH_HEADER_FIELD_LENGTH
				+ connUUIDBytes.length + CMD_CONN_ID_FIELD_LENGTH
				+ additionalCmdLength;

		byte[] result = new byte[thisLength];

		
		// Magic number
		System.arraycopy(MAGIC_NUMBER, 0, result, currPos, MAGIC_NUMBER.length);
		currPos += MAGIC_NUMBER.length;
		
		// Command ID
		System.arraycopy(getByteId(), 0, result, currPos, BYTE_CMD_ID_FIELD_LENGTH);
		currPos += BYTE_CMD_ID_FIELD_LENGTH;
		
		// Command length (including the magic number, the id, this field's size itself, uuid and connection id)
		byte[] cmdSize = i2b(thisLength);
		System.arraycopy(cmdSize, 0, result, currPos, CMD_LENGTH_HEADER_FIELD_LENGTH);
		currPos += CMD_LENGTH_HEADER_FIELD_LENGTH;

		
		// Write connection's UUID 
		System.arraycopy(connUUIDBytes, 0, result, currPos, connUUIDBytes.length);
		currPos += connUUIDBytes.length;

		// Write Connection ID
		byte[] connIdArr = i2b(_connId);
		System.arraycopy(connIdArr, 0, result, currPos, connIdArr.length);
		currPos += connIdArr.length;

		
		BuildGenericCmdHeaderParamPass pr = new BuildGenericCmdHeaderParamPass();
		
		pr._byteArr = result;
		pr._currPos = currPos;
		
		return pr;
		
	}
	
	int parseHeader(byte[] b) {
		int currPos = 0;
		
		// Read magic num
		_magicNumberArr = new byte[CmdMultiplexAbstract.MAGIC_NUMBER.length];
		System.arraycopy(b, currPos, _magicNumberArr, 0, _magicNumberArr.length);
		currPos += _magicNumberArr.length;
		
		// Read command id
		_parsedCmdIdArr = new byte[BYTE_CMD_ID_FIELD_LENGTH];
		System.arraycopy(b, currPos, _parsedCmdIdArr, 0, _parsedCmdIdArr.length);
		currPos += _parsedCmdIdArr.length;
		
		// Read command length
		byte[] cmdLengthArr = new byte[CMD_LENGTH_HEADER_FIELD_LENGTH];
		System.arraycopy(b, currPos, cmdLengthArr, 0, cmdLengthArr.length);
		currPos += cmdLengthArr.length;
		_parsedCmdLength = b2i(cmdLengthArr); 
		

		ConvertBytesToStringParamPass p;
		
		// Read connection UUID
		p = convertBytesToString(b, currPos);
		_connUUID = p._result;
		currPos = p._currPos;
		
		// Read connection id
		_connId = b2i(b, currPos);
		currPos += CMD_CONN_ID_FIELD_LENGTH; 
		
		return currPos;
	}
	
	
	/** Given an array of bytes, starting at position 'start', read the string that begins
	 * at that position. */
	ConvertBytesToStringParamPass convertBytesToString(byte[] b, int start) {
		
		int strLen = b2i(b, start);
		byte[] strBytes = new byte[strLen];
		
		// Copy the string bytes from b to strBytes
		System.arraycopy(b, start+4, strBytes, 0, strLen);
		
		try {
			String resultStr = new String(strBytes, "UTF-8");
			ConvertBytesToStringParamPass result = new ConvertBytesToStringParamPass();

			result._currPos = start + 4 + strLen;
			result._result = resultStr;
			
			return result;

		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/** Given a string, convert it to a format we can write to the stream. 
	 * Format is:
	 * - length of string data (in bytes)
	 * - string data (in bytes) */
	byte[] convertStringToBytes(String s) {
		byte[] result = null;
		try {
			byte[] strArr = s.getBytes("UTF-8");
			result = new byte[strArr.length+4];
			
			byte[] strLenArr = i2b(strArr.length);
			
			System.arraycopy(strLenArr, 0, result, 0, strLenArr.length);
			System.arraycopy(strArr, 0, result, strLenArr.length, strArr.length);
			
		} catch (UnsupportedEncodingException e) { e.printStackTrace(); }
		return result;
	}
	
}

/** Variable passing class for convertBytesToString(...) */
class ConvertBytesToStringParamPass {
	String _result;
	int _currPos;
}

/** Variable passing class for buildGenericCmdHeader(...) */
class BuildGenericCmdHeaderParamPass {
	byte[] _byteArr;
	int _currPos;
}