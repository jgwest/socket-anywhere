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

public class CmdAckCloseDataRequest extends CmdAbstractUuidConnIdInt {

	public final static int ID = 18;
	private final static byte[] BYTE_CMD_ID = {0x0, 0x18};

	public CmdAckCloseDataRequest(int lastPacketSeenByPeer, String nodeUUID, int connId) {
		super(ID, BYTE_CMD_ID, lastPacketSeenByPeer, nodeUUID, connId);
	}

	CmdAckCloseDataRequest() {
		super(ID, BYTE_CMD_ID);
	}
	
	
	@Override
	CmdAbstract instantiateInstance() {
		return new CmdAckCloseDataRequest();
	}

	@Override
	public String toString() {
		return CmdAckCloseDataRequest.class.getSimpleName()+" - final-packet-id-sent-by-sender:"+_fieldIntParam + "  nodeUUID:"+getFieldNodeUUID()+"  connectionID:"+getFieldConnectionId()+" cmd-id: "+_debugId;
	}
	
	/** The last packet that the other side of the connection sent us; eg the final packet of data.*/
	public int getFinalDataPacketIdSentByRemote() {
		return _fieldIntParam;
	}
	
	
}
