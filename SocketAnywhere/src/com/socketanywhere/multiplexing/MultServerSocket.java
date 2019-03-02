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

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class MultServerSocket implements IServerSocketTL {

	TLAddress _address;
	MultServerSocketListener _serverSocketListener;

	MultConnectionBrain _cb;
	
	boolean _isBound = false;
	boolean _isClosed = false;
	
	
	protected MultServerSocket(MultConnectionBrain cb) {
		_cb = cb;
	}
	
	protected MultServerSocket(TLAddress addr, MultConnectionBrain cb) throws IOException {
		_cb = cb;
		bind(addr);
	}

		
	@Override
	public ISocketTL accept() throws IOException {
		if(!_isBound) {
			MuLog.error("Attempting to accept on a server socket that is not bound");
			throw new IOException("Attempting to accept on a server socket that is not bound");
		}
		
		if(_cb.getInnerSocket() == null) {
			// Ensure that inner socket is created, connected
			_cb.connectInnerFactory();
		}
		
		return _serverSocketListener.blockOnWaitForNewSocketAndReturn();
		
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		_address = endpoint;
		_isBound = true;
		// TODO: ARCHITECTURE - We don't have a real bind.
		
		_serverSocketListener = new MultServerSocketListener(_address, _cb);
		
	}

	@Override
	public void close() throws IOException {
		
		if(_isBound) {
			_isClosed = true;
			_isBound = false;
			
			_serverSocketListener.close();
		} else {
			throw new IOException("Unable to close; server socket is not bound.");
		}
	}

	@Override
	public TLAddress getInetAddress() {
		return _address;
	}

	@Override
	public boolean isBound() {
		return _isBound;
	}

	@Override
	public boolean isClosed() {
		return _isClosed;
	}

}
