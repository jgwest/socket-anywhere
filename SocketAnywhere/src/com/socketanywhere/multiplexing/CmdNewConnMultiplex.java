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

public class CmdNewConnMultiplex extends CmdMultiplexAbstract {

	public final static int ID = 1;
	
	private final byte[] _byteCmdId = {0x0, 0x1};
	
	private final static int SERVER_PORT_FIELD_LENGTH = 4;


	private String _fieldConnectorIp;
	private int _fieldServerPort;
	
	public CmdNewConnMultiplex() {
	}
	
	public CmdNewConnMultiplex(String nodeUUID, int connectionId, String connectorIp, int serverPort) {
		setConnUUID(nodeUUID);
		setConnId(connectionId);
		_fieldConnectorIp = connectorIp;
		_fieldServerPort = serverPort;
	}

	@Override
	public byte[] buildCommand() {
		
		byte[] connectorIpBytes = convertStringToBytes(_fieldConnectorIp);
				
		int length =  connectorIpBytes.length + 4;
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(length);
		
		byte[] result = pr._byteArr;
		int currPos = pr._currPos;
		
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

		ConvertBytesToStringParamPass p;
		
		// connector ip address
		p = convertBytesToString(b, currPos);
		_fieldConnectorIp = p._result;
		currPos = p._currPos;
		
		// server port
		_fieldServerPort = b2i(b, currPos);
		currPos += SERVER_PORT_FIELD_LENGTH;
		
	}

	@Override
	public int getId() {
		return ID;
	}

	@Override
	byte[] getByteId() {
		return _byteCmdId;
	}

	
	public String getFieldConnectorIp() {
		return _fieldConnectorIp;
	}

	public int getFieldServerPort() {
		return _fieldServerPort;
	}

	
	@Override
	CmdMultiplexAbstract instantiateInstance() {
		return new CmdNewConnMultiplex();
	}
	
	@Override
	public String toString() {
		return CmdNewConnMultiplex.class.getSimpleName() + " nodeUUID:"+getCmdConnUUID()+"  connectionID:"+getCmdConnectionId()+"  connectorIp:"+_fieldConnectorIp+"  serverPort:"+_fieldServerPort; 
	}

}
