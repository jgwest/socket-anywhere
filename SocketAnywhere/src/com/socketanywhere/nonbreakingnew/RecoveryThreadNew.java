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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.socketanywhere.multiplexingnew.MQMessage;
import com.socketanywhere.multiplexingnew.MessageQueue;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.ITaggedSocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.util.ManagedThread;


/** When it is detected that a socket has died (specifically one where we initiated the connection, as opposed to a server socket), 
 * this thread spins up to try to establish a new socket. It keeps trying over and over again until a connection is re-established. */
public class RecoveryThreadNew extends ManagedThread {

	private final TLAddress _addr;
	
	private final Triplet _triplet;
	
	private final NBSocket _nbSocket;
	private final ConnectionBrainInterface _cbi;
	private final ConnectionBrain _cb;
	private final NBOptions _options;
	
	private AtomicLong _lastConnectAttemptInNanos = new AtomicLong(-1);
	
	public static enum RecoveryState { STATE_NOT_CLOSING, STATE_CLOSING, STATE_INITIATE_EXISTING_CONN, STATE_INITIATE_NEW_CONN}; 

	
	private final MessageQueue queue = new MessageQueue(this);
	
	public RecoveryThreadNew(NBSocket nbSocket, TLAddress addr, Triplet triplet, ConnectionBrain cb, NBOptions options) {
		super(RecoveryThreadNew.class.getName(), true);
		_nbSocket = nbSocket;
		_addr = addr;
		_triplet = triplet;
		
		NBLog.debug("@ Recovery thread started: "+triplet, NBLog.INTERESTING);
		
		_cb = cb;
		_cbi = cb.getInterface();
		_options = options;
	}
	
	public void run() {

		MyRecoveryThread currActiveThread = null;
		ISocketTLWrapper currActiveWrapper = null;
		boolean firstMessageReceived = false;
		
		NBLog.debug("@ Recovery thread running: "+_triplet, NBLog.INTERESTING);
		
		try {
			
			while(!_cbi.isConnectionClosed(_nbSocket)) { // !_nbSocket.isClosed()) { 
	
				if(!firstMessageReceived) {
					firstMessageReceived = true;
					NBLog.debug("First message received and processed: "+_nbSocket.getDebugTriplet(), NBLog.INTERESTING);
				}
				
				MQMessage msg = queue.getNextMessageBlockingTimeout(1000);
				
				if(msg != null) {
				
					Object[] params = (Object[])msg.getParam();
					
					RecoveryState state = (RecoveryState) params[0];
					
					if(state == RecoveryState.STATE_INITIATE_NEW_CONN) {
						
						if(currActiveThread != null || currActiveWrapper != null) {
							NBLog.severe("There is an existing curr active thread or wrapper when there shouldn't be.");
							continue;
						}
						
						NBLog.debug("RTN msg received: "+state.name()+" triplet: "+_triplet + "  state: "+state.name(), NBLog.INTERESTING);						
						
						currActiveThread = new MyRecoveryThread(state);
						currActiveThread.start();						
						
												
					} else {
						ISocketTLWrapper failingWrapper = (ISocketTLWrapper) params[1];
						
						boolean legit = currActiveWrapper == null || (failingWrapper.getGlobalId() > currActiveWrapper.getGlobalId() ) ;
						
						NBLog.debug("RTN msg received: " +state.name()+ " triplet: "+_triplet + "  state: "+state.name() + "  failing-wrapper-id: "+failingWrapper.getGlobalId() + "  legit: "+legit, NBLog.INTERESTING);
						
						if(legit) {
							
							if(currActiveThread != null) {
								// kill the old one
								// - close the socket represents
								currActiveThread.setKillThread(true);
							}
							
							currActiveThread = new MyRecoveryThread(state);
							currActiveThread.start();
							
							currActiveWrapper = failingWrapper;							
						}
						
					}
					
				}
				
			}
		} finally {
			NBLog.debug("RTN out, triplet: "+_triplet, NBLog.INTERESTING);
		}
		
	}
		
