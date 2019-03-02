package com.socketanywhere.nonbreakingnew;

import com.socketanywhere.multiplexingnew.MQMessage;
import com.socketanywhere.multiplexingnew.MessageQueue;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.ConnectionBrain.ThreadState;
import com.socketanywhere.nonbreakingnew.Entry.State;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;

public class ConnectionBrainInterface {
	
	// not immutable
	private final MessageQueue _queue;	

	
	public ConnectionBrainInterface(MessageQueue queue) {
		this._queue = queue;
	}

	// ---------------------------------

	static final String DEBUG_GET_THREAD_STATE_AND_STOP = "DEBUG_GET_THREAD_STATE_AND_STOP";

	public ThreadState getThreadStateAndStop() {
		MessageQueue response = new MessageQueue(DEBUG_GET_THREAD_STATE_AND_STOP);
		
		MQMessage m = new MQMessage(DEBUG_GET_THREAD_STATE_AND_STOP, ConnectionBrain.class, null, response);		
		_queue.addMessage(m);
		
		MQMessage result = response.getNextMessageBlocking();
		
		return (ThreadState)result.getParam();
	
	}
	
	
	static final String DEBUG_GET_ENTRY = "DEBUG_GET_ENTRY"; 
	
	public Entry debugGetEntry(NBSocket socket) {		
		
		MessageQueue response = new MessageQueue(DEBUG_GET_ENTRY);
		
		MQMessage m = new MQMessage(DEBUG_GET_ENTRY, ConnectionBrain.class, socket, response);		
		_queue.addMessage(m);
		
		MQMessage result = response.getNextMessageBlocking();
		
		return (Entry)result.getParam();
		
	}
	
	
	

	static final String ADD_SERV_SOCK_LISTENER = "ADD_SERV_SOCK_LISTENER";
	
	public void addServSockListener(TLAddress addr, NBServerSocketListener listener) {
		
		Object[] obj = new Object[2];
		obj[0] = new TLAddress(addr.getPort());
		obj[1] = listener;
		
		MQMessage m = new MQMessage(ADD_SERV_SOCK_LISTENER, ConnectionBrain.class, obj, null);		
		_queue.addMessage(m);
		
	}
	
	static final String REMOVE_SOCK_LISTENER = "REMOVE_SOCK_LISTENER"; 
	
	public void removeSockListener(TLAddress addr) {
		Object[] obj = new Object[1];
		obj[0] = addr;

		MQMessage m = new MQMessage(REMOVE_SOCK_LISTENER, ConnectionBrain.class, obj, null);
		_queue.addMessage(m);
		
	}
	

	static final String INITIATE_CONNECTION_EXISTING = "INITIATE_CONNECTION_EXISTING";	

