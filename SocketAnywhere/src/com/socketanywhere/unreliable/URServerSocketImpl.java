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

package com.socketanywhere.unreliable;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class URServerSocketImpl implements IServerSocketTL {
	
	
	private final ISocketFactory _socketFactory;
	
	IServerSocketTL _serverSocket;
	
	TLAddress _addr;
	final String _dbgNodeIdentifier;
	
	private final URCentralInfo _centralInfo;
	
	public URServerSocketImpl(ISocketFactory socketFactory, String dbgNodeIdentifier, URCentralInfo centralInfo) throws IOException{
		_socketFactory = socketFactory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
		_centralInfo = centralInfo;
	}

	public URServerSocketImpl(TLAddress addr, ISocketFactory socketFactory, String dbgNodeIdentifier, URCentralInfo centralInfo) throws IOException{
		_addr = addr;
		_socketFactory = socketFactory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
		_serverSocket = _socketFactory.instantiateServerSocket(addr);
		_centralInfo = centralInfo;
	}

	
	
	@Override
	public ISocketTL accept() throws IOException {
		URSocketImpl result = new URSocketImpl(_serverSocket.accept(), _dbgNodeIdentifier, _centralInfo);
		return result;
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		_serverSocket = _socketFactory.instantiateServerSocket();
		_addr = endpoint;
		_serverSocket.bind(endpoint);
	}

	@Override
	public void close() throws IOException {
		_serverSocket.close();
	}

	@Override
	public TLAddress getInetAddress() {
		return _serverSocket.getInetAddress();
	}

	@Override
	public boolean isBound() {
		return _serverSocket.isBound();
	}

	@Override
	public boolean isClosed() {
		return _serverSocket.isClosed();
	}

}
