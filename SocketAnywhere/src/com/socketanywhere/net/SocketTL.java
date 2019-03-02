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

package com.socketanywhere.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SocketTL implements ISocketTL {
	private ISocketTL _inner = null;
	private String _debugStr;
	
	/** This method should only be used by implementations of transport layers */
	public SocketTL(ISocketTL inner) {
		_inner = inner;		
	}
	
	public SocketTL() throws IOException { 
		_inner = SocketTLFactory.getInstance().instantiateSocket();
	}
	
	public SocketTL(TLAddress address) throws IOException { 
		_inner = SocketTLFactory.getInstance().instantiateSocket(address);
	}
	
	@Override
	public void close() throws IOException {
		_inner.close();

	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		_inner.connect(endpoint, timeout);
	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
		_inner.connect(endpoint);
	}

	@Override
	public TLAddress getAddress() {
		return _inner.getAddress();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return _inner.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return _inner.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		return _inner.isClosed();
	}

	@Override
	public boolean isConnected() {
		return _inner.isConnected();

	}
	
	@Override
	public String toString() {
		return _inner.toString();
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
