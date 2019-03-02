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

package com.socketanywhere.trafficmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class TMSocketImpl implements ISocketTL {

	ISocketFactory _innerFactory = new TCPSocketFactory();
	TLAddress _addr;
	
	ISocketTL _socket;
	
	InputStream _inputStream;
	OutputStream _outputStream;
	
	String _debugStr;
	
	TMStatsBrain _brain;
	
	
	public TMSocketImpl(ISocketFactory factory, TMStatsBrain brain) throws IOException {
		_innerFactory = factory;
		_brain = brain;
		
	}

	/** Uses the default factory (TCP) */
	public TMSocketImpl(TLAddress address, TMStatsBrain brain) throws IOException {
		_addr = address;
		_brain = brain;
		connect(address);
	}
	
	public TMSocketImpl(TLAddress address, ISocketFactory factory, TMStatsBrain brain) throws IOException {
		_addr = address;
		_brain = brain;
		_innerFactory = factory;
		connect(address);
	}
	
	// Called by server-socket only
	protected TMSocketImpl(ISocketTL socket, TMStatsBrain brain) throws IOException {
		_socket = socket;
		_addr = _socket.getAddress();
		_brain = brain;
	
		_inputStream = new TMInputStream(this, _socket.getInputStream(), _brain);
		_outputStream = new TMOutputStream(this, _socket.getOutputStream(), _brain);
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
		if(!_brain.checkConstraints()) {
			throw new IOException("Socket constraints have been met.");
		};
		
		_addr = endpoint;
		_socket = _innerFactory.instantiateSocket(endpoint);
		_brain.eventConnectionAttempt();
			
		_inputStream = new TMInputStream(this, _socket.getInputStream(), _brain);
		_outputStream = new TMOutputStream(this, _socket.getOutputStream(), _brain);
				
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
