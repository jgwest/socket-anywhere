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

public class CmdAckReadyToCloseConn extends CmdAbstractUuidConnId {

	public final static int ID = 17;
	private static final byte[] BYTE_CMD_ID = {0x0, 0x17};
	
	public CmdAckReadyToCloseConn(/*int intParam,*/String nodeUUID, int connId) {
		super(ID, BYTE_CMD_ID, nodeUUID, connId);
	}

	CmdAckReadyToCloseConn() {
		super(ID, BYTE_CMD_ID);
	}
	

	@Override
	CmdAbstract instantiateInstance() {
		return new CmdAckReadyToCloseConn();
	}

}
