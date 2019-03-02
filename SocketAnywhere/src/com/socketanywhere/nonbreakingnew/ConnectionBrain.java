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

package com.socketanywhere.nonbreakingnew;

import static com.socketanywhere.nonbreakingnew.CBUtil.assertBrainIsConnectee;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertBrainIsConnector;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertInState;
import static com.socketanywhere.nonbreakingnew.CBUtil.assertIsConnector;
import static com.socketanywhere.nonbreakingnew.CBUtil.switchToClose;
import static com.socketanywhere.nonbreakingnew.CBUtil.writeCommandToSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.socketanywhere.multiplexingnew.MQMessage;
import com.socketanywhere.multiplexingnew.MessageQueue;
import com.socketanywhere.net.ConstructorTransfer;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.ConnectionBrain.ThreadState;
import com.socketanywhere.nonbreakingnew.Entry.State;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataReceived;
import com.socketanywhere.nonbreakingnew.cmd.CmdJoinCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdJoinConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdNewConn;
import com.socketanywhere.util.ManagedThread;

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
		
	private final ConstructorTransfer<NBOptions> _options;
	
	// not immutable
	private final MessageQueue _queue;
	
	// immutable
	private final ISocketFactory _innerFactory;

	/** immutable - The Uuid for this node (specific to this ConnectionBrain) */
	protected final ConstructorTransfer<String> _ourUuid;
	
	private final boolean _isConnectorBrain;
	
	private final ConnectionBrainInterface _inter;
	
	public boolean isConnectorBrain() {
		return _isConnectorBrain;
	}
	
	
	/** Sugar */
	private static Object getParam(MQMessage m, int index) {
		return  ((Object[])m.getParam())[index-1];
	}

	public static class ThreadState {
		public NBOptions options;
		public String ourUuid;
		
		public boolean isConnectorBrain;
		
		/** Socket => Entry */
		public final Map<NBSocket, Entry> nbSocketToEntryMapInternal = new HashMap<NBSocket, Entry>(); // TODO: Curr - get rid of this

		/** UUID+Connection Id => Entry */
		public final Map<PairUUIDConnID, Entry> uuidMapInternal = new HashMap<PairUUIDConnID, Entry>(); 

		/** TCP Socket => NBSocket */
		// I attempted to remove this, but hit the issue of server socket (connectee) not have an nbsocket
		public final Map<ISocketTLWrapper, NBSocket> socketToNBSockMapInternal = new HashMap<ISocketTLWrapper, NBSocket>();

		/** Address(only contains port) => server socket listener for that port */
		public final Map<TLAddress, NBServerSocketListener> servSockListenerMap = new HashMap<TLAddress, NBServerSocketListener>(); 

		/** The next connection created will use this ID. */
		public int nextConnId = 0;
		
		public void putNBSocketToEntry(NBSocket sock, Entry e) {
			nbSocketToEntryMapInternal.put(sock, e);
		}
		
		public Entry getEntryFromNBSocket(NBSocket sock) {
			return nbSocketToEntryMapInternal.get(sock);
		}
		
		public NBSocket getNBSocketFromWrapper(ISocketTLWrapper wrapper) {
			return socketToNBSockMapInternal.get(wrapper);
		}
		
		public void putNBSocketToWrapper(ISocketTLWrapper wrapper, NBSocket socket) {
			socketToNBSockMapInternal.put(wrapper, socket);
		}
		
		
		public void putEntryConnInfo(PairUUIDConnID connInfo, Entry e) {
			uuidMapInternal.put(connInfo, e);
		}
		
		public Entry getEntryConnInfo(PairUUIDConnID connInfo) {
			return uuidMapInternal.get(connInfo);
		}

		ConnectionBrain brain;
	}
	
	
	private static void debugWriteStateOut(ThreadState state) {
		String tag = "[nb-dwso-"+state.ourUuid+"] ";
		
		String result = tag+"------------------------------------------\n";
		
		for(Map.Entry<NBSocket, Entry> e : state.nbSocketToEntryMapInternal.entrySet()) {
			NBSocket nbSock = e.getKey();
			Entry entry = e.getValue();
			
			try {
				result += tag+nbSock.getDebugTriplet().toString()+" "+entry.getState().name()+" is-avail: "+nbSock.getInputStream().available()+" bisdl:"+entry._bytesInSentDataList+" biwdl:"+entry._bytesInWaitingDataList+" last-acked:"+entry._lastPacketIdAcknowledged+" last-recv:"+entry._lastPacketIdReceived+" last-sent:"+entry._lastPacketIdSent+" sent-dp:"+entry._sentDataPackets.size()+" waiting-dp:"+entry._waitingDataPackets.size();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
		System.out.println(result);
				
		
	}

	
	private void run() {
		
		ThreadState state = new ThreadState();
		
		state.isConnectorBrain = _isConnectorBrain;
		
		state.options = _options.get();
		state.ourUuid = _ourUuid.get();
		
		state.brain = this;
		
		/** Socket => Entry */
//		final Map<NBSocket, Entry> nbSocketToEntrymap = state.nbSocketToEntrymap;

		/** Address(only contains port) => server socket listener for that port */
		final Map<TLAddress, NBServerSocketListener> servSockListenerMap = state.servSockListenerMap; 

		boolean continueLoop = true;
		
		long startTimeInNanos = System.nanoTime();
		
		Map<String, Long> messagesProcessedMap = new HashMap<String, Long>();
		
		long messagesProcessed = 0;
		
		
		long ONE_SECOND_IN_NANOS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
		
		long INTERVAL_BETWEEN_DEBUG_OUT_IN_NANOS = TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
		
		long nextDebugOutInNanos = System.nanoTime() + INTERVAL_BETWEEN_DEBUG_OUT_IN_NANOS;
		
		
		while(continueLoop) {
			
			Queue<MQMessage> queue = _queue.getNextMessagesBlockingTimeout(ONE_SECOND_IN_NANOS);
			
			if(System.nanoTime() > nextDebugOutInNanos) {
				debugWriteStateOut(state);
				nextDebugOutInNanos = System.nanoTime() + INTERVAL_BETWEEN_DEBUG_OUT_IN_NANOS;
			}
			
			if(queue.size() == 0) {
				continue;
			}
					
//			Queue<MQMessage> queue = _queue.getNextMessagesBlocking(false);

			message_queue: for(MQMessage m : queue) {
				
				messagesProcessed ++;
				
				Long l = messagesProcessedMap.get(""+m.getName());
				if(l == null) {
					l = 0l;
				} else {
					l++;
				}
				messagesProcessedMap.put(""+m.getName(), l);
	
				// Log the message
				if(NBLog.DEBUG) {
					
					if(messagesProcessed % 5000 == 0) {
						
						long elapstedTime = System.nanoTime() - startTimeInNanos;
						double seconds = (TimeUnit.MILLISECONDS.convert(elapstedTime, TimeUnit.NANOSECONDS)) / 1000d;
						
						NBLog.debug("------------------", NBLog.INTERESTING);
						NBLog.debug("Messages processed ["+state.ourUuid+"]: "+messagesProcessed+" per-second: "+(messagesProcessed/seconds)+"  cb-uuid:"+state.ourUuid, NBLog.INTERESTING);
						
						for(Map.Entry<String, Long> e : messagesProcessedMap.entrySet()) {
							NBLog.debug("processed ["+state.ourUuid+"]: "+e.getKey()+" " + e.getValue()  + " per-second: "+ (e.getValue()/seconds)  , NBLog.INTERESTING);
						}
						
						messagesProcessed = 0;
						messagesProcessedMap.clear();
						startTimeInNanos = System.nanoTime();
					}					
				}
				
				try {
					if(m.getName() == ConnectionBrainInterface.ADD_SERV_SOCK_LISTENER) {
						TLAddress addr = (TLAddress)getParam(m, 1);
						NBServerSocketListener listener = (NBServerSocketListener)getParam(m, 2);
						servSockListenerMap.put(addr, listener);
						
						if(NBLog.DEBUG) {
							NBLog.debug("process message: "+m.getName()+ " - "+addr, NBLog.INTERESTING);
						}
						
					} else if(m.getName() == ConnectionBrainInterface.REMOVE_SOCK_LISTENER) {
						TLAddress addr = (TLAddress)getParam(m, 1);
						servSockListenerMap.remove(addr.getPort());

						if(NBLog.DEBUG) {
							NBLog.debug("process message: "+m.getName()+ " - "+addr, NBLog.INTERESTING);
						}

					} else if(m.getName() == ConnectionBrainInterface.INIT_CONNECTION) {
						NBSocket s = (NBSocket)m.getParam();
						handleInitConnection(state, s, m);
						
					} else if(m.getName() == ConnectionBrainInterface.INITIATE_CONNECTION) {
						NBSocket s = (NBSocket)getParam(m, 1);
						ISocketTLWrapper w = (ISocketTLWrapper)getParam(m, 2);
	
						handleInitiateConnection(state, s, w, m);

					} else if(m.getName() == ConnectionBrainInterface.INITIATE_CONNECTION_EXISTING) {
						NBSocket s = (NBSocket)getParam(m, 1);
						ISocketTLWrapper wrapper = (ISocketTLWrapper)getParam(m, 2);
	
						handleInitiateExistingConnection(state, s, wrapper, m);
	
					} else if(m.getName() == ConnectionBrainInterface.EVENT_CONN_ERR_DETECTED_RECONNECT_IF_NEEDED) {
						ISocketTLWrapper sock = (ISocketTLWrapper)m.getParam();
						
						handleEventConnErrDetectedReconnectIfNeeded(state, sock, m);
						
					} else if(m.getName() == ConnectionBrainInterface.INITIATE_JOIN) {
						String nodeUUID = (String)getParam(m, 1);
						int connId = (Integer)getParam(m, 2);
						ISocketTLWrapper validConn = (ISocketTLWrapper)getParam(m, 3);
						handleInitiateJoin(state, nodeUUID, connId, validConn, m);
	
					} else if(m.getName() == ConnectionBrainInterface.EVENT_SEND_DATA) {
						NBSocket s = (NBSocket)getParam(m, 1);
						CmdData c = (CmdData)getParam(m, 2);
						handleEventSendData(state, s, c, m);
						
					} else if(m.getName() == ConnectionBrainInterface.EVENT_LOCAL_INIT_CLOSE) {
						NBSocket s = (NBSocket)m.getParam();							
						handleEventLocalInitClose(state, s, m);
						
					} else if(m.getName() == ConnectionBrainInterface.EVENT_COMMAND_RECEIVED) {
						ISocketTLWrapper sock = (ISocketTLWrapper)getParam(m, 1);
						CmdAbstract cmd = (CmdAbstract)getParam(m, 2);
						CBCommandHandler.handleEventCommandReceived(state, sock, cmd);
						
					} else if(m.getName() == ConnectionBrainInterface.IS_CONNECTION_CLOSED) {
						NBSocket s = (NBSocket)m.getParam();
						handleIsConnectionClosed(state, s, m);
						
					} else if(m.getName() == ConnectionBrainInterface.IS_CONNECTION_ESTABLISHED_OR_CLOSING) {
						NBSocket s = (NBSocket)m.getParam();
						handleIsConnectionEstablishedOrClosing(state, s, m);
					} else if(m.getName() == ConnectionBrainInterface.IS_CONNECTION_ESTABLISHED_OR_ESTABLISHING) {
						NBSocket s = (NBSocket)m.getParam();
						handleIsConnectionEstablishedOrEstablishing(state, s, m);
						
						
					} else if(m.getName() == ConnectionBrainInterface.GET_ENTRY_STATE) {
						ISocketTLWrapper s = (ISocketTLWrapper)m.getParam();
						handleGetEntryState(state, s, m);
						
					} else if(m.getName() == ConnectionBrainInterface.DEBUG_GET_ENTRY) {
						NBSocket n = (NBSocket)m.getParam();
						Entry entryResult = state.getEntryFromNBSocket(n);
						
						MQMessage result = new MQMessage(null, null, entryResult, null);
						m.getResponseQueue().addMessage(result);
						
					} else if(m.getName() == ConnectionBrainInterface.IS_INPUT_PIPE_CLOSED) {
						NBSocket s = (NBSocket)m.getParam();
						
						Entry e = state.getEntryFromNBSocket(s);

						Boolean result;
						
						if(e == null) {
							NBLog.debug("isInputPipeClosed - Unable to find given socket in socket map. ", NBLog.INTERESTING);
							result = false;
						} else {
							
							result = e._isInputPipeClosed;
							
//							if(e.getTriplet().areWeConnector()) {
//								result = (e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY || e.getState() == State.CONN_CLOSED_NEW);
//							} else {
//								result = (e.getState() == State.CONN_CLOSED_NEW);
//							}
						}

						m.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, result, null));

					} else if(m.getName() == ConnectionBrainInterface.IS_CLOSING_OR_CLOSED) {
						NBSocket s = (NBSocket)m.getParam();
						
						Entry e = state.getEntryFromNBSocket(s);

						Boolean result;
						
						if(e == null) {
							NBLog.debug("Unable to find given socket in socket map. - isClosingOrClosed", NBLog.INTERESTING);
							result = false;
						} else {
							
							result = ( e.getState() == State.CONN_CLOSING_INIT
									|| e.getState() == State.CONN_DEAD_CLOSING_NEW
									|| e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY 
									|| e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY 
									|| e.getState() == State.CONN_CLOSED_NEW )
									|| e._attemptingLocalClose;
						}

						m.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, result, null));						
						
					} else if(m.getName() == ConnectionBrainInterface.INITIATE_JOIN_CLOSE) {

						String nodeUUID = (String)getParam(m, 1);
						int connId = (Integer)getParam(m, 2);
						ISocketTLWrapper validConn = (ISocketTLWrapper)getParam(m, 3);
						
						handleInitiateJoinClose(state, nodeUUID, connId, validConn, m);
						
					} else if(m.getName() == ConnectionBrainInterface.EVENT_FLUSH_SOCKET) {
						NBSocket s = (NBSocket)getParam(m, 1);
						handleEventFlush(state, s, m);
						
					} else if(m.getName() == ConnectionBrainInterface.DEBUG_GET_THREAD_STATE_AND_STOP) {
						
						// Send back the thread state
						m.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, state, null));
						
						continueLoop = false;
						break message_queue;
					} else if(m.getName() == ConnectionBrainInterface.EVENT_INPUT_STREAM_IS_FULL) {
						NBSocket sock = (NBSocket)getParam(m, 1);
						Boolean isFull = (Boolean)getParam(m, 2);
					
						Entry e = state.getEntryFromNBSocket(sock);

						if(e != null) {
							// If the inputstream has gone from full to not full, then send CmdDataReceived
							if(!isFull && e.getState() == State.CONN_ESTABLISHED && e._lastPacketIdReceived > e._lastPacketIdAcknowledged) {
								CmdDataReceived dr = new CmdDataReceived(e._lastPacketIdReceived);
								
								e._dataReceivedSinceLastPacketIdAcknowledged = 0;
								e._timeSinceLastPacketIdAcknowledgedInNanos = System.nanoTime();
								e._lastPacketIdAcknowledged = e._lastPacketIdReceived;
					
								writeCommandToSocket(dr, e.getWrapper(), e);
								NBLog.sent(dr, e.getTriplet(), e.getWrapper(), NBLog.INFO);
							}
							
						} else {
							NBLog.debug("Event: InputStream is full - Unable to find given socket in socket map. ", NBLog.INTERESTING);
						}
						
					} else {
						NBLog.error("Unrecognized message: "+m);
						System.err.println("Unrecognized message: "+m);
					}
				} catch(Throwable t) {
					String th = SoAnUtil.convertStackTrace(t);
					NBLog.severe("THIS SHOULDN'T HAPPEN!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! "+th);
					t.printStackTrace();
				}
				
			}
		}
		
	}
		
	
	// Debug Constants ------------------------
	
	public static void startThreadStackDumpThread(final Thread analyzedThread, final String uuid) {
		Thread t = new Thread() {
			@Override
			public void run() {
				
				int invoc = 0;
				
				while(true) {
					try { Thread.sleep(10 * 1000); } catch (InterruptedException e) { /* ignore */ }
					
					int line = 0;
					NBLog.error("thing ("+uuid+") ("+invoc+")> -------------------------");
					StackTraceElement[] ste = analyzedThread.getStackTrace();
					for(StackTraceElement e : ste) {
						NBLog.error("thing ("+uuid+") ("+invoc+")> "+line+":" +e);
						line++;
					}
					
					invoc++;
					
				}
				
			}
		};
		t.setDaemon(true);
		t.start();
		
	}

	
	// ---------------------------------
	
	protected ConnectionBrain(ISocketFactory innerFactory, NBOptions options, boolean isConnectorBrain) {
		_ourUuid = new ConstructorTransfer<String>(UUID.randomUUID().toString());
		_options = new ConstructorTransfer<NBOptions>(options);
		_innerFactory = innerFactory;
		
		_queue = new MessageQueue(this.getClass().getName());
	
		_inter = new ConnectionBrainInterface(_queue);
		_isConnectorBrain = isConnectorBrain;
		
		ThreadBootstrap tb = new ThreadBootstrap(this);
		tb.start();
		
		// Mapper.getInstance().putIntoList("CB", this);
		
	}
	
	public ConnectionBrainInterface getInterface() {
		return _inter;
	}
	
	
	/** Connector - This is used in the case where the connector sent CmdNewConn, but it was not received by the connectee. */
	private static void handleInitiateExistingConnection(ThreadState state, NBSocket s, ISocketTLWrapper wrapper, MQMessage msg) {
		
		assertBrainIsConnector(state);
		
		Entry eNew = state.getEntryFromNBSocket(s);

		if(eNew == null) {
			NBLog.error("In initiateExistingConnection, NBSocketImpl was not found in the map. ");
			return;
		}
	
		assertIsConnector(s, wrapper);
		
		if(eNew.getWrapper().getGlobalId() > wrapper.getGlobalId()) {
			NBLog.severe("Entry wrapper global id is newer: "+eNew.getWrapper().getGlobalId() + " "+wrapper.getGlobalId()+" "+eNew.getState());
		}
		
		eNew.setState(State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK);
				
		state.putNBSocketToWrapper(wrapper, s);
		state.putNBSocketToEntry(s, eNew);
		
		eNew.setWrapper(wrapper);
		
		CmdNewConn cmd = new CmdNewConn(eNew.getTriplet().getConnectorUuid(), eNew.getTriplet().getConnectorId(), s.getAddress().getHostname(), eNew._port);
		NBLog.sent(cmd, eNew.getTriplet(), wrapper, NBLog.INTERESTING);		

		NBLog.connectionInitateExisting(eNew.getTriplet());
		
		writeCommandToSocket(cmd, wrapper, eNew);
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+msg.getName()+ " - triplet:"+eNew.getTriplet(), NBLog.INTERESTING);
		}

	}

	
	
	/** Connector */
	private static void handleInitConnection(ThreadState state, NBSocket s, MQMessage msg) {

		assertBrainIsConnector(state);

		Entry e = state.getEntryFromNBSocket(s);

		if(e != null) {
			NBLog.severe("In initiateConnection, NBSocketImpl was already found in the map. ");
			return;
		}
	
		assertIsConnector(s, null);
		
		e = new Entry();

		Triplet triplet = new Triplet(state.ourUuid, state.nextConnId, true);
		state.nextConnId++;
		e.setTriplet(triplet);

		
		e._nbSock = s;
		e.setState(State.CONN_INIT);
		

		e._port = s.getAddress().getPort();
		
		e._timeSinceLastPacketIdAcknowledgedInNanos = System.nanoTime(); 
		
		state.putNBSocketToEntry(s, e);
		state.putEntryConnInfo(new PairUUIDConnID(triplet), e);
//		state.uuidMap.put(new PairUUIDConnID(triplet), e);
		
		RecoveryThreadNew thread = new RecoveryThreadNew(s, s.getAddress(), e.getTriplet(), state.brain, state.options);
		e._recoveryThreadNew = thread;
		
		msg.getResponseQueue().addMessage(new MQMessage(null, null, triplet, null));

		thread.start();
		thread.initiateNewSocket();
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+msg.getName()+ " - triplet: "+triplet, NBLog.INTERESTING);
		}
	}

	
	/** Connector */
	private static void handleInitiateConnection(ThreadState state, NBSocket s, ISocketTLWrapper w, MQMessage msg) {
		
		assertBrainIsConnector(state);

//		Entry e = state.uuidMap.get(new PairUUIDConnID(s.getTriplet()));
		
		Entry e = state.getEntryFromNBSocket(s);

		if(e == null) {
			NBLog.severe("In initiateConnection, NBSocketImpl was not found in the map. "+s.getDebugId());
			return;
		}
	
		if(!e._nbSock.equals(s)) {
			NBLog.severe("In initiateConnection, the NBSockets did not match.");
			return;
		}
		
		if(e._nbSock.getAddress().getPort() != e._port) {
			NBLog.severe("Ports do not match.");
		}
		
		assertIsConnector(s, w);
		
		e.setWrapper(w);
		e.setState(State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK);
		
		state.putNBSocketToEntry(s, e);
		
		state.putNBSocketToWrapper(w, s);
		
		Triplet triplet = e.getTriplet();
		
		CmdNewConn cmd = new CmdNewConn(triplet.getConnectorUuid(), triplet.getConnectorId(), s.getAddress().getHostname(), e._port);
		NBLog.sent(cmd, triplet, w, NBLog.INTERESTING);
		writeCommandToSocket(cmd, w, e);
		
		msg.getResponseQueue().addMessage(new MQMessage(null, null, triplet, null));
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+msg.getName()+ " - triplet: "+triplet, NBLog.INTERESTING);
		}
	}

	
	/** Connector, called by recovery thread */
	private static void handleInitiateJoinClose(ThreadState state, String nodeUUID, int connId, ISocketTLWrapper validConn, MQMessage m) {
		
		assertBrainIsConnector(state);
		
		Entry entry = state.getEntryConnInfo(new PairUUIDConnID(nodeUUID, connId));
		if(entry == null) {
			NBLog.error("Join-close initiated on a connection that could not be found in the map.");
			return;
		}

		if(validConn.isFromServSock()) {
			NBLog.error("initiateJoinClose from server-sock's socket");
			throw new IllegalArgumentException("initiateJoin from server-sock's socket");
		}
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+m.getName()+ " - "+entry.getTriplet(), NBLog.INTERESTING);
		}

		
		assertInState(entry, new State[] { State.CONN_DEAD_CLOSING_NEW  } );
		
		if(entry.getWrapper().getGlobalId() > validConn.getGlobalId()) {
			NBLog.severe("Entry wrapper global id is newer: "+entry.getWrapper().getGlobalId() + " "+validConn.getGlobalId()+" "+entry.getState());
		}
		
		NBSocket oldSocket = entry._nbSock;
		
		assertIsConnector(oldSocket, validConn);
		
		CmdJoinCloseConn cjc = new CmdJoinCloseConn(nodeUUID, connId);
		entry.setState(State.CONN_CLOSING_INIT);
		entry.transferWaitingToSent();
		
		state.putNBSocketToWrapper(validConn, oldSocket);
		
		entry.setWrapper(validConn);
		

		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+m.getName()+ " - triplet: "+entry.getTriplet(), NBLog.INTERESTING);
		}

		writeCommandToSocket(cjc, validConn, entry);
		
		NBLog.sent(cjc, entry.getTriplet(), validConn, NBLog.INTERESTING);
		
	}

	
	/** Connector, called by recovery thread */
	private static void handleInitiateJoin(ThreadState state, String nodeUUID, int connId, ISocketTLWrapper validConn, MQMessage m) {
		
		assertBrainIsConnector(state);
		
		Entry entry = state.getEntryConnInfo(new PairUUIDConnID(nodeUUID, connId));
		if(entry == null) {
			NBLog.error("Join initiated on a connection that could not be found in the map.");
			return;
		}

		if(validConn.isFromServSock()) {
			NBLog.error("initiateJoin from server-sock's socket");
			throw new IllegalArgumentException("initiateJoin from server-sock's socket");
		}
		
		if(entry.getWrapper().getGlobalId() > validConn.getGlobalId()) {
			NBLog.severe("Entry wrapper global id is newer: "+entry.getWrapper().getGlobalId() + " "+validConn.getGlobalId()+" "+entry.getState());
		}

		
		NBSocket oldSocket = entry._nbSock;
		
		// We have a valid connection, now issue join
		CmdJoinConn cj = new CmdJoinConn(nodeUUID, connId);

		entry.setState(State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK);

		state.putNBSocketToWrapper(validConn, oldSocket);
		
		entry.setWrapper(validConn);
		
		
		writeCommandToSocket(cj, validConn, entry);
		
		NBLog.sent(cj, entry.getTriplet(), validConn, NBLog.INTERESTING);
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+m.getName() +" global-id:  "+validConn.getGlobalId()+"  "+entry.getTriplet(), NBLog.INTERESTING);
		}
		
	}
	
	
	/** Someone on our end has explicitly called close(), so send close to remote.  
	 * Connector/Connectee*/
	private static void handleEventLocalInitClose(ThreadState state, NBSocket s, MQMessage m) {
		Entry e = state.getEntryFromNBSocket(s);
		
		if(e == null) {
			NBLog.error("handleEventLocalInitClose - Unable to find given socket in socket map."+s.getDebugTriplet());
			return;
		}
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+m.getName()+ " - "+e.getTriplet(), NBLog.INTERESTING);
		}


		// If we are already closed or closing then we can ignore this
		if(e.getState() == State.CONN_CLOSED_NEW ||
				e.getState() == State.CONN_CLOSING_INIT ||
				e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY ||
				e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY ||
				e.getState() == State.CONN_CLOSED_NEW ||
				e.getState() == State.CONN_DEAD_CLOSING_NEW) {
			
			return;
		}
		
		
		if(e.getState() == State.CONN_ESTABLISHED) {
		
			switchToClose(e, null);
			
			return;
		} else {
			e._attemptingLocalClose = true;
		}
				
	}

	/** Connector/Connectee */
	private static void handleEventConnErrDetectedReconnectIfNeeded(ThreadState state, ISocketTLWrapper sock, MQMessage msg ) {
		
		
		
		
		
		
		
		
		
		
		
		
		
		

		sock.close(false);
		
		NBSocket s = state.getNBSocketFromWrapper(sock);
		
		if(s == null) {
			NBLog.debug("Unable to find nbsocket that corresponds to given tcpsocket", NBLog.INTERESTING);
			return;
		}
		
		// We have detected that the underlying thread has died, so attempt to reestablish
		Entry e = state.getEntryFromNBSocket(s);
		if(e == null) {
			NBLog.error("handleEventConnErrDetectedReconnectIfNeeded - Unable to find given socket in socket map."+s.getDebugTriplet());
			return;
		}
				
		// What I need here is a way to id
		if(e.getState() == State.CONN_CLOSED_NEW) {
			return;
		}
		
//		 TODO: Should I set DEAD here? (I tried it and it seems like it doesn't work)
		
		// The recovery thread should ONLY be started if we were the ones the initiated the connection (eg we are the connector[client], rather than the connectee[serversocket])
		if(!e.getTriplet().areWeConnector()) {
			return;
		} 

		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+msg.getName()+ " - global-id: "+sock.getGlobalId()+"  "+e.getTriplet(), NBLog.INTERESTING);
		}

		if(e._lastSocketTLThatWeAttemptedToRecover != null && e._lastSocketTLThatWeAttemptedToRecover >= sock.getGlobalId()) {

			NBLog.debug("handleEventConnErrDetectedEtc - ignoring due to newer global id. last global id recovered:"+e._lastSocketTLThatWeAttemptedToRecover+"    param sock: "+sock.getGlobalId()+"  triplet: "+e.getTriplet(), NBLog.INTERESTING);
			
			// It is possible that we will be told to start the recovery thread multiple times for a single socket; 
			// we should only start it once per socket.
			return;
		}
		
		RecoveryThreadNew.RecoveryState recoveryState;
		
		if(e.getState() ==  State.CONN_CLOSING_INIT  
			|| e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY 
			|| e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY
			|| e.getState() == State.CONN_CLOSED_NEW
			|| e.getState() == State.CONN_DEAD_CLOSING_NEW) {
			
			e.setState(State.CONN_DEAD_CLOSING_NEW);
			
			recoveryState = RecoveryThreadNew.RecoveryState.STATE_CLOSING;
		} else if(e.getState() == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK) {			
			e.setState(State.CONN_INIT);
			recoveryState = RecoveryThreadNew.RecoveryState.STATE_INITIATE_EXISTING_CONN;
			
		} else {
			e.setState(State.CONN_DEAD);
			recoveryState = RecoveryThreadNew.RecoveryState.STATE_NOT_CLOSING;
		}

		e._lastSocketTLThatWeAttemptedToRecover = sock.getGlobalId();
		
		e._recoveryThreadNew.socketFailed(sock, recoveryState);
		
	}
		
	/** Connector/connectee */
	private static final void handleGetEntryState(ThreadState state, ISocketTLWrapper s, MQMessage msg) {
		NBSocket ni = state.getNBSocketFromWrapper(s);
		
		State result;
		
		if(ni == null)  {
			result = null;
		} else {				
			Entry e = state.getEntryFromNBSocket(ni);
			
			if(e == null) {
				result = null; 
			} else {
				result = e.getState();				
			}
		}
		
		msg.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, result, null));
		
	}
	
	/** Connector/connectee */
	private static void handleIsConnectionClosed(ThreadState state, NBSocket s, MQMessage msg) {
		
		Entry e = state.getEntryFromNBSocket(s);

		Boolean result;
		
		if(e == null) {
			NBLog.debug("handleIsConnectionClosed - Unable to find given socket in socket map."+s.getDebugTriplet(), NBLog.INTERESTING);
			result = false;
		} else {
			result = (e.getState() == State.CONN_CLOSED_NEW);
		}

		msg.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, result, null));
		
	}
	
	/** Connector/connectee */
	private static void handleIsConnectionEstablishedOrClosing(ThreadState state, NBSocket s, MQMessage msg) {
		Entry e = state.getEntryFromNBSocket(s);
		
		Boolean result;
		if(e == null) {
			NBLog.debug("handleIsConnectionEstablishedOrClosing - Unable to find given socket in socket map. "+s.getDebugTriplet(), NBLog.INTERESTING);
			result = false;
		} else {
			
//			if(e.getState() == State.CONN_DEAD_CLOSING_NEW) {
//				NBLog.error("I was in handleIsConnectionEstablishedOrClosing and the state was CONN_DEAD_CLOSING_NEW");
//			}
			
			result = (e.getState() == State.CONN_ESTABLISHED 
//					|| e.getState() == State.CONN_DEAD_CLOSING_NEW
					|| e.getState() == State.CONN_CLOSING_INIT 
					|| e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY 
					|| e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY 
					|| e.getState() == State.CONN_CLOSED_NEW);
		}
		
		msg.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, result, null));
		
	}

	/** Connector/connectee */
	private static void handleIsConnectionEstablishedOrEstablishing(ThreadState state, NBSocket s, MQMessage msg) {
		Entry e = state.getEntryFromNBSocket(s);
		
		Boolean result;
		if(e == null) {
			NBLog.debug("handleIsConnectionEstablishedOrEstablishing - Unable to find given socket in socket map, triplet: "+s.getDebugTriplet(), NBLog.INTERESTING);
			result = false;
		} else {
			
//			if(e.getState() == State.CONN_DEAD_CLOSING_NEW) {
//				NBLog.error("I was in handleIsConnectionEstablishedOrClosing and the state was CONN_DEAD_CLOSING_NEW");
//			}
			
			// A connection is established or establishing if it is not attempting to be closed.
			
			result = !(e.getState() == State.CONN_DEAD_CLOSING_NEW 
					|| e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY 
					|| e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY 
					|| e.getState() == State.CONN_CLOSED_NEW);

		}
		
		msg.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, result, null));
		
	}

	
	/** Connector/connectee */
	private static void handleEventFlush(ThreadState state, NBSocket s,  MQMessage m) {
		Entry e = state.getEntryFromNBSocket(s);
		
		if(e == null) {
			NBLog.error("Unable to find given socket in socket map.");
			return;
		}
		
		if(e.getState() != State.CONN_ESTABLISHED) {
			// dataSent being called while not connected, so buffer the data.
			NBLog.debug("Unable to flush: connection not in correct state: state:"+e.getState() + " "+e.getTriplet(), NBLog.INTERESTING);
		}


		if(e.getWrapper() != null) {
			e.getWrapper().flush();			
		} 
		
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+m.getName()+ " - "+e.getTriplet(), NBLog.INFO);
		}

	}

	
	/** Connector/connectee */
	private static void handleEventSendData(ThreadState state, NBSocket s, CmdData c, MQMessage m) {
		Entry e = state.getEntryFromNBSocket(s);

		boolean result = true;
		try {
		
			if(e == null) {
				NBLog.error("Unable to find given socket in socket map.");
				result = false;
				return;
			}

	//		if(e.getState() != State.CONN_ESTABLISHED) {
	//			// dataSent being called while not connected, so buffer the data.
	//			NBLog.debug("Unable to send data command: connection not in correct state: state:"+e.getState() + " cmd:"+c+ "  "+e.getTriplet(), NBLog.INTERESTING);
	//		}
	
			if(e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY 
					|| e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY 
					|| e.getState() == State.CONN_CLOSING_INIT 
					||  e.getState() == State.CONN_CLOSED_NEW 
					|| e.getState() == State.CONN_DEAD_CLOSING_NEW 
					) {
				
				result = false;
				return;
				
			}
		} finally {
			// If failed, send back failure
			if(!result) {
				m.getResponseQueue().addMessage(new MQMessage(null, ConnectionBrain.class, (Boolean)result, null));
			}
		}

		result = true;
		
		e._lastPacketIdSent = c.getFieldPacketId();
		e.addSentDataPacket(c, m.getResponseQueue(), state);
		
		// If sending data is valid, then send it
		if(/*e.getState() == State.CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY ||
				e.getState() == State.CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY ||
				e.getState() == State.CONN_CLOSING_INIT || */
				e.getState() == State.CONN_ESTABLISHED) {

			if(e.getWrapper() != null) {
				writeCommandToSocket(c, e.getWrapper(), e);
				
				NBLog.sent("HEDS-"+e.getState(), c, e.getTriplet(), e.getWrapper(), NBLog.INFO);
			} else {
				NBLog.severe("Unable to send the command as the socket was not set.");
			}

		} 
		
		if(NBLog.DEBUG) {
			NBLog.debug("process message: "+m.getName()+ " - "+e.getTriplet(), NBLog.INFO);
		}
			

	}
	
	// TODO: CURR - Add callbacks to remove polling
			

	public ISocketFactory getInnerFactory() {
		return _innerFactory;
	}
	
	private class ThreadBootstrap extends ManagedThread {
		
		final ConnectionBrain _brain;
		
		public ThreadBootstrap(ConnectionBrain brain) {
			super(ConnectionBrain.class.getName(), true);
			_brain = brain;
		}
		
		@Override
		public void run() {
			_brain.run();
		}
	}
	
	
} // End brain

