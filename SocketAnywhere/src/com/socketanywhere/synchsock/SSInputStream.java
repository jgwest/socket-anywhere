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

public class SSInputStream extends InputStream {

	InputStream _inner;
	
	public SSInputStream(InputStream inner) {
		synchronized(SSSocketImpl.LOCK) {
			_inner = inner;
		}
	}
	
	@Override
	public int available() throws IOException {
		synchronized(SSSocketImpl.LOCK) { 
			return _inner.available();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized(SSSocketImpl.LOCK) {
			_inner.close();
		}
	}

	@Override
	public synchronized void mark(int readlimit) {
		synchronized(SSSocketImpl.LOCK) {
			_inner.mark(readlimit);
		}
	}

	@Override
	public boolean markSupported() {
		synchronized(SSSocketImpl.LOCK) {
			return _inner.markSupported();
		}
	}

	@Override
	public int read() throws IOException {
//		synchronized(PTSocketImpl.LOCK) {
			return _inner.read();
//		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
//		synchronized(PTSocketImpl.LOCK) {
			return _inner.read(b, off, len);
//		}
	}

	@Override
	public int read(byte[] b) throws IOException {
//		synchronized(PTSocketImpl.LOCK) {
			return _inner.read(b);
//		}
	}

	@Override
	public synchronized void reset() throws IOException {
		synchronized(SSSocketImpl.LOCK) {
			_inner.reset();
		}
	}

	@Override
	public long skip(long n) throws IOException {
		synchronized(SSSocketImpl.LOCK) {
			return _inner.skip(n);
		}
	}

}
