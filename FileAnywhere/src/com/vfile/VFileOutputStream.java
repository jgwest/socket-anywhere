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
import java.io.OutputStream;

import com.vfile.interfaces.IFileOutputStream;

public class VFileOutputStream extends OutputStream implements IFileOutputStream {
	IFileOutputStream _inner = null;
	
	public VFileOutputStream(VFile file) throws FileNotFoundException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileOutputStream(file.getInnerFile());
	}

	public VFileOutputStream(VFile file, boolean append) throws FileNotFoundException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileOutputStream(file.getInnerFile(), append);
	}

	public VFileOutputStream(String name) throws FileNotFoundException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileOutputStream(name);
	}

	public VFileOutputStream(String name, boolean append) throws FileNotFoundException {
		_inner = DefaultFileFactory.getDefaultInstance().createFileOutputStream(name, append);
	}
	
	public void close() throws IOException {
		_inner.close();
	}

	public void flush() throws IOException {
		_inner.flush();
	}

	public void write(byte[] b, int off, int len) throws IOException {
		_inner.write(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		_inner.write(b);
	}

	public void write(int arg0) throws IOException {
		_inner.write(arg0);
	}

}
