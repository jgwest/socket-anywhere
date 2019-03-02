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

import java.io.IOException;
import java.io.Writer;

import com.vfile.interfaces.IFileWriter;

public class VFileWriter extends Writer implements IFileWriter {
	
	IFileWriter _inner;

	public VFileWriter(String fileName) throws IOException { 
		_inner = DefaultFileFactory.getDefaultInstance().createFileWriter(fileName);
	}
	
	public VFileWriter(String fileName, boolean append) throws IOException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileWriter(fileName, append);
	}

	public VFileWriter(VFile file) throws IOException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileWriter(file.getInnerFile());
	}
	
	public VFileWriter(VFile file, boolean append) throws IOException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileWriter(file.getInnerFile(), append);
	}

	public Writer append(char c) throws IOException {
		return _inner.append(c);
	}

	public Writer append(CharSequence csq, int start, int end) throws IOException {
		return _inner.append(csq, start, end);
	}

	public Writer append(CharSequence csq) throws IOException {
		return _inner.append(csq);
	}

	public void close() throws IOException {
		_inner.close();
	}

	public void flush() throws IOException {
		_inner.flush();
	}

	public String getEncoding() {
		return _inner.getEncoding();
	}

	public void write(char[] cbuf, int off, int len) throws IOException {
		_inner.write(cbuf, off, len);
	}

	public void write(int c) throws IOException {
		_inner.write(c);
	}

	public void write(String str, int off, int len) throws IOException {
		_inner.write(str, off, len);
	}

	public void write(char[] cbuf) throws IOException {
		_inner.write(cbuf);
	}

	public void write(String str) throws IOException {
		_inner.write(str);
	}

}
