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
package com.vfile.jio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileInputStream;

public class JIOFileInputStream implements IFileInputStream {
	
	FileInputStream _inner = null;

	JIOFileInputStream(IFile file) throws FileNotFoundException {
		_inner = new FileInputStream(JIOFile.convert(file));
	}

	JIOFileInputStream(String name) throws FileNotFoundException {
		_inner = new FileInputStream(name);
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
	public void mark(int readlimit) {
		mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return _inner.markSupported();
	}

	@Override
	public int read() throws IOException {
		return _inner.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return _inner.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return _inner.read(b);
	}

	@Override
	public void reset() throws IOException {
		_inner.reset();
	}

	@Override
	public long skip(long arg0) throws IOException {
		return _inner.skip(arg0);
	}
	
}
