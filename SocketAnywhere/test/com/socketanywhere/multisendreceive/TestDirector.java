/*
	Copyright 2012 Jonathan West

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Test director is the central entity for each side:
 *  - each side of the test will instantiate one test director object
 *  - these test directors will connect to each other, and handshake to exchange data (for instance what ips are being listened on)
 *  - the test brain is started on both sides
 *  - finally, once the test brain completes, one of the two test directory sends log data to the other, 
 *    and the results are verified. */
public class TestDirector {
	
	Object _testDirectorLock = new Object();

	ISocketTL _connectedSocket;	
	ISocketFactory _socketFactory;
	
	// Optional - A separate factory can be used to establish the director socket 
	ISocketFactory _directorSocketFactory;
	
	/** Whether or not this test director began by listening on a port (versus connecting to a remote host port)*/
	boolean _beganListeningOnPort = false;
	
	TestBrain _brain = null;
	TestLog _log = null;
	TestDirectorReceiverThread _receiverThread;
	TestDirectorSenderThread _senderThread;
	
	/** The address of the remote test director we are connected to */
	String _localAddress = null;
	String _remoteAddress = null;

	/** Remote server agent ports we can connect to */
	ArrayList<Integer> _portsAvailableToConnectTo = new ArrayList<Integer>(); 

	int _nextAgentId = 1;

	/** Probably, but not necessarily, a unique ID*/
	private int _localDirectorId = -1; // "our" test director 
	private int _remoteDirectorId = -1; // the "other" test director 
	
	public int getLocalDirectorId() {
		return _localDirectorId;
	}
	
	public int getRemoteDirectorId() {
		return _remoteDirectorId;
	}
	
	public void setLocalDirectorId(int _debugDirectorId) {
		this._localDirectorId = _debugDirectorId;
	}
	
	public void setRemoteDirectorId(int _remoteDirectorId) {
		this._remoteDirectorId = _remoteDirectorId;
	}
	
	public TestDirector(ISocketFactory socketFactory, TestLog log, String localAddress, String remoteAddress) {
		_socketFactory = socketFactory;
		_log = log;
		_localDirectorId = (int)(Math.random()*100000);
		_remoteAddress = remoteAddress;
		_localAddress = localAddress;
	}
	
	/** Optional - A separate factory can be used to establish the director socket */
	public void setDirectorSocketFactory(ISocketFactory directorSocketFactory) {
		this._directorSocketFactory = directorSocketFactory;
	}
	
	public ISocketFactory getSocketFactory() {
		return _socketFactory;
	}
	
	
	public String getLocalAddress() {
		return _localAddress;
	}
	
	public String getRemoteAddress() {
		return _remoteAddress;
	}
	
	public int getNextAgentId() {
		synchronized(_testDirectorLock) {
			return 100+/*_debugDirectorId* (10 * 1000)+*/  _nextAgentId++;
		}
	}

	/** Inform the director that it should listen on the given port and wait for
	 * another test director to connect */
	public void directorListenOnPort(int port) {
		IServerSocketTL ss;
		try {
			_beganListeningOnPort = true;
			if(_directorSocketFactory == null) {
				ss = _socketFactory.instantiateServerSocket(new TLAddress(port));
			} else {
				ss = _directorSocketFactory.instantiateServerSocket(new TLAddress(port));
			}
			
			ISocketTL s = ss.accept();
			_connectedSocket = s;
			
			_receiverThread = new TestDirectorReceiverThread(this);
			_receiverThread.start();
			
			_senderThread = new TestDirectorSenderThread(this);
			_senderThread.start();
			
		} catch (IOException e) {
			_log.unexpectedGeneralError("Error on listening on port", e);
			e.printStackTrace();
		}
		
	}
	
	/** Call this before connect(...) or listenOnPort(...) */
	public void setTestBrain(TestBrain brain) {
		_brain = brain;
	}
	
	/** Connect to another test director on the given port */
	public void directorConnect(String host, int port) {
		try {
			ISocketTL s;
			if(_directorSocketFactory == null) {
				s = _socketFactory.instantiateSocket(new TLAddress(host, port));
			} else {
				s = _directorSocketFactory.instantiateSocket(new TLAddress(host, port));
			}
			_connectedSocket = s;
			
			_receiverThread = new TestDirectorReceiverThread(this);
			_receiverThread.start();
			
			_senderThread = new TestDirectorSenderThread(this);
			_senderThread.start();
			
			// Send our test director id to remote director
			writeStringToDirectorSocket("my-director-id "+_localDirectorId+"\n");
			
		} catch (UnknownHostException e) {
			_log.unexpectedGeneralError("IOException in director", e);
		} catch (IOException e) {
			_log.unexpectedGeneralError("IOException in director", e);
		}
	}
	
