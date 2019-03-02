/*
	Copyright 2012, 2016 Jonathan West

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

package com.socketanywhere.nonbreakingnew;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.ConnectionBrain.ThreadState;
import com.socketanywhere.nonbreakingnew.Entry.State;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckCloseDataRequest;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckDataRequestOnReconnect;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckJoinCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckJoinConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckNewConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckReadyToCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckReadyToJoin;
import com.socketanywhere.nonbreakingnew.cmd.CmdCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdCloseDataRequestNew;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataReceived;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataRequest;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataRequestOnReconnect;
import com.socketanywhere.nonbreakingnew.cmd.CmdJoinCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdJoinConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdNewConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdReadyToCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdReadyToJoin;
import com.socketanywhere.nonbreakingnew.cmd.ICmdUuidConnId;

import static com.socketanywhere.nonbreakingnew.CBUtil.assertBrainIsConnectee;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertBrainIsConnector;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertCommandMatchesBrain;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertEntryMatchesBrain;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertInState;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertIsConnectee;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertIsConnector;
import static com.socketanywhere.nonbreakingnew.CBUtil.switchToClose;
import static com.socketanywhere.nonbreakingnew.CBUtil.writeCommandToSocket;

public class CBCommandHandler {
	
	private final static long TEN_SECONDS_IN_NANOS =  TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS);

	/** Handle commands that don't require the connection state to be ESTABLISHED. */
	static void handleConnectAgnosticCommands(ThreadState state, ISocketTLWrapper sock, CmdAbstract cmd ) {
		
		ICmdUuidConnId cmdUUidConnId = (ICmdUuidConnId)cmd; 
	
		Entry e = state.getEntryConnInfo(new PairUUIDConnID(cmdUUidConnId.getFieldNodeUUID(), cmdUUidConnId.getFieldConnectionId()));
		if(e != null) {
			if(isEntryNewerThanReceivedSock(e, sock, cmd)) {
				return;
			}			
		}
		
		if(cmd.getId() == CmdJoinConn.ID) {
			// Connectee
			CmdJoinConn cj = (CmdJoinConn)cmd;
			
			if(e == null) {
				NBLog.error("JoinConn - Unable to join - uuid and connection id not found. S "+cj.getFieldNodeUUID()+" "+cj.getFieldConnectionId());
				return;
			}
			assertBrainIsConnectee(state);
			assertIsConnectee(e._nbSock, sock);
			
			NBLog.receivedCmd(cj, e.getTriplet(), sock, NBLog.INTERESTING);
			
			e.setWrapper(sock);
			
			// The existing nbsocket should now receive all data from this socket. 
			state.putNBSocketToWrapper(sock, e._nbSock);

			if(e.getState() == State.CONN_CLOSING_INIT 
					|| e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY
					|| e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY
					|| e.getState() == State.CONN_CLOSED_NEW
					|| e.getState() == State.CONN_DEAD_CLOSING_NEW) {
				
				// If the connection is in the process of being closed, then send 3
				
				CmdAckJoinConn newAckCmd = new CmdAckJoinConn(3, cj.getFieldNodeUUID(), cj.getFieldConnectionId());
				writeCommandToSocket(newAckCmd, sock, e);
		
				NBLog.sent(newAckCmd, e.getTriplet(), sock, NBLog.INTERESTING);
				
			} else {
				e.setState(State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2);
				
				CmdAckJoinConn newAckCmd = new CmdAckJoinConn(1, cj.getFieldNodeUUID(), cj.getFieldConnectionId());
				
				writeCommandToSocket(newAckCmd, sock, e);
				
				NBLog.sent(newAckCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			}
			
		}  else if(cmd.getId() == CmdAckCloseConn.ID) {
			// Connectee/Connector 
			
			// Ack of CmdCloseConn
			// 
			// Received by "closee"
			CmdAckCloseConn a = (CmdAckCloseConn)cmd;
			
			// Get the entry for the connection specified in the command
			if(e == null) {
				NBLog.error("Error: received ACK close cmd for a connection that couldn't be found.");
				return;
			}
			
			if(e.getState() == State.CONN_DEAD_CLOSING_NEW || e.getState() == State.CONN_DEAD) {
				NBLog.debug("Skipping command due to dead conn"+cmd, NBLog.INTERESTING);
				return;
			}
				
			if(e.getState() != State.CONN_CLOSING_INIT) {
				NBLog.severe("Invalid state at ack close: "+e.getState()+" "+e.getTriplet());
				return;
			}
			
			assertCommandMatchesBrain(state, a, e.getTriplet().areWeConnector());
			assertEntryMatchesBrain(state, e);
			
			NBLog.receivedCmd(a, e.getTriplet(), sock, NBLog.INTERESTING);

			CmdCloseDataRequestNew datReq = new CmdCloseDataRequestNew(e._lastPacketIdReceived+1);
			
			writeCommandToSocket(datReq, sock, e);
			
			NBLog.sent(datReq, e.getTriplet(), sock, NBLog.INTERESTING);
			
		} else if(cmd.getId() == CmdNewConn.ID) {
			
			assertBrainIsConnectee(state);
			
			// Connectee
			final CmdNewConn c = (CmdNewConn)cmd;
			
			if( e == null) {
				Triplet t = new Triplet(c.getFieldNodeUUID(),c.getFieldConnectionId(), false);
				NBSocket newSocket = new NBSocket(t, state.brain, state.options, true);
				newSocket.setRemoteAddr(sock.getAddress());
				newSocket.serverInit();
				
				e = new Entry();
				
				// Add the entry to our maps
				state.putNBSocketToEntry(newSocket, e);
				
				// Populate the entry
				e._nbSock = newSocket;
				e.setState(State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2);
				e.setTriplet(t);
				
//				e._uuidOfConnector = c.getFieldNodeUUID();
//				e._connId = c.getFieldConnectionId();
//				e._didWeInitConnection = false;
				
				e._lastPacketIdReceived = -1;
				e._lastPacketIdSent = -1;
				e._port = c.getFieldServerPort();
				e._timeSinceLastPacketIdAcknowledgedInNanos = System.nanoTime();
				e.setWrapper(sock);
				
				state.putEntryConnInfo(new PairUUIDConnID(t), e);
				state.putNBSocketToWrapper(sock, newSocket);
				
			} else {
				// This occurs when NewConn is received by the connectee, but the connector never receives the response.
				
				assertInState(e, new State[] { State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2, State.CONN_DEAD  } );
				assertIsConnectee(e._nbSock, sock);
				
				e._nbSock.setRemoteAddr(sock.getAddress());
				e._nbSock.serverInit();

				e.setState(State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2);
				
				e.setWrapper(sock);
				state.putNBSocketToWrapper(sock, e._nbSock);
			}
			
			NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);

			CmdAckNewConn newAckCmd = new CmdAckNewConn(1, e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
			
			writeCommandToSocket(newAckCmd, sock, e);
			
			NBLog.sent(newAckCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
						
			return;
		} else if(cmd.getId() == CmdCloseConn.ID) {
			CmdCloseConn c = (CmdCloseConn)cmd;
			
			// Command is received by the "closee"
			
			if(e == null) {
				NBLog.error("Error: received close cmd for a connection that couldn't be found.");
				return;
			}
			
			NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);

			if(!(e.getState() == State.CONN_CLOSING_INIT)) {
				// We have received a close cmd, so prevent the socket from allowing any more data to be sent
				NBLog.connectionClosed(e.getTriplet());
				
				e.assertNoWaitingDataPacket();
				
				e.setState(State.CONN_CLOSING_INIT);				
			}
			
			CmdAckCloseConn ackCmd = new CmdAckCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
			writeCommandToSocket(ackCmd, sock, e);
			NBLog.sent(ackCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			
			CmdCloseDataRequestNew cmdCloseRequest = new CmdCloseDataRequestNew(e._lastPacketIdReceived+1);
			writeCommandToSocket(cmdCloseRequest, sock, e);
			NBLog.sent(cmdCloseRequest, e.getTriplet(), sock, NBLog.INTERESTING);
			
						
			// end handle CmdCloseConn
		} else if(cmd.getId() == CmdJoinCloseConn.ID) {

			// Connectee
			CmdJoinCloseConn cj = (CmdJoinCloseConn)cmd;
			
			assertBrainIsConnectee(state);
			
			if(e == null) {
				NBLog.error("JoinCloseConn - Unable to join - uuid and connection id not found. S uuid: " + cj.getFieldNodeUUID()+" "+cj.getFieldConnectionId());
				return;
			}
						
			NBLog.receivedCmd(cj, e.getTriplet(), sock, NBLog.INTERESTING);
			
			e.setWrapper(sock);
			
			assertIsConnectee(e._nbSock, sock);

//			assertInState(e, new State[] { State.CONN_DEAD, State.CONN_DEAD_CLOSING_NEW, State.CONN_CLOSED_NEW  } );

			// The existing nbsocket should now receive all data from this socket. 
			state.putNBSocketToWrapper(sock, e._nbSock);
			
			e.setState(State.CONN_CLOSING_INIT);
			CmdAckJoinCloseConn newAckCmd = new CmdAckJoinCloseConn(1, cj.getFieldNodeUUID(), cj.getFieldConnectionId());
			
			writeCommandToSocket(newAckCmd, sock, e);

			NBLog.sent(newAckCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			e.transferWaitingToSent();
			
		}  else if(cmd.getId() == CmdAckJoinCloseConn.ID) {
			CmdAckJoinCloseConn ackCmd = (CmdAckJoinCloseConn)cmd;
			
			assertBrainIsConnector(state);
			
			if(e == null) {
				NBLog.error("Ack - JoinCloseConn - Unable to join - uuid and connection id not found: C "+ackCmd.getFieldNodeUUID()+ " "+ackCmd.getFieldConnectionId());
				return;
			}
			
			if(e.getState() == State.CONN_DEAD || e.getState() == State.CONN_DEAD_CLOSING_NEW) {
				NBLog.debug("Skipping command due to dead conn: " +ackCmd, NBLog.INFO);
				return;
			}
			
			assertEntryMatchesBrain(state, e);
			assertCommandMatchesBrain(state, ackCmd, e.getTriplet().areWeConnector());
			
			NBLog.receivedCmd(ackCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			assertIsConnector(e._nbSock, sock);
			assertInState(e, new State[] { State.CONN_CLOSING_INIT } );
						
			e.setWrapper(sock);

			// The existing nbsocket should now receive all data from this socket. 
			state.putNBSocketToWrapper(sock, e._nbSock);

			switchToClose(e, sock);
			
//			CmdCloseConn closeConn = new CmdCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());			
//			queueCommand(state, closeConn, e, null, sock);
			
		} else {
			NBLog.severe("Waiting for join, new conn or close, but received this:"+cmd.toString() );
		}
		
		return;
				
	}

	/** Connectee */
	private static void eventCommandReceivedConnectee(ThreadState state, ISocketTLWrapper sock, CmdAbstract cmd, Entry e) {

		assertBrainIsConnectee(state);
		
		if(!sock.isFromServSock()) {
			NBLog.error("eventCommandReceivedConnectee called on non server socket");
			throw new IllegalArgumentException("eventCommandReceivedConnectee called on non server socket");
		}
		
		if(e.getState() == State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2) {
			
			if(cmd.getId() == CmdAckNewConn.ID) {
				// Connectee
				CmdAckNewConn c = (CmdAckNewConn)cmd;
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				
				if(c.getFieldIntParam() == 2) {
					
					e.setState(State.CONN_ESTABLISHED);
					e.transferWaitingToSent(sock, true);
					
					NBLog.connectionCreated(e.getTriplet());
					
					// Locate the server socket which corresponds to this address
					NBServerSocketListener ssl = state.servSockListenerMap.get(new TLAddress(e._port));
					
					if(ssl == null) {
						NBLog.error("Unable to locate server socket listener for new connection.");
						return;
					}
					
					if(e._connecteeReturnedInServerSocketAccept == false) {
						// Pass the new socket to the server socket listener for that address
						ssl.informNewSocketAvailable(e._nbSock);
						e._connecteeReturnedInServerSocketAccept = true;
					}
					
				} else {
					NBLog.error("New conn received, waiting for ack2; but cmd didn't match:"+cmd);
				}
			} else {
				NBLog.error("New conn received, waiting for ack2; but received this cmd instead:"+cmd);
			}

			return;
		
		} else if(e.getState() == State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2) {
			
			if(cmd.getId() == CmdAckJoinConn.ID) {
				// Connectee
				
				CmdAckJoinConn c = (CmdAckJoinConn)cmd;
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				if(c.getFieldIntParam() == 2) {
					
					CmdDataRequestOnReconnect cdr = new CmdDataRequestOnReconnect(e._lastPacketIdReceived+1);
					
					writeCommandToSocket(cdr, sock, e);
					
					NBLog.sent(cdr, e.getTriplet(), sock, NBLog.INTERESTING);
					
					e.setState(State.CONNECTEE_JOIN_SENT_DATA_REQUEST_WAITING_FOR_ACK);
					
				} else {
					NBLog.severe("Join received, waiting for ack; got ack, but didn't match: "+cmd+ " "+e.getTriplet());
				}
			} else {
				NBLog.severe("Join received, waiting for ack; but got this cmd instead:"+cmd+ " "+e.getTriplet());
			}
			
			return;
		} else {
			NBLog.severe("Unrecognized state: "+cmd+" "+e.getTriplet() );
		}
		
		return;
	}
	
	/** Connector */
	private static void eventCommandReceivedConnector(ISocketTLWrapper sock, CmdAbstract cmd, Entry e, ThreadState state) {

		assertBrainIsConnector(state);

		assertCommandMatchesBrain(state, cmd, true);
		assertEntryMatchesBrain(state, e);
		
		if(sock.isFromServSock()) {
			NBLog.error("eventCommandReceivedConnectee called on server-socket's socket");
			throw new IllegalArgumentException("eventCommandReceivedConnectee called on server-socket's socket");
		}

		if(e.getState() == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK) {
			
			if(cmd.getId() == CmdAckNewConn.ID) {
				// Here Connector receives Ack of CmdNewConn

				// This command may be sent by the other host multiple times, so we must handle that appropriately here.
				// Our handler's implementation below is therefore idempotent.
				
				CmdAckNewConn c = (CmdAckNewConn)cmd;
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				if(c.getFieldIntParam() == 1) {
					
					boolean isFirstTime = false;
					
					CmdAckNewConn newAckCmd = new CmdAckNewConn(2, e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
					if(e.getState() != State.CONN_ESTABLISHED) {
						e.setState(State.CONN_ESTABLISHED);
						
						NBLog.connectionCreated(e.getTriplet());
						isFirstTime = true;
					}
					
					writeCommandToSocket(newAckCmd, sock, e);
					NBLog.sent(newAckCmd, e.getTriplet(), sock, NBLog.INTERESTING);
					
					if(isFirstTime) {
						e.transferWaitingToSent(sock, true);
						onConnectionEstablishedCheckIfClosing(e, sock);
					}
					

				} else {
					NBLog.error("New conn sent and waiting for ack1, but received ack1 but didn't match:"+cmd.toString());
				}
				
			} else {
				NBLog.error("New conn sent and waiting for ack1, but instead received this:"+cmd.toString());
			}
			
			return;
			
		} else if(e.getState() == State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK) {
			
			if(cmd.getId() == CmdAckJoinConn.ID) {
				// Connector
				CmdAckJoinConn c = (CmdAckJoinConn)cmd;
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				if(c.getFieldIntParam() == 1) {

					CmdAckJoinConn newAckCmd = new CmdAckJoinConn(2, e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());

					e.setState(State.CONNECTOR_JOIN_WAITING_FOR_DATA_REQUEST);
					e.setWrapper(sock);

					writeCommandToSocket(newAckCmd, sock, e);
					NBLog.sent(newAckCmd, e.getTriplet(), sock, NBLog.INTERESTING);

				} else if(c.getFieldIntParam() == 3) {
					// The remote peer is kicking us into close mode

					if(e.getState() != State.CONN_CLOSING_INIT) {
//						e._nbSock.newSocketEstablishedFromRecoveryThread(sock);
//						state.brain.getInterface().sendResetRecoveryThread(e._nbSock);
					}
					switchToClose(e, sock);
					
					e.transferWaitingToSent();
					
//					CmdCloseConn close = new CmdCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
//					writeCommandToSocket(close, e._nbSock, sock, e, null, state);
					

				} else {
					NBLog.error("Join sent, waiting for ack1; instead received this cmd:"+cmd);
				}
				
			} else {
				NBLog.error("Join sent, waiting for ack1; instead received this non-ack cmd: "+cmd);
			}
			
			return;
			
		} else {
			NBLog.severe("Unrecognized state in eventCommandReceivedConnector: "+e.getState());
			
		}
	}
	
	private static void handleCloseCommands(ThreadState state, ISocketTLWrapper sock, CmdAbstract cmd, Entry e) {
		
		assertEntryMatchesBrain(state, e);
		assertCommandMatchesBrain(state, cmd, e.getTriplet().areWeConnector());
		
		if(cmd.getId() == CmdCloseDataRequestNew.ID) {
		
			NBLog.receivedCmd((CmdCloseDataRequestNew)cmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			assertInState(e, new State[] { State.CONN_CLOSING_INIT} );
			
			int firstPacketToSend = ((CmdCloseDataRequestNew)cmd).getFieldFirstPacketToResend();
			
			// Acknowledge the request, and inform them of the total number of packets that will be sent
			CmdAckCloseDataRequest ack = new CmdAckCloseDataRequest( e._lastPacketIdSent, e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
			writeCommandToSocket(ack, sock, e);
			
//			sock.writeCommand(ack, e.getTriplet());		
			NBLog.sent(ack, e.getTriplet(), sock, NBLog.INTERESTING);
			
			// Ensure the data is in the correct order
			
			List<SentDataPacket> sdp = e.getNewPacketsAndRemoveOld(firstPacketToSend-1, state);
			for(SentDataPacket p : sdp) {
				writeCommandToSocket(p.getDataCmd(), sock, e);
				NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);											
			}
			
//			Collections.sort(e._sentDataPackets);
//			
//			for(SentDataPacket p : e._sentDataPackets) {
//				
//				if(p.getDataCmd().getFieldPacketId() >= firstPacketToSend) {
//					writeCommandToSocket(p.getDataCmd(), sock, e);
//					NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);							
//				}
//			}
			
//			// Send our own close data request, but only if we have not already sent one
//			if(e._socketOfLastCloseDataRequestSent == null || !e._socketOfLastCloseDataRequestSent.equals(sock)) {
//
//				e._socketOfLastCloseDataRequestSent = sock;
//
//				CmdCloseDataRequest datReq = new CmdCloseDataRequest(e._lastPacketIdReceived+1);
//				writeCommandToSocket(datReq, e._nbSock, sock, e, null, state);
//
//			}

		} else if(cmd.getId() == CmdAckCloseDataRequest.ID  ) {
			
			CmdAckCloseDataRequest ackCmd = (CmdAckCloseDataRequest)cmd;
			
			NBLog.receivedCmd(ackCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			int finalDataPacketIdSentByRemote = ackCmd.getFinalDataPacketIdSentByRemote();
	
			if(e.getTriplet().areWeConnector()) {
				
				assertInState(e, new State[] { State.CONN_CLOSING_INIT} );
				// Connector
				e._closeConnWaitingForLastPacketIdNew = finalDataPacketIdSentByRemote;
				
				NBLog.debug("connector: Setting waitforLastPacketId to "+ finalDataPacketIdSentByRemote+" "+e.getTriplet(), NBLog.INFO);
			} else {
				
				// Connectee

				assertInState(e, new State[] { State.CONN_CLOSING_INIT, State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY} );
				
				if(finalDataPacketIdSentByRemote == e._lastPacketIdReceived) {
					// We have received all the data, so communicate ready to close
					
					if(e.getState() == State.CONN_CLOSING_INIT) {
						e.setState(State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY);
						e._isInputPipeClosed = true;

						NBLog.debug("Sending ready to close from handleCloseCommands, finalDataPacketIdSentByRemote: "+finalDataPacketIdSentByRemote+"  "+e.getTriplet(), NBLog.INFO);
						
						CmdReadyToCloseConn readyToCloseCmd = new CmdReadyToCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
						writeCommandToSocket(readyToCloseCmd, sock, e);
						NBLog.sent(readyToCloseCmd, e.getTriplet(), sock, NBLog.INTERESTING);
					} else {
						/* we're already in the above state so nothing to do.*/
					}
					
					
//					if(e._socketOfLastCloseDataRequestSent == null || !e._socketOfLastCloseDataRequestSent.equals(sock)) {
//						CmdReadyToCloseConn readyToCloseCmd = new CmdReadyToCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
//						writeCommandToSocket(readyToCloseCmd, e._nbSock, sock, e, null, state);
//						e._socketOfLastReadyForCloseSent = sock;					
//					}
					

				} else if(finalDataPacketIdSentByRemote > e._lastPacketIdReceived) {
					NBLog.debug("connectee: Setting waitforLastPacketId to "+ finalDataPacketIdSentByRemote+" "+e.getTriplet(), NBLog.INFO);
					// We are missing data
					e._closeConnWaitingForLastPacketIdNew = finalDataPacketIdSentByRemote;
					
				} else {
					NBLog.severe("The remote peer should never claim to have sent us less than we have received. "+e.getTriplet());
					return;
				}

				
			}
			
			
		} else if(cmd.getId() == CmdReadyToCloseConn.ID) {
			CmdReadyToCloseConn readyCloseCmd = (CmdReadyToCloseConn)cmd;
			NBLog.receivedCmd(readyCloseCmd, e.getTriplet(), sock, NBLog.INTERESTING);

			if(e.getTriplet().areWeConnector()) {
				assertInState(e, new State[] { State.CONN_CLOSING_INIT} );				
			} else {
				assertInState(e, new State[] { State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY} );
			}
			
			CmdAckReadyToCloseConn ack = new CmdAckReadyToCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
			writeCommandToSocket(ack, sock, e);
			
			NBLog.sent(ack, e.getTriplet(), sock, NBLog.INTERESTING);
			
			if(e.getTriplet().areWeConnector()) {

				e._socketOfConnectorIfSeenConnecteeReadyToClose = sock;
				
				// If we have received all the data we needed (and the connectee is now sending us ready to close)...
				if(e._closeConnWaitingForLastPacketIdNew == e._lastPacketIdReceived) {
					
					NBLog.debug("connector has received final remote date packet: "+e._closeConnWaitingForLastPacketIdNew + " "+e.getTriplet(), NBLog.INFO);
					
					e._isInputPipeClosed = true;
					
					// 
					CmdReadyToCloseConn rc = new CmdReadyToCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
					writeCommandToSocket(rc, sock, e);
					NBLog.sent(rc, e.getTriplet(), sock, NBLog.INTERESTING);
					
//					e.assertNoAnyDataPacket();
					e.setState(State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY);
					e.clearSentPackets(state);
				
				} else {
					NBLog.debug("connector has not received final remote date packet: final: "+e._lastPacketIdReceived+"  "+e._closeConnWaitingForLastPacketIdNew + " "+e.getTriplet(), NBLog.INFO);
					// otherwise, we are still waiting for data, and will send ready once it is received.
				}
				
			} else {
				
				// We (connectee) have received ready from connector, so we are good to go.
				e.setState(State.CONN_CLOSED_NEW);
				
				CmdCloseThreadNew closeThread = new CmdCloseThreadNew(sock, e._nbSock, e.getTriplet(), state.brain);
				closeThread.start();
				
//				e.assertNoAnyDataPacket();
				e.clearSentPackets(state);
				
			}
			
		} else if(cmd.getId() == CmdAckReadyToCloseConn.ID) {

			CmdAckReadyToCloseConn ackCmd = (CmdAckReadyToCloseConn)cmd;
			NBLog.receivedCmd(ackCmd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			if(e.getTriplet().areWeConnector()) {
				// Connector
				assertInState(e, new State[] { State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY});
				
//				e.assertNoAnyDataPacket();
				e.setState(State.CONN_CLOSED_NEW);
				
				// Connector has received ack, which is the last step in the close command chain, so close.
				e.getWrapper().close(false);
				
			} else {
				// Connectee				
				assertInState(e, new State[] { State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY});
				
				// Ignore: the connectee is waiting for the ready to close from the connector.
			}
			
		} else {
			
			NBLog.error("Invalid command received in handleCloseCommands: "+cmd+" "+e.getTriplet());
			
			return;
			
		}
		
		
			
	}
	
	/** The central entry point for command processing, called by CB thread */
	static void handleEventCommandReceived(ThreadState state, ISocketTLWrapper sock, CmdAbstract cmd) {
		
		// The "pre-connect" commands are handled elsewhere
		if(cmd.getId() == CmdNewConn.ID || cmd.getId() == CmdJoinConn.ID || cmd.getId() == CmdJoinCloseConn.ID || cmd.getId() == CmdAckJoinCloseConn.ID) {
			CBCommandHandler.handleConnectAgnosticCommands(state, sock, cmd);
			return;
		}

		// After this point, an entry and an NBSocketImpl for this connection should exist in the maps
		
		NBSocket s = state.getNBSocketFromWrapper(sock);
		if(s == null) {
			NBLog.error("Unable to locate an NBSocketImpl for a given socket in the map. cmd:"+cmd);
			return;
		}
				
		Entry e = state.getEntryFromNBSocket(s);
		if(e == null) {
			NBLog.error("Unable to locate an entry for a given NBSocket in the map.");
			return;
		}
		
		if(e.getState() == State.CONN_DEAD || e.getState() == State.CONN_DEAD_CLOSING_NEW) {
			NBLog.debug("Connection is dead, so we are skipping this command: "+cmd+" "+e.getTriplet(), NBLog.INFO);
			return;
		}
		
		if(isEntryNewerThanReceivedSock(e, sock, cmd)) {
			return;
		}
		
		if(cmd.getId() == CmdCloseConn.ID) {
			CBCommandHandler.handleConnectAgnosticCommands(state, sock, cmd);
			return;
		}
		
		if(cmd.getId() == CmdAckCloseConn.ID) {
			CBCommandHandler.handleConnectAgnosticCommands(state, sock, cmd);
			return;
		}
		
		
		if(cmd.getId() == CmdCloseDataRequestNew.ID 
				|| cmd.getId() == CmdAckCloseDataRequest.ID 
				|| cmd.getId() == CmdAckReadyToCloseConn.ID
				|| cmd.getId() == CmdReadyToCloseConn.ID
			) {
			handleCloseCommands(state, sock, cmd, e);
			return; 
		}
		
		// Connectee States
		if(	e.getState() == State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2 
			|| 	e.getState() == State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2) {
			
			eventCommandReceivedConnectee(state, sock, cmd, e);
			return;
		}
		
		// Connector States
		if(e.getState() == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK 
			|| e.getState() == State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK) {
			eventCommandReceivedConnector(sock, cmd, e, state);
			return;
		}
		
		// Everything after this point is for either connector or connectee
		
		if(e.isInState(State.CONN_ESTABLISHED, State.CONN_CLOSING_INIT, State.CONNECTOR_JOIN_WAITING_FOR_READY_TO_JOIN, State.CONNECTEE_JOIN_SENT_DATA_REQUEST_WAITING_FOR_ACK )) {
			
			if(cmd.getId() == CmdData.ID) {
				
				handleCmdData(state, cmd, e, s, sock);
		
				return;
			} else if(cmd.getId() == CmdDataReceived.ID) { 
				// Acknowledging receipt of data that we have previously sent
				
				CmdDataReceived c = (CmdDataReceived)cmd;
				
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INFO);
				
				// Remove old
				e.getNewPacketsAndRemoveOld(c.getFieldLastPacketReceived(), state);
				
				// Remove any packets from our packet list that the remote connection 
				// has acknowledged that it received
//				for(Iterator<SentDataPacket> i = e._sentDataPackets.iterator(); i.hasNext();) {
//					SentDataPacket p = i.next();
//					if(p.getDataCmd().getFieldPacketId() <= c.getFieldLastPacketReceived()) {
//						i.remove();
//					}
//				}
				
//				e._lastPacketIdAckReceived = c.getFieldLastPacketReceived();
				
				NBLog.dataAckReceived(c, e.getTriplet());
				
				return;
			} else if(e.getState() == State.CONN_CLOSING_INIT ){
				NBLog.severe("Unknown Command: "+cmd+ " "+e.getTriplet() + " "+e.getState());
				// TODO: CURR - Figure out what other states need to be included here.
			}			
		}

		// TODO: CURR - Add boolean commandHandled to this thing, and remove all the error cases 

		if(e.getState() == State.CONNECTOR_JOIN_WAITING_FOR_DATA_REQUEST) {
			if(cmd.getId() == CmdDataRequestOnReconnect.ID) {
				
				assertBrainIsConnector(state);
				assertEntryMatchesBrain(state, e);
				
				// Received by Connector
				CmdDataRequestOnReconnect c = (CmdDataRequestOnReconnect)cmd;
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				
				// Ensure they are in the correct order
				
				List<SentDataPacket> sdp = e.getNewPacketsAndRemoveOld(c.getFieldFirstPacketToResend()-1, state);
				
				for(SentDataPacket p : sdp) {
					writeCommandToSocket(p.getDataCmd(), sock, e);
					NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);
				}
				
				
//				Collections.sort(e._sentDataPackets);
//				
//				for(Iterator<SentDataPacket> i = e._sentDataPackets.iterator(); i.hasNext();) {
//					SentDataPacket p = i.next();
//					
//					if(p.getDataCmd().getFieldPacketId() >= c.getFieldFirstPacketToResend()) {
//
//						writeCommandToSocket(p.getDataCmd(), sock, e);
//						NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);
//					} else {
//						i.remove();
//					}
//				}

				// Having fulfilled the data request of the remote connection, send our own data request
				// as an ack, with the param of the last packet id we received
				CmdAckDataRequestOnReconnect sendCmd = new CmdAckDataRequestOnReconnect(e._lastPacketIdReceived+1, e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
				
				writeCommandToSocket(sendCmd, sock, e);
				NBLog.sent(sendCmd, e.getTriplet(), sock, NBLog.INTERESTING);
				
				e.setState(State.CONNECTOR_JOIN_WAITING_FOR_READY_TO_JOIN);
				
			} else{
				NBLog.severe("Invalid command for this state: " + e.getState()+ " "+cmd);
			}
			return;
		}
		
		if(e.getState() == State.CONNECTEE_JOIN_SENT_DATA_REQUEST_WAITING_FOR_ACK) { 
			if(cmd.getId() == CmdAckDataRequestOnReconnect.ID) {
				CmdAckDataRequestOnReconnect c = ((CmdAckDataRequestOnReconnect)cmd);
				
				assertBrainIsConnectee(state);
				
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				
				// Ensure they are in the correct order
				
				List<SentDataPacket> sdp = e.getNewPacketsAndRemoveOld(c.getFirstPacketIdReqToSend()-1, state);
				
				for(SentDataPacket p : sdp) {
					writeCommandToSocket(p.getDataCmd(), sock, e);
					NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);					
				}
				
//				Collections.sort(e._sentDataPackets);
//				
//				for(Iterator<SentDataPacket> i = e._sentDataPackets.iterator(); i.hasNext();) {
//					SentDataPacket p = i.next();
//
//					if(p.getDataCmd().getFieldPacketId() >= c.getFirstPacketIdReqToSend()) {
//
//						writeCommandToSocket(p.getDataCmd(), sock, e);
//						NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);
//					} else {
//						i.remove();
//					}
//				}
				
				CmdReadyToJoin rtj = new CmdReadyToJoin(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
				
				NBLog.sent(rtj, e.getTriplet(),sock, NBLog.INTERESTING);
				writeCommandToSocket(rtj, sock, e);

			} else if(cmd.getId() == CmdAckReadyToJoin.ID) {
				
				assertBrainIsConnectee(state);

				CmdAckReadyToJoin cartj = (CmdAckReadyToJoin)cmd;
				
				NBLog.receivedCmd(cartj, e.getTriplet(), sock, NBLog.INTERESTING);
				e.setState(State.CONN_ESTABLISHED);
				
				// We have synchronized our packets and may now clear them
				e.clearSentPackets(state);
				
				e.transferWaitingToSent(sock, true);

				// Return through nbserversocket.accept(...) if necessary.
				if(e._connecteeReturnedInServerSocketAccept == false) {
					NBServerSocketListener ssl = state.servSockListenerMap.get(new TLAddress(e._port));
					
					// Pass the new socket to the server socket listener for that address
					ssl.informNewSocketAvailable(e._nbSock);
					e._connecteeReturnedInServerSocketAccept = true;
				}

				onConnectionEstablishedCheckIfClosing(e, sock);
			
			} else{
				NBLog.severe("Invalid command for this state: " + e.getState()+ " "+cmd);
			}
			
			return;

		}
		
		if(e.getState() == State.CONNECTOR_JOIN_WAITING_FOR_READY_TO_JOIN) {
			
			if(cmd.getId() == CmdReadyToJoin.ID) {
				assertBrainIsConnector(state);
				assertCommandMatchesBrain(state, cmd, true);
				assertEntryMatchesBrain(state, e);
				
				// At this point we have necessarily sent all the packets that we have
				e.clearSentPackets(state);
				
				CmdReadyToJoin rtj = (CmdReadyToJoin)cmd;
				
				NBLog.receivedCmd(rtj, e.getTriplet(), sock, NBLog.INTERESTING);
				e.setState(State.CONN_ESTABLISHED);
				
				CmdAckReadyToJoin artj = new CmdAckReadyToJoin(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
				NBLog.sent(artj, e.getTriplet(), sock, NBLog.INTERESTING);
				
				writeCommandToSocket(artj, sock, e);
				
				e.transferWaitingToSent(sock, true);
				
				onConnectionEstablishedCheckIfClosing(e, sock);
				
			} else{
				NBLog.severe("Invalid command for this state: " + e.getState()+ " "+cmd);
			}
			
			return;
		}
		
		
		if(e.getState() == State.CONN_ESTABLISHED) {
				
			if(cmd.getId() == CmdDataRequest.ID) {
				CmdDataRequest c = (CmdDataRequest)cmd;
				
				NBLog.receivedCmd(c, e.getTriplet(), sock, NBLog.INTERESTING);
				
				// Ensure they are in the correct order
//				Collections.sort(e._sentDataPackets);
				
				NBLog.dataRequested(c, e.getTriplet());
				
				List<SentDataPacket> sdp = e.getNewPacketsAndRemoveOld(c.getFieldFirstPacketToResend()-1, state);
				
				for(SentDataPacket p : sdp) {
					writeCommandToSocket(p.getDataCmd(), sock, e);
					NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);					
				}
				
				
//				for(SentDataPacket p : e._sentDataPackets) {
//					
//					if(p.getDataCmd().getFieldPacketId() >= c.getFieldFirstPacketToResend()) {
//
//						writeCommandToSocket(p.getDataCmd(), sock, e);
//						NBLog.sent(p.getDataCmd(), e.getTriplet(), sock, NBLog.INFO);
//											
//					}
//				}
//				
			} else {
				NBLog.severe("Unknown Command: "+cmd+ " "+e.getTriplet() + " "+e.getState());
			}
			
			return;
			
		} else if(e.getState() == State.CONN_DEAD) {
			NBLog.debug("Received command on connection that is CONN_DEAD. Cmd:"+cmd+" "+e.getTriplet(), NBLog.INTERESTING);
			return;
		} else if(e.getState() == State.CONN_CLOSING_INIT && cmd.getId() == CmdDataRequestOnReconnect.ID) {
			NBLog.debug("Ignoring command received on conn closing init: "+e.getTriplet()+" "+cmd, NBLog.INTERESTING);
			return;
		} else {
			NBLog.severe("Unrecognized state: "+e.getState() + " "+cmd+ " "+e.getTriplet());
		}
		
	}
	
	/** Connector/Connectee 
	 * 
	 * This will be called in both CONN_ESTABLISHED and CONN_DEAD
	 * */
	private static void handleCmdData(ThreadState state, CmdAbstract cmd, Entry e, NBSocket s, ISocketTLWrapper sock) {
		// Data sent to us
		CmdData dataCmd = (CmdData)cmd;

		assertEntryMatchesBrain(state, e);
		
		NBLog.receivedCmd(dataCmd, e.getTriplet(), sock, NBLog.INFO);
		
		// Don't update the entry if we missed data (eg if there is a gap in the numbering of data packets)
		if(dataCmd.getFieldPacketId() - e._lastPacketIdReceived > 1) {
//			NBLog.debug("Gap in the numbering of packets, rerequesting data", NBLog.INTERESTING);
			NBLog.error("Gap in the numbering of packets, rerequesting data: packet-received "+dataCmd.getFieldPacketId() + " last packet received: "+e._lastPacketIdReceived+" "+e.getTriplet());
	
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			// TODO: Is this the issue?
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			CmdDataRequest cd = new CmdDataRequest(e._lastPacketIdReceived+1);

			writeCommandToSocket(cd, sock, e);
			
			NBLog.sent(cd, e.getTriplet(), sock, NBLog.INTERESTING);
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			
			

			// Don't proceed any farther; we don't want to receive out of order data.
			return;
			
		}
		
		if(dataCmd.getFieldPacketId() <= e._lastPacketIdReceived) {
			NBLog.debug("Received a data packet that we have already previously received/processed. Ignoring. "+e.getTriplet(), NBLog.INFO);
			// Ignore the duplicate data case;
			return;
		}
		
		e._lastPacketIdReceived = dataCmd.getFieldPacketId();
		e._dataReceivedInBytes += dataCmd.getFieldDataLength();
//		e._timeLastDataReceived = System.currentTimeMillis();

		
		e._dataReceivedSinceLastPacketIdAcknowledged += dataCmd.getFieldDataLength();
		
		// TODO: LOWER - If this gets too full it will BLOOOOOOOOOOOOOOOOOOOOOOOOOOOOCKKKKK
		s.eventReceivedDataCmdFromRemoteConn(dataCmd);

		NBLog.dataReceived(dataCmd, e.getTriplet());
		
//		Mapper.getInstance().putIntoList(e.getTriplet(), dataCmd);
//		Mapper.getInstance().put(dataCmd, false);

		
		// Send data received command only if certain threshold is met
		if(e.getState() == State.CONN_ESTABLISHED &&  
				( e._dataReceivedSinceLastPacketIdAcknowledged >= 1024 /** 1024 * 10*/ || System.nanoTime() - e._timeSinceLastPacketIdAcknowledgedInNanos > TEN_SECONDS_IN_NANOS)
				// TODO: CURR - Only 1KB?
			) {
			// 10 seconds or 1KB
			
			NBSocketInputStream nis = s.internalGetInputStream();
			
			boolean isInputStreamBufferFull = false;
			if(state.options.getMaxDataReceivedBuffer() != -1) { // if there is a buffer limit...
				int bufferSize = nis.internalGetContentsSize();
				if(bufferSize > state.options.getMaxDataReceivedBuffer()) { // .. and we're over it...
					isInputStreamBufferFull = true;
				}
			}
			
			// Only send CmdDataReceived if our buffer is not full (this prevents the other side from sending us too much data)
			if(!isInputStreamBufferFull) {
				CmdDataReceived dr = new CmdDataReceived(dataCmd.getFieldPacketId());
	
				e._dataReceivedSinceLastPacketIdAcknowledged = 0;
				e._timeSinceLastPacketIdAcknowledgedInNanos = System.nanoTime();
				e._lastPacketIdAcknowledged = dataCmd.getFieldPacketId();
	
				writeCommandToSocket(dr, sock, e);
				NBLog.sent(dr, e.getTriplet(), sock, NBLog.INFO);
			}

		}
		
		if(e.getState() == State.CONN_CLOSING_INIT) {
			// If we have received that last data packet that we were waiting for, and we are the connectee
			if(e._closeConnWaitingForLastPacketIdNew == dataCmd.getFieldPacketId()) {
				
				 if(e.getTriplet().areWeConnector()) {
					 // Connector
					 // Note: All data received at this point is new; because we exclude duplicate or unwanted data at the top of this method.
					 // Thus this branch will only run once.
					 
					 // If we have seen ready to close from the other side (and by being here, we now have all our data)
					 if(e._socketOfConnectorIfSeenConnecteeReadyToClose != null && e._socketOfConnectorIfSeenConnecteeReadyToClose.equals(sock)) {
						 
						 NBLog.out("Sending ready to close from handleCmdData, waitingForLastPacketId was "+e._closeConnWaitingForLastPacketIdNew+" "+e.getTriplet());
						 
						 // ... then send ready to close
						 CmdReadyToCloseConn rc = new CmdReadyToCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
						 writeCommandToSocket(rc, sock, e);
						 
						 NBLog.sent(rc, e.getTriplet(), sock, NBLog.INTERESTING);
						 
						 e.assertNoAnyDataPacket();
						 e.clearSentPackets(state);
						 e._isInputPipeClosed = true;
						 e.setState(State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY);
						 
					 }
					 
				 } else {
					 // Connectee

					e._isInputPipeClosed = true;
					e.setState(State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY);
					
					CmdReadyToCloseConn readyCmd = new CmdReadyToCloseConn(e.getTriplet().getConnectorUuid(), e.getTriplet().getConnectorId());
					
					writeCommandToSocket(readyCmd, sock, e);
					NBLog.sent(readyCmd, e.getTriplet(), sock, NBLog.INTERESTING);
				 }
			} else {
				NBLog.debug("Waiting for last data packet,"+e._closeConnWaitingForLastPacketIdNew+"  "+e.getState().name()+" "+e.getTriplet(), NBLog.INFO);
			}
		}

	}	

	/** If the local user has called close() on the NBSocket, then we need to switch to close in CB 
	 * as soon as the connection is established, whcich is now the case.*/
	private static void onConnectionEstablishedCheckIfClosing(Entry e, ISocketTLWrapper wrapper) {
		
		assertInState(e, new State[] { State.CONN_ESTABLISHED });
		
		if(e._attemptingLocalClose) {
			switchToClose(e, wrapper);
		}
	}
	
	private static boolean isEntryNewerThanReceivedSock(Entry e, ISocketTLWrapper sock, CmdAbstract cmd) {
		if(
				( e.getWrapper() != null && sock != null && e.getWrapper().getGlobalId() > sock.getGlobalId()) ||
				
				
				(e != null && sock != null && e._lastSocketTLThatWeAttemptedToRecover != null && sock.getGlobalId() == e._lastSocketTLThatWeAttemptedToRecover) 
			) {
			NBLog.debug("Ignoring command as it is for an old ISocketTLWrapper "+e.getState()+ " "+sock.getGlobalId() + " " +e.getTriplet()+" "+cmd, NBLog.INTERESTING);
			return true;
		}
		
		return false;
	}
	


}