class Entry {
	// TODO: LOWER - Consider referencing these through getters/setters. 
	
	static enum State {
		CONN_INIT,
		
		CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2, // A node has connected to us and sent us CmdNewConn, we have sent a reply and are waiting for their reply
		CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2, // A node has connected to us to re-establish a connect; it has sent us CmdJoin, we have sent a reply and are waiting for their reply. 
		
		CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK, // Connecting node is attempting to connect, has connected and sent NewConn cmd, is waiting for an reply 
		CONNECTOR_JOIN_SENT_WAITING_FOR_ACK, // Connecting node is attempting to rejoin a connection, has connected and sent CmdJoin, is waiting for reply
		
		CONNECTOR_JOIN_WAITING_FOR_DATA_REQUEST, // The connectee has acknowledged our join, and now we are waiting for them to send CmdDataRequestOnReconnect  
		CONNECTEE_JOIN_SENT_DATA_REQUEST_WAITING_FOR_ACK, // The connectee is waiting for joinack 2 before sending data request
		CONNECTOR_JOIN_WAITING_FOR_READY_TO_JOIN,
		
		CONN_ESTABLISHED, 	// Underlying socket connection is currently established and ready to send/receive data
		
		// TODO: It seems like CONN_DEAD is only set for the thread that will start recovery (the other problem goes from CONNECTED to join_received)
		CONN_DEAD,			// Underlying socket connection has died; we need to attempt to reestablish in recovery thread.
		
