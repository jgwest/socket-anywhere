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

package com.socketanywhere.nonbreakingnew.cmd;

public abstract class CmdAbstractUuidConnIdInt extends CmdAbstract implements ICmdUuidConnId {

	/** Generic optional param, whose purpose is determined by the command being acknowledged*/
	int _fieldIntParam = 0;
	
	/** Node UUID for the connection that ack is sent on */
	private String _fieldNodeUUID;
	
	/** Connection id for the connection that ack is sent on */
	private int _fieldConnectionId;
	
	
	private final static int CONN_ID_FIELD_LENGTH = 4;
	
	public CmdAbstractUuidConnIdInt(int id, byte[] byteCmdId, int lastPacketSeenByPeer, String nodeUUID, int connId) {
		super(id, byteCmdId);
		
		this._fieldIntParam = lastPacketSeenByPeer;
		this._fieldNodeUUID = nodeUUID;
		this._fieldConnectionId = connId;
	}

	CmdAbstractUuidConnIdInt(int id, byte[] byteCmdId) {
		super(id, byteCmdId);
	}
	

	@Override
	public
	byte[] buildCommand() {
		
		byte[] nodeUUIDBytes = convertStringToBytes(_fieldNodeUUID);
		
		int length = MAGIC_NUMBER.length + getByteId().length + CMD_LENGTH_HEADER_FIELD_LENGTH + 4 /* tag of acked cmd (int) */ + 4 /* int param of cmd*/ + nodeUUIDBytes.length + CONN_ID_FIELD_LENGTH;
		
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
		
		
		// Write the generic Int param 
		byte[] intParamArr = i2b(_fieldIntParam);
		System.arraycopy(intParamArr, 0, result, currPos, intParamArr.length);
		currPos += intParamArr.length;
	
		
		return result;
	}

	@Override
	public void parseCommand(byte[] b) {
		int currPos = parseHeader(b);
		
		// Read UUID for connection
		ConvertBytesToStringParamPass p = convertBytesToString(b, currPos);
		_fieldNodeUUID = p._result;
		currPos = p._currPos;
		
		// Read connection id for connection
		_fieldConnectionId = b2i(b, currPos);
		currPos += CONN_ID_FIELD_LENGTH;
		
		_fieldIntParam = b2i(b, currPos);
		currPos += 4;
		
	}
	
	public String getFieldNodeUUID() {
		return _fieldNodeUUID;
	}
	
	public int getFieldConnectionId() {
		return _fieldConnectionId;
	}
	
}
