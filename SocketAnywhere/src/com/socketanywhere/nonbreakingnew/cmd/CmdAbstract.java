/*
	Copyright 2012, 2013 Jonathan West

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

package com.socketanywhere.nonbreakingnew.cmd;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicLong;

public abstract class CmdAbstract {
	
	public static final byte[] MAGIC_NUMBER = { 0x14, 0x25, 0x36, 0x47 };
	
	public static final int BYTE_CMD_ID_FIELD_LENGTH = 2;
	public static final int CMD_LENGTH_HEADER_FIELD_LENGTH = 4;
	
	byte[] _magicNumberArr;
	byte[] _parsedCmdIdArr;
	int _parsedCmdLength;
	
	final int _id;
	
	final byte[] _byteCmdId;
	
	private final static AtomicLong nextDebugId = new AtomicLong(0); 
			
	long _debugId = nextDebugId.getAndIncrement();
	
	// ---------------------------------------------------------
	
	
	public abstract byte[] buildCommand();
	
	/** Parse the byte array and store fields in command class */
	public abstract void parseCommand(byte[] b);
	
	abstract CmdAbstract instantiateInstance();
	
	public abstract String toString();
	
	// ---------------------------------------------------------
	
	

	
	protected CmdAbstract(int id, byte[] byteCmdId) {
		this._id = id;
		this._byteCmdId = byteCmdId;
	}
	
	/** Convert int to 4 bytes */
	public static byte[] i2b(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}
	
	/** Convert 4 bytes to int */
	public static int b2i(byte[] b, int start) {
		return (b[start] << 24) + ((b[start+1] & 0xFF) << 16) + ((b[start+2] & 0xFF) << 8) + (b[start+3] & 0xFF);
	}

	/** Convert 4 bytes to int */
	public static int b2i(byte[] b) {
		return b2i(b, 0);
//		return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
	}

	
	/**
	 * Returns a byte array that contains the header fields with the correct values for the subclass
	 * @param fullCmdLength Length of the full command as determined by subclass
	 * @return Returns a couple of values inside the param pass class (the byte array, and the post-header position in the byte array)
	 */
	BuildGenericCmdHeaderParamPass buildGenericCmdHeader(int fullCmdLength) {
		
		int currPos = 0;
		byte[] result = new byte[fullCmdLength];
		
		// Magic number
		System.arraycopy(MAGIC_NUMBER, 0, result, currPos, MAGIC_NUMBER.length);
		currPos += MAGIC_NUMBER.length;
		
		// Command ID
		System.arraycopy(getByteId(), 0, result, currPos, BYTE_CMD_ID_FIELD_LENGTH);
		currPos += BYTE_CMD_ID_FIELD_LENGTH;
		
		// Command length (including the magic number, the id, and this field's size itself)
		byte[] cmdSize = i2b(fullCmdLength);
		System.arraycopy(cmdSize, 0, result, currPos, CMD_LENGTH_HEADER_FIELD_LENGTH);
		currPos += CMD_LENGTH_HEADER_FIELD_LENGTH;

		BuildGenericCmdHeaderParamPass pr = new BuildGenericCmdHeaderParamPass();
		
		pr._byteArr = result;
		pr._currPos = currPos;
		
		return pr;
		
	}
	
	int parseHeader(byte[] b) {
		int currPos = 0;
		
		// Read magic num
		_magicNumberArr = new byte[CmdAbstract.MAGIC_NUMBER.length];
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
		
		return currPos;
	}
	
	
	/** Given an array of bytes, starting at position 'start', read the string that begins
	 * at that position. */
	static ConvertBytesToStringParamPass convertBytesToString(byte[] b, int start) {
		
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
	static byte[] convertStringToBytes(String s) {
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

	/** If the command was parsed by us (as opposed to built by us), return the total length of the cmd */
	public int getParsedCmdLength() {
		return _parsedCmdLength;
	}
	
	final public int getId() {
		return _id;
	}

	final byte[] getByteId() {
		return _byteCmdId;
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