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

package com.socketanywhere.unreliable;

import java.io.IOException;
import java.io.InputStream;

public class URInputStream extends InputStream {

	URSocketImpl _ourSocket;
	InputStream _inner;
	
	public URInputStream(URSocketImpl ourSocket, InputStream inner) {
		_ourSocket = ourSocket;
		_inner = inner;
	}
	
	@Override
	public int available() throws IOException {
		return _inner.available();
	}

	@Override
	public void close() throws IOException {
		_inner.close();
		
	}

	@Override
	public synchronized void mark(int readlimit) {
		_inner.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return _inner.markSupported();
	}

	@Override
	public int read() throws IOException {
		int result;
		
		try {
			result = _inner.read();
			if(result == -1) {
			} else if(result > 0) {
				byte[] b = new byte[1];
				_ourSocket.getCentralInfo().getConnInfo().eventReceivedData(this, b, 0, result);
				b[0] = (byte)result;
			}
		} catch(IOException e) {
			throw e;
		}
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result;
		try {
			result = _inner.read(b, off, len);
			_ourSocket.getCentralInfo().getConnInfo().eventReceivedData(this, b, off, result);
			
		} catch(IOException e) {
			throw e;
		}
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int result;
		
		try {
			
			result = _inner.read(b);
			_ourSocket.getCentralInfo().getConnInfo().eventReceivedData(this, b, 0, result);
			
		} catch(IOException ex) {
			throw ex;
		}
		
		return result;
	}

	@Override
	public synchronized void reset() throws IOException {
		_inner.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return _inner.skip(n);
	}

	public URSocketImpl getOurSocket() {
		return _ourSocket;
	}
}
