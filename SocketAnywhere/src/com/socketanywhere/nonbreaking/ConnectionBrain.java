/*
	Copyright 2012, 2013 Jonathan West

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreaking.Entry.State;

/** The state data for all connections of a particular factory are handled by a single instance of this class.
 *  Most of the important work is done by this class; it is truly a centralized "brain" which handles
 *  all the connection handling work.
 *  
 *  It sends/received commands, handles the requests/response of those commands, handles state transitions
 *  of connections, etc. 
 *  
 * */
public class ConnectionBrain {
	
	// Member variables
	
	/** Socket => Entry */
	Map<NBSocket, Entry> _nbSocketToEntrymap = new HashMap<NBSocket, Entry>();
	
	/** UUID+Connection Id => Entry */
	Map<PairUUIDConnID, Entry> _uuidMap = new HashMap<PairUUIDConnID, Entry>();

	/** TCP Socket => NBSocket */
	Map<ISocketTL, NBSocket> _socketToNBSockMap = new HashMap<ISocketTL, NBSocket>();
	
	/** Address(only contains port) => server socket listener for that port */
	Map<TLAddress, NBServerSocketListener> _servSockListenerMap = new HashMap<TLAddress, NBServerSocketListener>(); 
	
	/** Thread through which commands are sent by queueCommand(...) method. */
	CommandSenderThread _commandSenderThread = new CommandSenderThread(this);
	
	/** The Uuid for this node (specific to this ConnectionBrain) */
	String _ourUuid;
	
	/** The next connection created will use this ID. */
	int _nextConnId = 0;
	
	// Debug Constants ------------------------

	/** Special debug-only setting that can be used to simulate connection failure for testing purposes. */
	private final static boolean DEBUG_SIMULATE_CONN_FAILURE = false;
	
	
	ISocketFactory _innerFactory;
	
	NBOptions _options;
	
	// ---------------------------------
	
	protected ConnectionBrain(ISocketFactory innerFactory, NBOptions options) {
		_ourUuid = UUID.randomUUID().toString();
		_options = options;
		_innerFactory = innerFactory;
		_commandSenderThread.start();
	}
	
	public synchronized void addServSockListener(TLAddress addr, NBServerSocketListener listener) {
		_servSockListenerMap.put(new TLAddress(addr.getPort()), listener);
	}
	
	public synchronized void removeSockListener(TLAddress addr) {
		_servSockListenerMap.remove(new TLAddress(addr.getPort()));
	}
	
	
	/** On a given nbsocket, send the commands to create a new connection. */
	public synchronized void initiateConnection(NBSocket s) {
		Entry e = _nbSocketToEntrymap.get(s);
		
		if(e != null) {
			NBLog.error("In initiateConnection, NBSocketImpl was already found in the map. ");
			return;
		}
		e = new Entry();
		
		e._state = State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK;
		e._uuidOfConnector = _ourUuid;
		e._connId = _nextConnId;
		_nextConnId++;
		
		e._port = s.getAddress().getPort();
		e._didWeInitConnection = true;
		e._nbSock = s;

		_socketToNBSockMap.put(s._inner, s);
		_nbSocketToEntrymap.put(s, e);
		_uuidMap.put(new PairUUIDConnID(e._uuidOfConnector ,e._connId), e);
		
		CmdNewConn cmd = new CmdNewConn(e._uuidOfConnector, e._connId, s.getAddress().getHostname(), e._port);

		queueCommand(cmd, Entry.State.CONN_ESTABLISHED, s._inner);
		
	}

	
	/** On a given nbsocket, send the commands to create a new connection. */
	public synchronized void initiateJoin(String nodeUUID, int connId, ISocketTL validConn) {

		Entry entry = _uuidMap.get(new PairUUIDConnID(nodeUUID, connId));
		if(entry == null) {
			NBLog.error("Join initiated on a connection that could not be found in the map.");
			return;
		}

		NBSocket oldSocket = entry._nbSock;
		
		// We have a valid connection, now issue join
		CmdJoinConn cj = new CmdJoinConn(nodeUUID, connId);

		entry._state = State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK;

		_socketToNBSockMap.put(validConn, oldSocket);
		
		queueCommand(cj, Entry.State.CONN_ESTABLISHED, validConn);
		
	}
	
	/** Someone on our end has explicitly called close(), so send close to remote */
	public synchronized void eventLocalInitClose(NBSocket s) {
		Entry e = _nbSocketToEntrymap.get(s);
		
		if(e == null) {
			NBLog.error("Unable to find given socket in socket map. - isConnectionEstablished");
			return;
		}
		
		e._attemptingToClose = true;
		
		// Start the close thread, whose job is to keep sending close() until an acknowledgment is received.
		CmdCloseThread t = new CmdCloseThread(s, e._uuidOfConnector, e._connId, this);
		t.start();
				
	}

