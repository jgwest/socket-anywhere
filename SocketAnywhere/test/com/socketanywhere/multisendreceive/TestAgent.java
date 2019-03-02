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

package com.socketanywhere.multisendreceive;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.socketanywhere.multiplexingnew.MNMultSocket;
import com.socketanywhere.net.ByteHolder;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.NBLog;
import com.socketanywhere.nonbreakingnew.NBSocket;
import com.socketanywhere.util.ManagedThread;

/** 
 *  Much of the actual connect/disconnection/data sending takes place in TestAgent; though TestAgent itself
 *  only does what it is told by TestBrain.
 * 
 *  Is created in one of two ways:
 *  - It is told to connect to another connection by a test brain, or;
 *  - It is given an already connected server socket from a ServerAgent.
 *  
 *  From here, the agent will connect as needed, and send bytes when it is told to do so by the test brain. 
 *  It will also record the bytes that it receives to the TestLog. */
public class TestAgent {
	
	public static final DebugThread dt = new DebugThread(); 	
	private static final AtomicBoolean dtStarted = new AtomicBoolean();
	
	
	
	/** The id of this agent */
	final int _agentId;
	
	/** The id of the remote agent we are connected to (this ID comes from remote test director)*/
	int _remoteId = -1;

	/** Underlying socket connection */
	private ISocketTL _socket;
	
	final ISocketFactory _socketFactory;
	
	/** Reference to the test director's test log */
	TestLog _testLog;
	
	/** At what point in time did the agent spawn/connect */
	Long _agentStartTime = null;
	
	/** Reference to the test log entries for this specific agent and remote agent connection (this prevents us
	 * from having to loop it up in test log every time we need to log)*/
	private TestLogEntries _testLogEntries;
	
	MSRTestAgentInputStreamReader _readerThread;

//	//	Position in the array _lastBytesSentArr of the next empty position in the array 
//	int _lastBytesSent = 0;
//
//	// Array that contains previous data this agent sent. This is used to calculate checksum for test log. 
//	byte[] _lastBytesSentArr = new byte[4096];
//	Object _lastBytesSentLock = new Object();
	
	DebugByteContainer2 _debugBytesSent;
	
	private ByteContainer _bytesSent;
	
	DebugByteContainer2 _debugBytesReceived;
	private ByteContainer _bytesReceived;

//	// Position in the array _lastBytesReceivedArr of the next empty position in the array
//	int _lastBytesReceived = 0;
//	
//	// Array that contains previous data this agent received. This is used to calculate checksum for test log.
//	byte[] _lastBytesReceivedArr = new byte[4096];
//	Object _lastBytesReceivedLock = new Object();

	
	private final Object _socketLock2 = new Object();
	
	/** Whether or not disconnect() was called locally */
	boolean _localDisconnect = false;
	
	/** Whether or not the agent has disconnected. */
	boolean _disconnected = false;
	
	private final boolean _serverSpawned;

	private boolean _finalDisconnectCalled = false;
	
	public TestAgent(ISocketFactory factory, int agentId, TestLog testLog, boolean serverSpawned) {
		_socketFactory = factory;
		_agentId = agentId;
		_testLog = testLog;
		_serverSpawned = serverSpawned;
		
		synchronized(dt) {
			if(!dtStarted.get()) {
				dtStarted.set(true);
				dt.start();
			}
		}
		
	}
	
	
	private void setDisconnected(boolean val) {
		if(val) {
			in("setDisconnected");
		}
		_disconnected = val;
		if(val) {
			out("setDisconnected");
		}
	}
	
