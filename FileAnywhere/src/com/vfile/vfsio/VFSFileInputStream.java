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

package com.vfile.vfsio;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.vfs.FileSystemException;

import com.vfile.interfaces.IFileInputStream;

public class VFSFileInputStream extends InputStream implements IFileInputStream {
	VFSFile _file;
	InputStream _inner;
	
	public VFSFileInputStream(VFSFile file) throws IOException {
		_file = file;
		try {
			_inner = _file._fo.getContent().getInputStream();
		} catch (FileSystemException e) {
			throw(e);
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
	public long skip(long n) throws IOException {
		return _inner.skip(n);
	}

	@Override
	public void mark(int readlimit) {
		_inner.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return _inner.markSupported();
	}

	@Override
	public void reset() throws IOException {
		_inner.reset();
	}

}
