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

// TODO: LOWER - Is it possible to refuse a join connection; should we need to?

public class CmdJoinConn extends CmdAbstract {

	public final static int ID = 5;
	
	public final static int CONN_ID_FIELD_LENGTH = 4;
	
	private final byte[] _byteCmdId = {0x0, 0x5};
	
	private String _fieldNodeUUID;
	private int _fieldConnectionId;
	
	public CmdJoinConn(String nodeUUID, int connectionId) {
		_fieldNodeUUID = nodeUUID;
		_fieldConnectionId = connectionId;
	}
	
	public CmdJoinConn() {
	}
	
	@Override
	byte[] buildCommand() {
		byte[] nodeUUIDBytes = convertStringToBytes(_fieldNodeUUID);
				
		int length = MAGIC_NUMBER.length + _byteCmdId.length + CMD_LENGTH_HEADER_FIELD_LENGTH 
			+ nodeUUIDBytes.length + CONN_ID_FIELD_LENGTH;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;
		
		// uuid
		System.arraycopy(nodeUUIDBytes, 0, result, currPos, nodeUUIDBytes.length);
		currPos += nodeUUIDBytes.length;

		// connection id
		byte[] connIdArr = i2b(_fieldConnectionId);
		System.arraycopy(connIdArr, 0, result, currPos, CONN_ID_FIELD_LENGTH);
		currPos += CONN_ID_FIELD_LENGTH;

		return result;

	}

	@Override
	void parseCommand(byte[] b) {
		int currPos = parseHeader(b);

		// uuid
		ConvertBytesToStringParamPass p = convertBytesToString(b, currPos);
		_fieldNodeUUID = p._result;
		currPos = p._currPos;
				
		// connection id
		_fieldConnectionId = b2i(b, currPos);
		currPos += CONN_ID_FIELD_LENGTH; 
		
	}

	
	@Override
	int getId() {
		return ID;
	}

	@Override
	byte[] getByteId() {
		return _byteCmdId;
	}
	
	public String getFieldNodeUUID() {
		return _fieldNodeUUID;
	}
	
	public int getFieldConnectionId() {
		return _fieldConnectionId;
	}
	
	@Override
	CmdAbstract instantiateInstance() {
		return new CmdJoinConn();
	}
	
	@Override
	public String toString() {
		return CmdJoinConn.class.getSimpleName() + " nodeUUID:"+_fieldNodeUUID+"  connectionID:"+_fieldConnectionId; 
	}

}