	// we have detected a failure on an nbsocket, and the isockettlwrapper that failed
	
	public void socketFailed(ISocketTLWrapper failingWrapper, RecoveryState state)  {
		Object[] params = new Object[2];
		params[0] = state;
		params[1] = failingWrapper;
		
		queue.addMessage(new MQMessage(null, this.getClass(), params, null));
		
	}
	
	public void initiateNewSocket() {
		Object[] params = new Object[1];
		params[0] = RecoveryState.STATE_INITIATE_NEW_CONN;

		queue.addMessage(new MQMessage(null, this.getClass(), params, null));
				
	}

	private static long convertToNanos(long val) {
		if(val <= 0) {
			return val;
		} else {
			return TimeUnit.NANOSECONDS.convert(val, TimeUnit.MILLISECONDS); 
		}
	}

	private class MyRecoveryThread extends ManagedThread {

		private final Object threadLock = new Object();
		private boolean threadKilled = false; 
		
		private final RecoveryState recoveryState;

		public MyRecoveryThread(RecoveryState recoveryState) {
			super(MyRecoveryThread.class.getName(), true);
			this.recoveryState = recoveryState;
		}

		@Override
		public void run() {
			ISocketTLWrapper validConn = null;
			
			final long TIME_TO_WAIT_FOR_JOIN_SUCCESS_IN_NANOS =
					convertToNanos(_options.getRecoveryThreadTimeToWaitForJoinSuccess() != -1 ? 
							_options.getRecoveryThreadTimeToWaitForJoinSuccess() : 10 * 1000);
			
			final long TIME_TO_WAIT_BTWN_FAILURES_IN_NANOS = 
					convertToNanos(_options.getRecoveryThreadTimeToWaitBtwnFailures() != -1 ? 
							_options.getRecoveryThreadTimeToWaitBtwnFailures() : 1 * 1000);

			long localLastConnectAttemptInNanos = _lastConnectAttemptInNanos.get();
			if(localLastConnectAttemptInNanos != -1 && TIME_TO_WAIT_BTWN_FAILURES_IN_NANOS != -1) {
				
				long timeToSleepInNanos = (localLastConnectAttemptInNanos + TIME_TO_WAIT_BTWN_FAILURES_IN_NANOS) - System.nanoTime();

				if(timeToSleepInNanos > 0) {
					try { TimeUnit.NANOSECONDS.sleep( timeToSleepInNanos  ); } catch (InterruptedException e) { e.printStackTrace(); }
				}
				
			}
			
			_lastConnectAttemptInNanos.set(System.nanoTime());
						
			boolean continueLoop = true;
			
			while(continueLoop) {
			
				ISocketTLWrapper localValidConn = null;
				try {
					ISocketTL newSock = _cb.getInnerFactory().instantiateSocket(_addr);
					
					if(newSock != null) {
						
						localValidConn = new ISocketTLWrapper(newSock, false, _cb);
						if(newSock instanceof ITaggedSocketTL) {
							Map<String, Object> m = ((ITaggedSocketTL)newSock).getTagMap();
							synchronized(m) {
								m.put("id", (Long)localValidConn.getGlobalId());
							}
							
						}
						
						synchronized(threadLock) {
							validConn = localValidConn;

							if(threadKilled) {
								NBLog.debug("RTN - Thread killed1: "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
																
								// TODO: CURR - What about all those empty unassociated sockets I was leaving open?
								
								try { validConn.close(false); } catch(Exception e) {}
								_cbi.eventConnErrDetectedReconnectIfNeeded(validConn); // TODO: CURR - Passing an unassociated connection doesn't seem useful?
								
								return;
							}
						}
						if(localValidConn != null) {
							continueLoop = false;
						}
						
					} else {
						localValidConn = null;
						continueLoop = true;
					}
					
				} catch (Throwable e1) {
					localValidConn = null;
					continueLoop = true;
				}
				
				if(continueLoop && TIME_TO_WAIT_BTWN_FAILURES_IN_NANOS > 0) {
					try { TimeUnit.NANOSECONDS.sleep(TIME_TO_WAIT_BTWN_FAILURES_IN_NANOS); } catch (InterruptedException e) { /* ignore */ }
				}
			
			} // end-while
			
			
			// The new socket has been initialized, so start listening on it.
			NBSocketListenerThread socketListenerThread = new NBSocketListenerThread(validConn, _cb, _options);
			socketListenerThread.start();

			if(recoveryState == RecoveryState.STATE_CLOSING) {
				
				NBLog.debug("RTN - initiateJoinClose: "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
				
				_cbi.initiateJoinClose(_triplet.getConnectorUuid(), _triplet.getConnectorId(), validConn);
			
			} else if(recoveryState == RecoveryState.STATE_NOT_CLOSING) {
				
				NBLog.debug("RTN - initiateJoin: "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
				
				_cbi.initiateJoin(_triplet.getConnectorUuid(), _triplet.getConnectorId(), validConn);
				
			} else if(recoveryState == RecoveryState.STATE_INITIATE_EXISTING_CONN) {
				
				NBLog.debug("RTN - initiateConnectionExisting: "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
				
				_cbi.initiateConnectionExisting(_nbSocket, validConn);
				
			} else if(recoveryState == RecoveryState.STATE_INITIATE_NEW_CONN) {
				
				NBLog.debug("RTN - initiateConnection: "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
				_cbi.initiateConnection(_nbSocket, validConn);
			
			} else {
				NBLog.severe("Unrecognized recovery state.");
				return;
			}
			
			
			long startTimeInNanos = System.nanoTime();
			while(
					(TIME_TO_WAIT_FOR_JOIN_SUCCESS_IN_NANOS != -1 && System.nanoTime() - startTimeInNanos <= TIME_TO_WAIT_FOR_JOIN_SUCCESS_IN_NANOS) 
					&&  !_cbi.isConnectionEstablishedOrClosing(_nbSocket) 
					&& (validConn.isConnected() && !validConn.isClosed()) )  {
				
				// WAIT for change in connection brain state, or timeout
				synchronized(threadLock) {
					if(threadKilled) {
						NBLog.debug("RTN - Thread killed2: "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
						break;
					} else {
						try {
							NBLog.debug("Waiting for recovery thread join/connect/initiate-connect: "+ TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeInNanos, TimeUnit.NANOSECONDS) + "  "+_triplet+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
							threadLock.wait(1 * 1000); // TODO: CURR - This can be reduced.
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				
			}
			
			if(_cbi.isConnectionEstablishedOrClosing(_nbSocket)  && !threadKilled) {
				NBLog.debug("RTN - success! "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
				// Success!
			} else {
				
				NBLog.debug("RTN - failed  "+_nbSocket.getDebugTriplet()+" global-id: "+validConn.getGlobalId(), NBLog.INTERESTING);
				
				if(!validConn.isClosed() && ( System.nanoTime() - startTimeInNanos > TIME_TO_WAIT_FOR_JOIN_SUCCESS_IN_NANOS && TIME_TO_WAIT_FOR_JOIN_SUCCESS_IN_NANOS != -1)) {
					
					Entry e = _cbi.debugGetEntry(_nbSocket);
					NBLog.severe("Recovery thread - Timed out on recovery thread ("+TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeInNanos, TimeUnit.NANOSECONDS)+" msecs).  triplet: "+_triplet + "  entry-state:"+e.getState());
				}
				
				socketListenerThread.stopParsing();
				try { validConn.close(false); } catch(Exception e) {}
				
				_cbi.eventConnErrDetectedReconnectIfNeeded(validConn);
				
			}
				
		}
		
		
		public void setKillThread(boolean killThread) {
			synchronized(threadLock) {
				this.threadKilled = killThread;
				threadLock.notify();
			}
		}
	}
}
