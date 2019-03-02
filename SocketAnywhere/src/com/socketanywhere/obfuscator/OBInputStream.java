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

package com.socketanywhere.obfuscator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class OBInputStream extends InputStream {
	InputStream _inner;
	
	List<IDataTransformer> _dataTransformerList;
	
	
	public OBInputStream(InputStream inner, List<IDataTransformer> transformerList) {
		_inner = inner;
		_dataTransformerList = transformerList;
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
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int c = read(b);
		if(c == -1) return -1;
		else return b[0];		
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = _inner.read(b, off, len);
		if(result < 1) return result;
		
		// Decrypt from end to beginning
		for(int x = _dataTransformerList.size()-1; x >= 0; x--) {
			_dataTransformerList.get(x).decrypt(b, off, result);
		}
		
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public void reset() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long skip(long n) throws IOException {
		if(n == 0) return 0;
		
		return _inner.skip(n);
//		throw new UnsupportedOperationException();
	}

}