	/** This method is called when we have detected a problem with the socket; start the recovery thread if needed. */
	public synchronized void eventConnErrDetectedReconnectIfNeeded(ISocketTL sock) {
		
		NBSocket s = _socketToNBSockMap.get(sock);
		
		if(s == null) {
			NBLog.error("Unable to find nbsocket that corresponds to given tcpsocket");
			return;
		}
		
		// We have detected that the underlying thread has died, so attempt to reestablish
		Entry e = _nbSocketToEntrymap.get(s);
		if(e == null) {
			NBLog.error("Unable to find given socket in socket map. - isConnectionEstablished");
			return;
		}
				
			// What I need here is a way to id
			if(e._state == State.CONN_CLOSED) {
				return;
			}
			
			// The recovery thread should ONLY be started if we were the ones the initiated the connection (eg we are the connector[client], rather than the connectee[serversocket])
			if(!e._didWeInitConnection) {
				return;
			} 
			
						
			if(e._recoveryThread != null) {
//				NBLog.debug("Recovery thread is not null, so not restarting a new one.");
				return;
			}
	
			e._state = State.CONN_DEAD;
	

			RecoveryThread rt = new RecoveryThread(s, s.getAddress(), e._uuidOfConnector, e._connId, this, _options);
						
			e._recoveryThread = rt;
			
			rt.start();
			
	}
	
	/** Called by socket to update the brain, to let the brain know the recovery thread has terminated. */
	@SuppressWarnings("unused")
	public synchronized void resetRecoveryThread(NBSocket s) {
		// We have detected that the underlying thread has died, so attempt to reestablish
		Entry e = _nbSocketToEntrymap.get(s);
				
		if(e == null) {
			Entry e2 = _nbSocketToEntrymap.get(s);
			NBLog.error("Unable to find given socket in socket map. - resetRecoveryThread");
			return;
		}
		
		NBLog.debug("Reseting recovery thread.");

		e._recoveryThread = null;
	}
	
	protected synchronized State getConnectionState(NBSocket s) {
		Entry e = _nbSocketToEntrymap.get(s);
		
		if(e == null) {
			NBLog.error("Unable to find given socket in socket map. - isConnectionEstablished");
			return null;
		}
		return e._state;
		
	}
	
	public synchronized State entryState(ISocketTL s) {
		NBSocket ni = _socketToNBSockMap.get(s);
		if(ni == null) return null;
		Entry e = _nbSocketToEntrymap.get(ni);
		
		if(e == null) {
			return null; 
		}
		
		return e._state;
	}
	
	/** Is the connection closed (eg has it closed in an orderly fashion, due to a close() call. ) */
	public synchronized boolean isConnectionClosed(NBSocket s) {
		Entry e = _nbSocketToEntrymap.get(s);
		
		if(e == null) {
			NBLog.debug("Unable to find given socket in socket map. - isConnectionClosed");
			return false;
		}
		return e._state == State.CONN_CLOSED;
		
	}
	
	public synchronized boolean isConnectionEstablished(NBSocket s) {
		Entry e = _nbSocketToEntrymap.get(s);
		
		if(e == null) {
			NBLog.debug("Unable to find given socket in socket map. - isConnectionEstablished");
			return false;
		}
		return e._state == State.CONN_ESTABLISHED;
		
	}
	
	public synchronized void eventDataSent(NBSocket s, CmdData c) {
		Entry e = _nbSocketToEntrymap.get(s);
		
		if(e == null) {
			NBLog.error("Unable to find given socket in socket map.");
			return;
		}
		if(e._state != State.CONN_ESTABLISHED) {
			// dataSent being called while not connected, so buffer the data.
			NBLog.debug("Unable to send data command: connection not in correct state: state:"+e._state + " cmd:"+c);
		}
		
		e._lastPacketIdSent = c.getFieldPacketId();
		e._sentDataPackets.add(new SentDataPacket(c));
		
	}

