package com.socketanywhere.nonbreakingnew;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.socketanywhere.multiplexingnew.MQMessage;
import com.socketanywhere.multiplexingnew.MessageQueue;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.nonbreakingnew.Entry.State;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;
import com.socketanywhere.util.ManagedThread;

class SocketOutWriter extends ManagedThread {

	private final MessageQueue _mq = new MessageQueue(SocketOutWriter.class.getName());
	private final ISocketTLWrapper _innerSockWrapper;
	
	private final ISocketTL _innerSock;
	private final ConnectionBrainInterface _inter;
	
	/** False if the thread is running, true otherwise*/
	private final AtomicBoolean _isNowDead = new AtomicBoolean(false);
	
	private static final AtomicLong _openSockets = new AtomicLong(0);
	
	public SocketOutWriter(/*NBSocket nb,*/ ISocketTL sock, ISocketTLWrapper innerSock, ConnectionBrain cb) {
		super(SocketOutWriter.class.getName(), true);
		this._innerSock = sock;
		this._innerSockWrapper = innerSock;
		this._inter = cb.getInterface();
	}
	
	private enum MessageType { SEND_CMD, CLOSE_SOCKET, IS_CONNECTED, IS_CLOSED, FLUSH };
	
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
	
	
	@Override
	public void run() {
		
		long lastDataCmdIdWritten = -1;
		
		// Whether or not we have already informed the CB that this connection has died (and thus should start the recovery thread)
		boolean informedCBThatConnectionHasDied = false;
		
		if(_innerSock instanceof NBSocket) {
			NBLog.severe("Invalid inner socket.");
			setDead(true);
			throw new IllegalArgumentException("Invalid inner socket.");
		}
		
		if(!_innerSock.isConnected()) { 
			NBLog.severe("Socket in SOW in DOA.  global-id:"+_innerSockWrapper.getGlobalId());
		}

		_openSockets.incrementAndGet();
		
		Triplet outTriplet = null;
		
		long connectExpiryTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS);
		
