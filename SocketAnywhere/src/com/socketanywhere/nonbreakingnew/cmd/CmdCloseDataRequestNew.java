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

public class CmdCloseDataRequestNew extends CmdAbstract {

	public final static int ID = 10;
	
	private final static byte[] BYTE_CMD_ID = {0x0, 0x10};
	
	private static final int FIRST_PACKET_TO_RESEND_FIELD_LENGTH = 4;
	
	/** Command id of the command being acknowledged */
	private int _fieldFirstPacketToResend = -1;
	
	public CmdCloseDataRequestNew(int firstPacketToRsend) {
		super(ID, BYTE_CMD_ID);
		_fieldFirstPacketToResend = firstPacketToRsend;
	}

	public CmdCloseDataRequestNew() {
		super(ID, BYTE_CMD_ID);
		
	}
	
	@Override
	public byte[] buildCommand() {
		int length = MAGIC_NUMBER.length + BYTE_CMD_ID.length + CMD_LENGTH_HEADER_FIELD_LENGTH + FIRST_PACKET_TO_RESEND_FIELD_LENGTH;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;

		// Int param
		byte[] firstPacketArr = i2b(_fieldFirstPacketToResend);
		System.arraycopy(firstPacketArr, 0, result, currPos, FIRST_PACKET_TO_RESEND_FIELD_LENGTH);
		currPos += FIRST_PACKET_TO_RESEND_FIELD_LENGTH;
		
		return result;
	}

	@Override
	public void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		_fieldFirstPacketToResend = b2i(b, currPos);
		currPos += FIRST_PACKET_TO_RESEND_FIELD_LENGTH;
		
	}

	@Override
	CmdAbstract instantiateInstance() {
		return new CmdCloseDataRequestNew();
	}

	@Override
	public String toString() {
		return CmdCloseDataRequestNew.class.getSimpleName()+" - firstPacketToResend: "+_fieldFirstPacketToResend;
	}
	
	public int getFieldFirstPacketToResend() {
		return _fieldFirstPacketToResend;
	}
}