	/** Handle commands that don't require the connection state to be ESTABLISHED. */
	private synchronized void handlePreConnectCommands(ISocketTL sock, CmdAbstract cmd) {
		
		if(cmd.getId() == CmdJoinConn.ID) {
			CmdJoinConn c = (CmdJoinConn)cmd;
			
			Entry e = _uuidMap.get(new PairUUIDConnID(c.getFieldNodeUUID(), c.getFieldConnectionId()));
			if(e == null) {
				NBLog.error("Unable to join - uuid and connection id not found.");
				return;
			}
			
			e._nbSock.newSocketTransferredFromServerSocket(sock);

			// The existing nbsocket should now receive all data from this socket. 
			_socketToNBSockMap.put(sock, e._nbSock);
			
			e._state = State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2;
			
			CmdAck newAckCmd = new CmdAck(c.getId(), 1, c.getFieldNodeUUID(), c.getFieldConnectionId());
			
			queueCommand(newAckCmd, Entry.State.CONN_ESTABLISHED, sock);
			
		}  else if(cmd.getId() == CmdAck.ID && ((CmdAck)cmd).getFieldCmdIdOfAckedCmd() == CmdCloseConn.ID) {
			// Ack of CmdCloseConn
			// 
			// Received by "closee"
			CmdAck a = (CmdAck)cmd;
			
			// Get the entry for the connection specified in the command
			Entry e2 = _uuidMap.get(new PairUUIDConnID(a.getFieldNodeUUID(), a.getFieldConnectionId()));
			if(e2 == null) {
				NBLog.error("Error: received ACK close cmd for a connection that couldn't be found.");
				return;
			}
			
			// At this point, we hava already sent CmdClose, and now we have received a response. 
			// So we can close the connection.
			e2._state = State.CONN_CLOSED;
			e2._nbSock.closeInnerSocket();
			e2._attemptingToClose = false;
				
		} else if(cmd.getId() == CmdNewConn.ID) {
			// Connectee
			CmdNewConn c = (CmdNewConn)cmd;
			
			if(_uuidMap.get(new PairUUIDConnID(c.getFieldNodeUUID(), c.getFieldConnectionId())) != null) {
				NBLog.error("NewConn received, but the connection already exists in the map");
				return;
			}
			
			NBSocket newSocket = new NBSocket(this, _innerFactory, _options);
			newSocket.setRemoteAddr(sock.getAddress());
			newSocket.serverInit(sock, false);
			
			Entry e = new Entry();
			
			// Add the entry to our maps
			_nbSocketToEntrymap.put(newSocket, e);
			
			// Populate the entry
			e._state = State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2;
			e._uuidOfConnector = c.getFieldNodeUUID();
			e._connId = c.getFieldConnectionId();
			e._didWeInitConnection = false;
			e._lastPacketIdReceived = -1;
			e._lastPacketIdSent = -1;
			e._port = c.getFieldServerPort();
			e._nbSock = newSocket;
			
			_uuidMap.put(new PairUUIDConnID(e._uuidOfConnector, e._connId), e);
			_socketToNBSockMap.put(sock, newSocket);
			
			CmdAck newAckCmd = new CmdAck(c.getId(), 1, e._uuidOfConnector, e._connId);
			
			queueCommand(newAckCmd, Entry.State.CONN_ESTABLISHED, sock);
						
		} else if(cmd.getId() == CmdCloseConn.ID) {
			CmdCloseConn c = (CmdCloseConn)cmd;
			
			// Command is received by the "closee"
			
			// Get the entry for the connection specified in the command
			Entry e2 = _uuidMap.get(new PairUUIDConnID(c.getFieldNodeUUID(), c.getFieldConnectionId()));
			if(e2 == null) {
				NBLog.error("Error: received close cmd for a connection that couldn't be found.");
				return;
			}

			if(!e2._attemptingToClose) {
			
				// We have received a close cmd, so prevent the socket from allowing any more data to be sent
				e2._nbSock.informRemoteClose();

				e2._state = Entry.State.CONN_CLOSED;
				
				NBLog.connectionClosed(e2._uuidOfConnector, e2._connId);
				
				e2._attemptingToClose = true;
			}
			
			// Every time we receive a close conn, send an ACK response; this may be sent multiple times.
			// We keep sending when asked; the remote connection will keep reconnecting and sending
			// to us if it hasn't received our ACK yet. 
			CmdAck ackCmd = new CmdAck(CmdCloseConn.ID, 0, e2._uuidOfConnector, e2._connId);
			
			// It is the job of the host that initiated the close() (the "closer") to actually close the 
			// underlying socket. However, the "closee" must handle the case where for some reason
			// the "closer" did not close the socket.
			// 
			// So the "closee" will do it 2 minutes after it last sent a close ack.  
			e2._timeAtLastAckCloseCommandSent = System.currentTimeMillis();
			if(e2._ackCloseThread == null) {
				e2._ackCloseThread = new AckCloseThread(e2);
				e2._ackCloseThread.start();
			}
			
				try {
					e2._nbSock.writeToInnerSocket(ackCmd.buildCommand());
				} catch (IOException ee) {							
					NBLog.debug("Unable to write ack command in response to connection closed. (This may be normal result of closing connection)");							
					return;
				}

		} else {
			NBLog.error("Waiting for join, new conn or close, but received this:"+cmd.toString());
		}
		
		return;
				
	}
	
