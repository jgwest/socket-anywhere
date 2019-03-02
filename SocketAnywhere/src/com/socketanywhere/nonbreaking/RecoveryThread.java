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
import java.net.UnknownHostException;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;


/** When it is detected that a socket has died (specifically one where we initiated the connection, as opposed to a server socket), 
 * this thread spins up to try to establish a new socket. It keeps trying over and over again until a connection is re-established. */
public class RecoveryThread extends Thread {

	TLAddress _addr;
	String _nodeUUID;
	int _connectionId;
	NBSocket _nbSocket;
	ConnectionBrain _cb;
	NBOptions _options;
	
	public RecoveryThread(NBSocket nbSocket, TLAddress addr, String nodeUUID, int connectionId, ConnectionBrain cb, NBOptions options) {
		super(RecoveryThread.class.getName());
		_nbSocket = nbSocket;
		_addr = addr;
		_nodeUUID = nodeUUID;
		_connectionId = connectionId;
		_cb = cb;
		_options = options;
		
		setDaemon(true);
	}
	
	public void run() {
		final int TIME_TO_WAIT_FOR_JOIN_SUCCESS = 
				_options.getRecoveryThreadTimeToWaitForJoinSuccess() != -1 ? 
						_options.getRecoveryThreadTimeToWaitForJoinSuccess() : 10 * 1000;
		
		final int TIME_TO_WAIT_BTWN_FAILURES = 
				_options.getRecoveryThreadTimeToWaitBtwnFailures() != -1 ? 
						_options.getRecoveryThreadTimeToWaitBtwnFailures() : 2 * 2000;
				
		boolean contLoop = true;
		
		NBLog.debug("recovery thread - run(), starting addr:"+_addr+"  nodeUUID:"+_nodeUUID+"  connId:"+_connectionId);
		
		ISocketTL validConn;
		
		ISocketTL s = null;
		
		while(contLoop) {
			boolean connectionSuccess = false;
			try {
				s = _cb.getInnerFactory().instantiateSocket(_addr);
				connectionSuccess = true;
				validConn = s;
				// The new socket has been initialized, so start listening on it.
				NBSocketListenerThread socketListenerThread = new NBSocketListenerThread(s, _cb, _options);
				socketListenerThread.start();
				
				_cb.initiateJoin(_nodeUUID, _connectionId, validConn);
				
				long startTime = System.currentTimeMillis();
				while(System.currentTimeMillis() - startTime <= TIME_TO_WAIT_FOR_JOIN_SUCCESS && 
						!_cb.isConnectionEstablished(_nbSocket))  {
					
					// WAIT for change in connection brain state, or timeout
					try { Thread.sleep(500); } catch (InterruptedException e) { throw new RuntimeException(e); }
				}
				
				if(System.currentTimeMillis() - startTime > TIME_TO_WAIT_FOR_JOIN_SUCCESS) {
					NBLog.debug("Recovery thread - Timed out on recovery thread.");
				}
				
				if(_cb.isConnectionEstablished(_nbSocket)) {
					contLoop = false;
					connectionSuccess = true;
					
				} else {
					socketListenerThread.stopParsing();
					connectionSuccess = false;
					try { s.close(); } catch(IOException e) {}
				}
				
			} catch (UnknownHostException e) {
				NBLog.debug("Recovery Thread - UnknownHostException on connect");
			} catch (IOException e) {
				NBLog.debug("Recovery Thread - IOException on connect");
			} catch(Exception e) {
				NBLog.error("Recovery thread - Unknown exception in recovery thread:"+e);
			}
			
			contLoop = !connectionSuccess;
			
			if(contLoop) {
				try {
					Thread.sleep(TIME_TO_WAIT_BTWN_FAILURES);
				} catch (InterruptedException e) { throw new RuntimeException(e); }
			}
		}
		
		NBLog.debug("Recovery thread completed, for starting addr:"+_addr+"  nodeUUID:"+_nodeUUID+"  connId:"+_connectionId);
				
				
	}
		
}
