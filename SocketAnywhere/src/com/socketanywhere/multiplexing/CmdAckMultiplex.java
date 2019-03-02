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

package com.socketanywhere.multiplexing;

public class CmdAckMultiplex extends CmdMultiplexAbstract {

	public final static int ID = 2;
	private final byte[] _byteCmdId = {0x0, 0x2};

	
	/** Command id of the command being acknowledged */
	private int _fieldCmdIdOfAckedCmd = -1;
	
	/** Generic optional param, whose purpose is determined by the command being acknowledged*/
	private int _fieldIntParam = 0;
		
	
	public CmdAckMultiplex(int cmdIdOfAckedCmd, int intParam, String nodeUUID, int connId) {
		if(cmdIdOfAckedCmd == ID) {
			// Command cannot ACK itself
			MuLog.error("Invalid cmd id.");
		}
		
		_fieldCmdIdOfAckedCmd = cmdIdOfAckedCmd;
		_fieldIntParam = intParam;
		setConnUUID(nodeUUID);
		setConnId(connId);
	}

	protected CmdAckMultiplex() {
	}
	

	@Override
	public byte[] buildCommand() {
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(8 /* size of our two int params*/);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;
		
		// Write the Cmd Id we are acknowledging
		byte[] cmdIdArr = i2b(_fieldCmdIdOfAckedCmd);
		System.arraycopy(cmdIdArr, 0, result, currPos, cmdIdArr.length);
		currPos += cmdIdArr.length;
		
		// Write the generic Int param 
		byte[] intParamArr = i2b(_fieldIntParam);
		System.arraycopy(intParamArr, 0, result, currPos, intParamArr.length);
		currPos += intParamArr.length;
	
		
		return result;
	}

	@Override
	public void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		_fieldCmdIdOfAckedCmd = b2i(b, currPos);
		currPos += 4;

		_fieldIntParam = b2i(b, currPos);
		currPos += 4;
		
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
		return new CmdAckMultiplex();
	}

	

	@Override
	public String toString() {
		return CmdAckMultiplex.class.getSimpleName()+" - cmdIdOfAckedCmd:"+ (CmdMultFactory.getInstance().getCommandById(_fieldCmdIdOfAckedCmd).getClass().getSimpleName()) + "  intParam:"+_fieldIntParam + " " +super.toString();
	}
	
	
	
	public int getFieldCmdIdOfAckedCmd() {
		return _fieldCmdIdOfAckedCmd;
	}
	
	public int getFieldIntParam() {
		return _fieldIntParam;
	}
	
}