	private synchronized void eventCommandReceivedConnectee(ISocketTL sock, CmdAbstract cmd, Entry e) {
		
		if(e._state == State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2) {
			
			if(cmd.getId() == CmdAck.ID) {

				// Connectee
				CmdAck c = (CmdAck)cmd;
				if(c.getFieldCmdIdOfAckedCmd() == CmdNewConn.ID && c.getFieldIntParam() == 2) {
					
					e._state = State.CONN_ESTABLISHED;
					NBLog.connectionCreated(e._uuidOfConnector, e._connId);
					
					// Locate the server socket which corresponds to this address
					NBServerSocketListener ssl = _servSockListenerMap.get(new TLAddress(e._port));
					
					if(ssl == null) {
						NBLog.error("Unable to locate server socket listener for new connection.");
						return;
					}
					
					// Pass the new socket to the server socket listener for that address
					ssl.informNewSocketAvailable(e._nbSock);
					
				} else {
					NBLog.error("New conn received, waiting for ack2; but cmd didn't match:"+cmd);
				}
			} else {
				NBLog.error("New conn received, waiting for ack2; but received this cmd instead:"+cmd);
			}

			return;
		
		} else if(e._state == State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2) {
			
			if(cmd.getId() == CmdAck.ID) {
				// Connectee
				CmdAck c = (CmdAck)cmd;
				if(c.getFieldCmdIdOfAckedCmd() == CmdJoinConn.ID && c.getFieldIntParam() == 2) {
					
					boolean firstTimeReceivingThisCommand = false;
					if(e._state != State.CONN_ESTABLISHED) {
						e._state = State.CONN_ESTABLISHED;
						firstTimeReceivingThisCommand = true;
					}
					
					
					NBLog.connectionJoined(e._uuidOfConnector, e._connId);
					
					if(firstTimeReceivingThisCommand) {
						// We don't want to send the data request on reconnect multiple times,
						// so we only send it the first time we receive this command.

						CmdDataRequestOnReconnect cdr = new CmdDataRequestOnReconnect(e._lastPacketIdReceived+1);
						
						if(!writeCommandToSocket(cdr, e._nbSock, e)) {
							NBLog.debug("Unable to write 2nd ack command in state CONN_ESTABLISHED(new)");
							// Return if write failed.
							return;
						}					
						
						NBLog.dataRequestedOnReconnectConnectee(cdr);
		
					}

										
					
				} else {
					NBLog.error("Join received, waiting for ack; got ack, but didn't match: "+cmd);
				}
			} else {
				NBLog.error("Join received, waiting for ack; but got this cmd instead:"+cmd);
			}
			
			return;
		}
		
		return;
	}
	
