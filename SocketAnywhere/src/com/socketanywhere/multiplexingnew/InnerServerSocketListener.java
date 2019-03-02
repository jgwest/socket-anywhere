/*
	Copyright 2014 Jonathan West

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

import com.socketanywhere.net.ConstructorTransfer;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Creates a ServerSocket with the factory, then accepts on it:
 * - accepted sockets are passed to a new instance of  MNSocketListenerThread */
public class InnerServerSocketListener extends Thread {
	
	private final ConstructorTransfer<MNConnectionBrain> _brain;
	private final ConstructorTransfer<ISocketFactory> _factory;
	
	private final ConstructorTransfer<TLAddress> _listenAddr;
	
	private boolean _continueThread = true;
	
	public InnerServerSocketListener(ISocketFactory factory, TLAddress listenAddr, MNConnectionBrain brain) {
		super(InnerServerSocketListener.class.getName());
		setDaemon(true);
		_brain = new ConstructorTransfer<MNConnectionBrain>(brain);
		_factory = new ConstructorTransfer<ISocketFactory>(factory);
		_listenAddr = new ConstructorTransfer<TLAddress>(listenAddr);
		
	}
	
	@Override
	public void run() {
		
		ISocketFactory factory = _factory.get();
		
		TLAddress listenAddr = _listenAddr.get();
		
		MNConnectionBrain brain = _brain.get(); 
		
		IServerSocketTL servSocket = null;
		
		while(servSocket == null && _continueThread) {
			try {
				servSocket = factory.instantiateServerSocket(listenAddr);
				
			} catch (IOException e) {
				e.printStackTrace();
				try { Thread.sleep(5* 1000); } catch (InterruptedException e1) { _continueThread = false;}
			}
			
		}
		
		while(_continueThread) {
			
			ISocketTL socket = null;
			try {
				socket = servSocket.accept();
			} catch (IOException e) {
				_continueThread = false;
			}
			
			if(socket != null) {
				MNSocketWriter writer = new MNSocketWriter(socket, 0);
				MNSocketListenerThread sockListener = new MNSocketListenerThread(socket, writer, brain);
				sockListener.start();
			}
			
		}
		
		
	}

	
	public void setContinueThread(boolean continueThread) {
		this._continueThread = continueThread;
	}
	
}