		CONN_CLOSING_INIT,  
		CONN_CLOSING_CONNECTEE__SENT_READY__WAITING_FOR_READY, 
		CONN_CLOSING_CONNECTOR__RECEIVED_READY__SENT_READY,
		CONN_CLOSED_NEW,

		CONN_DEAD_CLOSING_NEW
		
	}
	
	public Entry() {
	}
	
	private Triplet _triplet;
	
	public void setTriplet(Triplet triplet) {
		this._triplet = triplet;
	}
	
	public Triplet getTriplet() {
		return _triplet;
	}
	
	
	private State _state = State.CONN_INIT; 

	public State getState() {
		return _state;
	}
	
	public boolean isInState(State... states) {
		for(State param : states) {
			if(_state == param) {
				return true;
			}
		}
		return false;
	}
	
	public void setState(State state) {
//		NBLog.debug("[nbsock "+_nbSock.getDebugId() + "] triplet: "+_triplet+"   ["+_nbSock.isDebugFromServSock()+"] State "+_state.name()+" to "+state.name(), NBLog.INTERESTING);
		NBLog.stateChange(_nbSock, getTriplet(), getState(), state);
		this._state = state;
	}
	
	
	private ISocketTLWrapper _wrapper;
	
	public ISocketTLWrapper getWrapper() {
		return _wrapper;
	}
	