	private synchronized void eventCommandReceivedConnector(ISocketTL sock, CmdAbstract cmd, Entry e) {
		
		if(e._state == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK) {
			
			if(cmd.getId() == CmdAck.ID) {

				// This command may be sent by the other host multiple times, so we must handle that appropriately here.
				// Our handler's implementation below is therefore idempotent.
				
				CmdAck c = (CmdAck)cmd;
				if(c.getFieldCmdIdOfAckedCmd() == CmdNewConn.ID && c.getFieldIntParam() == 1) {
					
					CmdAck newAckCmd = new CmdAck(CmdNewConn.ID, 2, e._uuidOfConnector, e._connId);
					if(e._state != State.CONN_ESTABLISHED) {
						e._state = State.CONN_ESTABLISHED;
					}
					
					if(!writeCommandToSocket(newAckCmd, e._nbSock, e)) {
						NBLog.debug("Unable to write 2nd ack command in state CONN_ESTABLISHED(new)");
						// Return if write failed.
						return;
					}					

				} else {
					NBLog.error("New conn sent and waiting for ack1, but received ack1 but didn't match:"+cmd.toString());
				}
				
			} else {
				NBLog.error("New conn sent and waiting for ack1, but instead received this:"+cmd.toString());
			}
			
			return;
			
		} else if(e._state == State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK) {

			// This command may be sent by the other host multiple times, so we must handle that appropriately here.
			// Our handler's implementation below is therefore idempotent.
			
			if(cmd.getId() == CmdAck.ID) {
				// Connector
				CmdAck c = (CmdAck)cmd;
				if(c.getFieldCmdIdOfAckedCmd() == CmdJoinConn.ID && c.getFieldIntParam() == 1) {
					
					CmdAck newAckCmd = new CmdAck(CmdJoinConn.ID, 2, e._uuidOfConnector, e._connId);
					if(e._state != State.CONN_ESTABLISHED) {
						e._state = State.CONN_ESTABLISHED;
						e._nbSock.newSocketEstablishedFromRecoveryThread(sock);
						resetRecoveryThread(e._nbSock);
					}

					if(!writeCommandToSocket(newAckCmd, e._nbSock, e)) {
						NBLog.debug("Unable to write 2nd ack command in state CONN_ESTABLISHED (join)");
						// Return if write failed.
						return;
					}					

				} else {
					NBLog.error("Join sent, waiting for ack1; instead received this cmd:"+cmd);
				}
				
			} else {
				NBLog.error("Join sent, waiting for ack1; instead received this non-ack cmd: "+cmd);
			}
			
			return;
			
		}		
	}
	
	
	public synchronized void eventCommandReceived(ISocketTL sock, CmdAbstract cmd) {

		NBLog.debug("["+_ourUuid+"] Command received: ["+cmd+"]");
				
		// The "pre-connect" commands are handled elsewhere
		
		if(cmd.getId() == CmdNewConn.ID) {
			handlePreConnectCommands(sock, cmd);
			return;
		}
		
		if(cmd.getId() == CmdCloseConn.ID || cmd.getId() == CmdJoinConn.ID) {
			handlePreConnectCommands(sock, cmd);
			return;
		}
		if(cmd.getId() == CmdAck.ID) {
			CmdAck a = (CmdAck)cmd;
			if(a.getFieldCmdIdOfAckedCmd() == CmdCloseConn.ID) {
				handlePreConnectCommands(sock, cmd);
				return;
			}
		}
		
		// After this point, an entry and an NBSocketImpl for this connection should exist in the maps
		
		NBSocket s = _socketToNBSockMap.get(sock);
		if(s == null) {
			NBLog.error("Unable to locate an NBSocketImpl for a given socket in the map. cmd:"+cmd);
			return;
		}
		
		Entry e = _nbSocketToEntrymap.get(s);
		if(e == null) {
			NBLog.error("Unable to locate an entry for a given NBSocket in the map.");
			return;
		}
		
		NBLog.debug("["+_ourUuid+"] Above command - current state: ["+e._state.name()+"]");
				
		// Connectee States
		if(	e._state == State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2 
			|| 	e._state == State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2) {
			
			eventCommandReceivedConnectee(sock, cmd, e);
			return;
		}
		
		// Connector States
		if(e._state == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK 
			|| e._state == State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK) {
			eventCommandReceivedConnector(sock, cmd, e);
			return;
		}
		
		// Everything after this point is for either connector or connectee
		
		if(e._state == State.CONN_ESTABLISHED) {
			
			if(cmd.getId() == CmdData.ID) {
				// Data sent to us
				CmdData c = (CmdData)cmd;

				// Don't update the entry if we missed data (eg if there is a gap in the numbering of data packets)
				if(c.getFieldPacketId() - e._lastPacketIdReceived > 1) {
					NBLog.debug("Gap in the numbering of packets, rerequesting data");
					
					CmdDataRequest cd = new CmdDataRequest(e._lastPacketIdReceived+1);

					if(!writeCommandToSocket(cd, s, e)) {
						// Return if write failed.
						return;
					}

					// Don't proceed any farther; we don't want to receive out of order data.
					return;
					
				}
				
				if(c.getFieldPacketId() <= e._lastPacketIdReceived) {
					NBLog.debug("Received a data packet that we have already previously received/processed. Ignoring.");
					// Ignore the duplicate data case;
					return;
				}
				
				
				e._lastPacketIdReceived = c.getFieldPacketId();
				e._dataReceivedInBytes += c.getFieldDataLength();
				e._timeLastDataReceived = System.currentTimeMillis();
		
				
				e._dataReceivedSinceLastPacketIdAcknowledged += c.getFieldDataLength();
				
				s.eventReceivedDataCmdFromRemoteConn(c);
				NBLog.dataReceived(c);

				// Send data received command only if certain threshold is met
				if(e._dataReceivedSinceLastPacketIdAcknowledged >= 1024 * 1024 * 10 || System.currentTimeMillis() - e._timeSinceLastPacketIdAcknowledged > 10 * 1000) {
					// 10 seconds or 10MB
										
					CmdDataReceived dr = new CmdDataReceived(c.getFieldPacketId());
					
					if(!writeCommandToSocket(dr, s, e)) {
						// Return if write failed.
						return;
					}
					
					e._dataReceivedSinceLastPacketIdAcknowledged = 0;
					e._timeSinceLastPacketIdAcknowledged = System.currentTimeMillis();
					e._lastPacketIdAcknowledged = c.getFieldPacketId();

				} 

				// A special debug-only setting that can be used to simulate connection failure for
				// testing purposes.
				if(DEBUG_SIMULATE_CONN_FAILURE) {
					if((Math.random() * 100f) < 0.1f) {  
						try {
							e._nbSock._inner.close();
							NBLog.debug("* DEBUG_SIMULATE_CONN_FAILURE - Socket nuked!");
						} catch (IOException e1) {
						}
					}
				}
				

				
			} else if(cmd.getId() == CmdDataReceived.ID) {
				// Acknowledging receipt of data that we have previously sent
				
				CmdDataReceived c = (CmdDataReceived)cmd;
				
				// Remove any packets from our packet list that the remote connection 
				// has acknowledged that it received
				for(Iterator<SentDataPacket> i = e._sentDataPackets.iterator(); i.hasNext();) {
					SentDataPacket p = i.next();
					if(p.getDataCmd().getFieldPacketId() <= c.getFieldLastPacketReceived()) {
						i.remove();
					}
					
				}
				
				e._lastPacketIdAckReceived = c.getFieldLastPacketReceived();
				
				NBLog.dataAckReceived(c);
				
				
			} else if(cmd.getId() == CmdDataRequest.ID) {
				CmdDataRequest c = (CmdDataRequest)cmd;
				
				// Ensure they are in the correct order
				Collections.sort(e._sentDataPackets);
				
				for(SentDataPacket p : e._sentDataPackets) {
					
					if(p.getDataCmd().getFieldPacketId() >= c.getFieldFirstPacketToResend()) {

						if(!writeCommandToSocket(p.getDataCmd(), s, e)) {
							// Return if write failed.
							return;
						}					
						
					}
				}
			} else if(cmd.getId() == CmdDataRequestOnReconnect.ID) {
				// Received by Connector
				CmdDataRequestOnReconnect c = (CmdDataRequestOnReconnect)cmd;
				
				// Ensure they are in the correct order
				Collections.sort(e._sentDataPackets);
				
				for(SentDataPacket p : e._sentDataPackets) {
					
					if(p.getDataCmd().getFieldPacketId() >= c.getFieldFirstPacketToResend()) {

						if(!writeCommandToSocket(p.getDataCmd(), s, e)) {
							// Return if write failed.
							return;
						}					

					}
				}

				// Having fulfilled the data request of the remote connection, send our own data request
				// as an ack, with the param of the last packet id we received
				CmdAck sendCmd = new CmdAck(CmdDataRequestOnReconnect.ID, e._lastPacketIdReceived+1, e._uuidOfConnector, e._connId);
				
				writeCommandToSocket(sendCmd, s, e);
								
				NBLog.dataRequestedOnReconnectConnector(sendCmd);
				
			} else if(cmd.getId() == CmdAck.ID && ((CmdAck)cmd).getFieldCmdIdOfAckedCmd() == CmdDataRequestOnReconnect.ID) {
				CmdAck c = ((CmdAck)cmd);
				
				// Received by connectee (will only be sent once)

				// Ensure they are in the correct order
				Collections.sort(e._sentDataPackets);
				
				for(SentDataPacket p : e._sentDataPackets) {
					
					if(p.getDataCmd().getFieldPacketId() >= c.getFieldIntParam()) {

						if(!writeCommandToSocket(p.getDataCmd(), s, e)) {
							// Return if write failed.
							return;
						}						
						
					}
				}
				
			} else {
				NBLog.error("Unknown Command: "+cmd);
			}
			
			return;
			
		} else if(e._state == State.CONN_DEAD) {
			NBLog.debug("Received command on connection that is CONN_DEAD. Cmd:"+cmd);
			return;
			
		} 
		
	}
	