		final long ONE_SECOND_IN_NANOS = TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);

		try {
			OutputStream os = null;
			try {
				os = _innerSock.getOutputStream();
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			} 
			
			// After we have signalled that we are dead, we should process one more round of messages, to make sure we get any that are queued after setting it.
			boolean oneLastMessageProcessAfterSettingDead = false;
			
	
//			while(threadDeathExpiryInNanos == null || System.nanoTime() < threadDeathExpiryInNanos) {
			
			while(!isNowDead() || ( oneLastMessageProcessAfterSettingDead == false ) ) {
				
				if(isNowDead()) {
					oneLastMessageProcessAfterSettingDead = true;
				}
			
	
				Queue<MQMessage> q = _mq.getNextMessagesBlockingTimeout(ONE_SECOND_IN_NANOS);
				
				boolean setExpiry = false;
				
				for(MQMessage m : q) {
					
					Object[] params = (Object[])m.getParam();
					
					MessageType msgType = (MessageType)params[0];
										
					if(msgType == MessageType.SEND_CMD) {
						Triplet msgTriplet = (Triplet)params[2];
						
						if(outTriplet == null) {
							outTriplet = msgTriplet;
						} else {
							if(!outTriplet.equals(msgTriplet)) {
								NBLog.severe("Attempting to write out a command for a different triplet then was previously written: msg: "+msgTriplet+ "  prev: "+outTriplet);
								
							}
						}
						
						if(_innerSock.isConnected() && !_innerSock.isClosed()) {
						
							CmdAbstract cmd = (CmdAbstract)params[1];

							if(cmd.getId() == CmdData.ID) {
								CmdData data = (CmdData)cmd;
								if(lastDataCmdIdWritten != -1 && data.getFieldPacketId() != lastDataCmdIdWritten+1 && data.getFieldPacketId() != lastDataCmdIdWritten) {
//									NBLog.error("Received data out of order. data packet id: "+data.getFieldPacketId() + " lastData packet id written: " +lastDataCmdIdWritten);
								} else {
									lastDataCmdIdWritten = data.getFieldPacketId();
								}
								
								NBLog.debug("SOW Sending command: "+cmd+" global-id: "+_innerSockWrapper.getGlobalId(), NBLog.INFO);
							} else {
								NBLog.debug("SOW Sending command: "+cmd+ " global-id: "+_innerSockWrapper.getGlobalId(), NBLog.INTERESTING);
							}
							
							try {
								os.write(cmd.buildCommand());
//								os.flush();
//								innerSockWrapper.internalWriteBytes(cmd.buildCommand());
							} catch (IOException e) {
								_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
								informedCBThatConnectionHasDied = true;
								setExpiry = true;
		//						inter.eventWriteToSocketFailed(nb);
							}
						}
						
					} else if(msgType == MessageType.CLOSE_SOCKET) {
						
						NBLog.debug("SOW closing socket. global-id: "+_innerSockWrapper.getGlobalId(), NBLog.INTERESTING);
						
						setExpiry = true;
						try {
							_innerSock.close();
						} catch (IOException e) {
							// Ignore
						}
						
						// If is blocking close, then send a reply
						if(m.getResponseQueue() != null) {
							m.getResponseQueue().addMessage(new MQMessage(null, null, null, null));
						}

					} else if(msgType == MessageType.FLUSH) {
						try {
							os.flush();
						} catch (IOException e) {
							_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
							informedCBThatConnectionHasDied = true;
							setExpiry = true;
						}

					} else if(msgType == MessageType.IS_CONNECTED) {
						m.getResponseQueue().addMessage(new MQMessage(null, null, (Boolean)_innerSock.isConnected(), null));;

					} else if(msgType == MessageType.IS_CLOSED) {
						m.getResponseQueue().addMessage(new MQMessage(null, null, (Boolean)_innerSock.isClosed(), null));;
						
					} else {
						NBLog.severe("Unrecognized SOW msg: "+msgType.name()+" global-id: "+_innerSockWrapper.getGlobalId());
						return;
					}
				} // end for
				
				Entry.State currEntryState = _inter.entryState(_innerSockWrapper);
				if(currEntryState == State.CONN_CLOSED_NEW || currEntryState == State.CONN_DEAD_CLOSING_NEW 
						|| currEntryState == State.CONN_DEAD) {
					setExpiry = true;
				}
				
//				if(currEntryState == null) {
//					System.out.println("null "+ " "+ wasNotNullAtSomePoint+ " "+_innerSockWrapper.getGlobalId());
//				} else {
//					wasNotNullAtSomePoint = true;
//				}

//				NBLog.out("Current state: ["+_innerSockWrapper.getGlobalId()+"] "+_innerSock.isClosed()+ " "+_innerSock.isConnected());
								
				if(!_innerSock.isConnected() || _innerSock.isClosed()) {
					setExpiry = true;
				}

//				// If we are not connected after 30 seconds, then start the recovery thread 
				if(!setExpiry && connectExpiryTimeInNanos != -1 && connectExpiryTimeInNanos < System.nanoTime()) {

					// If the connection is tenative (has not yet been established)
					if( currEntryState == State.CONN_INIT  
							||  currEntryState == State.CONNECTEE_JOIN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2
							|| currEntryState == State.CONNECTEE_NEW_CONN_RECEIVED_ACK1_SENT_WAITING_FOR_ACK2 
							|| currEntryState == State.CONNECTOR_JOIN_SENT_WAITING_FOR_ACK 
							|| currEntryState == State.CONNECTOR_NEW_CONN_SENT_WAITING_FOR_ACK
							|| currEntryState == State.CONNECTOR_JOIN_WAITING_FOR_DATA_REQUEST
							|| currEntryState == State.CONNECTEE_JOIN_SENT_DATA_REQUEST_WAITING_FOR_ACK
							|| currEntryState == State.CONNECTOR_JOIN_WAITING_FOR_READY_TO_JOIN
							|| currEntryState == null) {		
						
						NBLog.debug("SocketOutWriter informing expired. global-id: "+_innerSockWrapper.getGlobalId(), NBLog.INFO);
						_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);
//						NBLog.debug("[jgw] site 6 "+_innerSockWrapper.getGlobalId(), NBLog.INTERESTING);
						informedCBThatConnectionHasDied = true;
						setExpiry = true;

					} else {
						// The connection is either established, closed, or dead; in any case, this is acceptable.
						connectExpiryTimeInNanos = -1;
						
					}
					
				}

				// The thread will stay alive for up to 10 seconds after it has closed.
				if(setExpiry && !isNowDead()) {

					NBLog.debug("SocketOutWriter has set expiry. global-id: "+_innerSockWrapper.getGlobalId(), NBLog.INFO);
					
					setDead(true);
				}
				
			} // end outer while
		
		} finally { // end try
			setDead(true);

			// If the connection for this is still valid, then start the recovery thread.
			Entry.State currEntryState = _inter.entryState(_innerSockWrapper);
			if(currEntryState != null) {
				if(!informedCBThatConnectionHasDied && currEntryState != null && currEntryState != State.CONN_CLOSED_NEW && currEntryState != State.CONN_DEAD && currEntryState != State.CONN_DEAD_CLOSING_NEW) {				
					_inter.eventConnErrDetectedReconnectIfNeeded(_innerSockWrapper);					
				}
	
				NBLog.debug("SocketOutWriter is out. "+currEntryState.name()+ " global-id:"+_innerSockWrapper.getGlobalId(), NBLog.INFO);
			}
			
			NBLog.debug("open sockets: "+_openSockets.decrementAndGet(), NBLog.INTERESTING);
			
			_innerSockWrapper.close(false); 
		}
		
	}
	
	public void flush() {
		synchronized(_isNowDead) {
			if(isNowDead()) {
				return;
			}
			
			Object[] params = new Object[1];
			params[0] = MessageType.FLUSH;
			
			_mq.addMessage(new MQMessage(null, ISocketTLWrapper.class, params, null ));
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
			
			_mq.addMessage(new MQMessage(null, ISocketTLWrapper.class, params, responseQueue));		
			
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
					
			_mq.addMessage(new MQMessage(null, ISocketTLWrapper.class, params, responseQueue));		
			
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
			
			
			
			_mq.addMessage(new MQMessage(null, ISocketTLWrapper.class, params, responseQueue));
			
		} // end synch

		if(blocking) {
			responseQueue.getNextMessageBlocking();
		}

		
	}
	
	public void sendCommand(CmdAbstract cmd, Triplet t) {
		
		synchronized(_isNowDead) {
			
			if(isNowDead()) {
				NBLog.debug("sendCommand received is SOW when is dead: " +cmd, NBLog.INTERESTING);
				return;
			}
			
			Object[] params = new Object[3];
			params[0] = MessageType.SEND_CMD;
			params[1] = cmd;
			params[2] = t;
			
			_mq.addMessage(new MQMessage(null, ISocketTLWrapper.class, params, null ));
		} // end synch
		
	}
	
	public ISocketTL debugGetInnerSock() {
		return _innerSock;
	}
}