	public void setWrapper(ISocketTLWrapper wrapper) {
		NBLog.debug("Setting wrapper to: "+wrapper.getGlobalId()+" "+getTriplet(), NBLog.INFO);
		this._wrapper = wrapper;
//		Mapper.getInstance().put(_wrapper, value);
	}
	
	NBSocket _nbSock; // NBSocket that this entry is used for

	// TODO: CLEANUP - Anywhere else we can use triplet, rather htan the old string int and boolean? 
		
	int _port; // Port of the socket connection
	
	// Specialized threads
	Long _lastSocketTLThatWeAttemptedToRecover = null; // The Global ID of the last ISocketTLWrapper that we attempted to start the recovery thread on
	
	RecoveryThreadNew _recoveryThreadNew = null;
	
	
	/** Whether or not the connectee NBSocket has been returned through NBServerSocket.accept(). This can happen either through connect, or through join, depending on the state.
	 * This should only ever happen once for a single nbsocket.  */
	boolean _connecteeReturnedInServerSocketAccept = false;
	

	boolean _attemptingLocalClose = false;
		
	// Data Received by us -------
	int _lastPacketIdReceived = -1; // id of last received data packet
	long _dataReceivedInBytes = 0;
	
//	long _timeLastDataReceived; // time since we last received any data on this channel
//	long _timeLastDataReceivedInNanos; // time since we last received any data on this channel
	