	private /* synchronized*/ boolean writeCommandToSocket(CmdAbstract cmd, NBSocket s, Entry e) {
		try {
			s.writeToInnerSocket(cmd.buildCommand());
			return true;
		} catch (IOException e1) {
			NBLog.debug("Unable to write data to connection");							
			e._state = State.CONN_DEAD;
			eventConnErrDetectedReconnectIfNeeded(s._inner);
			return false;
		}		
	}
	
		
	protected synchronized void queueCommand(CmdAbstract cmd, Entry.State exitState, ISocketTL sock) {
		
		boolean writeFailed = false;
		
		synchronized(sock) {
			try {
				sock.getOutputStream().write(cmd.buildCommand());
			} catch (IOException e) {
				eventConnErrDetectedReconnectIfNeeded(sock);
				writeFailed = true;
			}				
		}
		
		if(!writeFailed) {
			_commandSenderThread.addCommand(cmd, exitState, sock);
		}
		
	}
	
	public ISocketFactory getInnerFactory() {
		return _innerFactory;
	}
}


/** The purpose of this thread is to continue sending a given command until it is 
 * received and acted upon by the remote machine. */
class CommandSenderThread extends Thread {
	
	ConnectionBrain _cb;
	boolean _threadRunning = true;
	
	/** List of commands that we still need to send */
	List<CommandSenderEntry> _list = new ArrayList<CommandSenderEntry>();
		
	public CommandSenderThread(ConnectionBrain cb) {
		setName(CommandSenderThread.class.getName());
		_cb = cb;
		setDaemon(true);
	}
	
