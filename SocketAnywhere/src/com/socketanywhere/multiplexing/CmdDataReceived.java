/*
	Copyright 2016 Jonathan West

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

public class CmdDataReceived extends CmdMultiplexAbstract {

	public final static int ID = 5;
	private final byte[] _byteCmdId = {0x0, 0x5};

	
	/** Command id of the command being acknowledged */
	private long  _bytesReceivedField = -1;
			
	
	public CmdDataReceived(long bytesReceived, String nodeUUID, int connId) {
		
		_bytesReceivedField = bytesReceived;
		setConnUUID(nodeUUID);
		setConnId(connId);
	}

	protected CmdDataReceived() {
	}
	

	@Override
	public byte[] buildCommand() {
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(8 /* size of int param*/);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;
		
		// Write the Cmd Id we are acknowledging
		byte[] cmdIdArr = l2b(_bytesReceivedField);
		System.arraycopy(cmdIdArr, 0, result, currPos, cmdIdArr.length);
		currPos += cmdIdArr.length;
				
		return result;
	}

	@Override
	public void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		_bytesReceivedField = b2l(b, currPos);
		currPos += 8;

	}

	@Override
	public int getId() {
		return ID;
	}

	@Override
	byte[] getByteId() {
		return _byteCmdId;
	}

	@Override
	CmdMultiplexAbstract instantiateInstance() {
		return new CmdDataReceived();
	}

	@Override
	public String toString() {
		return CmdDataReceived.class.getSimpleName()+" - bytesReceived:"+ _bytesReceivedField+" "+super.toString();
	}
	
	public long getBytesReceived() {
		return _bytesReceivedField;
	}
		
}
