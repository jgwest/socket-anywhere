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

public class CmdCloseConnMultiplex extends CmdMultiplexAbstract {
	
	public final static int ID = 4;
		
	private final byte[] _byteCmdId = {0x0, 0x4};
	
	public CmdCloseConnMultiplex() {
		
	}
	
	public CmdCloseConnMultiplex(String nodeUUID, int connectionId) {
		setConnUUID(nodeUUID);
		setConnId(connectionId);
	}

	@Override
	public byte[] buildCommand() {
		
		BuildGenericCmdHeaderParamPass pr = buildGenericCmdHeader(0);
		
		byte[] result = pr._byteArr;
		
		return result;
	}

	@Override
	public void parseCommand(byte[] b) {
		parseHeader(b);
		
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
		return new CmdCloseConnMultiplex();
	}

	
	@Override
	public String toString() {
		return CmdCloseConnMultiplex.class.getSimpleName()+" - nodeUUID:"+getCmdConnUUID()+ "  connectionId:"+getCmdConnectionId();
	}

}