	/**
	 * Add a command to the queue
	 * @param cmd Command to send
	 * @param exitState When the connection enters this state, stop sending the command
	 * @param sock Socket to write the command onto
	 */
	public void addCommand(CmdAbstract cmd, Entry.State exitState, ISocketTL sock) {
		CommandSenderEntry cse = new CommandSenderEntry();
		cse._cmd = cmd;
		cse._cmdBytes = cmd.buildCommand();
		cse._exitState = exitState;
		cse._sock = sock;
		
		synchronized(_list) {
			_list.add(cse);
		}
	}
	
	protected void stopThread() {
		_threadRunning = false;
	}
	
	@Override
	public void run() {
		List<CommandSenderEntry> listCopy = new ArrayList<CommandSenderEntry>();

		while(_threadRunning) { 
			
			// Create an unsynched copy of the list
			synchronized(_list) {
				listCopy.addAll(_list);
			}
			
			for(Iterator<CommandSenderEntry> it = listCopy.iterator(); it.hasNext(); ) {
				// For each command in the list...
				CommandSenderEntry cse = it.next();
				
				boolean removeCommand = false;

				Entry.State cseState = cse._exitState;

				if(cseState == _cb.entryState(cse._sock) 
						|| cseState == Entry.State.CONN_DEAD
						|| cseState == Entry.State.CONN_CLOSED
						|| cseState == Entry.State.CONN_ESTABLISHED) {
					// Either our previous send attempt succeeded (as indicated by entry state matching cseState) 
					// or the connection has succeeded/failed on its own, so remove
					removeCommand = true;
				} else {
					
					// We still need to keep sending the command..
					
					boolean writeError = false;
					
					synchronized(cse._sock) {
						try {
							if(cse._sock.isConnected() && !cse._sock.isClosed()) {
								// Send command
								cse._sock.getOutputStream().write(cse._cmdBytes);
							} else {
								writeError = true;
							}
						} catch (IOException e) {
							writeError = true;
						}				
					}
					
					if(writeError) {
						// Write error means we've detected the socket is toast; remove the command. 
						_cb.eventConnErrDetectedReconnectIfNeeded(cse._sock);
						removeCommand = true;
					}
				}
				
				if(removeCommand) {
					synchronized (_list) {
						_list.remove(cse);
					}					
				}
			
			}
			
			listCopy.clear();			
			
			
			// TODO: LOWER - Need a better sleep impl here
			try { Thread.sleep(3000); } catch (InterruptedException e) { /* ignore */ }
		}
	}
	
	
	class CommandSenderEntry {
		CmdAbstract _cmd;
		byte[] _cmdBytes;
		
		Entry.State _exitState; 
		ISocketTL _sock;
	}
	
	
}


class Entry {
	// TODO: LOWER - Consider referencing these through getters/setters. 
	
	enum State {

		CONN_INIT,
		
		CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2, // A node has connected to us and sent us CmdNewConn, we have sent a reply and are waiting for their reply
		CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2, // A node has connected to us to re-establish a connect; it has sent us CmdJoin, we have sent a reply and are waiting for their reply. 
		
		CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK, // Connecting node is attempting to connect, has connected and sent NewConn cmd, is waiting for an reply 
		CONNECTOR_JOIN_SENT_WAITING_FOR_ACK, // Connecting node is attempting to rejoin a connection, has connected and sent CmdJoin, is waiting for reply
		
		CONN_ESTABLISHED, 	// Underlying socket connection is currently established and ready to send/receive data
		CONN_DEAD,			// Underlying socket connection has died; we need to attempt to reestablish in recovery thread.
		CONN_CLOSED		// Socket has been closed in an orderly fashion
	}
	
	State _state = State.CONN_INIT; 

	NBSocket _nbSock; // NBSocket that this entry is used for
	
	String _uuidOfConnector; // UUID of the node that initiated the connection
	int _connId; // Connection id from the node that initiated the connection. 
	boolean _didWeInitConnection; // Whether or not this node was the one that inited the connection.

	
	int _port; // Port of the socket connection
	
	
	// Specialized threads
	RecoveryThread _recoveryThread = null; // Reference to recovery thread, if this entry is currently in recovery

	AckCloseThread _ackCloseThread = null; // Reference to Ack Close thread, if we have received a close command from remote node
	boolean _attemptingToClose = false; // Are we currently trying to close this process (for instance, due to a .close() call on socket)
	long _timeAtLastAckCloseCommandSent = -1; // The time at which we last sent an acknowledgment of a remote close command. After X minutes, this value should be used to close the underlying socket.
	
	
	// Data Received by us -------
	int _lastPacketIdReceived = -1; // id of last received data packet
	long _dataReceivedInBytes = 0;
	long _timeLastDataReceived; // time since we last received any data on this channel

