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

public class CmdDataRequest extends CmdAbstract {

	public final static int ID = 7;
	
	private final byte[] _byteCmdId = {0x0, 0x7};
	
	private static final int FIRST_PACKET_TO_RESEND_FIELD_LENGTH = 4;
	
	/** Command id of the command being acknowledged */
	private int _fieldFirstPacketToResend = -1;
	
	public CmdDataRequest(int firstPacketToRsend) {
		_fieldFirstPacketToResend = firstPacketToRsend;
	}

	public CmdDataRequest() {
		
	}
	
	@Override
	byte[] buildCommand() {
		int length = MAGIC_NUMBER.length + _byteCmdId.length + CMD_LENGTH_HEADER_FIELD_LENGTH + FIRST_PACKET_TO_RESEND_FIELD_LENGTH;
		
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
	void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		_fieldFirstPacketToResend = b2i(b, currPos);
		currPos += FIRST_PACKET_TO_RESEND_FIELD_LENGTH;
		
	}

	
	@Override
	int getId() {
		return ID;
	}

	@Override
	byte[] getByteId() {
		return _byteCmdId;
	}

	@Override
	CmdAbstract instantiateInstance() {
		return new CmdDataRequest();
	}

	@Override
	public String toString() {
		return CmdDataRequest.class.getSimpleName()+" - firstPacketToResend: "+_fieldFirstPacketToResend;
	}
	
	public int getFieldFirstPacketToResend() {
		return _fieldFirstPacketToResend;
	}
}