	int _lastPacketIdAcknowledged = -1; // last data packet we sent for CmdDataReceived for
	
	long _timeSinceLastPacketIdAcknowledgedInNanos;
	
//	long _timeSinceLastPacketIdAcknowledged; // time since we sent the last CmdDataReceived for data received
	long _dataReceivedSinceLastPacketIdAcknowledged; // amount of data we've received since we last sent CmdDataReceived
	
	
	// Data Sent by us -------
	int _lastPacketIdSent = -1; // the id of the last packet we sent 

	final List<SentDataPacket> _waitingDataPackets = new ArrayList<SentDataPacket>();
	long _bytesInWaitingDataList = 0;
	
	final List<SentDataPacket> _sentDataPackets = new ArrayList<SentDataPacket>(); // the sent packets that the remote node has not yet confirmed
	long _bytesInSentDataList = 0;
	
	boolean _isInputPipeClosed = false;
	
	int _closeConnWaitingForLastPacketIdNew = -1;
	
	// Used by connector only	
	ISocketTLWrapper _socketOfConnectorIfSeenConnecteeReadyToClose = null;
	
	@Override
	public String toString() {
		return "Entry["+hashCode()+"]- triplet: "+_triplet+"  conn-id:State:"+_state + " recovery thread:"+_recoveryThreadNew+" "+" nbSock:["+_nbSock+"]";
	}
	
