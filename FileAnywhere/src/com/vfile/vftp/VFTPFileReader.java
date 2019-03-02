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

package com.vfile.vftp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileReader;

public class VFTPFileReader extends Reader implements IFileReader {

	InputStreamReader _isr = null;
	VFTPClient _host = null;
	
	public VFTPFileReader(VFTPClient host, String fileName) throws FileNotFoundException, IOException {
		VFTPFileInputStream is = new VFTPFileInputStream(host, fileName);
		_isr = new InputStreamReader(is);
	}
	
	public VFTPFileReader(VFTPClient host, IFile file) throws FileNotFoundException, IOException {
		VFTPFileInputStream is = new VFTPFileInputStream(host, file.getPath());
		_isr = new InputStreamReader(is);
	}
	
	@Override
	public void close() throws IOException {
		_isr.close();
	}

	@Override
	public String getEncoding() {
		return _isr.getEncoding();

	}

	@Override
	public void mark(int readAheadLimit) throws IOException {
		_isr.mark(readAheadLimit);
	}

	@Override
	public boolean markSupported() {
		return _isr.markSupported();
	}

	@Override
	public int read() throws IOException {
		return _isr.read();
	}

	@Override
	public int read(char[] cbuf, int offset, int length) throws IOException {
		return _isr.read(cbuf, offset, length);

	}

	@Override
	public int read(char[] cbuf) throws IOException {
		return _isr.read(cbuf);
	}

	@Override
	public boolean ready() throws IOException {
		return _isr.ready();
	}

	@Override
	public void reset() throws IOException {
		_isr.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return _isr.skip(n);
	}

}