	public static String getAgentDebugStr(ISocketTL socket) {
		String result = null;
		
		if(socket instanceof NBSocket) {
			NBSocket nb = (NBSocket)socket;
			if(nb.getDebugTriplet() != null) {
				result  = nb.getDebugTriplet().toString();
			}
		} else if(socket instanceof MNMultSocket) {
			MNMultSocket mn = (MNMultSocket)socket;
			try {
				result = mn.getConnectionNodeUUID()+":"+mn.getConnectionId()+":"+mn.isFromServerSocket();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		} else {
			result = socket.getClass().getSimpleName();
		}
		return result;

	}
	
	
	/** Called by a ServerAgent: when someone connects to a socket that ServerAgent is 
	 * listening on, a TestAgent is spawned; this is the method that is called on the 
	 * spawn TestAgent to communicate needed information. */
	public void serverSpawned(ISocketTL socket, String host, int port) {
		
		// Runs on ServerAgent thread
		
		try {
			
			String agentDebugStr = getAgentDebugStr(socket);

			if(socket instanceof NBSocket) {
				NBSocket nb = (NBSocket)socket;
				NBLog.out(" server spawned agent id is "+_agentId+" for "+nb.getDebugTriplet());
				
				
//				if(nb.getDebugTriplet() != null) {
//					agentDebugStr = nb.getDebugTriplet().toString();
//				}
			}
			
			
//			synchronized(_socketLock) {
				_agentStartTime = System.currentTimeMillis();

				synchronized(_socketLock2) {
					_socket = socket;
				}
				in("serverSpawned");
				
				writeAgentId();
				
				boolean readRemoteId = readRemoteId();
				if(!readRemoteId) {
					setDisconnected(true);
					try {
						socket.close();
					} catch (Throwable e) { }
					return;
				}
	
				_testLogEntries = _testLog.getTLE(_agentId, _remoteId);
				
				_bytesSent = new ByteContainer(_testLogEntries, ByteContainer.Type.SEND);
				_bytesReceived = new ByteContainer(_testLogEntries, ByteContainer.Type.RECEIVE);
				
				_debugBytesSent = new DebugByteContainer2(agentDebugStr+"-sent", socket);
				_debugBytesReceived = new DebugByteContainer2(agentDebugStr+"-received", socket);
				
				_testLogEntries.setAgentDebugStr(agentDebugStr);
				_testLogEntries.remoteInitConnect();
				
				_readerThread = new MSRTestAgentInputStreamReader(this);
				_readerThread.start();
//			}
		} finally {
			out("serverSpawned");	
		}
		
		
	}
	
	/** Tell the test agent to connect to the given host/port */
	public void connect(String host, int port) {
		
		// Runs on TestBrain thread
		
		try {
			
			in("connect");
			
			long startTimeInNanos = System.nanoTime();
			synchronized(_socketLock2) {
			
				_socket = _socketFactory.instantiateSocket(new TLAddress(host, port));
			}

			String agentDebugStr = null;

			agentDebugStr = getAgentDebugStr(_socket);
				
			if(_socket instanceof NBSocket) {
				NBSocket nb = (NBSocket)_socket;
				NBLog.out("client spawned agent id is "+_agentId+" for "+nb.getDebugTriplet());
//				if(nb.getDebugTriplet() != null) {
//					agentDebugStr = nb.getDebugTriplet().toString();
//				}
			}
			
			in("connect2");
			
			if(System.nanoTime() - startTimeInNanos > TimeUnit.NANOSECONDS.convert(10, TimeUnit.SECONDS)) {
				System.err.println("ERROR: Took longer than 10 seconds to connect.");
			}
			
			// Write integer to remote agent to inform them of our agent id
			writeAgentId();
			
			// Read the remote agent id from the remote connection
			boolean readRemoteId = readRemoteId();
			if(!readRemoteId) {
				setDisconnected(true);
				getSocket().close();
				return;
			}
						
			_testLogEntries = _testLog.getTLE(_agentId, _remoteId);
			_bytesSent = new ByteContainer(_testLogEntries, ByteContainer.Type.SEND);
			_bytesReceived = new ByteContainer(_testLogEntries, ByteContainer.Type.RECEIVE);
			_debugBytesSent = new DebugByteContainer2(agentDebugStr+"-sent", _socket);
			_debugBytesReceived = new DebugByteContainer2(agentDebugStr+"-received", _socket);
			
			_testLogEntries.setAgentDebugStr(agentDebugStr);
			_testLogEntries.localInitConnect();
			
			_agentStartTime = System.currentTimeMillis();
			_readerThread = new MSRTestAgentInputStreamReader(this);
			_readerThread.start();

			
		} catch (UnknownHostException e) {
			_testLog.getUnknownTLE(_agentId).unexpectedError("connect() error", e);
			setDisconnected(true);
		} catch (IOException e) {
			_testLog.getUnknownTLE(_agentId).unexpectedError("connect() error, port:"+port, e);
			setDisconnected(true);
		} catch(Throwable t) {
			_testLog.getUnknownTLE(_agentId).unexpectedError("connect() error, port:"+port, t);
			setDisconnected(true);			
		} finally {
			out("connect");
		}
	}
	
	/** Write our agent id as a integer byte array to remote */
	private void writeAgentId() {
		
		// TestBrain or ServerAgent thread
		
		try {
			in("writeAgentId");

//			synchronized(_socketLock) {
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//				DataOutputStream daos = new DataOutputStream(baos);
//				daos.writeInt(_agentId);
//
//				baos.writeTo(_socket.getOutputStream());
				
				
				DataOutputStream daos = new DataOutputStream(getSocket().getOutputStream());
				daos.writeInt(_agentId);
				
//			}
			
		} catch (IOException e) {
			_testLog.getUnknownTLE(_agentId).unexpectedError("unable to write agent id, port:"+getSocket().getAddress(), e);
		} finally {
			out("writeAgentId");			
		}
	}
	
	/** This is called by others to inform the test agent that the test has ended */
	public void informTestEnd() {
		disconnect();
	}
	
	/** Read the integer bytes from the remote machine and store as remote id */
	private boolean readRemoteId() {
		int idBytesRemaining = 4;
		byte[] idByteArr = new byte[4];
		boolean timedOutError = false;
		
		try {
			in("readRemoteId");


			InputStream is = getSocket().getInputStream();
			
			boolean contLoop = true;
			
			long startTime = System.currentTimeMillis();
			while(contLoop) {
				byte[] bytesReadArr = new byte[idBytesRemaining];
				int bytesRead = is.read(bytesReadArr);
				
				if(bytesRead > 0) {
					System.arraycopy(bytesReadArr, 0, idByteArr, 4-idBytesRemaining, bytesRead);
					idBytesRemaining -= bytesRead;
				} else if(bytesRead == -1) {
					contLoop = false;
				}
				
				if(idBytesRemaining == 0) {
					contLoop = false;
				}
				
				if(System.currentTimeMillis() - startTime >= 30 * 1000) {
					NBLog.err("Timed out waiting for read");
//					// time out after 30 seconds of waiting
//					contLoop = false;
//					timedOutError = true;
				}
			}
			
			if(idBytesRemaining > 0) {
				
				_testLog.getUnknownTLE(_agentId).unexpectedError("Unable to read remote id. timedOut?:["+timedOutError+"]");
				return false;
			} else {
				 ByteArrayInputStream bais = new ByteArrayInputStream(idByteArr);
				 DataInputStream dais = new DataInputStream(bais);
				 _remoteId = dais.readInt();
				 
				 if(_remoteId <= 0) {
					 _testLog.getUnknownTLE(_agentId).unexpectedError("Unable to read remote id");
					 return false;
				 }
			}

		} catch (IOException e) {
			_testLog.getUnknownTLE(_agentId).unexpectedError("Unable to read remote id", e);
			return false;
		} finally {
			out("readRemoteId");
		}
		
		return true;
		
	}
	
	/** Called when a local/remote disconnect is detected */
	protected void handleDisconnect(String from, boolean send, boolean receive) {
		
		setDisconnected(true);
		
		//	System.err.println("Handle disconnect called on: "+_agentId+" "+_remoteId+" "+ ((NBSocket)getSocket()).getDebugTriplet());

		if(getSocket() instanceof NBSocket) {
			NBLog.out("Handle disconnect called on: "+_agentId+" "+_remoteId+" "+ ((NBSocket)getSocket()).getDebugTriplet()+""+_bytesSent.debugGetLastBytes()+" "+_bytesReceived.debugGetLastBytes() + " "+from);			
		}
		

		if(send) {
			_bytesSent.handleDisconnect();
			_debugBytesSent.handleDisconnect(from+"-"+send+"-"+receive);
		}

		if(receive) {
			_bytesReceived.handleDisconnect();
			_debugBytesReceived.handleDisconnect(from+"-"+send+"-"+receive);
		}
		
//		synchronized(_lastBytesReceivedLock) {
//			
//			if(_lastBytesReceived > 0) {
//				_testLogEntries.receiveData(_lastBytesReceivedArr, 0, _lastBytesReceived);
//				_lastBytesReceived = 0;
//			}			
//		} 
	}
	
	public int getAgentId() {
		return _agentId;
	}
	
	public int getRemoteAgentId() {
		return _remoteId;
	}
	
	public Long getAgentStartTime() {
		return _agentStartTime;
	}
	

	public boolean isDisconnected() {
		return _disconnected;
	}
	
	/** Initiates a disconnect by this agent from the connection (a local-initiated disconnect) */
	public void disconnect() {
		
		// Called from Brain thread, and TestDirectorReceiverThread
		
		try {
			in("disconnect");
			if(_disconnected) { return; }
			
			setDisconnected(true);
			_localDisconnect = true;
		
			try { getSocket().close(); } catch (IOException e) {}

			new Thread() {
				public void run() {
					try { Thread.sleep(10 * 1000); } catch (InterruptedException e) {}
					
//					synchronized(_socketLock) {
						handleDisconnect("disconnect", true, true); // should possibly be true, false?
						_testLogEntries.localInitDisconnect();
//					}					
				};
			}.start();
			
			
				// _socket.shutdownOutput();
//			}
		} catch (Throwable e) {
			_testLogEntries.unexpectedError("disconnect() error", e);
		} finally {
			out("disconnect");
		}
	}
	
	private void recordSend(byte[] bytes, int offset, int length) {
		
		// Called on TestBrain thread
		
		_bytesSent.record(bytes, offset, length);
		_debugBytesSent.record(bytes, offset, length);

//		synchronized(_lastBytesSentLock) {
//
//			// Only copy as many bytes from 'bytes' as to fit into _lastBytesSentArr
//			int bytesToCopy = Math.min(length, _lastBytesSentArr.length - _lastBytesSent);
//			System.arraycopy(bytes, offset, _lastBytesSentArr, _lastBytesSent, bytesToCopy);
//			_lastBytesSent += bytesToCopy;
//
//			if(_lastBytesSent == _lastBytesSentArr.length) {
//				_testLogEntries.sentData(_lastBytesSentArr, 0, _lastBytesSentArr.length);
//				_lastBytesSent = 0;
//			}
//			
//			// If there were bytes left over in the array, call this method again with the remaining data
//			if(bytesToCopy < length) {
//				recordSend(bytes, bytesToCopy+offset, length-bytesToCopy);
//			}
//
//		}
		
	}
	
	private void in(String name) {
		
		
		if(getSocket() instanceof NBSocket) {
			NBSocket nb = (NBSocket)getSocket();
//			NBLog.out("TestAgent."+name+" in ("+_agentId+") "+( nb != null ? nb.getTriplet() : " NULL") +" {");
		} else {
//			System.out.println("TestAgent."+name+ " in "+_socket);
		}
		
	}

	private void out(String name) {
		if(getSocket() instanceof NBSocket) {
			NBSocket nb = (NBSocket)getSocket();
//			NBLog.out("} TestAgent."+name+" out ("+_agentId+") "+(nb != null ? nb.getTriplet() : " NULL"));
		} else {
//			System.out.println("TestAgent."+name+ " out "+_socket);
		}
		
	}


	/** Send data on the socket; log the data sent in our test log */
	public void send(byte[] bytes, int offset, int length) {
		
		// Called on TestBrain thread.
		
		if(isDisconnected()) {
			NBLog.err("Attempt to write after disconnect. " + ((NBSocket)getSocket()).getDebugTriplet());
			return;
		}

		boolean writeSuccess = false;

		in("send");
		
//		synchronized(_socketLock) {
			try {
				getSocket().getOutputStream().write(bytes, offset, length);
				writeSuccess = true;
			} catch (Exception e) {
				if(!getSocket().isClosed()) {
					_testLogEntries.unexpectedError("send() error isSocketConnected:"+getSocket().isConnected()+" port:"+getSocket().getAddress(), e);
				}
				handleDisconnect("send "+e.getMessage()+" "+SoAnUtil.convertStackTrace(e).replace("\r", "").replace("\n", ""), true, false); // Added December 10th, 2016
			}
//		}
		
		out("send");
		
		if(writeSuccess) {
			recordSend(bytes, offset, length);
			
//			_bytesSent.record(bytes, offset, length);
//			synchronized(_lastBytesSentLock) {
			
//				
//			}
		}
	}
	
	/** Record in our test log that we received data */
	private void recordReceive(byte[] bytes, int offset, int length) {

		_bytesReceived.record(bytes, offset, length);
		
		_debugBytesReceived.record(bytes, offset, length);
		
//		synchronized(_lastBytesReceivedLock) {
//			// Only copy as many bytes from 'bytes' as to fit into _lastBytesReceivedArr
//			int bytesToCopy = Math.min(length, /* bytes remaining in last bytes received array*/ _lastBytesReceivedArr.length - _lastBytesReceived);
//			System.arraycopy(bytes, offset, _lastBytesReceivedArr, _lastBytesReceived, bytesToCopy);
//			_lastBytesReceived += bytesToCopy;
//	
//			if(_lastBytesReceived == _lastBytesReceivedArr.length) {
//				_testLogEntries.receiveData(_lastBytesReceivedArr, 0, _lastBytesReceivedArr.length);
//				_lastBytesReceived = 0;
//			}
//			
//			// If there were bytes left over in the array, call this method again with the remaining data
//			if(bytesToCopy < length) {
//				recordReceive(bytes, bytesToCopy+offset, length-bytesToCopy);
//			}
//			
//		}		
	}
	
//	public boolean isActive() {
//		synchronized(_socketLock) {
//			return _socket.isConnected();
//		}
//	}
	
	
	/** Listens on the socket and input stream and detects/handles various communication events */
	class MSRTestAgentInputStreamReader extends Thread {
		TestAgent _agent;
		InputStream _is;
		TestLog _testLog;
		
		public MSRTestAgentInputStreamReader(TestAgent agent) {
			super(MSRTestAgentInputStreamReader.class.getName());
			setPriority(NORM_PRIORITY+2);
			setDaemon(true);
			
			_agent = agent;
			_testLog = _agent._testLog;
			
			synchronized(_agent.getSocket()) {
				try {
					_is =_agent.getSocket().getInputStream();
				} catch (IOException e) {
					_agent._testLogEntries.unexpectedError("MSRTestAgent - get IS error", e);
				}
			}
		}
		
		@Override
		public void run() {
			boolean contLoop = true;
			
			while(contLoop) {
				byte[] bytearr = new byte[16384];
				try {
					int bytesRead = _is.read(bytearr);
					
					if(bytesRead > 0) {
//						fp(_agent.getAgentId() + " / " + _agent.getRemoteAgentId() + " - received "+bytesRead);
						_agent.recordReceive(bytearr, 0, bytesRead);
					} else if(bytesRead == -1) {
						contLoop = false;
						_agent.handleDisconnect("run - bytesRead == -1", false, true);
						_agent._testLogEntries.remoteInitDisconnect();
					}
				} catch (Throwable e) {				
					if(!_localDisconnect) {
						// Only print the exception if it was unexpected
						_agent._testLogEntries.unexpectedError("MSRTestAgent - read() error", e);
					} 
					contLoop = false;
					_agent.handleDisconnect("run throwable", false, true); // Added: December 10, 2016
					
				}
			}
			
		}
	}
	
	public ISocketTL getSocket() {
		synchronized(_socketLock2) {
			return _socket;
		}
	}

	public boolean isServerSpawned() {
		return _serverSpawned;
	}
	
	public boolean isFinalDisconnectCalled() {
		return _finalDisconnectCalled;
	}

	public void setFinalDisconnectCalled(boolean finalDisconnectCalled) {
		_finalDisconnectCalled = finalDisconnectCalled;
	}
	
	public void finalDisconnect() {
		handleDisconnect("finalDisconnect", true, false);
	}
	
	
//	public String getDebugStr() {
//		if(_socket != null && _socket instanceof ITaggedSocketTL) {
//			
//			ITaggedSocketTL s = (ITaggedSocketTL)_socket;
//			
//			Object o = s.getTagMap().get("id");
//			if(o != null) {
//				return o.toString();
//			}
//		}
//		
//		return null;
//	}
}

//class ByteContainer {
//	// Position in the array _lastBytesReceivedArr of the next empty position in the array
//	private int _lastBytesReceived = 0;
//	
//	// Array that contains previous data this agent received. This is used to calculate checksum for test log.
//	private byte[] _lastBytesReceivedArr = new byte[512];
//	
//	private final String debugStr;
//	
//	ByteContainer(String debugStr) {
//		this.debugStr = debugStr;
//	}
//	
//	/** Record in our test log that we received data */
//	protected synchronized void recordReceive(byte[] bytes, int offset, int length) {
//		
//		// Only copy as many bytes from 'bytes' as to fit into _lastBytesReceivedArr
//		int bytesToCopy = Math.min(length, /* bytes remaining in last bytes received array*/ _lastBytesReceivedArr.length - _lastBytesReceived);
//		System.arraycopy(bytes, offset, _lastBytesReceivedArr, _lastBytesReceived, bytesToCopy);
//		_lastBytesReceived += bytesToCopy;
//
//		if(_lastBytesReceived == _lastBytesReceivedArr.length) {
//			
//			short c = 0;
//			for(int x = 0; x < _lastBytesReceivedArr.length; x++) {
//				c += _lastBytesReceivedArr[x];
//			}
//			
//			NBLog.out(debugStr+" "+Integer.toHexString(c%16));
//			_lastBytesReceived = 0;
//		}
//		
//		// If there were bytes left over in the array, call this method again with the remaining data
//		if(bytesToCopy < length) {
//			recordReceive(bytes, bytesToCopy+offset, length-bytesToCopy);
//		}
//	}
//	
//
//	
//}



class DebugThread extends ManagedThread {