	/* EVENT_SEND_DATA response message queues */
	private List<MessageQueue> _waitingDataSenders = new ArrayList<MessageQueue>(); 
	
	public void assertNoWaitingDataPacket() {
		if(_waitingDataPackets.size() > 0) {
			NBLog.severe("Waiting data packets found.");
		}
	}
	
	public void assertNoAnyDataPacket() {
		assertNoWaitingDataPacket();

		if(_sentDataPackets.size() > 0) {
			NBLog.severe("Sent data packets found. "+_triplet);
		}
	}
	
	public void transferWaitingToSent() {
		transferWaitingToSent(null, false);	
	}
	
	public void transferWaitingToSent(ISocketTLWrapper sock, boolean send) {
		
		NBLog.debug("[data2] Transfer waiting to sent "+getTriplet()+ " "+getState().name(), NBLog.INFO);
		
		if(send && _waitingDataPackets.size() > 0) {
			assertInState(this, new State[] { State.CONN_ESTABLISHED});
			
			if(_sentDataPackets.size() > 0) {
				NBLog.severe("Sent data packets had data when it should not, size: "+_sentDataPackets.size()+" "+getTriplet());
			}
			
			for(SentDataPacket sdp : _waitingDataPackets) {
				writeCommandToSocket(sdp.getDataCmd(), getWrapper(), this);
				NBLog.sent(sdp.getDataCmd(), getTriplet(), getWrapper(), NBLog.INTERESTING);
			}
			
		}
		
		_sentDataPackets.addAll(_waitingDataPackets);
		_waitingDataPackets.clear();
		_bytesInSentDataList += _bytesInWaitingDataList;
		_bytesInWaitingDataList = 0;
//		_bytesInSentAndWaitingDataList = 0;
//		
//		for(SentDataPacket sdp : _sentDataPackets) {
//			_bytesInSentAndWaitingDataList += sdp.getDataCmd().getFieldDataLength();
//		}
		
	}
	
	
//	private static final long MAX_SENT_DATA_PACKET_BYTES = 1024;