	int _lastPacketIdAcknowledged = -1; // last data packet we sent for CmdDataReceived for
	long _timeSinceLastPacketIdAcknowledged; // time since we sent the last CmdDataReceived for data received
	long _dataReceivedSinceLastPacketIdAcknowledged; // amount of data we've received since we last sent CmdDataReceived
	
	
	// Data Sent by us -------
	int _lastPacketIdSent = -1; // the id of the last packet we sent 
	int _lastPacketIdAckReceived = -1; // the last id for which we received an acknowledgment of our sent data
	List<SentDataPacket> _sentDataPackets = new ArrayList<SentDataPacket>(); // the sent packets that the remote node has not yet confirmed
	
	
	
	@Override
	public String toString() {
		return "Entry["+hashCode()+"]- did-we-estab:"+_didWeInitConnection+" uuid-of-estab:"+_uuidOfConnector+" conn-id:State:"+_state + " recovery thread:"+_recoveryThread+" "+" nbSock:["+_nbSock+"]";
	}
}

/** Contained in a list of packets in Entry*/
class SentDataPacket implements Comparable<SentDataPacket> {
	
	public SentDataPacket(CmdData data) {
		this._data = data;
	}
	
	private CmdData _data;
	
	public CmdData getDataCmd() {
		return _data;
	}

	@Override
	public int compareTo(SentDataPacket o) {
		return this._data.getFieldPacketId() - o._data.getFieldPacketId();
	}
}

class PairUUIDConnID {
	
	private String _uuid;
	private int _connId;
	
	
	public PairUUIDConnID(String uuid, int connId) {
		this._uuid = uuid;
		this._connId = connId;
	}
	
	
	public String getUuid() {
		return _uuid;
	}
	public int getConnId() {
		return _connId;
	}
	
	
	@Override
	public int hashCode() {
		return _uuid.hashCode()*100 + _connId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof PairUUIDConnID)) {
			return false;
		}
		PairUUIDConnID other = (PairUUIDConnID)obj;
		
		if(other.getConnId() != getConnId()) {
			return false;
		}
		
		if(!other.getUuid().equals(getUuid())) {
			return false;
		}
		
		return true;
	}
	
}


/* The job of this thread is to keep sending close() until an acknowledgment is received. */
class CmdCloseThread extends Thread {
	
	NBSocket _socket;
	String _uuidOfConnector;
	int _connectionId;
	ConnectionBrain _cb;
	
	
	public CmdCloseThread(NBSocket socket, String uuidOfConnector, int connectionId, ConnectionBrain cb) {
		super(CmdCloseThread.class.getName() + " uuidOfConnector:"+uuidOfConnector+"  connId:"+connectionId);
		setDaemon(true);
		_socket = socket;
		_uuidOfConnector = uuidOfConnector;
		_connectionId = connectionId;
		_cb = cb;
	}
	
	@Override
	public void run() {
		ConnectionBrain cb = _cb;
		
		boolean continueLoop = true;
		
		// This loop will keep sending CmdClose until the connection brain 
		// receives an acknowledgment of the CmdClose (above), and closes the connection.

		while(continueLoop && !cb.isConnectionClosed(_socket)) {

			// Is the connection established?
			if(_socket.isUnderlyingSocketConnected()) {
				
				CmdCloseConn c = new CmdCloseConn(_uuidOfConnector, _connectionId);
				
				try {
					// If so, send close command.
					_socket.writeToInnerSocket(c.buildCommand());
					
					try { Thread.sleep(5 * 1000); } catch (InterruptedException e) { }
					
					// Successful? Then stop looping.
					if(cb.isConnectionClosed(_socket)) {
						continueLoop = false;
						_socket.closeInnerSocket();
					} 
					
				} catch (IOException ex) {
					NBLog.debug("Was unable to write close connection after explicit close() call.");
				}
				
			} 
			
			if(continueLoop) {
				// Connection not currently received, so keep waiting.
				try { Thread.sleep(4 * 1000); } catch (InterruptedException e) { }
			}
			
		}
	}
}

class AckCloseThread extends Thread {
	Entry _entry;
	
	public AckCloseThread(Entry entry) {
		super(AckCloseThread.class.getName());
		_entry = entry;
	}
	
	public void run() {
		
		try {
			// Close the underlying socket after 2 minutes.
			// (the host that initiated the close [which is not us] _should_ have closed it themselves; this handles
			//  the case where, for some reason, they were not able to)
			while(_entry._timeAtLastAckCloseCommandSent == -1 
					|| System.currentTimeMillis() - _entry._timeAtLastAckCloseCommandSent <= 1000 * 60 * 2) {
				
				Thread.sleep(4 * 1000);
			}
		} catch(InterruptedException e) { }

		_entry._state = Entry.State.CONN_CLOSED;
		
		if(!_entry._nbSock.isUnderlyingSocketConnected()) {
			_entry._nbSock.closeInnerSocket();
		}
	};
}