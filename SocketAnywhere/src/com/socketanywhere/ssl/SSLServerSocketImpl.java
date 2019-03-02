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

package com.socketanywhere.ssl;
import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;


public class SSLServerSocketImpl implements IServerSocketTL {
	TLAddress _addr = null;
	IServerSocketTL _ss = null;
	String _pathToKeystore;
	
	ISocketFactory _socketFactory = new TCPSocketFactory();

	public SSLServerSocketImpl(ISocketFactory factory, String pathToKeystore) throws IOException {
		_socketFactory = factory;
		_pathToKeystore = pathToKeystore;
		_ss = _socketFactory.instantiateServerSocket();

}

	
	public SSLServerSocketImpl(String pathToKeystore) throws IOException {
				_pathToKeystore = pathToKeystore;		
			_ss = _socketFactory.instantiateServerSocket();
	}
	
	public SSLServerSocketImpl(TLAddress address, ISocketFactory socketFactory, String pathToKeystore) throws IOException {
		_addr = address;
		_pathToKeystore = pathToKeystore;
		_socketFactory = socketFactory;
		_ss = _socketFactory.instantiateServerSocket(new TLAddress(_addr.getPort())); 
			//new ServerSocket(_addr.getPort());
	}
		
	

	public SSLServerSocketImpl(TLAddress address, String pathToKeystore) throws IOException {
		_addr = address;
		_pathToKeystore = pathToKeystore;
		_ss = _socketFactory.instantiateServerSocket(new TLAddress(_addr.getPort())); 
			//new ServerSocket(_addr.getPort());
	}
		
	@Override
	public ISocketTL accept() throws IOException {
		ISocketTL s = _ss.accept();
		return new SSLSocketImpl(s, _pathToKeystore);
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		_addr = endpoint;
//		InetSocketAddress isa = new InetSocketAddress(_addr.getHostname(), _addr.getPort());
		_ss.bind(_addr);
	}

	@Override
	public void close() throws IOException {
		_ss.close();
	}

	@Override
	public TLAddress getInetAddress() {
		return _addr;
	}

	@Override
	public boolean isBound() {
		return _ss.isBound();
	}

	@Override
	public boolean isClosed() {
		return _ss.isClosed();
	}

}