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

/** Wraps a given input stream; logs the data received, and passes the data to the caller. */
public class PTInputStream extends InputStream {

	PTSocketImpl _ourSocket;
	InputStream _inner;
	
	public PTInputStream(PTSocketImpl ourSocket, InputStream inner) {
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
		
		PTLogger.getEntry(_ourSocket).localInitDisconnect("InputStream close() call");
		PTLogger.getEntry(_ourSocket).serialize();
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
				PTLogger.getEntry(_ourSocket).remoteInitDisconnect("read() - returned -1");
				PTLogger.getEntry(_ourSocket).serialize();
			} else if(result > 0) {
				byte[] b = new byte[1];
				b[0] = (byte)result;
				PTLogger.getEntry(_ourSocket).receiveData(b, 0, 1);
				PTLogger.getEntry(_ourSocket).serialize();
			}
		} catch(IOException e) {
			if(e.getMessage().contains("socket closed")) {
				PTLogger.getEntry(_ourSocket).remoteInitDisconnect("read() - IOE["+e.getMessage()+"]");
				PTLogger.getEntry(_ourSocket).serialize();
			}
			throw e;
		}
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result;
		try {
			result = _inner.read(b, off, len);
			
			if(result == -1) {
				PTLogger.getEntry(_ourSocket).remoteInitDisconnect("read(3) - returned -1");
				PTLogger.getEntry(_ourSocket).serialize();
			} else if(result > 0) {
				PTLogger.getEntry(_ourSocket).receiveData(b, off, result);
				PTLogger.getEntry(_ourSocket).serialize();
			}
			
		} catch(IOException e) {
			if(e.getMessage().contains("socket closed")) {
				PTLogger.getEntry(_ourSocket).remoteInitDisconnect("read(3) - IOE ["+e.getLocalizedMessage()+"]");
				PTLogger.getEntry(_ourSocket).serialize();
			}			
			throw e;
		}
		return result;
	}

	@SuppressWarnings("unused")
	@Override
	public int read(byte[] b) throws IOException {
		int result;
		
		TestLogEntries e = PTLogger.getEntry(_ourSocket);
		
		try {
			
			result = _inner.read(b);

			if(result == -1) {
				PTLogger.getEntry(_ourSocket).remoteInitDisconnect("read(1) - returned -1");
				PTLogger.getEntry(_ourSocket).serialize();
			} else if(result > 0) {
				PTLogger.getEntry(_ourSocket).receiveData(b, 0, result);
				PTLogger.getEntry(_ourSocket).serialize();
			}
			
			
		} catch(IOException ex) {
			if(ex.getMessage().contains("socket closed")) {
				PTLogger.getEntry(_ourSocket).remoteInitDisconnect("read(1) - IOE ["+ex.getMessage()+"]");
				PTLogger.getEntry(_ourSocket).serialize();
			}			
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

}
