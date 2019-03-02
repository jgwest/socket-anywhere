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

package com.socketanywhere.forwarder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** This thread listens on a server socket; when it receives a connection on that server
 * socket, it will use the given SocketFactory to connect to a second host/port. It will
 * then forward the data sent/received from the first socket, to the second socket, and vice versa.
 * 
 * It will create threads that bind these two sockets, and then go back to listening on the 
 * server socket.
 * 
 * Example:
 * 1.  (Server Socket)
 * 2.  A -> (Server Socket)
 * 3.  A ->  (Server Socket) |  (New Socket) -> B 
 * 4.  A <-> (Socket) <-> B		|  (Server Socket)
 * 
 * */
public class ForwarderThread extends Thread {
	
	private static final boolean DEBUG = false;
	
	IServerSocketTL _input;
	ISocketFactory _outputFactory;
	TLAddress _outputAddr;
	boolean _threadRunning = true;
	
	int _debugNumSocketsAccepted = 0;
	
	Object _lock = new Object();
//	List<UnidirectionalConnectionBindThread> _connThreads = new ArrayList<UnidirectionalConnectionBindThread>(); // locked by 'lock' object
	
	
	public ForwarderThread(IServerSocketTL input, ISocketFactory output, TLAddress outputAddr) {
		super(ForwarderThread.class.getName());
		_input = input;
		_outputFactory = output;
		_outputAddr = outputAddr;

		if(DEBUG) {
			synchronized(_debugSocketList) {
				if(!_debugSocketList.isAlive()) {
					_debugSocketList.start();
				}
			}
		}
	}
	
	public static DebugSocketList _debugSocketList = new DebugSocketList();
	
	public void run() {
		while(isThreadRunning()) {
			try {
				
				if(DEBUG) System.out.println("Forwarder Thread - pre accept()");
				
				// Wait for someone to try to connect to us on our server socket
				ISocketTL acceptedSock = _input.accept();
				
				if(DEBUG) {
					_debugNumSocketsAccepted++;
					_debugSocketList.addSocketToList(acceptedSock);
					System.out.println("Forwarder Thread - post accept()");
				}
				
				acceptedSock.setDebugStr("A) tcp sock ["+_debugNumSocketsAccepted+"]");
				
				if(DEBUG) { System.out.println("Forwarder Thread ("+hashCode()+") with input accepted a new socket [serv-sock:"+_input+"]"); }
								
				if(!isThreadRunning()) {
					acceptedSock.close();
				} else {

					// Establish the connection to the address/TL that we are forwarding to
					
					ISocketTL destSock = _outputFactory.instantiateSocket(_outputAddr);
					
					destSock.setDebugStr("B) mult sock");
					
					
					if(DEBUG) {
						_debugSocketList.addSocketToList(destSock);
					}
					
					// Read data from accepted socket and write it to dest sock
					UnidirectionalConnectionBindThread ut = new UnidirectionalConnectionBindThread(acceptedSock, destSock);
					ut.start();
					
					// Read data from dest Sock, and write it to accepted sock
					UnidirectionalConnectionBindThread ut2 = new UnidirectionalConnectionBindThread(destSock, acceptedSock);
					ut2.start();
					
//					synchronized(_lock) {
//						if(isThreadRunning())  {
//							_connThreads.add(ut);
//							_connThreads.add(ut2);
//						}
//					}
				}
				
				
			} catch (IOException e) { 
				e.printStackTrace(); 
			}
		}
	}
	
	public void terminateAll() {
		synchronized(_lock) {
			_threadRunning = false;
			
//			for(UnidirectionalConnectionBindThread t : _connThreads) {
//				t.endThread();
//			}
		}
		
	}
	
	private boolean isThreadRunning() {
		
		synchronized (_lock) {
			return _threadRunning;
		}
	}
	
}
