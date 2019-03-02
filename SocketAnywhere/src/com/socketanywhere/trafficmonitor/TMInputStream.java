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

public class TMInputStream extends InputStream {

	TMSocketImpl _ourSocket;
	InputStream _inner;
	TMStatsBrain _brain;

	public TMInputStream(TMSocketImpl ourSocket, InputStream inner, TMStatsBrain brain) {
		_ourSocket = ourSocket;
		_inner = inner;
		_brain = brain;
	}

	private void checkConstraints() throws IOException {
		if(!_brain.checkConstraints()) {
			try { close(); } catch (IOException e) { }
			throw new IOException("Socket constraints have been met.");
		}
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
		checkConstraints();
		
		int result;
		result = _inner.read();
		if(result != -1) {
			_brain.eventDataReceived(1);
		}
		
		checkConstraints();
		
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		checkConstraints();
		
		int result;
		result = _inner.read(b, off, len);
		
		if(result != -1) {
			_brain.eventDataReceived(result);
		}
		
		checkConstraints();
		
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		checkConstraints();
		
		int result;
		result = _inner.read(b);
		
		if(result != -1) {
			_brain.eventDataReceived(result);
		}
		
		checkConstraints();
	
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

}
