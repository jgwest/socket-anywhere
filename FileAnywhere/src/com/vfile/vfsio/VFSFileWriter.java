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
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.vfile.interfaces.IFileWriter;

public class VFSFileWriter extends Writer implements IFileWriter {

	VFSFile _file;
	OutputStreamWriter _inner = null;
	
	
	public VFSFileWriter(VFSFile file) throws IOException {
		this(file, false);
	}
	
	public VFSFileWriter(VFSFile file, boolean append) throws IOException {
		_file = file;
		_inner = new OutputStreamWriter(new VFSFileOutputStream(file, append));
	}
	
	@Override
	public void close() throws IOException {
		_inner.close();
	}

	@Override
	public void flush() throws IOException {
		_inner.flush();
	}

	@Override
	public String getEncoding() {
		return _inner.getEncoding();
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		_inner.write(cbuf, off, len);
	}

	@Override
	public void write(int c) throws IOException {
		_inner.write(c);
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		_inner.write(str, off, len);
	}

	@Override
	public Writer append(char c) throws IOException {
		return _inner.append(c);
	}

	@Override
	public Writer append(CharSequence csq, int start, int end) throws IOException {
		return _inner.append(csq, start, end);
	}

	@Override
	public Writer append(CharSequence csq) throws IOException {
		return _inner.append(csq);
	}

	@Override
	public void write(char[] cbuf) throws IOException {
		_inner.write(cbuf);
	}

	@Override
	public void write(String str) throws IOException {
		_inner.write(str);
	}

}
