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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Corresponds to a normal Socket; it instantiates a normal Socket with the given host/port 
 * and calls its methods in order to fulfill the ISocketTL interface. */
public class TCPSocketImpl implements ISocketTL {

	Socket _socket = null;
	TLAddress _addr = null;
	String _debugStr;
	
	protected TCPSocketImpl(Socket socket) {
		_socket = socket;
		_addr = new TLAddress(socket.getInetAddress().getHostName(), socket.getPort());
	}
	
	public TCPSocketImpl() {
	}
	
	public TCPSocketImpl(TLAddress address) throws IOException {
		_addr = address;
		try {
			_socket = new Socket();
			_socket.setKeepAlive(true); // Added Dec 2014
//			_socket.setReceiveBufferSize(256 * 1024);
			_socket.connect(new InetSocketAddress(_addr.getHostname(), _addr.getPort()));
			
		} catch(IOException e) {
			TCPLogger.error("Unable to connect on TCP Socket: "+e, e);
			throw(e);
		}
	}
	
	@Override
	public void close() throws IOException {
		_socket.close();
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Implement timeout.
		this.connect(endpoint);

	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
		_addr = endpoint;
				
		_socket = new Socket();
		_socket.setKeepAlive(true); // Added Dec 2014
//		_socket.setReceiveBufferSize(1024 * 256);
		InetSocketAddress isa = new InetSocketAddress(endpoint.getHostname(), endpoint.getPort());
		
		_socket.connect(isa);

	}

	@Override
	public TLAddress getAddress() {
		return _addr;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return _socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return _socket.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		if(_socket == null) {
			return false;
		} else {
			return _socket.isClosed();
		}
	}

	@Override
	public boolean isConnected() {
		if(_socket == null) {
			return false;
		} else {
			return _socket.isConnected();
		}
	}
	
	@Override
	public int hashCode() {
		return _socket.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return _socket.equals(((TCPSocketImpl)obj)._socket);
	}
	
	@Override
	public String toString() {
		return "[TCPSocketImpl] "+(_debugStr != null ? "dbg-str:.["+_debugStr+"]." : "" )+" _socket.toString():s{"+_socket.toString()+"}s";
	}

	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}

	@Override
	public String getDebugStr() {
		return _debugStr;
	}
	
	public Socket getInnerSocket() {
		return _socket;
	}
}
