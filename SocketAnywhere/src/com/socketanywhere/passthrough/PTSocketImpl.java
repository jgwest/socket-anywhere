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
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Purpose of this class is to log any events that occur on the underlying innerfactory socket,
 * and then pass the event to the underlying implementation. */
public class PTSocketImpl implements ISocketTL {

//	public static final Object LOCK = new Object();
	
	ISocketFactory _innerFactory = new TCPSocketFactory();
	TLAddress _addr;
	
	ISocketTL _socket;
	
	InputStream _inputStream;
	OutputStream _outputStream;
	
	String _debugStr;
	
	String _dbgNodeIdentifier;
	
	public PTSocketImpl(ISocketFactory factory, String dbgNodeIdentifier ) throws IOException {
		_innerFactory = factory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
		
	}

	/** Uses the default factory (TCP) */
	public PTSocketImpl(TLAddress address) throws IOException {
		_addr = address;
		connect(address);
	}
	
	public PTSocketImpl(TLAddress address, ISocketFactory factory, String dbgNodeIdentifier) throws IOException {
		_addr = address;
		_innerFactory = factory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
		connect(address);
	}
	
	// Called by server-socket only
	protected PTSocketImpl(ISocketTL socket, String dbgNodeIdentifier) throws IOException {
		_socket = socket;
		_addr = _socket.getAddress();
		_dbgNodeIdentifier = dbgNodeIdentifier;
	
		_inputStream = new PTInputStream(this, _socket.getInputStream());
		_outputStream = new PTOutputStream(this, _socket.getOutputStream());			
	}


	
	@Override
	public void close() throws IOException {
		_socket.close();
		
		PTLogger.getEntry(this).localInitDisconnect("PTSocketImpl - close()");
		PTLogger.getEntry(this).serialize();
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
			
		_inputStream = new PTInputStream(this, _socket.getInputStream());
		_outputStream = new PTOutputStream(this, _socket.getOutputStream());
		
		PTLogger.getEntry(this).localInitConnect();
		PTLogger.getEntry(this).serialize();
		
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
