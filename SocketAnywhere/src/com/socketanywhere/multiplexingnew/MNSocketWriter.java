/*
	Copyright 2017 Jonathan West

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
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.socketanywhere.multiplexing.CmdMultiplexAbstract;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.util.ManagedThread;

public class MNSocketWriter extends ManagedThread {

	final MessageQueue _mq;
	final ISocketTL _innerSocket;
	
	/** False if the thread is running, true otherwise*/
	private final AtomicBoolean _isNowDead = new AtomicBoolean(false);
	
	private static final AtomicLong _openSockets = new AtomicLong(0);
	
	private final TLAddress address;

	
	@Override
	public void start() {
		
		if(MNConnectionBrain.USE_NEW) {
			super.start();
		} else {
			return;
		}
		
	}
	
	public MNSocketWriter(ISocketTL innerSocket, int x) {
		super(MNSocketWriter.class.getName(), true);
		setPriority(Thread.NORM_PRIORITY+2);
		
		address = innerSocket.getAddress();
		
		_innerSocket = innerSocket;
		_mq = new MessageQueue(this.getClass().getName()+"-"+_innerSocket.toString());
	}
	
	private enum MessageType { SEND_CMD, CLOSE_SOCKET, IS_CONNECTED, IS_CLOSED, FLUSH };
	
	@Override
	public void run() {
		
		MnLog.debug("MNSocketWriter started." + this.toString());
		
		// Whether or not we have already informed the CB that this connection has died
		boolean informedCBThatConnectionHasDied = false;
		
		if(_innerSocket instanceof MNMultSocket) {
			MnLog.error("Invalid inner socket.");
			setDead(true);
			throw new IllegalArgumentException("Invalid inner socket.");
		}
		
		if(!_innerSocket.isConnected()) { 
			MnLog.error("Socket in SOW in DOA.  "+_innerSocket);
		}

		_openSockets.incrementAndGet();
		
		long connectExpiryTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS);
		
		final long ONE_SECOND_IN_NANOS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

		try {
			OutputStream os = null;
			try {
				os = _innerSocket.getOutputStream();
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new RuntimeException(e1);
			} 
			
			// After we have signalled that we are dead, we should process one more round of messages, to make sure we get any that are queued after setting it.
			boolean oneLastMessageProcessAfterSettingDead = false;
			
	
			while(!isNowDead() || ( oneLastMessageProcessAfterSettingDead == false ) ) {
				
				if(isNowDead()) {
					oneLastMessageProcessAfterSettingDead = true;
				}
			
				Queue<MQMessage> q = _mq.getNextMessagesBlockingTimeout(ONE_SECOND_IN_NANOS);
				
				boolean setExpiry = false;
				
				if(q.size() > 0) {
					MnLog.debug("MNSW processing new messages: "+q.size()+" "+this.toString());
				}
				
				
//				int numSendCmds = 0;
//				for(MQMessage m : q) {
//					Object[] params = (Object[])m.getParam();
//					
//					MessageType msgType = (MessageType)params[0];
//					
//					if(msgType == MessageType.SEND_CMD) {
//						
//						CmdMultiplexAbstract data= (CmdMultiplexAbstract)params[1];
//						numSendCmds++;
//					}					
//				}
//				if(numSendCmds > 1) {
//					System.out.println("num send cmds:"+numSendCmds);
//				}
				
				for(MQMessage m : q) {
					
					Object[] params = (Object[])m.getParam();
					
					MessageType msgType = (MessageType)params[0];
					
					if(msgType == MessageType.SEND_CMD) {
						
						CmdMultiplexAbstract data= (CmdMultiplexAbstract)params[1];

						try {
						
							MnLog.debug("MNSW received:" +m+" data: "+data+"  writer:"+this.toString());
							
							if(_innerSocket.isConnected() && !_innerSocket.isClosed()) {
								
								MnLog.debug("SOW Sending data: "+data+ " "+_innerSocket+ " writer:"+this.toString());
								
								try {
									os.write(data.buildCommand());
	//								os.flush();
	//								innerSockWrapper.internalWriteBytes(cmd.buildCommand());
								} catch (IOException e) {
									e.printStackTrace();
									MnLog.error("Error occured on writing data to output socket "+_innerSocket);
									// TODO: CURR - Add back or remove error detection
	//								_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
									
	//								NBLog.debug("[jgw] site 4 "+_innerSockWrapper.getGlobalId(), NBLog.INTERESTING);
									informedCBThatConnectionHasDied = true;
									setExpiry = true;
			//						inter.eventWriteToSocketFailed(nb);
								}
							}
						
						} finally {
							// Send arbitrary response
							MQMessage originalMsg = (MQMessage)params[2];
							if(originalMsg != null) {
								originalMsg.getResponseQueue().addMessage(new MQMessage(null, null, null, null));
							}
						}
						
						
					} else if(msgType == MessageType.CLOSE_SOCKET) {
						
						MnLog.debug("SOW closing socket. "+_innerSocket);
						
						setExpiry = true;
						try {
							_innerSocket.close();
						} catch (IOException e) {
							// Ignore
						}
						
						// If is blocking close, then send a reply
						if(m.getResponseQueue() != null) {
							m.getResponseQueue().addMessage(new MQMessage(null, null, null, null));
						}

					} else if(msgType == MessageType.FLUSH) {
						
						// Send arbitrary response
						MQMessage originalMsg = (MQMessage)params[1];
						if(originalMsg != null) {
							originalMsg.getResponseQueue().addMessage(new MQMessage(null, null, null, null));
						}
						
						try {
							os.flush();
						} catch (IOException e) {
							e.printStackTrace();
//							_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
//							NBLog.debug("[jgw] site 5 "+_innerSockWrapper.getGlobalId(), NBLog.INTERESTING);
							informedCBThatConnectionHasDied = true;
							setExpiry = true;
						}

					} else if(msgType == MessageType.IS_CONNECTED) {
						m.getResponseQueue().addMessage(new MQMessage(null, null, (Boolean)_innerSocket.isConnected(), null));;

					} else if(msgType == MessageType.IS_CLOSED) {
						m.getResponseQueue().addMessage(new MQMessage(null, null, (Boolean)_innerSocket.isClosed(), null));;
						
					} else {
						MnLog.error("Unrecognized SOW msg: "+msgType.name()+" "+_innerSocket);
						return;
					}
				} // end for
				
				// This logic would previously check to see that the socket was still active
//				Entry.State currEntryState = _inter.entryState(_innerSockWrapper);
//				if(currEntryState == State.CONN_CLOSED_NEW || currEntryState == State.CONN_DEAD_CLOSING_NEW 
//						|| currEntryState == State.CONN_DEAD) {
//					setExpiry = true;
//				}
				
				if(!_innerSocket.isConnected() || _innerSocket.isClosed()) {
					setExpiry = true;
				}

//				// If we are not connected after 30 seconds, then start the recovery thread 
				if(!setExpiry && connectExpiryTimeInNanos != -1 && connectExpiryTimeInNanos < System.nanoTime()) {

//					// If the connection is tenative (has not yet been established)
//					if( currEntryState == State.CONN_INIT  
//							||  currEntryState == State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2
//							|| currEntryState == State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2 
//							|| currEntryState == State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK 
//							|| currEntryState == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK
//							|| currEntryState == State.CONNECTOR_JOIN_WAITING_FOR_DATA_REQUEST
//							|| currEntryState == State.CONNECTEE_JOIN_SENT_DATA_REQUEST_WAITING_FOR_ACK
//							|| currEntryState == State.CONNECTOR_JOIN_WAITING_FOR_READY_TO_JOIN
//							|| currEntryState == null) {		
//						
//						MnLog.debug("SocketOutWriter informing expired. global-id: "+_innerSocket);
//						_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
////						NBLog.debug("[jgw] site 6 "+_innerSockWrapper.getGlobalId(), NBLog.INTERESTING);
//						informedCBThatConnectionHasDied = true;
//						setExpiry = true;
//
//					} else {
						// The connection is either established, closed, or dead; in any case, this is acceptable.
						connectExpiryTimeInNanos = -1;
						
//					}
					
				}

				// The thread will stay alive for up to 10 seconds after it has closed.
				if(setExpiry && !isNowDead()) {

					MnLog.debug("SocketOutWriter has set expiry. global-id: "+_innerSocket);
					
					setDead(true);
				}
				
			} // end outer while
		
		} finally { // end try
			setDead(true);

			if(!informedCBThatConnectionHasDied /*&& currEntryState != null && currEntryState != State.CONN_CLOSED_NEW && currEntryState != State.CONN_DEAD && currEntryState != State.CONN_DEAD_CLOSING_NEW*/) {
				// TODO: CURR - Add back or remove error detection
//				_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
			}
	
			MnLog.debug("MNSocketOutWriter is out. "+_innerSocket);			
			MnLog.debug("MN open sockets: "+_openSockets.decrementAndGet());
		
			try { _innerSocket.close(); } catch (IOException e) { /* ignore */ }
		}
		
	}
	
	public void flush(MQMessage originalMessage) {
		synchronized(_isNowDead) {
			if(isNowDead()) {
				return;
			}
			
			Object[] params = new Object[2];
			params[0] = MessageType.FLUSH;
			params[1] = originalMessage;
			
			_mq.addMessage(new MQMessage(null, MNSocketWriter.class, params, null ));
		} // end synch

	}
	
	
	public boolean isConnected() {
		MessageQueue responseQueue = new MessageQueue(this);
		
		synchronized(_isNowDead) {
			
			if(isNowDead()) {
				return false;
			}
	
			Object[] params = new Object[1];
			params[0] = MessageType.IS_CONNECTED;
			
			_mq.addMessage(new MQMessage(null, MNSocketWriter.class, params, responseQueue));		
			
		} // end synch
		
		MQMessage resp = responseQueue.getNextMessageBlocking();
		
		return (Boolean)resp.getParam();
		
		
	}

	public boolean isClosed() {
		MessageQueue responseQueue = new MessageQueue(this);
		
		synchronized(_isNowDead) {
			
			if(isNowDead()) {
				return true;
			}
			
			Object[] params = new Object[1];
			params[0] = MessageType.IS_CLOSED;
					
			_mq.addMessage(new MQMessage(null, MNSocketWriter.class, params, responseQueue));		
			
		} // end synch
		
		MQMessage resp = responseQueue.getNextMessageBlocking();
		
		return (Boolean)resp.getParam();		
	}

	
	public void closeSocket(boolean blocking) {
		MessageQueue responseQueue = null;
		
		if(blocking) {
			responseQueue = new MessageQueue(this);
		}
		
		synchronized(_isNowDead) {
			if(isNowDead()) {
				return;
			}
			
			Object[] params = new Object[1];
			params[0] = MessageType.CLOSE_SOCKET;
			
			
			
			_mq.addMessage(new MQMessage(null, MNSocketWriter.class, params, responseQueue));
			
		} // end synch

		if(blocking) {
			responseQueue.getNextMessageBlocking();
		}

		
	}
	
	public void sendCommand(CmdMultiplexAbstract data, MQMessage msg) {
		
		synchronized(_isNowDead) {
			
			if(isNowDead()) {
				MnLog.debug("sendCommand received is SOW when is dead: " +data);
				return;
			}
			
			Object[] params = new Object[3];
			params[0] = MessageType.SEND_CMD;
			params[1] = data;
			params[2] = msg;
			
			_mq.addMessage(new MQMessage(null, MNSocketWriter.class, params, null ));
		} // end synch
		
	}
	
	public ISocketTL debugGetInnerSock() {
		return _innerSocket;
	}
	
	private boolean isNowDead() {
		synchronized(_isNowDead) {
			return _isNowDead.get();
		}
	}
	
	private void setDead(boolean val) {
		synchronized(_isNowDead) {
			_isNowDead.set(val);
		}
	}
	
	public TLAddress getAddress() {
		return address;
	}


	public ISocketTL getInnerSocket() {
		return _innerSocket;
	}
}
