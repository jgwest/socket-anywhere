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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Implementation of a self-contained SSL socket, which uses default options, and does not do 
 * certificate authentication. */
public class SSLSocketImpl implements ISocketTL {
	Socket _socket = null;
	TLAddress _addr = null;
	
	SSLSocket _sslSocket = null;
	
	ISocketFactory _innerFactory = new TCPSocketFactory();
	
	String _debugStr;

	
	private SSLServerBinding _binding = null;
	
	static {
		
	}
	
	public void initBinding(String pathToKeyStore) {
		if(_binding == null) {
			_binding = new SSLServerBinding(pathToKeyStore);
			_binding.init();
		}
	}

	
	/** Called by SSLServerSocketImpl*/
	protected SSLSocketImpl(Socket socket, String pathToKeystore) throws IOException {
		initBinding(pathToKeystore);
		_socket = socket;
		_addr = new TLAddress(socket.getInetAddress().getHostName(), socket.getPort());
		serverHandshake();
	}

	/** Also called by SSLServerSocketImpl*/
	protected SSLSocketImpl(ISocketTL socket, String pathToKeystore) throws IOException {
		initBinding(pathToKeystore);
		_socket = new SocketAdaptor(socket);
		
		if(socket.getAddress() != null) {
			String hostname = socket.getAddress() != null ? socket.getAddress().getHostname() : null;
			_addr = new TLAddress(hostname, socket.getAddress().getPort());
		}
		
		serverHandshake();
	}

	
	public SSLSocketImpl(String pathToKeyStore) {
		initBinding(pathToKeyStore);
	}
	
	public SSLSocketImpl(TLAddress address, ISocketFactory factory, String pathToKeyStore) throws IOException {
		initBinding(pathToKeyStore);
		_innerFactory = factory;
		connect(address);
	}

	
	public SSLSocketImpl(ISocketFactory factory, String pathToKeyStore) throws IOException {
		initBinding(pathToKeyStore);
		_innerFactory = factory;
	}
	
	public SSLSocketImpl(TLAddress address, String pathToKeyStore) throws IOException {
		initBinding(pathToKeyStore);
		connect(address);
	}
	
	
	
	@Override
	public void close() throws IOException {		
		_sslSocket.close();
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Implement timeout.
		this.connect(endpoint);

	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
		_addr = endpoint;
		
		_socket = new SocketAdaptor(_innerFactory.instantiateSocket());
		
		InetSocketAddress isa = new InetSocketAddress(endpoint.getHostname(), endpoint.getPort());
		
		_socket.connect(isa);
		
		clientHandshake();
		
	}
	
	
	private void clientHandshake() throws IOException {
		SSLSocket ssls = ClientSecureConnectionImpl.initSSL(_socket);
		_sslSocket = ssls;		
	}
	
	private void serverHandshake() {
		_sslSocket = _binding.convertToSSLSocket(_socket);
	}
	

	@Override
	public TLAddress getAddress() {
		return _addr;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return _sslSocket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return _sslSocket.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		if(_sslSocket == null) {
			return false;
		} else {
			return _sslSocket.isClosed();
		}
	}

	@Override
	public boolean isConnected() {
		if(_sslSocket == null) {
			return false;
		} else {
			return _sslSocket.isConnected();
		}
	}


	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}


	@Override
	public String getDebugStr() {
		return _debugStr;
	}
}
