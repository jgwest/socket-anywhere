/*
	Copyright 2012, 2019 Jonathan West

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
package com.socketanywhere.multiplexingnew;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Queue;
import java.util.UUID;

import com.socketanywhere.multiplexing.CmdAckMultiplex;
import com.socketanywhere.multiplexing.CmdCloseConnMultiplex;
import com.socketanywhere.multiplexing.CmdDataMultiplex;
import com.socketanywhere.multiplexing.CmdDataReceived;
import com.socketanywhere.multiplexing.CmdMultiplexAbstract;
import com.socketanywhere.multiplexing.CmdNewConnMultiplex;
import com.socketanywhere.multiplexing.MuLog;
import com.socketanywhere.multiplexingnew.MNEntry.MNState;
import com.socketanywhere.multiplexingnew.MNMultSocketInputStream.DataConsumedMessage;
import com.socketanywhere.net.ConstructorTransfer;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;

public class MNConnectionBrain extends Thread {
	
	public static final boolean USE_NEW = true; 
	
	// All members are immutable unless otherwise noted
	
	private final ConstructorTransfer<ISocketTL> _innerSocket; // should not be accessed outside run() or the static methods that run calls 
	
	private final ConstructorTransfer<ISocketFactory> _innerFactory; // should not be accessed outside run() or the static methods that run calls

	/** Address to connect to a listening server socket on; the listening server sock must be another
	 * MultConnectionBrain that is listening on listenAddr below. Once connected, we will store
	 * this socket on innerSocket, and multiplex data over this socket.  */
	private final ConstructorTransfer<TLAddress> _connectAddr; // should not be accessed outside run() or the static methods that run calls
	
	/** Address to wait for another MultConnectionBrain to attach to us; we will accept() this socket
	 * and then use it to multiplex data on. */
	private final ConstructorTransfer<TLAddress> _listenAddr; // should not be accessed outside run() or the static methods that run calls
	
	private final String _ourUuid;
	
	// not immutable
	private final MessageQueue _queue;
	
	// immutable
	private final MNOptions _options;
	
	public static MNConnectionBrain createConnectionBrainOnClientSocket(TLAddress connectAddr, ISocketFactory factory, MNOptions options) {
		MNConnectionBrain brain = new MNConnectionBrain(connectAddr, null, factory, options);
		brain.start();
		return brain;
	}

	public static MNConnectionBrain createConnectionBrainOnServSocket(TLAddress listenAddr, ISocketFactory factory, MNOptions options) {
		MNConnectionBrain brain = new MNConnectionBrain(null, listenAddr, factory, options);
		brain.start();
		return brain;
	}
	
	private MNConnectionBrain(TLAddress connectAddr, TLAddress listenAddr, ISocketFactory innerFactory, MNOptions options) {
		super(MNConnectionBrain.class.getName());
		_connectAddr = new ConstructorTransfer<TLAddress>(connectAddr);
		_listenAddr = new ConstructorTransfer<TLAddress>(listenAddr);
		_innerFactory = new ConstructorTransfer<ISocketFactory>(innerFactory);
		
		_innerSocket = new ConstructorTransfer<ISocketTL>(null);
		
		_ourUuid = UUID.randomUUID().toString();
		_queue = new MessageQueue(MNConnectionBrain.class.getName());
		
		_options =  options;

		init();		
	}
	
	public MNConnectionBrain(ISocketTL innerSocket, MNOptions options) {
		super(MNConnectionBrain.class.getName());
		_innerSocket = new ConstructorTransfer<ISocketTL>(innerSocket);
		_connectAddr = null;
		_listenAddr = null;
		_innerFactory = null;
		
		_ourUuid = UUID.randomUUID().toString();
		_queue = new MessageQueue(MNConnectionBrain.class.getName());

		_options = options;
		init();
	}
	
	private void init() {
		setDaemon(true);
	}
	
	
	private static class ThreadState {
		
		/** Socket => Entry */
		Map<MNMultSocket, MNEntry> _multSocketToEntrymap = new HashMap<MNMultSocket, MNEntry>();

		/** UUID+Connection Id => Entry */
		Map<MNPairUUIDConnID, MNEntry> _uuidMap = new HashMap<MNPairUUIDConnID, MNEntry>();
		
		MNOptions _options;
	}
	
	
	private static void debugWriteStateOut(String ourUuid, Map<MNMultSocket, MNEntry> multSocketToEntrymap) {
		String tag = "[mn-dwso-"+ourUuid+"] ";
		
		String result = tag+"------------------------------------------\n";
		
		for(Map.Entry<MNMultSocket, MNEntry> e : multSocketToEntrymap.entrySet()) {
			MNMultSocket multSock = e.getKey();
			MNEntry entry = e.getValue();
			
			try {
				result += tag+multSock.getConnectionNodeUUID()+":"+multSock.getConnectionId()+":"+multSock.isFromServerSocket()+"   - "+entry._state.name()
				+" bwiis:"+entry._bytesWaitingInInputStream+"  is-avail:"+multSock.getInputStream().available()
				+" weSentBytes: "+entry._sentBytes+" theyAckedOurSentBytes: "+ entry._ackedSentBytes+" totalBytesReceived:" +  entry._totalBytesReceived+ " \n";
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
		System.out.println(result);
				
		
	}
			
	@Override
	public void run() {

		/** Address(only contains port) => server socket listener for that port */
		Map<TLAddress, MNMultServerSocketListener> servSockListenerMap = new HashMap<TLAddress, MNMultServerSocketListener>(); 
		
		ThreadState threadState = new ThreadState();
		
		threadState._options = _options;

		/** The UUID for this node (specific to this ConnectionBrain) */
		String ourUuid = _ourUuid;
		
		/** The next connection created will use this ID. */		
		MutableObject<Integer> nextConnId = new MutableObject<Integer>(0);
		
		MessageQueue mnSocketListenerThreadBlockingQueue = null;
		
		// These variables handle our connection with the inner socket ------------------
		
		ISocketFactory innerFactory = _innerFactory.get(); 

		TLAddress connectAddr = _connectAddr.get(); 
		
		TLAddress listenAddr = _listenAddr.get(); 
		
		ISocketTL innerSocketNew = _innerSocket.get();
		
		MNSocketWriter writer = null; 

		if(innerSocketNew != null) {
			// Use it.
			
			writer = new MNSocketWriter(innerSocketNew, 0);
			// Added Dec 2014.
			MNSocketListenerThread sockListener = new MNSocketListenerThread(innerSocketNew, writer, this);
			sockListener.start();
			
		} else if(innerFactory != null && (connectAddr != null || listenAddr != null) && (connectAddr == null || listenAddr == null)) {
			try {
				ISocketTL clientSocket = connectInnerFactory(innerFactory, connectAddr, listenAddr, this);
				if(clientSocket != null) {
					writer = new MNSocketWriter(clientSocket, 0);
					MNSocketListenerThread sockListener = new MNSocketListenerThread(clientSocket, writer, this);
					sockListener.start();
					innerSocketNew = clientSocket;
				} else {
					/** server case returns null*/
				}
				
			} catch (IOException e) {
				System.err.println("Unable to connect to remote brain. Exception:");
				e.printStackTrace();
				return;
			}
			
		} else {
			System.err.println("Invalid MNConnectionBrain configuration.");
			return;
		}
		
		MnLog.debug("Post connectInnerFactory.");
		
		boolean continueLoop = true;
		
		Queue<MQMessage> unprocessedMessages = null;
		
		long ONE_SECOND_IN_NANOS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
		
		long INTERVAL_BETWEEN_DEBUG_OUT_IN_NANOS = TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
		
		long nextDebugOutInNanos = System.nanoTime() + INTERVAL_BETWEEN_DEBUG_OUT_IN_NANOS;
		
		while(continueLoop) {
						
			if(unprocessedMessages == null || unprocessedMessages.size() == 0) {
//				unprocessedMessages = _queue.getNextMessagesBlocking(false);
				unprocessedMessages = _queue.getNextMessagesBlockingTimeout(ONE_SECOND_IN_NANOS);
			}
			
			if(System.nanoTime() > nextDebugOutInNanos) {
				debugWriteStateOut(ourUuid, threadState._multSocketToEntrymap);
				
				nextDebugOutInNanos = System.nanoTime() + INTERVAL_BETWEEN_DEBUG_OUT_IN_NANOS;
			}
			
			if(unprocessedMessages.size() == 0) {
				continue;
			}
			
			MQMessage currMessage = unprocessedMessages.poll();
			
			if(MnLog.DEBUG) {
				MnLog.debug("currMessage: "+currMessage);
			}
			
			boolean msgError = true;
			
			try {
				// MultSocketListenerThread -----------------------------------------------
				
				if(currMessage.getSource() == MNSocketListenerThread.class) {
					
					// Handles: COMMAND_RECEIVED
					if(currMessage.getName().equals(MNSocketListenerThread.COMMAND_RECEIVED)) {
						handleSocketListenerThreadCmd(currMessage, this, threadState, servSockListenerMap);
						msgError = false;
						
					// Handles: BLOCK_UNTIL_NOT_FULL
					} else if(currMessage.getName().equals(MNSocketListenerThread.BLOCK_UNTIL_NOT_FULL)) {

						boolean respond = true;
						
						// If we have a max buffer data limit...
						if(_options != null && _options.getMaxReceiveBufferInBytes() != -1) {
							// How much data is in all our buffers..
							long allInputStreamSize = 0;
							for(MNEntry e : threadState._multSocketToEntrymap.values()) {
								allInputStreamSize += e._bytesWaitingInInputStream;
							}
							
							// If we are over our data buffer limit
							if(allInputStreamSize > _options.getMaxReceiveBufferInBytes()) {
								respond = false;
								mnSocketListenerThreadBlockingQueue = currMessage.getResponseQueue();
							}							
						}
							
						if(respond) {
							// Unblock the MNSocketListenerThread if our buffer is not too big
							currMessage.getResponseQueue().addMessage(new MQMessage(null, null, null, null));
						}
						
						msgError = false;
					}
					
				// MNMultSocket ----------------------------------------------------
				} else if(currMessage.getSource() == MNMultSocket.class) {
					
					// Handles: COMMAND_LOCAL_INIT_CLOSE, COMMAND_INITIATE_CONNECTION, COMMAND_FLUSH_INNER_SOCKET, COMMAND_WRITE_TO_INNER_SOCKET
					handleMNSocketCommands(currMessage, threadState, ourUuid, nextConnId,  writer, innerSocketNew);
					msgError = false;
					
				// MNMultServerSocketListener ------------------------------------------
				} else if(currMessage.getSource() == MNMultServerSocketListener.class) {
					
					// Handles: COMMAND_ADD_SERV_SOCK_LISTENER, COMMAND_REMOVE_SERV_SOCK_LISTENER
					handleMNServerSocketListenerCommands(currMessage, servSockListenerMap);
					msgError = false;
					
				
				// MNMultSocketInputStream ----------------------------------------
				} else if(currMessage.getSource() == MNMultSocketInputStream.class) {
					
					// Handles: DATA_CONSUMED
					if(currMessage.getName() == MNMultSocketInputStream.DATA_CONSUMED) {
						
						DataConsumedMessage param = (DataConsumedMessage)currMessage.getParam();
						
						MNEntry entry = threadState._multSocketToEntrymap.get(param.getSocket());
						
						if(entry != null) {
							entry._bytesWaitingInInputStream -= param.getBytesConsumed();
							
							// If we have a max buffer data limit...
							if(_options != null && _options.getMaxReceiveBufferInBytes() != -1) {
								
								// If the thread is currently blocked on us....
								if(mnSocketListenerThreadBlockingQueue != null) {
									
									// How much data is in all our buffers..
									long allInputStreamSize = 0;
									for(MNEntry e : threadState._multSocketToEntrymap.values()) {
										allInputStreamSize += e._bytesWaitingInInputStream;
									}
									
									// If we are now under our data buffer limit
									if(allInputStreamSize <= _options.getMaxReceiveBufferInBytes()) {
										mnSocketListenerThreadBlockingQueue.addMessage(new MQMessage(null, null, null, null));
										mnSocketListenerThreadBlockingQueue = null;
									}
								}

							}
							
							// if we have a max send buffer data limit...
							if(_options != null && _options.getMaxSendBufferPerSocketInBytes() != -1) {
								
								if(entry._bytesWaitingInInputStream < _options.getMaxSendBufferPerSocketInBytes()) {
//									entry._isInputStreamFlaggedAsTooLarge = true;
									
									CmdDataReceived dataReceivedCmd = new CmdDataReceived(entry._totalBytesReceived, entry._uuidOfConnector, entry._connId);
									writeCommandIs(dataReceivedCmd, entry._writer, null);
						
								}

								
//								hi2();
							}							
							
						} else {
							// TODO: CHECK THIS - This is likely not an error, but rather the socket being closed.
							MnLog.error("Unable to locate socket in socket to entrymap.");
						}
						
						
						
						msgError = false;
					}
					
				} else if(currMessage.getSource() == null && currMessage.getName().equals("Debug")) {
					handleDebugCommand(currMessage, threadState);
					
					msgError = false;
					
				} else {
					msgError = true;
					
				}
				
			} catch(Throwable t) {
				t.printStackTrace();
			}
			
			
			if(msgError) {
				System.err.println("Unrecognized message: "+currMessage);
			}

			
		}
		
	}
	
	private static class DebugComparator implements Comparator<Map.Entry<MNMultSocket, MNEntry>>{

		@Override
		public int compare(Entry<MNMultSocket, MNEntry> o1, Entry<MNMultSocket, MNEntry> o2) {
			return o1.getValue()._connId - o2.getValue()._connId;
		}
		
	}
	
	private static void handleDebugCommand(MQMessage currMessage, ThreadState threadState) {
		
		String result = "";
		
		List<Map.Entry<MNMultSocket, MNEntry>> list = new ArrayList<Entry<MNMultSocket, MNEntry>>();
		
		list.addAll(threadState._multSocketToEntrymap.entrySet());
		
		DebugComparator dc = new DebugComparator();
		
		Collections.sort(list, dc /*new Comparator<Map.Entry<MNMultSocket, MNEntry>>() {

			@Override
			public int compare(Entry<MNMultSocket, MNEntry> o1, Entry<MNMultSocket, MNEntry> o2) {
				return o1.getValue()._connId - o2.getValue()._connId;
			}
			
		}*/);
		
		for(Map.Entry<MNMultSocket, MNEntry> e : list) {
			
			result += e.getKey()+" | "+e.getValue()+"<br/>";
		}
		
		MQMessage resultMessage = new MQMessage(null, null, result, null);
		currMessage.getResponseQueue().addMessage(resultMessage);		

	}
	

	@SuppressWarnings("unchecked")
	private static void handleMNServerSocketListenerCommands(MQMessage currMessage, Map<TLAddress, MNMultServerSocketListener> servSockListenerMap) {
		boolean msgError = true;
		
		if(currMessage.getName().equals(MNMultServerSocketListener.COMMAND_ADD_SERV_SOCK_LISTENER)) {
			ArrayList<Object> params = (ArrayList<Object>)currMessage.getParam();

			TLAddress address = (TLAddress)params.get(0);
			MNMultServerSocketListener msl = (MNMultServerSocketListener)params.get(1);
		
			// map key: only add the port here
			servSockListenerMap.put(new TLAddress(address.getPort()), msl);

			msgError = false;
			
			sendArbitraryResponse(currMessage);
			
		} else if(currMessage.getName().equals(MNMultServerSocketListener.COMMAND_REMOVE_SERV_SOCK_LISTENER)) {

			TLAddress addr = (TLAddress)currMessage.getParam();
			
			servSockListenerMap.remove(new TLAddress(addr.getPort()));
			msgError = false;
			
			sendArbitraryResponse(currMessage);
		}
		
		if(msgError) {
			System.err.println("Unrecognized message or thrown exception: "+currMessage);
		}

	}
	
	private static void sendArbitraryResponse(MQMessage inputMessage) {
		MQMessage response = new MQMessage("arbitrary text", MNConnectionBrain.class, null, null);
		inputMessage.getResponseQueue().addMessage(response);
	}
	
	private static void handleMNSocketCommands(MQMessage currMessage, ThreadState threadState, String ourUUID, MutableObject<Integer> nextConnId, MNSocketWriter innerSocketWriter, ISocketTL innerSocket2) {
		boolean msgError = true;

		MNMultSocketClassMessageParam payload = (MNMultSocketClassMessageParam)currMessage.getParam();
		
		MNMultSocket sock = payload.getSocket();
		
		MNEntry e;
		e = threadState._multSocketToEntrymap.get(sock);
		
		try {
			
			if(currMessage.getName().equals(MNMultSocket.COMMAND_INITIATE_CONNECTION)) {

				e = null; // Entry is null here
								
				initiateConnection(currMessage, sock, threadState, ourUUID, nextConnId, innerSocketWriter, innerSocket2);
				
				msgError = false;
				
			} else {

				boolean responseSent = false;
				
				try {
					if(e == null || (e._state == MNState.CONN_CLOSED) ) {
						MnLog.debug("Entry was null for: "+payload.getCmd());
						
						// Reply with false, to indicate that the write failed as the connection was already closed.
						MQMessage response = new MQMessage(null, MNConnectionBrain.class, Boolean.FALSE, null);
						currMessage.getResponseQueue().addMessage(response);
						
						responseSent = true;
						return;
					}

					if(currMessage.getName().equals(MNMultSocket.COMMAND_WRITE_TO_INNER_SOCKET)) {
												
						if(USE_NEW) {

							MnLog.wroteCommand(payload.getCmd(), e._writer);
							
							MQMessage sendCommandParam = currMessage;
							
							if(threadState._options.getMaxSendBufferPerSocketInBytes() != -1) {
								
								// send buffer has a max size limit, in options
								e._sentBytes += payload.getCmd().getFieldDataLength(); 
								
								// total sent bytes - acknowledged sent bytes = total outstanding bytes between us and the remote peer, which should be lower than MaxSendBufferPerSocketInBytes  
								if(e._sentBytes - e._ackedSentBytes > threadState._options.getMaxSendBufferPerSocketInBytes()) {
									// We are over the buffer limit
									// We null the send command param, as we will not be responding to it in MnSocketWriter as we usually do; rather we will respond to it in CmdDataReceived
									// The idea is we wait for the remote peer to indicate that the data has been received, rather than assuming it after send completes.
									sendCommandParam = null;
									e._outputStreamResponseQueue = currMessage.getResponseQueue();
									System.err.println("Oxyl - hit limit: "+(e._sentBytes - e._ackedSentBytes)+" "+e._multSock);

								} else {
									// We are under the buffer limit, so we will respond in MNSocketWriter 
									
								}
							} else {
								// no send buffer limit set, in options
							}
							
							e._writer.sendCommand(payload.getCmd(), sendCommandParam);
							responseSent = true;
						} else {
							MnLog.wroteCommand(payload.getCmd(), e._innerSocket2);
							
							byte[] data = payload.getCmd().buildCommand();
							e._writer.getInnerSocket().getOutputStream().write(data);
//								e._innerSocket2.getOutputStream().write(data);
						}
						
						msgError = false;
						
					} else if(currMessage.getName().equals(MNMultSocket.COMMAND_FLUSH_INNER_SOCKET)) {
						if(USE_NEW) {
							e._writer.flush(currMessage);
							responseSent = true;
						} else {
//							e._writer.getInnerSocket().getOutputStream().flush();
							e._innerSocket2.getOutputStream().flush();
						}
						msgError = false;
						
					} else if(currMessage.getName().equals(MNMultSocket.COMMAND_LOCAL_INIT_CLOSE)) {
//						if(e == null) {
//							MnLog.error("eventLocalInitClose - Unable to find given socket in socket map: sock.isClosed:"+sock.isClosed()+", "+sock);
//							return;
//						}
						
						CmdCloseConnMultiplex c = new CmdCloseConnMultiplex(e._uuidOfConnector, e._connId);
		
						try {
//							writeCommandIs(c, e._writer, e._writer.getInnerSocket());
							
							if(USE_NEW) {
								if(MnLog.DEBUG) { MnLog.wroteCommand(c, e._writer); }
								e._writer.sendCommand(c, currMessage);
								responseSent = true;
							} else {
								
								writeCommandIs(c, e._writer, e._innerSocket2);
							}
	//						writeCommandMult(c, sock);
						} catch (IOException e1) {
							e1.printStackTrace();
		//					MuLog.debug("IOE on write of close command.");
						}
		
						e._state = MNState.CONN_CLOSED;
						
						msgError = false;
					}
				} finally {
					if(!responseSent) {
						sendArbitraryResponse(currMessage);
					}
				}
				
			}
			
		} catch(Throwable t) {
			t.printStackTrace();
			msgError = true;
		}
		
		if(msgError) {
			System.err.println("Unrecognized message or thrown exception: "+currMessage);
		}
	}
	
	
	private static void handleSocketListenerThreadCmd(MQMessage msg, MNConnectionBrain brain, ThreadState state, Map<TLAddress, MNMultServerSocketListener> servSockListenerMap) throws IOException {
		
		MNSocketListenerThreadCommandReceived mslMessage = (MNSocketListenerThreadCommandReceived)msg.getParam();
		
		ISocketTL innerSocket2 = mslMessage.getInnerSocket2();
		MNSocketWriter writer = mslMessage.getWriter();
		
		CmdMultiplexAbstract cmd = mslMessage.getCmd();
		
		if(cmd.getId() == CmdAckMultiplex.ID) {
			CmdAckMultiplex c = (CmdAckMultiplex)cmd;
			
			MNEntry e;
			e = state._uuidMap.get(new MNPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
			
			if(e == null) { 
				if(c.getFieldCmdIdOfAckedCmd() != CmdCloseConnMultiplex.ID) {
					MnLog.error("Unable to locate the entry specified in ack command "+c);
				} 
				return;
			}

			if(c.getFieldCmdIdOfAckedCmd() == CmdNewConnMultiplex.ID) {

				if(e._state == MNState.CONN_NEW_CONN_SENT_WAITING_FOR_ACK && c.getFieldIntParam() == 1) {
					// We are now free to send data to connectee
					CmdAckMultiplex ackResp = new CmdAckMultiplex(CmdNewConnMultiplex.ID, 2, cmd.getCmdConnUUID(), cmd.getCmdConnectionId());
					writeCommandIs(ackResp, writer, innerSocket2);
					e._state = MNState.CONN_ESTABLISHED;
					
					// Inform the socket that the connection is ready
					e._multSock.eventSignalConnected();
										
				} else  if(e._state == MNState.CONN_NEW_CONN_ACK1_SENT_WAITING_FOR_ACK && c.getFieldIntParam() == 2) {
					// We are now free to send data from connectee
					e._state = MNState.CONN_ESTABLISHED;
					
					MNMultServerSocketListener l = servSockListenerMap.get(new TLAddress(e._port));
					if(l == null) {
						MnLog.error("Unable to locate the server socket for the address that is specified.");
						return;
					}
					
					l.informNewSocketAvailable(e._multSock);
					return;
				} else {
					MnLog.error("Unrecognized command/state for received ack command. "+e._state + " " + cmd);
					return;
				}
				
			} else if(c.getFieldCmdIdOfAckedCmd() == CmdCloseConnMultiplex.ID) {
				// Received by the person who initially sent the command

				if(e._outputStreamResponseQueue != null) {
					e._outputStreamResponseQueue.addMessage(new MQMessage(null, null, null, null)); // False indicates the write failed, as we received close before we received ack of the data
//					e._outputStreamResponseQueue.addMessage(new MQMessage(null, null, Boolean.FALSE, null)); // False indicates the write failed, as we received close before we received ack of the data
					e._outputStreamResponseQueue = null;
				}
				
//				System.out.println("* removing(a) "+c.getCmdConnUUID()+ " "+c.getCmdConnectionId());
				state._uuidMap.remove(new MNPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
				state._multSocketToEntrymap.remove(e._multSock);

				e._state = MNState.CONN_CLOSED;
				
				
			} else {
				MnLog.error("Unrecognized command/state for received command.");
			}

			
		} else if(cmd.getId() == CmdCloseConnMultiplex.ID) {
			
			CmdCloseConnMultiplex c = (CmdCloseConnMultiplex)cmd;

			MNEntry e;
			
//			System.out.println("* removing(b) "+c.getCmdConnUUID()+ " "+c.getCmdConnectionId());
			e = state._uuidMap.remove(new MNPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
			if(e != null) {
				state._multSocketToEntrymap.remove(e._multSock);
			}				
			
			if(e == null) {
				MnLog.error("Unable to locate the entry specified in the close command.");
				return;
			}
			e._state = MNState.CONN_CLOSED;

			if(e._outputStreamResponseQueue != null) {
				e._outputStreamResponseQueue.addMessage(new MQMessage(null, null, null, null)); // False indicates the write failed, as we received close before we received ack of the data
//				e._outputStreamResponseQueue.addMessage(new MQMessage(null, null, Boolean.FALSE, null)); // False indicates the write failed, as we received close before we received ack of the data
				e._outputStreamResponseQueue = null;
			}
			
			CmdAckMultiplex ackResp = new CmdAckMultiplex(CmdCloseConnMultiplex.ID, 0, cmd.getCmdConnUUID(), cmd.getCmdConnectionId());
			writeCommandIs(ackResp, writer, innerSocket2);
			
			e._multSock.informRemoteClose();
			
			
		} else if(cmd.getId() == CmdDataMultiplex.ID) {
			CmdDataMultiplex c = (CmdDataMultiplex)cmd;
			
			MNEntry e = state._uuidMap.get(new MNPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
			
			if(e == null) {
				MnLog.error("Unable to locate the entry specified in data command in CmdDataMultiplex");
				return;
			}
			
//			if(state._options != null && state._options.getMaxReceiveBufferInBytes() != -1) {
			e._bytesWaitingInInputStream += c.getFieldDataLength();
//			}
			
			e._totalBytesReceived += c.getFieldDataLength();
			
			e._multSock.eventReceivedDataCmdFromRemoteConn(c);
			
			// If we have a max buffer limit
			if(state._options != null && state._options.getMaxSendBufferPerSocketInBytes() != -1) {
				
//				// TODO: CURR - Pretty sure this is actually receive, and so should use getMaxReceivedBuffer (though not 100%)
//				if(e._bytesWaitingInInputStream > state._options.getMaxSendBufferPerSocketInBytes()) {
//					e._isInputStreamFlaggedAsTooLarge = true;
//				}
//				hi();
				
			} else {
				CmdDataReceived dataReceivedCmd = new CmdDataReceived(e._totalBytesReceived, e._uuidOfConnector, e._connId);
				writeCommandIs(dataReceivedCmd, writer, innerSocket2);
				
			}
			
		} else if(cmd.getId() == CmdNewConnMultiplex.ID) {
			// Someone is trying to connect to us.
			
			CmdNewConnMultiplex c = (CmdNewConnMultiplex)cmd;
			
			MNMultSocket newSocket = new MNMultSocket(brain);
			
			if(USE_NEW) {
				newSocket.setRemoteAddr(writer.getAddress());
			} else {
				newSocket.setRemoteAddr(innerSocket2.getAddress());
			}
			
			newSocket.serverInit(null, c.getCmdConnUUID(), c.getCmdConnectionId());
			
			MNEntry e = new MNEntry(writer, innerSocket2);
			
			e._state = MNEntry.MNState.CONN_NEW_CONN_ACK1_SENT_WAITING_FOR_ACK;
			e._uuidOfConnector = c.getCmdConnUUID();
			e._connId = c.getCmdConnectionId();
			e._didWeInitConnection = false;
			
			state._uuidMap.put(new MNPairUUIDConnID(e._uuidOfConnector, e._connId), e);
			
			state._multSocketToEntrymap.put(newSocket,  e);
			
			e._multSock = newSocket;
			
			e._port = c.getFieldServerPort(); // Port of the socket connection
					
			CmdAckMultiplex ackResp = new CmdAckMultiplex(CmdNewConnMultiplex.ID, 1, e._uuidOfConnector, e._connId);
			writeCommandIs(ackResp, writer, innerSocket2);
			
		} else if(cmd.getId() == CmdDataReceived.ID) {
			MNEntry e = state._uuidMap.get(new MNPairUUIDConnID(cmd.getCmdConnUUID(), cmd.getCmdConnectionId()));
			
			if(e == null) {
				MnLog.error("Unable to locate the entry specified in CmdDataReceived "+cmd.getCmdConnUUID()+":"+cmd.getCmdConnectionId());
				return;
			}
			
			// This command is only used when there is a send limit
			if(state._options.getMaxSendBufferPerSocketInBytes() != -1) {
				CmdDataReceived dataReceivedCmd = (CmdDataReceived)cmd;
				
				long previousValue = e._ackedSentBytes;
				
				e._ackedSentBytes = dataReceivedCmd.getBytesReceived();
				
				// Sanity check to make sure the value doesn't decrease
				if(previousValue > e._ackedSentBytes) {
					MnLog.error("The acknowledged sent bytes value was invalid. "+e._ackedSentBytes+" "+previousValue);
					return;
				}
				
				if(e._outputStreamResponseQueue != null) {
					// If our outputstream is currently waiting for a response
					
					// If we are now under the limit, send inform the callback, otherwise keep waiting 
					if(e._sentBytes - e._ackedSentBytes < state._options.getMaxSendBufferPerSocketInBytes()) {
						e._outputStreamResponseQueue.addMessage(new MQMessage(null, null, null, null));
						e._outputStreamResponseQueue = null;
						
						System.err.println("Oxyl - under limit: "+(e._sentBytes - e._ackedSentBytes)+" "+e._multSock);
					}
				}
				
			}

		} else {
			System.err.println("Missing ID.");
		}
	}
		
	private static void writeCommandIs(CmdMultiplexAbstract cmd, MNSocketWriter writer, ISocketTL sock) throws IOException {
		
		if(USE_NEW) {
			if(MnLog.DEBUG) { MnLog.wroteCommand(cmd, writer); }
			writer.sendCommand(cmd, null);
		} else {
			
			if(MnLog.DEBUG) { MnLog.wroteCommand(cmd, sock); }
			
			if(sock instanceof MNMultSocket) {
				throw new IllegalArgumentException("Invalid sock type.");
			}
			
			boolean writeFailed = false;
			
			try {
				sock.getOutputStream().write(cmd.buildCommand());
			} catch (IOException e) {
				writeFailed = true;
				if(MnLog.DEBUG) {
					e.printStackTrace();
				}
			}
			
			if(writeFailed) {
				MnLog.error("Unable to write command to ISocket in writeCommand(...) of MCB");
				throw new IOException("Unable to write command to ISocket in writeCommand(...) of MCB");
			}

		}
		
		
//		boolean writeFailed = false;
		
//		try {
//		
		
		// TODO: CURR - I commented out write failure logic here, so we need to handle it elsewhere
		
//			writer.sendCommand(cmd.buildCommand());
			
//			sock.getOutputStream().write(cmd.buildCommand());
//		} catch (IOException e) {
//			writeFailed = true;
//			if(MnLog.DEBUG) {
//				e.printStackTrace();
//			}
//		}				
		
//		if(writeFailed) {
//			MnLog.error("Unable to write command to ISocket in writeCommand(...) of MCB");
//			throw new IOException("Unable to write command to ISocket in writeCommand(...) of MCB");
//		}
		
	}

		
	/** *
	 * 
	 * @param sock
	 * @return connection id
	 * @throws IOException 
	 */
	private static void initiateConnection(MQMessage message, MNMultSocket sock, ThreadState threadState, String ourUUID, MutableObject<Integer> nextConnId, MNSocketWriter innerSocketWriter, ISocketTL innerSock2) throws IOException {
		
		int resultConnectionId = -1;
		
		try {
			MNEntry e = null;
			
			if(USE_NEW) {
				if(innerSocketWriter == null) {
					System.err.println("Unsupported operation: cannot initiate a remote connection from a connection brain backed by server sockets.");
					resultConnectionId = -1;
					return;
				} 
				
			} else {
				if(innerSock2 == null) {
					System.err.println("Unsupported operation: cannot initiate a remote connection from a connection brain backed by server sockets.");
					resultConnectionId = -1;
					return;
				} 
				
			}
			
			if(sock.getConnectionId()!= -1) { 
	
				e = threadState._multSocketToEntrymap.get(sock);
			}
			
			if(e != null) {
				MnLog.error("In initiateConnection, MultSocketImpl was already found in the map. ");
				resultConnectionId = -1;
			} else {
				
				e = new MNEntry(innerSocketWriter, innerSock2);
				
				e._state = MNEntry.MNState.CONN_NEW_CONN_SENT_WAITING_FOR_ACK;
				e._uuidOfConnector = ourUUID;
		
				e._connId = nextConnId.get();
				
				nextConnId.set(nextConnId.get()+1);
				
				e._port = sock.getAddress().getPort();
				e._didWeInitConnection = true;
				e._multSock = sock;
				
		
				// TODO: MEDIUM - How to handle these
				sock._didWeInitConnection = true;
				sock._connectionId = e._connId;
				sock.setConnectionNodeUUID(e._uuidOfConnector);
		
				threadState._multSocketToEntrymap.put(sock, e);
				threadState._uuidMap.put(new MNPairUUIDConnID(e._uuidOfConnector ,e._connId), e);
				
				CmdNewConnMultiplex cmd = new CmdNewConnMultiplex(e._uuidOfConnector, e._connId, sock.getAddress().getHostname(), e._port);
		
				writeCommandIs(cmd, innerSocketWriter, innerSock2);
				
//				writeCommandMult(cmd, sock);
				
				resultConnectionId = e._connId;
			}
		
		} finally {
			
			MQMessage resultMessage = new MQMessage("arbitrary value", MNConnectionBrain.class, resultConnectionId, null);
			message.getResponseQueue().addMessage(resultMessage);
			
		}
		

	}

	
	private static ISocketTL connectInnerFactory(ISocketFactory innerSocketFactory, TLAddress connectAddr, TLAddress listenAddr, MNConnectionBrain brain) throws IOException {
//		MuLog.debug("connectInnerFactory - " + this.hashCode() + " " + _ourUuid + " ["+_connectAddr + " | "+_listenAddr+"]");
		
		ISocketTL innerSocket = null;
		
		if(connectAddr != null) {
			long startedTryingToConnect = System.currentTimeMillis();
			
			// Keep trying to connect 
			while(innerSocket == null && System.currentTimeMillis() - startedTryingToConnect <= 15 * 1000) {
				
				ISocketTL socket = null;
				try {
					socket = innerSocketFactory.instantiateSocket(connectAddr);
					innerSocket = socket;
					
					SoAnUtil.appendDebugStr(socket, "connectAddress created in connInnerFactory.");
				} catch(IOException e) {
					if(MuLog.DEBUG) {
						MuLog.debug("IOE on attempt to connect, will try to reconnect."+e);
					}
				}
				
				if(innerSocket == null) {
					try { Thread.sleep(2000); } catch(InterruptedException e) {}
				}
			}

			return innerSocket;
			
		} else if(listenAddr != null) {

			InnerServerSocketListener listener = new InnerServerSocketListener(innerSocketFactory, listenAddr, brain);
			listener.start();
			
			return null;
		} else {
			MnLog.error("User did not specfic a connect address or listen, along with the factory");
			return null;
		}
	}

	
	public MessageQueue getMessageQueue() {
		return _queue;
	}
	
	public String getOurUuid() {
		return _ourUuid;
	}
	
	public MNOptions getOptions() {
		return _options;
	}
}

class MNEntry {
	// TODO: LOWER - Consider referencing these through getters/setters. 
	
	enum MNState {

		CONN_INIT,

		CONN_NEW_CONN_SENT_WAITING_FOR_ACK,
		CONN_NEW_CONN_ACK1_SENT_WAITING_FOR_ACK,
		
		CONN_ESTABLISHED, 	// Underlying socket connection is currently established and ready to send/receive data
		CONN_CLOSED		// Socket has been closed in an orderly fashion
	}
	
	/** A single writer is shared between all MNEntry of a brain. This MNSocketWriter is created on initial brain setup.*/
	final MNSocketWriter _writer;
	
	public MNEntry(MNSocketWriter innerSocketWriter, ISocketTL innerSocket) {
		_innerSocket2 = innerSocket;
		_writer = innerSocketWriter;
	}
	
	MNState _state = MNState.CONN_INIT; 

	MNMultSocket _multSock; // MNSocket that this entry is used for
	
	final ISocketTL _innerSocket2; // The inner socket that is used to send commands for this socket
	
	String _uuidOfConnector; // UUID of the node that initiated the connection
	int _connId; // Connection id from the node that initiated the connection. 
	boolean _didWeInitConnection; // Whether or not this node was the one that initiated the connection.
	
	int _port; // Port of the socket connection
	
	long _bytesWaitingInInputStream = 0;
	
	MessageQueue _outputStreamResponseQueue;
	long _sentBytes = 0; // Bytes queued to be sent (not necessarily sent yet, by MNSocketWriter)
//	long _unackedSentBytes = 0;
	
	long _ackedSentBytes = 0; // The number of bytes the remote peer has indicated that they have received from us; this value comes from CmdDataReceived
	
	long _totalBytesReceived = 0; // Total bytes received during the lifetime of the socket entry

	boolean _isInputStreamFlaggedAsTooLarge = false;
	
	@Override
	public String toString() {
		return "State: "+_state.toString()+", didWeInit: "+_didWeInitConnection+", port: "+_port+", connId:"+_connId+", uuidOfConnector:"+_uuidOfConnector+" ";
	}
		
}

class MNPairUUIDConnID {
	
	private String _uuid;
	private int _connId;
	
	
	public MNPairUUIDConnID(String uuid, int connId) {
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
		if(!(obj instanceof MNPairUUIDConnID)) {
			return false;
		}
		MNPairUUIDConnID other = (MNPairUUIDConnID)obj;
		
		if(other.getConnId() != getConnId()) {
			return false;
		}
		
		if(!other.getUuid().equals(getUuid())) {
			return false;
		}
		
		return true;
	}
	
}