	private List<DebugByteContainer> list = new ArrayList<DebugByteContainer>();
	public DebugThread() {
		super(DebugThread.class.getName(), true);
	}

	@Override
	public void run() {
		
		System.err.println("DebugThread is sleeping.");
		try {
			Thread.sleep((long)(4.5 * 60 * 1000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		FileWriter fw;
		try {
			fw = new FileWriter("d:\\delme\\dt.log");
			
			for(DebugByteContainer c : list ) {
				
				String str0 = "["+c.getSocket().isClosed()+"]"+c.getDebugStr()+" - ("+c.getTotalBytesReceived()+") ";
				System.out.println(str0);
				
				String str = c.getDebugStr()+" - ("+c.getTotalBytesReceived()+") "+c.getSb().toString();
				fw.write((str+"\r\n"));
				
//				System.out.println(str);
				
			}

			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.err.println("DebugThread dt.log dumped. ");
		
	}
	
	public void add(DebugByteContainer dbc) {
		synchronized(list) {
			list.add(dbc);
		}
	}
}


class DebugByteContainer2 extends DebugByteContainer {
	
//	public static enum Type {SEND, RECEIVE}; 
//	
//	private final Type _type;
//	
//	

//	private final String _debugStr; 
//	
//	private StringBuilder sb = new StringBuilder();
	
	public DebugByteContainer2(String debugStr, ISocketTL socket) {
		super(debugStr, socket);
		
//		TestAgent.dt.add(this);
		
//		debugStr = debugStr.replace(" ", "");
//		debugStr = debugStr.replace("{", "");
//		debugStr = debugStr.replace("}", "");
//		debugStr = debugStr.trim();
		
//		_debugStr = debugStr;
		
	}
	
	private final ByteHolder byteHolder = new ByteHolder();
		
	/** Record in our test log that we received data */
	protected synchronized void record(byte[] bytes, int offset, int length) {

		_totalBytesProcessed += length;
		
		byteHolder.addBytes(bytes, offset, length);

		int REDUCTION_SIZE = 64; // 8
		
		while(byteHolder.getContentsSize() >= REDUCTION_SIZE) {
			byte[] barr = byteHolder.extractAndRemove(REDUCTION_SIZE);

			int c = 0;
			for(int z = 0; z < barr.length; z++) {
				c += barr[z];
			}
			
			c = Math.abs(c);
			c = c % 35;
			sb.append(Integer.toString(c, 36));	
		}
		
	}
	
	protected synchronized void handleDisconnect(String from) {
		
		sb.append("{"+from+"-|");
		
		if(byteHolder.getContentsSize() > 0) {
			
			int REDUCTION_SIZE = 64; // 8
			
			while(byteHolder.getContentsSize() >= REDUCTION_SIZE) {
				byte[] barr = byteHolder.extractAndRemove(REDUCTION_SIZE);

				int c = 0;
				for(int z = 0; z < barr.length; z++) {
					c += barr[z];
				}
				
				c = Math.abs(c);
				c = c % 35;
				sb.append(Integer.toString(c, 36));	
			}
			
			// If it is still > 0
			if(byteHolder.getContentsSize() > 0) {
				
				byte[] barr = byteHolder.extractAndRemove(byteHolder.getContentsSize());
				
				int c = 0;
				for(int z = 0; z < barr.length; z++) {
					c += barr[z];
				}
				
				c = Math.abs(c);
				c = c % 35;
				sb.append(Integer.toString(c, 36));
			}
			
		}
		sb.append("}");
	}
	
	protected synchronized int debugGetLastBytes() {
		return 0;
//		return _lastBytesReceived;
	}
	
}

class DebugByteContainer {
	
//	public static enum Type {SEND, RECEIVE}; 
//	
//	private final Type _type;
//	
//	
	
	private final ISocketTL _socket;
	


	final String _debugStr; 
	
	StringBuilder sb = new StringBuilder();
	
	long _totalBytesProcessed = 0;
	
	public DebugByteContainer(String debugStr, ISocketTL socket) {
		TestAgent.dt.add(this);
		
//		debugStr = debugStr.replace(" ", "");
//		debugStr = debugStr.replace("{", "");
//		debugStr = debugStr.replace("}", "");
//		debugStr = debugStr.trim();
		
		_socket = socket;
		_debugStr = debugStr;
		
	}
	
	public String getDebugStr() {
		return _debugStr;
	}
	
	public synchronized StringBuilder getSb() {
		StringBuilder sb2 = sb;
		sb = null;
		return sb2;
	}
	
	// Position in the array _lastBytesReceivedArr of the next empty position in the array
	private int _lastBytesReceived = 0;
	
	// Array that contains previous data this agent received. This is used to calculate checksum for test log.
	private byte[] _lastBytesReceivedArr = new byte[16];
	
	public synchronized long getTotalBytesReceived() {
		return _totalBytesProcessed;
	}
	
	/** Record in our test log that we received data */
	protected synchronized void record(byte[] bytes, int offset, int length) {
		
		// Only copy as many bytes from 'bytes' as to fit into _lastBytesReceivedArr
		int bytesToCopy = Math.min(length, /* bytes remaining in last bytes received array*/ _lastBytesReceivedArr.length - _lastBytesReceived);
		System.arraycopy(bytes, offset, _lastBytesReceivedArr, _lastBytesReceived, bytesToCopy);
		_lastBytesReceived += bytesToCopy;
		

		if(_lastBytesReceived == _lastBytesReceivedArr.length) {
			
			int c = 0;
			for(int x = 0; x < _lastBytesReceivedArr.length; x++) {
				c += _lastBytesReceivedArr[x];
			}
			
			c = Math.abs(c);
			c = c % 16;
			sb.append(Integer.toHexString(c));
			
			
//			if(_type == Type.RECEIVE) {
//				_testLogEntries.receiveData(_lastBytesReceivedArr, 0, _lastBytesReceivedArr.length);
//			} else {
//				_testLogEntries.sentData(_lastBytesReceivedArr, 0, _lastBytesReceivedArr.length);
//			}
			_lastBytesReceived = 0;
		}
		
		// If there were bytes left over in the array, call this method again with the remaining data
		if(bytesToCopy < length) {
			record(bytes, bytesToCopy+offset, length-bytesToCopy);				
		}
	}
	
	protected synchronized void handleDisconnect(String from) {
		if(_lastBytesReceived > 0) {
//			if(_type == Type.RECEIVE) {
//				_testLogEntries.receiveData(_lastBytesReceivedArr, 0, _lastBytesReceived);
//			} else {
//				_testLogEntries.sentData(_lastBytesReceivedArr, 0, _lastBytesReceived);
//			}
			_lastBytesReceived = 0;
		}			
	}
	
	protected synchronized int debugGetLastBytes() {
		return _lastBytesReceived;
	}

	public ISocketTL getSocket() {
		return _socket;
	}
}

class ByteContainer2 {
	
	public static enum Type {SEND, RECEIVE}; 
	
	private final TestLogEntries _testLogEntries;
	private final Type _type;
	
	
	public ByteContainer2(TestLogEntries testLogEntries, Type t) {
		_testLogEntries = testLogEntries;
		_type = t;
	}

	private final ByteHolder byteHolder = new ByteHolder();
	
	final static int REDUCTION_SIZE = 4096; 
	
	/** Record in our test log that we received data */
	protected synchronized void record(byte[] bytes, int offset, int length) {
		
		byteHolder.addBytes(bytes, offset, length);

		while(byteHolder.getContentsSize() >= REDUCTION_SIZE) {
			byte[] barr = byteHolder.extractAndRemove(REDUCTION_SIZE);

			if(_type == Type.RECEIVE) {
				_testLogEntries.receiveData(barr, 0, barr.length);
			} else {
				_testLogEntries.sentData(barr, 0, barr.length);
			}
		}
		
	}
	
	protected synchronized void handleDisconnect() {
		
		while(byteHolder.getContentsSize() >= REDUCTION_SIZE) {
			byte[] barr = byteHolder.extractAndRemove(REDUCTION_SIZE);

			if(_type == Type.RECEIVE) {
				_testLogEntries.receiveData(barr, 0, barr.length);
			} else {
				_testLogEntries.sentData(barr, 0, barr.length);
			}
		}
		
		// Handle remaining bytes (number is < REDUCTION_SIZE)
		if(byteHolder.getContentsSize() > 0) {
			byte[] barr = byteHolder.extractAndRemove(byteHolder.getContentsSize());
			if(_type == Type.RECEIVE) {
				_testLogEntries.receiveData(barr, 0, barr.length);
			} else {
				_testLogEntries.sentData(barr, 0, barr.length);
			}
		}			
	}
	
	protected synchronized int debugGetLastBytes() {
		return byteHolder.getContentsSize();
	}
	
}

class ByteContainer {
	
	public static enum Type {SEND, RECEIVE}; 
	
	private final TestLogEntries _testLogEntries;
	private final Type _type;
	
	
	public ByteContainer(TestLogEntries testLogEntries, Type t) {
		_testLogEntries = testLogEntries;
		_type = t;
	}
	
	// Position in the array _lastBytesReceivedArr of the next empty position in the array
	private int _lastBytesReceived = 0;
	
	// Array that contains previous data this agent received. This is used to calculate checksum for test log.
	private byte[] _lastBytesReceivedArr = new byte[4096];
		
	/** Record in our test log that we received data */
	protected synchronized void record(byte[] bytes, int offset, int length) {
		
		// Only copy as many bytes from 'bytes' as to fit into _lastBytesReceivedArr
		int bytesToCopy = Math.min(length, /* bytes remaining in last bytes received array*/ _lastBytesReceivedArr.length - _lastBytesReceived);
		System.arraycopy(bytes, offset, _lastBytesReceivedArr, _lastBytesReceived, bytesToCopy);
		_lastBytesReceived += bytesToCopy;

		if(_lastBytesReceived == _lastBytesReceivedArr.length) {
			if(_type == Type.RECEIVE) {
				_testLogEntries.receiveData(_lastBytesReceivedArr, 0, _lastBytesReceivedArr.length);
			} else {
				_testLogEntries.sentData(_lastBytesReceivedArr, 0, _lastBytesReceivedArr.length);
			}
			_lastBytesReceived = 0;
		}
		
		// If there were bytes left over in the array, call this method again with the remaining data
		if(bytesToCopy < length) {
			record(bytes, bytesToCopy+offset, length-bytesToCopy);				
		}
	}
	
	protected synchronized void handleDisconnect() {
		if(_lastBytesReceived > 0) {
			if(_type == Type.RECEIVE) {
				_testLogEntries.receiveData(_lastBytesReceivedArr, 0, _lastBytesReceived);
			} else {
				_testLogEntries.sentData(_lastBytesReceivedArr, 0, _lastBytesReceived);
			}
			_lastBytesReceived = 0;
		}			
	}
	
	protected synchronized int debugGetLastBytes() {
		return _lastBytesReceived;
	}
	
}