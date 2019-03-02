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

package com.socketanywhere.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class ExSocketImpl implements ISocketTL {

	ISocketFactory _innerFactory = new TCPSocketFactory();
	TLAddress _addr;
	
	ISocketTL _socket;
	
	InputStream _inputStream;
	OutputStream _outputStream;
	
	String _debugStr;
	
	
	public ExSocketImpl(ISocketFactory factory) throws IOException {
		_innerFactory = factory;
		
	}

	/** Uses the default factory (TCP) */
	public ExSocketImpl(TLAddress address) throws IOException {
		_addr = address;
		connect(address);
	}
	
	public ExSocketImpl(TLAddress address, ISocketFactory factory) throws IOException {
		_addr = address;
		_innerFactory = factory;
		connect(address);
	}
	
	// Called by server-socket only
	protected ExSocketImpl(ISocketTL socket) throws IOException {
		_socket = socket;
		_addr = _socket.getAddress();
	
		_inputStream = new ExInputStream(this, _socket.getInputStream());
		_outputStream = new ExOutputStream(this, _socket.getOutputStream());			
	}


	
	@Override
	public void close() throws IOException {
		_socket.close();		
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Implement timeout
		_addr = endpoint;
		connect(endpoint);
	}

//	@Override
	public void connect(TLAddress endpoint) throws IOException {
		_addr = endpoint;
		_socket = _innerFactory.instantiateSocket(endpoint);
			
		_inputStream = new ExInputStream(this, _socket.getInputStream());
		_outputStream = new ExOutputStream(this, _socket.getOutputStream());
				
	}

	@Override
	public InputStream getInputStream() throws IOException {
		
		return _inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {

		return _outputStream;
		
	}

	@Override
	public boolean isClosed() {
		boolean result;
		
		if(_socket == null) {
			result = false;
		} else {
			result = _socket.isClosed();
		}
		return result;
	}

	@Override
	public boolean isConnected() {
		
		boolean result;
		
		if(_socket == null) {
			result = false;
		} else {
			result = _socket.isConnected();
		}
		return result;
	}

	@Override
	public int hashCode() {
		synchronized(_socket) {
			return _socket.hashCode();
		}
	}
	
	@Override
	public TLAddress getAddress() {
		synchronized(_socket) {
			TLAddress result = _addr; // new TLAddress(_socket.getInetAddress().getHostName(), _socket.getPort());
			return result;
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
