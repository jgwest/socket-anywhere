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


public abstract class CmdAbstractUuidConnId extends CmdAbstract implements ICmdUuidConnId {

	/** Node UUID for the connection that ack is sent on */
	private String _fieldNodeUUID;
	
	/** Connection id for the connection that ack is sent on */
	private int _fieldConnectionId;
	
	
	private final static int CONN_ID_FIELD_LENGTH = 4;
	
	protected CmdAbstractUuidConnId(int cmdId, byte[] byteCmdId, String nodeUUID, int connId) {
		super(cmdId, byteCmdId);
		
		this._fieldNodeUUID = nodeUUID;
		this._fieldConnectionId = connId;
	}
	
	protected CmdAbstractUuidConnId(int cmdId, byte[] byteCmdId) {
		super(cmdId, byteCmdId);		
	}

	@Override
	public
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
		
		
		// Write the generic Int param 
		byte[] intParamArr = i2b(0 /*_fieldIntParam*/); // always zero
		System.arraycopy(intParamArr, 0, result, currPos, intParamArr.length);
		currPos += intParamArr.length;
	
		
		return result;
	}

	@SuppressWarnings("unused")
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
		
		int fieldIntParam = b2i(b, currPos); // always 0		
		currPos += 4;
		
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" - nodeUUID:"+_fieldNodeUUID+"  connectionID:"+_fieldConnectionId+" cmd-id: "+_debugId;
	}
		
	public final String getFieldNodeUUID() {
		return _fieldNodeUUID;
	}
	
	public final int getFieldConnectionId() {
		return _fieldConnectionId;
	}
	
}
