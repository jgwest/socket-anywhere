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

// TODO: LOWER - Is it possible to refuse a join connection; should we need to?

public class CmdJoinConn extends CmdAbstractUuidConnId {

	public final static int ID = 5;
	
	private final static byte[] BYTE_CMD_ID = {0x0, 0x5};
	
	public CmdJoinConn(String nodeUUID, int connectionId) {
		super(ID, BYTE_CMD_ID, nodeUUID, connectionId);
	}
	
	public CmdJoinConn() {
		super(ID, BYTE_CMD_ID);
	}
		
	@Override
	CmdAbstract instantiateInstance() {
		return new CmdJoinConn();
	}
	
}
