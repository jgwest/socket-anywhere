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

package com.socketanywhere.nonbreaking;

public class CmdData extends CmdAbstract {

	public final static int ID = 1;
	
	private final byte[] _byteCmdId = {0x0, 0x1};
	
	private final static int DATA_LENGTH_HEADER_FIELD_LENGTH = 4;
	private final static int PACKET_ID_HEADER_FIELD_LENGTH = 4;
	
	private int _fieldDataLength;
	private byte[] _fieldData;
	private int _fieldPacketId;

	public CmdData(byte[] data, int dataLength, int packetId) {
		_fieldData = data;
		_fieldDataLength = dataLength;
		_fieldPacketId = packetId;
	}
	
	public CmdData() {
	}
	
	@Override
	byte[] buildCommand() {
		int length = MAGIC_NUMBER.length + _byteCmdId.length + CMD_LENGTH_HEADER_FIELD_LENGTH + PACKET_ID_HEADER_FIELD_LENGTH + DATA_LENGTH_HEADER_FIELD_LENGTH + _fieldDataLength;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;

		
		// Packet ID
		byte[] packetIdArr = i2b(_fieldPacketId);
		
		System.arraycopy(packetIdArr, 0, result, currPos, PACKET_ID_HEADER_FIELD_LENGTH);
		currPos += PACKET_ID_HEADER_FIELD_LENGTH;
		
		// Data Length
		byte[] dataLengthArr = i2b(_fieldDataLength);
		
		System.arraycopy(dataLengthArr, 0, result, currPos, DATA_LENGTH_HEADER_FIELD_LENGTH);
		currPos += DATA_LENGTH_HEADER_FIELD_LENGTH;

		// Data
		System.arraycopy(_fieldData, 0, result, currPos, _fieldDataLength);
		
		return result;
	}
	
	@Override
	void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		// packet id
		byte[] packetIdArr = new byte[PACKET_ID_HEADER_FIELD_LENGTH];
		System.arraycopy(b, currPos, packetIdArr, 0, packetIdArr.length);
		currPos += packetIdArr.length;
		
		_fieldPacketId = b2i(packetIdArr);
		
		// data length
		byte[] dataLengthArr = new byte[DATA_LENGTH_HEADER_FIELD_LENGTH];
		System.arraycopy(b, currPos, dataLengthArr, 0, dataLengthArr.length);
		currPos += dataLengthArr.length;

		_fieldDataLength = b2i(dataLengthArr);
		
		
		// data
		_fieldData = new byte[_fieldDataLength];
		System.arraycopy(b, currPos, _fieldData, 0, _fieldDataLength);
		
	}
	
	@Override
	int getId() {
		return ID;
	}
	
	@Override
	byte[] getByteId() {
		return _byteCmdId;
	}

	
	
	public int getFieldDataLength() {
		return _fieldDataLength;
	}

	public byte[] getFieldData() {
		return _fieldData;
	}

	public int getFieldPacketId() {
		return _fieldPacketId;
	}

	@Override
	CmdAbstract instantiateInstance() {
		return new CmdData();
	}
		
	@Override
	public String toString() {
		return CmdData.class.getSimpleName() + " - dataLength:"+_fieldDataLength + "  packetId:"+_fieldPacketId;
	}

}
