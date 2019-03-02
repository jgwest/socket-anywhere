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

package com.socketanywhere.nonbreakingnew.cmd;

public class CmdDataReceived extends CmdAbstract {

	public final static int ID = 4;	
	private final static byte[] BYTE_CMD_ID = {0x0, 0x4};
	
	private final static int PACKET_ID_FIELD_LENGTH = 4;
	
	// The last packet received by the sender of the command
	private int _fieldLastPacketReceived;
	
	
	public CmdDataReceived() {
		super(ID, BYTE_CMD_ID);
	}
	
	public CmdDataReceived(int lastPacketReceived) {
		super(ID, BYTE_CMD_ID);
		_fieldLastPacketReceived = lastPacketReceived;
	}
	
	@Override
	public byte[] buildCommand() {
		int length = MAGIC_NUMBER.length + BYTE_CMD_ID_FIELD_LENGTH + CMD_LENGTH_HEADER_FIELD_LENGTH + PACKET_ID_FIELD_LENGTH;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;
		
		// Packet ID
		byte[] packetIdArr = i2b(_fieldLastPacketReceived);
		System.arraycopy(packetIdArr, 0, result, currPos, PACKET_ID_FIELD_LENGTH);
		currPos += PACKET_ID_FIELD_LENGTH;

		return result;

	}

	@Override
	public void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
				
		// packet id
		_fieldLastPacketReceived = b2i(b, currPos);
		currPos += PACKET_ID_FIELD_LENGTH; 
		
	}

	public int getLastPacketReceived() {
		return _fieldLastPacketReceived;
	}
	

	@Override
	CmdAbstract instantiateInstance() {
		return new CmdDataReceived();
	}
	
	@Override
	public String toString() {
		return CmdDataReceived.class.getSimpleName() + " lastPacketReceived:"+_fieldLastPacketReceived; 
	}
	
	public int getFieldLastPacketReceived() {
		return _fieldLastPacketReceived;
	}
}
