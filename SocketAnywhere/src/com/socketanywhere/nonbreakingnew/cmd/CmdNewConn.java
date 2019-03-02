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

public class CmdNewConn extends CmdAbstract implements ICmdUuidConnId {

	public final static int ID = 2;
	
	private final static byte[] BYTE_CMD_ID = {0x0, 0x2};
	
	private final static int CMD_ID_FIELD_LENGTH = 4;
	private final static int SERVER_PORT_FIELD_LENGTH = 4;

	private String _fieldNodeUUID;
	private int _fieldConnectionId;
	private String _fieldConnectorIp;
	private int _fieldServerPort;
	
	public CmdNewConn() {
		super(ID, BYTE_CMD_ID);
	}
	
	public CmdNewConn(String nodeUUID, int connectionId, String connectorIp, int serverPort) {
		super(ID, BYTE_CMD_ID);
		this._fieldNodeUUID = nodeUUID;
		this._fieldConnectionId = connectionId;
		this._fieldConnectorIp = connectorIp;
		this._fieldServerPort = serverPort;
	}

	@Override
	public byte[] buildCommand() {
		
		byte[] nodeUUIDBytes = convertStringToBytes(_fieldNodeUUID);
		byte[] connectorIpBytes = convertStringToBytes(_fieldConnectorIp);
				
		int length = MAGIC_NUMBER.length + BYTE_CMD_ID.length + CMD_LENGTH_HEADER_FIELD_LENGTH 
			+ nodeUUIDBytes.length + 4 + connectorIpBytes.length + 4;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;
		
		// Write connection's UUID 
		System.arraycopy(nodeUUIDBytes, 0, result, currPos, nodeUUIDBytes.length);
		currPos += nodeUUIDBytes.length;

		// Write Connection ID
		byte[] connIdArr = i2b(_fieldConnectionId);
		System.arraycopy(connIdArr, 0, result, currPos, connIdArr.length);
		currPos += connIdArr.length;
		
		// Write Connection IP addr
		System.arraycopy(connectorIpBytes, 0, result, currPos, connectorIpBytes.length);
		currPos += connectorIpBytes.length;
		
		// Write server port
		byte[] portArr = i2b(_fieldServerPort);
		System.arraycopy(portArr, 0, result, currPos, portArr.length);
		currPos += portArr.length;
		
		return result;
	}

	@Override
	public void parseCommand(byte[] b) {
		int currPos = parseHeader(b);

		// uuid
		ConvertBytesToStringParamPass p = convertBytesToString(b, currPos);
		_fieldNodeUUID = p._result;
		currPos = p._currPos;
				
		// connection id
		_fieldConnectionId = b2i(b, currPos);
		currPos += CMD_ID_FIELD_LENGTH; 
		
		// connector ip address
		p = convertBytesToString(b, currPos);
		_fieldConnectorIp = p._result;
		currPos = p._currPos;
		
		// server port
		_fieldServerPort = b2i(b, currPos);
		currPos += SERVER_PORT_FIELD_LENGTH;
		
	}

	public String getFieldNodeUUID() {
		return _fieldNodeUUID;
	}

	public int getFieldConnectionId() {
		return _fieldConnectionId;
	}

	public String getFieldConnectorIp() {
		return _fieldConnectorIp;
	}

	public int getFieldServerPort() {
		return _fieldServerPort;
	}

	
	@Override
	CmdAbstract instantiateInstance() {
		return new CmdNewConn();
	}
	
	@Override
	public String toString() {
		return CmdNewConn.class.getSimpleName() + " nodeUUID:"+_fieldNodeUUID+"  connectionID:"+_fieldConnectionId+"  connectorIp:"+_fieldConnectorIp+"  serverPort:"+_fieldServerPort; 
	}

}
