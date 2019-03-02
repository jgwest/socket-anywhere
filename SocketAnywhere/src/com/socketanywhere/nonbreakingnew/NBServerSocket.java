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

package com.socketanywhere.nonbreakingnew;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Listens on an underlying socket factory, and, if a socket is returned, wrap it with our NBSocket code,
 * and return it to the caller. */
public class NBServerSocket implements IServerSocketTL {

	private TLAddress _address;
	private NBServerSocketListener _serverSocketListener;
	private final ISocketFactory _factory;
	
	private final NBOptions _options;
	
	private final ConnectionBrain _cb;
	
	private boolean _isBound = false;
	private boolean _isClosed = false;
	
	
//	public NBServerSocket(ISocketFactory factory) {
//		_options = new NBOptions();
//		_cb = new ConnectionBrain(_factory, _options);
//		_factory = factory;
//	}
	
	public NBServerSocket(ConnectionBrain cb, ISocketFactory factory, NBOptions options) {
		_cb = cb;
		_options = options;
		_factory = factory;
	}
	
	public NBServerSocket(TLAddress addr, ConnectionBrain cb, ISocketFactory factory, NBOptions options) throws IOException {
		_cb = cb;
		_factory = factory;
		_options = options;
		bind(addr);
	}
		
	@Override
	public ISocketTL accept() throws IOException {
		if(!_isBound) {
			NBLog.error("Attempting to accept on a server socket that is not bound");
			throw new IOException("Attempting to accept on a server socket that is not bound");
		}
		
		return _serverSocketListener.blockOnWaitForNewSocketAndReturn();
		
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		_address = endpoint;
		_isBound = true;
		
		_serverSocketListener = new NBServerSocketListener(_address, _cb, _factory, _options);
		_serverSocketListener.start();
		
		
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