	/** Called by the test brain to inform the director that it is complete*/
	public void informTestBrainComplete() {
		if(_beganListeningOnPort) {
			// We are the boss
			writeStringToDirectorSocket("end tests\n");
		} else {
			// We are the subordinate, wait for boss to inform us to send results :)
		}
	}
	
	
	protected void writeStringToDirectorSocket(String s) {
		synchronized(_testDirectorLock) {
			try {
				if(MultiSendReceiveMain.DEBUG) {
					MultiSendReceiveMain.println("* Director("+_localDirectorId+") sending - "+s);
				}
				_connectedSocket.getOutputStream().write(s.getBytes());
			} catch (IOException e) {
				_log.unexpectedGeneralError("IOException in director", e);
			}
		}		
	}
	
	
	public void informCompareResults(TestLog remoteTestLog) {
		
		ResultComparator rc = new ResultComparator(this, false);
		rc.checkResults(_log, remoteTestLog);

	}
	
	/** This method is used by server agents to tell us that they are now listening on a given port. */
	public void informListeningOnPort(int port) {
		synchronized(_testDirectorLock) {
			writeStringToDirectorSocket(("port "+port+"\n"));
		}
	}

	/** This method is used by server agents to tell us that they have stopped listening on a given port. */
	public void informStopListeningOnPort(int port) {
		synchronized(_testDirectorLock) {
			writeStringToDirectorSocket(("remove-port "+port+"\n"));
		}
	}

	
	/** Returns a copy of the list of ports to connect to. Since it is a copy you do not need to synchronize on this.*/
	@SuppressWarnings("unchecked")
	public List<Integer> getPortsAvailableToConnectTo() {
		synchronized(_testDirectorLock) {
			return (ArrayList<Integer>)_portsAvailableToConnectTo.clone();
		}
	}
}

class TestDirectorSenderThread extends Thread {
	TestDirector _director;
	
	public TestDirectorSenderThread(TestDirector director) {
		super(TestDirectorSenderThread.class.getName());
		_director = director;
		
	}
	
	@Override
	public void run() {
		_director._brain.run();
	}
}

class TestDirectorReceiverThread extends Thread {
	TestDirector _director;
	
	public TestDirectorReceiverThread(TestDirector director) {
		super(TestDirectorReceiverThread.class.getName());
		_director = director;
	}
	
	@Override
	public void run() {
		try {
			InputStream is = _director._connectedSocket.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			
			TestLog _remoteTestLog = null;
			
			String line;
			while( (line = br.readLine()) != null ) {
				// Listen on the input stream of the socket, and process commands from the remote
				// test director.
				line = line.trim();
				
//				if(MultiSendReceiveMain.DEBUG) {
					MultiSendReceiveMain.println("* Director("+_director.getLocalDirectorId()+") received: "+line);
//				}
				
				if(line.startsWith("my-director-id ")) {
					String l = line;
					l = l.substring(line.indexOf(" ")+1).trim();
					int remoteId = Integer.parseInt(l);
					_director.setRemoteDirectorId(remoteId);
				}
				
				if(line.startsWith("remove-port ")) {
					String l = line;
					l = l.substring(line.indexOf(" ")+1).trim();
					int port = Integer.parseInt(l);
					
					synchronized(_director._testDirectorLock) {
						_director._portsAvailableToConnectTo.remove((Integer)port);
					}					
				}
					
				if(line.startsWith("port ")) {
					String l = line;
					l = l.substring(line.indexOf(" ")+1).trim();
					int port = Integer.parseInt(l);
					
					synchronized(_director._testDirectorLock) {
						_director._portsAvailableToConnectTo.add((Integer)port);
					}
					
				}
				
				// The remote test director is outputting its results, so process them and compare.
				if(line.startsWith("result start")) {
					_remoteTestLog = new TestLog();
					_remoteTestLog.setLocalDirectorId(_director.getRemoteDirectorId());
					
					// Read the results from the input stream
					_remoteTestLog.deserializeTestLog(br);
					_director.informCompareResults(_remoteTestLog);
					_director._brain.informTestEnd();
					break;
				}
				
				// Remote test director has informed us to end, so send our test log data
				if(line.startsWith("end ")) {
					// Send our results to the remote test director
					_director._log.serializeTestLog(_director);
					
					_director._brain.informTestEnd();
					break;
				}
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			_director._log.unexpectedGeneralError("Error in TestDirectorReceiveThread", e);
		}
		
	}
}
