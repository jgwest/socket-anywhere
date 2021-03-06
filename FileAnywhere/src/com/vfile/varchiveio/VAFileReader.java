/* Copyright 2013 Jonathan West

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

package com.vfile.varchiveio;

import java.io.IOException;
import java.io.Reader;

import net.java.truevfs.access.TFileReader;

import com.vfile.interfaces.IFileReader;

public class VAFileReader extends Reader implements IFileReader {

	TFileReader _inner;
	
	public VAFileReader(VAFile f) throws IOException {
		_inner = new TFileReader(f._inner);
	}

	@Override
	public void close() throws IOException {
		_inner.close();
	}

	@Override
	public String getEncoding() {
		return _inner.getEncoding();
	}

	@Override
	public int read() throws IOException {
		return _inner.read();
	}

	@Override
	public int read(char[] cbuf, int offset, int length) throws IOException {
		return _inner.read(cbuf, offset, length);
	}

	@Override
	public boolean ready() throws IOException {
		return _inner.ready();
	}

	@Override
	public void mark(int readAheadLimit) throws IOException {
		_inner.mark(readAheadLimit);
	}

	@Override
	public boolean markSupported() {
		return _inner.markSupported();
	}

	@Override
	public int read(char[] cbuf) throws IOException {
		return _inner.read(cbuf);
	}

	@Override
	public void reset() throws IOException {
		_inner.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return _inner.skip(n);
	} 
	
}
