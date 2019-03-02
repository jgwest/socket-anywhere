/*
	Copyright 2012,2013 Jonathan West

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

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.ITaggedSocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.util.ManagedThread;

/** Binds on a given port, and on connect, starts a new thread listener to allow us to receive commands on the socket.
 * 
 * If the user sends a NewConn command, the connection brain will automatically establish the connection, and then pass it back to us; 
 * we will add it to our queue, and any accept() calls will return a socket from this queue. 
 * 
 * */
public class NBServerSocketListener extends ManagedThread {
	private final TLAddress _address;
	private final IServerSocketTL _inner;
//	private final ISocketFactory _factory; // Factory is used to create _inner
	
	boolean _isBound = false;
	boolean _isClosed = false;
	
	private final Queue<NBSocket> _queue = new LinkedList<NBSocket>();
	private final NBOptions _options;
	
	private final ConnectionBrain _cb;
	
	private final ConnectionBrainInterface _cbi;
	

	public NBServerSocketListener(TLAddress address, ConnectionBrain cb, ISocketFactory factory, NBOptions options) throws IOException {
		super(NBServerSocketListener.class.getName()+"-"+address, true);
		
		_options = options;
//		_factory = factory;
		_address = address;
		_inner = factory.instantiateServerSocket(address);
		_isBound = true;
		_cb = cb;
		
		_cbi = cb.getInterface();
		_cbi.addServSockListener(_address, this);
	}
	
	
	/** Called by ConnectionBrain to let us know a new socket is available for this server socket. */
	public void informNewSocketAvailable(NBSocket socket) {
		synchronized(_queue) {
			_queue.add(socket);
			_queue.notify();
		}
	}
	
	/** Wait for the NBServerSocket to have a new connection, and return it*/
	public NBSocket blockOnWaitForNewSocketAndReturn() {
		
		NBSocket result = null;
		try {
			while(result == null) {
				synchronized(_queue) {
					if(_queue.size() > 0) {
						result = _queue.remove();
					} else {
						_queue.wait();						
					}
				}
				
			}
		} catch(InterruptedException e) { }
				
		return result;
	}
	
	
	public void run() {
		if(!_isBound) {
			NBLog.error("Server socket listener was not bound on thread start.");
			return;
		}
		
		try {
			while(_isBound) {
				ISocketTL s = _inner.accept();
				
				if(!_isBound) {
					NBLog.debug("The server socket was closed, so we toss the leftover socket.", NBLog.INTERESTING);
					try { s.close(); } catch(IOException e) { }
				} else {
					// Someone has connected, start the socket listen thread to listen for commands on that socket 
					NBLog.debug("Server socket accept()ed from inner socket, so we started the listen thread on it.", NBLog.INTERESTING);
					
					ISocketTLWrapper wrapper = new ISocketTLWrapper(s, true, _cb);
					
					if(s instanceof ITaggedSocketTL) {
						Map<String, Object> m = ((ITaggedSocketTL)s).getTagMap();
						synchronized(m) {
							m.put("id", (Long)wrapper.getGlobalId());
						}
						
					}

					NBSocketListenerThread t = new NBSocketListenerThread(wrapper, _cb, _options);
					t.start();
				}
			}
			
		} catch(IOException ioe) {
			NBLog.error("IOException on ServerSocket accept: "+ioe);
			ioe.printStackTrace();
			_cbi.removeSockListener(_address);
			return;
		}
		
	}

	public void close() throws IOException {
		
		if(_isBound) {
			_isBound = false;
			_inner.close();
			_cbi.removeSockListener(_address);
		} else {
			throw new IOException("Unable to close; server socket is not bound.");
		}
	}

}
