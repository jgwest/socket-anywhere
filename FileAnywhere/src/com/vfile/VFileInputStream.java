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

package com.vfile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.vfile.interfaces.IFileInputStream;

public class VFileInputStream extends InputStream implements IFileInputStream {
	
	IFileInputStream _inner = null;

	public VFileInputStream(VFile file) throws FileNotFoundException  {
		_inner = DefaultFileFactory.getDefaultInstance().createFileInputStream(file.getInnerFile());
	}
	
	public VFileInputStream(String name) throws FileNotFoundException  {
		_inner = DefaultFileFactory.getDefaultInstance().createFileInputStream(name);
	}
	
	public int available() throws IOException {
		return _inner.available();
	}

	public void close() throws IOException {
		_inner.close();
	}

	public int read() throws IOException {
		return _inner.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return _inner.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		return _inner.read(b);
	}

	public long skip(long n) throws IOException {
		return _inner.skip(n);
	}

	public synchronized void mark(int readlimit) {
		_inner.mark(readlimit);
	}

	public boolean markSupported() {
		return _inner.markSupported();
	}

	public void reset() throws IOException {
		_inner.reset();
	}

}
