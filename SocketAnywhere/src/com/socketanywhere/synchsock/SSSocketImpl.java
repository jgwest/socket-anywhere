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

package com.socketanywhere.synchsock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Wraps another socket factory, and synchronizes on its operations. */
public class SSSocketImpl implements ISocketTL {

	public static final Object LOCK = new Object();
	
	ISocketFactory _innerFactory = new TCPSocketFactory();
	TLAddress _addr;
	
	ISocketTL _socket;
	
	InputStream _inputStream;
	OutputStream _outputStream;
	
	String _debugStr;
	
	
	public SSSocketImpl(ISocketFactory factory) throws IOException {
		synchronized(LOCK) {
			_innerFactory = factory;
		}
		
	}

	/** Uses the default factory (TCP) */
	public SSSocketImpl(TLAddress address) throws IOException {
		synchronized(LOCK) {
			_addr = address;
			connect(address);
		}
	}
	
	public SSSocketImpl(TLAddress address, ISocketFactory factory) throws IOException {
		synchronized(LOCK) {
			_addr = address;
			_innerFactory = factory;
			connect(address);
		}
	}
	
	// Called by server-socket only
	protected SSSocketImpl(ISocketTL socket) throws IOException {
		synchronized(LOCK) {
			_socket = socket;
			_addr = _socket.getAddress();
	
			_inputStream = new SSInputStream(_socket.getInputStream());
			_outputStream = new SSOutputStream(_socket.getOutputStream());
		}
				
	}


	
	@Override
	public void close() throws IOException {
		synchronized(LOCK) {		
			_socket.close();
		}
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		synchronized(LOCK) {		
			// TODO: LOWER - Implement timeout
			_addr = endpoint;
			connect(endpoint);
		}
	}

//	@Override
	public void connect(TLAddress endpoint) throws IOException {
		synchronized(LOCK) {		
			System.out.println(Thread.currentThread().getId()+"> OBSocketImpl1 - connecting to "+endpoint);
			_addr = endpoint;
			_socket = _innerFactory.instantiateSocket(endpoint);
			
			System.out.println(Thread.currentThread().getId()+"> OBSocketImp2");
			_inputStream = new SSInputStream(_socket.getInputStream());
			_outputStream = new SSOutputStream(_socket.getOutputStream());
			
			System.out.println(Thread.currentThread().getId()+"> OBSocketImp3");
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		
		synchronized(LOCK) {		
//		System.err.println("PT - getIS called.");
			return _inputStream;
		}
//		return _socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		synchronized (LOCK) {
			return _outputStream;
		}
//		System.err.println("PT - getOS called.");
		
	}

	@Override
	public boolean isClosed() {
		synchronized(LOCK) {
			boolean result;
			
			if(_socket == null) {
				result = false;
			} else {
				result = _socket.isClosed();
			}
	//		System.err.println("PT isClosed() called. "+result);
			return result;
		}
	}

	@Override
	public boolean isConnected() {
		synchronized (LOCK) {
			
			boolean result;
			
			if(_socket == null) {
				result = false;
			} else {
				result = _socket.isConnected();
			}
	//		System.err.println("PT isConnected() called. "+result);
			return result;
		
		}
	}

	@Override
	public int hashCode() {
		synchronized(_socket) {
			return _socket.hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		synchronized(_socket) {
			return _socket.equals(obj);
		}
	}
	
	@Override
	public TLAddress getAddress() {
		synchronized(_socket) {
			TLAddress result = _addr; // new TLAddress(_socket.getInetAddress().getHostName(), _socket.getPort());
	//		System.err.println("PT getAddress() called. "+result);
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