	void addSentDataPacket(CmdData data, MessageQueue responseQueue, ThreadState state) {
		
		if(getState() == State.CONN_ESTABLISHED) {

			_sentDataPackets.add(new SentDataPacket(data));
			_bytesInSentDataList+= data.getFieldDataLength();
			
		} else {
			
			_waitingDataPackets.add(new SentDataPacket(data));
			_bytesInWaitingDataList += data.getFieldDataLength();
			
		}
		
		NBLog.debug("[data2] Adding data packet "+data+" "+getTriplet()+ " "+getState().name(), NBLog.INFO);
		
		String debugDest;
		
		// If this is a limit on the data receiver, and we are over it...
		if(state.options.getMaxDataReceivedBuffer() != -1 && 
				_bytesInWaitingDataList + _bytesInSentDataList > state.options.getMaxDataReceivedBuffer()) {
			// Add our callback to the list of waiting callbacks
			_waitingDataSenders.add(responseQueue);
			NBLog.debug("[data] Adding to waiting data senders: "+responseQueue.getDebug()+" "+getTriplet(), NBLog.INFO);
			debugDest = "waiting";
		} else {
			// We are not blocked, so return true
			responseQueue.addMessage(new MQMessage(null, ConnectionBrain.class, (Boolean)true, null));
			debugDest = "sent";
		}
		
		NBLog.debug("[data] Add sent data packet, status: ["+debugDest+"]  "+data+" "+responseQueue.getDebug(), NBLog.INFO);
	}

	
//	void addSentDataPacketOld(CmdData data, MessageQueue responseQueue) {
//		_sentDataPackets.add(new SentDataPacket(data));
//		_bytesInSentAndWaitingDataList += data.getFieldDataLength();
//		
//		NBLog.debug("[data2] Adding data packet "+data+" "+getTriplet(), NBLog.INTERESTING);
//		
//		String debugDest;
//		
//		if(_bytesInSentAndWaitingDataList > MAX_SENT_DATA_PACKET_BYTES) {
//			_waitingDataSenders.add(responseQueue);
//			NBLog.debug("[data] Adding to waiting data senders: "+responseQueue.getDebug()+" "+getTriplet(), NBLog.INFO);
//			debugDest = "waiting";
//		} else {
//			responseQueue.addMessage(new MQMessage(null, ConnectionBrain.class, (Boolean)true, null));
//			debugDest = "sent";
//		}
//		
//		NBLog.debug("[data] Add sent data packet, status: ["+debugDest+"]  "+data+" "+responseQueue.getDebug(), NBLog.INFO);
//	}
	
