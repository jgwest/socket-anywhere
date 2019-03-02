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

package com.socketanywhere.multiplexing;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import com.socketanywhere.net.TLAddress;

/** Binds on a given port, and on connect, starts a new thread listener to allow us to receive commands on the socket.
 * 
 * If the user sends a NewConn command, the connection brain will automatically establish the connection, and then pass it back to us; 
 * we will add it to our queue, and any accept() calls will return a socket from this queue. 
 * 
 * */
public class MultServerSocketListener {
	TLAddress _address;
	
	boolean _isBound = false;
	boolean _isClosed = false;
	
	Queue<MultSocket> _queue = new LinkedList<MultSocket>();
	
	MultConnectionBrain _cb;

	public MultServerSocketListener(TLAddress address, MultConnectionBrain cb) throws IOException {
		_address = address;
		_isBound = true;
		_cb = cb;
		
		_cb.addServSockListener(_address, this);
	}
	
	
	/** Called by ConnectionBrain to let us know a new socket is available for this server socket. */
	public void informNewSocketAvailable(MultSocket socket) {
		synchronized(_queue) {
			_queue.add(socket);
			_queue.notify();
		}
	}
	
	/** Wait for the NBServerSocket to have a new connection, and return it*/
	public MultSocket blockOnWaitForNewSocketAndReturn() {
		
		MultSocket result = null;
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
	
	public void close() throws IOException {
		
		if(_isBound) {
			_isBound = false;
			_cb.removeSockListener(_address);
		} else {
			throw new IOException("Unable to close; server socket is not bound.");
		}
	}

}
