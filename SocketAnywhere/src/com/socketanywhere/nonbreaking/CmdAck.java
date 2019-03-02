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


public class CmdAck extends CmdAbstract {

	public final static int ID = 6;
	private final byte[] _byteCmdId = {0x0, 0x6};

	
	/** Command id of the command being acknowledged */
	private int _fieldCmdIdOfAckedCmd = -1;
	
	/** Generic optional param, whose purpose is determined by the command being acknowledged*/
	private int _fieldIntParam = 0;
	
	/** Node UUID for the connection that ack is sent on */
	private String _fieldNodeUUID;
	
	/** Connection id for the connection that ack is sent on */
	private int _fieldConnectionId;
	
	
	private final static int CONN_ID_FIELD_LENGTH = 4;
	
	public CmdAck(int cmdIdOfAckedCmd, int intParam, String nodeUUID, int connId) {
		if(cmdIdOfAckedCmd == ID) {
			NBLog.error("Invalid cmd id.");
		}
		
		this._fieldCmdIdOfAckedCmd = cmdIdOfAckedCmd;
		this._fieldIntParam = intParam;
		this._fieldNodeUUID = nodeUUID;
		this._fieldConnectionId = connId;
	}

	CmdAck() {
	}
	

	@Override
	byte[] buildCommand() {
		
		byte[] nodeUUIDBytes = convertStringToBytes(_fieldNodeUUID);
		
		int length = MAGIC_NUMBER.length + _byteCmdId.length + CMD_LENGTH_HEADER_FIELD_LENGTH + 4 /* tag of acked cmd (int) */ + 4 /* int param of cmd*/ + nodeUUIDBytes.length + CONN_ID_FIELD_LENGTH;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;

		
		// Write uuid of connection
		System.arraycopy(nodeUUIDBytes, 0, result, currPos, nodeUUIDBytes.length);
		currPos += nodeUUIDBytes.length;

		// Write connection id of connection
		byte[] connIdArr = i2b(_fieldConnectionId);
		System.arraycopy(connIdArr, 0, result, currPos, CONN_ID_FIELD_LENGTH);
		currPos += CONN_ID_FIELD_LENGTH;		
		
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
	void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		// Read UUID for connection
		ConvertBytesToStringParamPass p = convertBytesToString(b, currPos);
		_fieldNodeUUID = p._result;
		currPos = p._currPos;
		
		// Read connection id for connection
		_fieldConnectionId = b2i(b, currPos);
		currPos += CONN_ID_FIELD_LENGTH;
		
		_fieldCmdIdOfAckedCmd = b2i(b, currPos);
		currPos += 4;

		_fieldIntParam = b2i(b, currPos);
		currPos += 4;
		
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
		return new CmdAck();
	}

	

	@Override
	public String toString() {
		return CmdAck.class.getSimpleName()+" - cmdIdOfAckedCmd:"+_fieldCmdIdOfAckedCmd + "  intParam:"+_fieldIntParam;
	}
	
	
	
	public int getFieldCmdIdOfAckedCmd() {
		return _fieldCmdIdOfAckedCmd;
	}
	
	public int getFieldIntParam() {
		return _fieldIntParam;
	}
	
	public String getFieldNodeUUID() {
		return _fieldNodeUUID;
	}
	
	public int getFieldConnectionId() {
		return _fieldConnectionId;
	}
	
}
