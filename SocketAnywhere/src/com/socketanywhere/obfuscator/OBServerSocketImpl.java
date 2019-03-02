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

package com.socketanywhere.obfuscator;

import java.io.IOException;
import java.util.List;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class OBServerSocketImpl implements IServerSocketTL {
	
	ISocketFactory _socketFactory = new TCPSocketFactory();
	
	List<IDataTransformer> _dataTransformer;

	IServerSocketTL _serverSocket;
	
	TLAddress _addr;
	
	public OBServerSocketImpl(List<IDataTransformer> transformer, ISocketFactory socketFactory) throws IOException{
		_socketFactory = socketFactory;
		_dataTransformer = transformer;
	}

	public OBServerSocketImpl(TLAddress addr, List<IDataTransformer> transformer, ISocketFactory socketFactory) throws IOException{
		_addr = addr;
		_socketFactory = socketFactory;
		_dataTransformer = transformer;
		
		_serverSocket = _socketFactory.instantiateServerSocket(_addr);
	}

	
	
	@Override
	public ISocketTL accept() throws IOException {
		ISocketTL result = new OBSocketImpl(_dataTransformer, _serverSocket.accept());
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
		return _addr;
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
