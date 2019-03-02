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

import java.io.IOException;
import java.net.SocketTimeoutException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.NBLog;
import com.socketanywhere.nonbreakingnew.NBSocket;

/** Thread that listens on a given port and hostname, and when it gets connected to by a remote agent, it will
 * create a new TestAgent and pass the connection information to that.
 * 
 * The new TestAgent will handle the connection, and the ServerAgent will go back to listening
 * on it's server port.  
 * */
public class ServerAgent extends Thread {

	final TestDirector _testDirector;
	final TestBrain _testBrain;
	final TestLog _testLog;
	boolean _continueListening = true;
	
	final ISocketFactory _socketFactory;
	String _host = null;
	int _port = -1;
	
	// Whether or not the server agent is able to listen on the port it is given (null means no result yet)
	Boolean _listenOnPortResult = false;
	
	public ServerAgent(ISocketFactory factory, TestDirector testDirector, TestBrain testBrain, TestLog t, String host, int port) {
		super(ServerAgent.class.getName());
		setDaemon(true);
		
		_socketFactory = factory;
		
		_testDirector = testDirector;
		_testBrain = testBrain;
		_testLog = t;
		_host = host;
		_port = port;
	}
	
	/** Whether or not the server agent is able to listen on the port it is given (null means no result yet) */	
	public Boolean getListenOnPortResult() {
		return _listenOnPortResult;
	}
	
	/** Start thread to listen on the given port*/
	public void startListening() {
		this.start();
	}
	
	/** Stop thread that is listening on the given port*/
	public void stopListening() {
		_continueListening = false;
	}
	
	public void stopListeningOnPort() {
		// Inform that the port is no longer listenable
		_testBrain.removePortListeningOn(_port);
		
		// Wait 10 seconds before actually shutting down the port
		new Thread() {

			public void run() {
				try { Thread.sleep(10000); } catch (InterruptedException e) {}
				stopListening();
			}
			
		}.start();
	}
	
	/** Listen on the given port; called by run().*/
	private void listenOnPort(String host, int port) {
		try {
			_host = host;

			IServerSocketTL ss = _socketFactory.instantiateServerSocket(new TLAddress(host, port));
			
			_listenOnPortResult = true;
			
			_testBrain.addPortListeningOn(port);
			
			// TODO: LOWER - Implement setSoTimeout
//			ss.setSoTimeout(15 * 1000);
			while(_continueListening) {
				try {
					ISocketTL s = ss.accept();
										
					if(_continueListening) {
						int nextAgentID = _testDirector.getNextAgentId();
						
						// Every time someone connects to our port, spawn a new TestAgent
						
						TestAgent a = new TestAgent(_socketFactory, nextAgentID, _testLog, true);
						a.serverSpawned(s, host, port);

						// (a "server spawned" test agent is one that was created as part of an accept() call on a server socket) 
						_testBrain.addServerSpawnedTestAgent(a);
					} else {
						s.close();
					}

				} catch(SocketTimeoutException e) { /* ignore */ }
			}
			
		} catch (IOException e) {
			_listenOnPortResult = false;
			_testLog.unexpectedGeneralError("Listen on port error", e);
		}
	}
	
	
	@Override
	public void run() {
		listenOnPort(_host, _port);
	}
}
