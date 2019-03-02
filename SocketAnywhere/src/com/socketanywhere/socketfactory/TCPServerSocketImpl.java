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

package com.socketanywhere.socketfactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;


public class TCPServerSocketImpl implements IServerSocketTL {
	TLAddress _addr = null;
	ServerSocket _ss = null;

	public TCPServerSocketImpl() throws IOException {
		_ss = new ServerSocket();
//		_ss.setReceiveBufferSize(256 * 1024);
	}

	public TCPServerSocketImpl(TLAddress address) throws IOException {
		_addr = address;
		_ss = new ServerSocket();
//		_ss.setReceiveBufferSize(256 * 1024);
		
		if(address.getHostname() != null) {
			_ss.bind(new InetSocketAddress(address.getHostname(), address.getPort()));
		} else {
			_ss.bind(new InetSocketAddress(address.getPort()));
		}
	}
		
	@Override
	public ISocketTL accept() throws IOException {
		Socket s = _ss.accept();
		s.setKeepAlive(true); // Added Dec 2014
		return new TCPSocketImpl(s);
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		_addr = endpoint;
		InetSocketAddress isa;
		
		if(_addr.getHostname() != null) {
			isa = new InetSocketAddress(_addr.getHostname(), _addr.getPort());
		} else {
			isa = new InetSocketAddress(_addr.getPort());
		}
		_ss.bind(isa);
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