	List<SentDataPacket> getNewPacketsAndRemoveOld(int lastDataPacketToRemove, ThreadState state) {
		
		List<SentDataPacket> result = new ArrayList<SentDataPacket>();
		
		Collections.sort(_sentDataPackets);
		
		int nextExpectedDataPacket = lastDataPacketToRemove+1;
		
		for(Iterator<SentDataPacket> i = _sentDataPackets.iterator(); i.hasNext();) {
			SentDataPacket p = i.next();
			
			if(p.getDataCmd().getFieldPacketId() >= nextExpectedDataPacket) {
				 if(p.getDataCmd().getFieldPacketId() != nextExpectedDataPacket) {
					 NBLog.severe("Missing data packet: found " +p.getDataCmd().getFieldPacketId()+" expected "+nextExpectedDataPacket+" "+getTriplet());
				 } else {
					 nextExpectedDataPacket++;					 
				 }
			}

			if(p.getDataCmd().getFieldPacketId() > lastDataPacketToRemove) {
				result.add(p);
			} else {
				NBLog.debug("[data2] Removing data packet "+p.getDataCmd()+" "+getTriplet(), NBLog.INFO);
				i.remove();
				_bytesInSentDataList -= p.getDataCmd().getFieldDataLength();
			}
		}
		
		checkAndInformWaitingSenders(state);
		
		return result;

	}
	
	private void checkAndInformWaitingSenders(ThreadState state) {
		
		// If there is not a limit on the buffer, or we are under it...
		if(state.options.getMaxDataReceivedBuffer() == -1 || 
				_bytesInWaitingDataList + _bytesInSentDataList < state.options.getMaxDataReceivedBuffer()) {
			
			NBLog.debug("[data] checkAndInformWaitingSenders, clearing all: "+getTriplet(), NBLog.INFO);
			// Inform and clear all the waiting senders.
			for(MessageQueue mq : _waitingDataSenders) {
				mq.addMessage(new MQMessage(null, ConnectionBrain.class, (Boolean)true, null));
				
				NBLog.debug("[data] Removing waiting data sender: "+mq.getDebug()+" "+getTriplet(), NBLog.INFO);
			}
			_waitingDataSenders.clear();
		} else {
			if(_waitingDataSenders.size() > 0) {
				NBLog.debug("[data] checkAndInformWaitingSenders, NOT clearing all: "+getTriplet(), NBLog.INFO);
			}
		}
	}
		
	void clearSentPackets(ThreadState state) {
		
		for(SentDataPacket sdp : _sentDataPackets) {
			NBLog.debug("[data2] Removing data packet "+sdp.getDataCmd()+" "+getTriplet(), NBLog.INFO);	
		}
		_sentDataPackets.clear();
		_bytesInSentDataList = 0;
		checkAndInformWaitingSenders(state);
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
	
	private final String _uuid;
	private final int _connId;
	
	public PairUUIDConnID(String uuid, int connId) {
		_uuid = uuid;
		_connId = connId;
	}
	
	public PairUUIDConnID(Triplet t) {
		this._uuid = t.getConnectorUuid();
		this._connId = t.getConnectorId();
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


/** Connectee -  The purpose of this thread is to close the underlying socket after close completes. This thread is not
 * necessary if the connector properly terminates the socket after close (which it is supposed to do.)*/
class CmdCloseThreadNew extends ManagedThread {
	
	@SuppressWarnings("unused")
	private final NBSocket _socket;
//	private final String _uuidOfConnector;
//	private final int _connectionId;
	@SuppressWarnings("unused")
	private final Triplet _triplet;
	private final ISocketTLWrapper _wrapper;
	
	public CmdCloseThreadNew(ISocketTLWrapper wrapper, NBSocket socket, Triplet triplet, ConnectionBrain cb) {
		super(CmdCloseThreadNew.class.getName() + " "+triplet, true);
		
		assertBrainIsConnectee(cb);
		
		_triplet = triplet;
		_wrapper = wrapper;
		_socket = socket;
	}
	
	@Override
	public void run() {
		
		boolean continueLoop = false;
		// This loop will keep sending CmdClose until the connection brain 
		// receives an acknowledgment of the CmdClose (above), and closes the connection.

		long expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS);
		
		do {
			
			continueLoop = !_wrapper.isClosed();
						
			if(continueLoop) {
				// Connection not currently received, so keep waiting.
				try { Thread.sleep(4 * 1000); } catch (InterruptedException e) { }
			}
			
		} while(continueLoop && System.nanoTime() < expireTimeInNanos);
		
		if(continueLoop) {
			NBLog.debug("CmdCloseThread expired, and closed.", NBLog.INTERESTING);
		}

		// Close the socket
		_wrapper.close(false);
	}
}
