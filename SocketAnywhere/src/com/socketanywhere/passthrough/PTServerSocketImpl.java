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

package com.socketanywhere.passthrough;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class PTServerSocketImpl implements IServerSocketTL {
	
	
	ISocketFactory _socketFactory = new TCPSocketFactory();
	
	IServerSocketTL _serverSocket;
	
	TLAddress _addr;
	String _dbgNodeIdentifier;
	
	public PTServerSocketImpl(ISocketFactory socketFactory, String dbgNodeIdentifier) throws IOException{
		_socketFactory = socketFactory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
	}

	public PTServerSocketImpl(TLAddress addr, ISocketFactory socketFactory, String dbgNodeIdentifier) throws IOException{
		_addr = addr;
		_socketFactory = socketFactory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
		_serverSocket = _socketFactory.instantiateServerSocket(addr);
	}

	
	
	@Override
	public ISocketTL accept() throws IOException {
		PTSocketImpl result = new PTSocketImpl(_serverSocket.accept(), _dbgNodeIdentifier);
		PTLogger.getEntry(result).remoteInitConnect();
		PTLogger.getEntry(result).serialize();
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
