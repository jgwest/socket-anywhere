/*
	Copyright 2012, 2013 Jonathan West

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

package com.vfile.s3io;

import java.io.IOException;
import java.io.InputStream;

import org.jets3t.service.S3ServiceException;

import com.vfile.interfaces.IFileInputStream;

public class S3FileInputStream extends InputStream implements IFileInputStream {
	private final boolean DEBUG = false;
	
	S3File _file;
	InputStream _inner;
	
	public S3FileInputStream(S3File file) {
		_file = file;
	}
	
	private void openInnerIfNeeded() throws IOException {
		synchronized(this) {
			if(_inner != null) return;
			
			try {
				if(!_file.exists()) {
					throw new IOException("File does not exist.");
				}
				_inner = _file.getS3ObjectFull().getDataInputStream();

			} catch (S3ServiceException e) {
				S3Log.err("S3ServiceException in openInnerIfNeeded(...) "+e);
				throw new IOException("Unable to open file"+e);
			}
			
		}
	}
	
	@Override
	public int available() throws IOException {
		openInnerIfNeeded();
		
		int result = _inner.available();
		
		if(DEBUG) { System.out.println("available() returns "+result); }
		return result;
	}

	@Override
	public void close() throws IOException {
		openInnerIfNeeded();
		
		if(DEBUG) { System.out.println("close() called."); }
		_inner.close();
	}

	@Override
	public int read() throws IOException {
		openInnerIfNeeded();
		
		if(DEBUG) { System.out.println("read() in."); }
		int result = _inner.read();
		if(DEBUG) { System.out.println("read() returned: "+result); }
		
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		openInnerIfNeeded();
		
		if(DEBUG) { System.out.println("read(byte[] b, int off, int len) in."); }
		int result = _inner.read(b, off, len);
		if(DEBUG) { System.out.println("read(byte[] b, int off, int len) returned: "+result); }
		
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		
		openInnerIfNeeded();
		
		if(DEBUG) { System.out.println("read(byte[] b) in."); }
		int result =_inner.read(b);
		if(DEBUG) { System.out.println("read(byte[] b) returned: "+result); }
		
		return result;
	}

	@Override
	public long skip(long n) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

}
