package com.socketanywhere.nonbreakingnew;

import com.socketanywhere.nonbreakingnew.ConnectionBrain.ThreadState;
import com.socketanywhere.nonbreakingnew.Entry.State;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.ICmdUuidConnId;

public class CBUtil {
	
	static void assertBrainIsConnector(ThreadState state) {
		
		if(!state.isConnectorBrain || !state.brain.isConnectorBrain()) {
			NBLog.severe("Brain is not right type, should be connector");
		}
	}

	static void assertBrainIsConnectee(ThreadState state) {
		if(state.isConnectorBrain || state.brain.isConnectorBrain()) {
			NBLog.severe("Brain is not right type, should be connectee");
		}
	}
	
	static void assertBrainIsConnectee(ConnectionBrain cb) {
		if(cb.isConnectorBrain()) {
			NBLog.severe("Brain is not right type, should be connectee");
		}
	}

	
	static void assertCommandMatchesBrain(ThreadState state, CmdAbstract c, boolean didWeInitiateConnection) {
		
		if(!didWeInitiateConnection) {
			return;
		}

		String connUuid = null;
		
		if(c instanceof ICmdUuidConnId) {
			ICmdUuidConnId cmd = (ICmdUuidConnId)c;
			connUuid = cmd.getFieldNodeUUID();
		}
		

//		int connId = -1;
//		
//		
//		switch(c.getId()) {
//			case CmdAckCloseConn.ID:
//				CmdAckCloseConn c1 = (CmdAckCloseConn)c;
//				connUuid = c1.getFieldNodeUUID();
//				connId = c1.getFieldConnectionId();
//				break;
//			case CmdAckCloseDataRequest.ID:
//				CmdAckCloseDataRequest c2 = (CmdAckCloseDataRequest)c;
//				connUuid = c2.getFieldNodeUUID();
//				connId = c2.getFieldConnectionId();
//				break;
//			case CmdAckDataRequestOnReconnect.ID:
//				CmdAckDataRequestOnReconnect c3 = (CmdAckDataRequestOnReconnect)c;
//				connUuid = c3.getFieldNodeUUID();
//				connId = c3.getFieldConnectionId();
//				break;
//			case CmdAckJoinCloseConn.ID:
//				CmdAckJoinCloseConn c4 = (CmdAckJoinCloseConn)c;
//				connUuid = c4.getFieldNodeUUID();
//				connId = c4.getFieldConnectionId();
//				break;
//			case CmdAckJoinConn.ID:
//				CmdAckJoinConn c5 = (CmdAckJoinConn)c;
//				connUuid = c5.getFieldNodeUUID();
//				connId = c5.getFieldConnectionId();
//				break;
//			case CmdAckNewConn.ID:
//				CmdAckNewConn c6 = (CmdAckNewConn)c;
//				connUuid = c6.getFieldNodeUUID();
//				connId = c6.getFieldConnectionId();
//				break;
//			case CmdAckReadyToCloseConn.ID:
//				CmdAckReadyToCloseConn c7 = (CmdAckReadyToCloseConn)c;
//				connUuid = c7.getFieldNodeUUID();
//				connId = c7.getFieldConnectionId();
//				break;
//			case CmdCloseConn.ID:
//				CmdCloseConn c8 = (CmdCloseConn)c;
//				connUuid = c8.getFieldNodeUUID();
//				connId = c8.getFieldConnectionId();
//				break;
//			case CmdJoinCloseConn.ID:
//				CmdJoinCloseConn c10 = (CmdJoinCloseConn)c;
//				connUuid = c10.getFieldNodeUUID();
//				connId = c10.getFieldConnectionId();
//				break;
//			case CmdJoinConn.ID:
//				CmdJoinConn c11 = (CmdJoinConn)c;
//				connUuid = c11.getFieldNodeUUID();
//				connId = c11.getFieldConnectionId();
//				break;
//			case CmdNewConn.ID:
//				CmdNewConn c12 = (CmdNewConn)c;
//				connUuid = c12.getFieldNodeUUID();
//				connId = c12.getFieldConnectionId();
//				break;
//			case CmdReadyToCloseConn.ID:
//				CmdReadyToCloseConn c13 = (CmdReadyToCloseConn)c;
//				connUuid = c13.getFieldNodeUUID();
//				connId = c13.getFieldConnectionId();
//				break;
//			case CmdReadyToJoin.ID:
//				CmdReadyToJoin c14 = (CmdReadyToJoin)c;
//				connUuid = c14.getFieldNodeUUID();
//				connId = c14.getFieldConnectionId();
//				break;
//		}
		
		if(connUuid == null) {
			return;
		}
		
		if(!state.ourUuid.equals(connUuid)) {
			NBLog.severe("UUID of received command did not match UUID of connector brain brain: "+state.ourUuid + " "+c );
			return;
		}
		
	}
	
	static void assertEntryMatchesBrain(ThreadState state, Entry e) {
		if(e.getTriplet() != null) {
			Triplet t = e.getTriplet();
			if(t.areWeConnector()) {				
				
				if(!t.getConnectorUuid().equals(state.ourUuid)) {
					NBLog.severe("Processing an entry which does not match our brain UUID.");
				}
			}
		}
		
	}
	

	static void assertIsConnector(NBSocket socket, ISocketTLWrapper wrapper) {
		
		boolean failed = false;
		if(socket.isDebugFromServSock()) {
			failed = true;
		}		
		
		if(wrapper != null && wrapper.isFromServSock()) {
			failed = true;
		}
		
		if(failed) {
			NBLog.severe("Attempted to use connectee code for connector.");
			throw new IllegalArgumentException("Attempted to use connectee code for connector.");
		}		
	}
	
	static void assertIsConnectee(NBSocket socket, ISocketTLWrapper wrapper) {
		boolean failed = false;
		if(!socket.isDebugFromServSock()) {
			failed = true;
		}		
		
		if(wrapper != null && !wrapper.isFromServSock()) {
			failed = true;
		}
		
		if(failed) {
			NBLog.severe("Attempted to use connector code for connectee.");
			throw new IllegalArgumentException("Attempted to use connector code for connectee.");
		}
	}

	static void assertInState(Entry e, State[] states) {
		State entryState = e.getState();
		
		for(State st : states) {
			if(entryState == st) {
				return;
			}
		}
		
		NBLog.severe("Entry in invalid state: "+e.getState().name()+ " "+e.getTriplet());		
	}

	static void writeCommandToSocket(CmdAbstract cmd, ISocketTLWrapper innerSock, Entry e) {
		
		if(e.getWrapper() != null && innerSock != null && !e.getWrapper().equals(innerSock)) {
			NBLog.severe("entry socket differs from the socket we are being asked to write to. triplet:"+e.getTriplet()+"  wrapper: "+e.getWrapper().getGlobalId()+ " "+innerSock.getGlobalId()+"  cmd: "+cmd);
		}
		
		// TODO: CURR - This appears to be doing the same thing? 
		if(innerSock == null) {
			e.getWrapper().writeCommand(cmd, e.getTriplet());
		} else {
			innerSock.writeCommand(cmd, e.getTriplet());
		}

	}
	
	
	static void switchToClose(Entry e, ISocketTLWrapper wrapper) {
		e.setState(State.CONN_CLOSING_INIT);

		CmdCloseConn c = new CmdCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
		NBLog.sent(c, e.getTriplet(), e.getWrapper(), NBLog.INTERESTING);
		
		writeCommandToSocket(c, wrapper, e);
	}


}