	/** On a given nbsocket, send the commands to create a new connection. */
	public void initiateConnectionExisting(NBSocket s, ISocketTLWrapper wrapper) {
		MessageQueue responseQueue = new MessageQueue(INITIATE_CONNECTION_EXISTING);
		
		Object[] obj = new Object[2];
		obj[0] = s;
		obj[1] = wrapper;
		
		MQMessage m = new MQMessage(INITIATE_CONNECTION_EXISTING, ConnectionBrain.class, obj, responseQueue);
		_queue.addMessage(m);
		
	}

	
	static final String INIT_CONNECTION = "INIT_CONNECTION";
	/** On a given nbsocket, send the commands to create a new connection. */
	public Triplet initConnection(NBSocket s) {
		
		MessageQueue responseQueue = new MessageQueue(INIT_CONNECTION);
				
		MQMessage m = new MQMessage(INIT_CONNECTION, ConnectionBrain.class, s, responseQueue);
		_queue.addMessage(m);

		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Triplet)response.getParam();

	}
	
	
	static final String INITIATE_CONNECTION = "INITIATE_CONNECTION";	
	
	/** On a given nbsocket, send the commands to create a new connection. */
	public Triplet initiateConnection(NBSocket s, ISocketTLWrapper wrapper) {
		
		MessageQueue responseQueue = new MessageQueue(INITIATE_CONNECTION);
		
		Object[] params = new Object[2];
		params[0] = s;
		params[1] = wrapper;
		
		MQMessage m = new MQMessage(INITIATE_CONNECTION, ConnectionBrain.class, params, responseQueue);
		_queue.addMessage(m);

		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Triplet)response.getParam();

	}
	

	static final String INITIATE_JOIN_CLOSE = "INITIATE_JOIN_CLOSE";
	
	/** Called by recovery thread: initiate join to an existing conection */
	public void initiateJoinClose(String nodeUUID, int connId, ISocketTLWrapper validConn) {
		Object[] params = new Object[3];
		params[0] = nodeUUID;
		params[1] = connId;
		params[2] = validConn;
		
		MQMessage msg = new MQMessage(INITIATE_JOIN_CLOSE, ConnectionBrain.class, params, null);
		_queue.addMessage(msg);
		
	}

	
		
	static final String INITIATE_JOIN = "INITIATE_JOIN";
	
	/** Called by recovery thread: initiate join to an existing conection */
	public void initiateJoin(String nodeUUID, int connId, ISocketTLWrapper validConn) {
		Object[] params = new Object[3];
		params[0] = nodeUUID;
		params[1] = connId;
		params[2] = validConn;
		
		MQMessage msg = new MQMessage(INITIATE_JOIN, ConnectionBrain.class, params, null);
		_queue.addMessage(msg);
		
	}
	
		
	static final String EVENT_LOCAL_INIT_CLOSE = "EVENT_LOCAL_INIT_CLOSE";
	
	public void eventLocalInitClose(NBSocket s) {
		
		_queue.addMessage(new MQMessage(EVENT_LOCAL_INIT_CLOSE, ConnectionBrain.class, s, null));
		
	}
	
	static final String EVENT_CONN_ERR_DETECTED_RECONNECT_IF_NEEDED = "EVENT_CONN_ERR_DETECTED_RECONNECT_IF_NEEDED";
	
	/** This method is called when we have detected a problem with the socket; start the recovery thread if needed. */
	public void eventConnErrDetectedReconnectIfNeeded(ISocketTLWrapper sock) {
		MQMessage m = new MQMessage(EVENT_CONN_ERR_DETECTED_RECONNECT_IF_NEEDED, ConnectionBrain.class, sock, null);
		_queue.addMessage(m);		
	}
	
	
	public static final String GET_ENTRY_STATE = "GET_ENTRY_STATE";
	
	protected State entryState(ISocketTLWrapper s) {
		MessageQueue responseQueue = new MessageQueue(GET_ENTRY_STATE);
		
		_queue.addMessage(new MQMessage(GET_ENTRY_STATE, ConnectionBrain.class, s, responseQueue));
		
		return (State)responseQueue.getNextMessageBlocking().getParam();
		
	}
	
	static final String IS_INPUT_PIPE_CLOSED = "IS_INPUT_PIPE_CLOSED";
	public boolean isInputPipeClosed(NBSocket s) {
		MessageQueue responseQueue = new MessageQueue("isConnectionClosed");
		
		_queue.addMessage(new MQMessage(IS_INPUT_PIPE_CLOSED, ConnectionBrain.class, s, responseQueue));
		
		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Boolean)response.getParam();
		
	}
	
	
	
	static final String IS_CONNECTION_CLOSED = "IS_CONNECTION_CLOSED";
	
	/** Is the connection closed (eg has it closed in an orderly fashion, due to a close() call. ) */
	public boolean isConnectionClosed(NBSocket s) {
		
		MessageQueue responseQueue = new MessageQueue(IS_CONNECTION_CLOSED+ " "+s.getDebugTriplet());
		
		_queue.addMessage(new MQMessage(IS_CONNECTION_CLOSED, ConnectionBrain.class, s, responseQueue));
		
		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Boolean)response.getParam();
		
	}
	
	public static final String IS_CLOSING_OR_CLOSED = "IS_CLOSING_OR_CLOSED";
	public boolean isConnectionClosingOrClosed(NBSocket s) {
		
		MessageQueue responseQueue = new MessageQueue(IS_CLOSING_OR_CLOSED);
		
		_queue.addMessage(new MQMessage(IS_CLOSING_OR_CLOSED, ConnectionBrain.class, s, responseQueue));
		
		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Boolean)response.getParam();
		
	}	
	
	
	
	
	
	public static final String IS_CONNECTION_ESTABLISHED_OR_CLOSING = "IS_CONNECTION_ESTABLISHED_OR_CLOSING"; 
	
	
	public boolean isConnectionEstablishedOrClosing(NBSocket s) {

		MessageQueue responseQueue = new MessageQueue(IS_CONNECTION_ESTABLISHED_OR_CLOSING);
		
		_queue.addMessage(new MQMessage(IS_CONNECTION_ESTABLISHED_OR_CLOSING, ConnectionBrain.class, s, responseQueue));
		
		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Boolean)response.getParam();
		
	}
	
	
	public static final String IS_CONNECTION_ESTABLISHED_OR_ESTABLISHING = "IS_CONNECTION_ESTABLISHED_OR_ESTABLISHING";
	
	
	public boolean isConnectionEstablishedOrEstablishing(NBSocket s) {
		MessageQueue responseQueue = new MessageQueue(IS_CONNECTION_ESTABLISHED_OR_ESTABLISHING);
		
		_queue.addMessage(new MQMessage(IS_CONNECTION_ESTABLISHED_OR_ESTABLISHING, ConnectionBrain.class, s, responseQueue));
		
		MQMessage response = responseQueue.getNextMessageBlocking();
		return (Boolean)response.getParam();
	}
	
	
	static final String EVENT_SEND_DATA = "EVENT_SEND_DATA";
	
	public boolean eventDataSent(NBSocket s, CmdData c) {
		Object[] params = new Object[2];
		params[0] = s;
		params[1] = c;
		
		
		MessageQueue responseQueue = new MessageQueue(EVENT_SEND_DATA+"-"+s.getDebugTriplet()+" "+c);
		
		_queue.addMessage(new MQMessage(EVENT_SEND_DATA, ConnectionBrain.class, params, responseQueue));
		
		// Wait for response to let us know it could be sent
		boolean success = (Boolean)responseQueue.getNextMessageBlocking().getParam();
		
		return success;
	}

	static final String EVENT_FLUSH_SOCKET = "FLUSH_SOCKET";
	
	public void eventFlushSocket(NBSocket s) {
		Object[] params = new Object[1];
		params[0] = s;
		
		_queue.addMessage(new MQMessage(EVENT_FLUSH_SOCKET, ConnectionBrain.class, params, null));
		
	}

	
	
	static final String EVENT_COMMAND_RECEIVED = "EVENT_COMMAND_RECEIVED"; 
	
	public void eventCommandReceived(ISocketTLWrapper sock, CmdAbstract cmd) {
		Object[] params = new Object[2];
		params[0] = sock;
		params[1] = cmd;
		_queue.addMessage(new MQMessage(EVENT_COMMAND_RECEIVED, ConnectionBrain.class, params, null));
	}
	
	
	
	static final String EVENT_INPUT_STREAM_IS_FULL = "EVENT_INPUT_STREAM_BUFFER_CHANGE";
	public void eventInputStreamIsFull(NBSocket sock, boolean isFull) {
		Object[] params = new Object[2];
		params[0] = sock;
		params[1] = (Boolean)isFull;
		_queue.addMessage(new MQMessage(EVENT_INPUT_STREAM_IS_FULL, ConnectionBrain.class, params, null));
	}
	
	
	
//	static final String EVENT_WRITE_TO_SOCKET_FAILED = "EVENT_WRITE_TO_SOCKET_FAILED";
//	
//	protected void eventWriteToSocketFailed(NBSocket socket) {
//		// I believe this is unused.
//		_queue.addMessage(new MQMessage(EVENT_WRITE_TO_SOCKET_FAILED, ConnectionBrain.class, socket, null));		
//	}
	
	
//	public static final String RESET_RECOVERY_THREAD = "RESET_RECOVERY_THREAD";
//	
//	
//	/** Called by socket to update the brain, to let the brain know the recovery thread has terminated. */
//	public void sendResetRecoveryThread(NBSocket s) {
//		MQMessage msg = new MQMessage(RESET_RECOVERY_THREAD, ConnectionBrain.class, s, null);
//		_queue.addMessage(msg);
//	}
		

}
