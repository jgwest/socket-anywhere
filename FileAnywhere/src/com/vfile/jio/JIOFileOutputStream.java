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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileOutputStream;

public class JIOFileOutputStream implements IFileOutputStream {
	
	private FileOutputStream _inner;
	
	JIOFileOutputStream(IFile file) throws FileNotFoundException {
		_inner = new FileOutputStream(JIOFile.convert(file));
	}

	JIOFileOutputStream(IFile file, boolean append) throws FileNotFoundException {
		_inner = new FileOutputStream(JIOFile.convert(file), append);
	}

	JIOFileOutputStream(String name) throws FileNotFoundException {
		_inner = new FileOutputStream(name);
	}

	JIOFileOutputStream(String name, boolean append) throws FileNotFoundException {
		_inner = new FileOutputStream(name, append);
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
	public void write(byte[] b, int off, int len) throws IOException {
		_inner.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		_inner.write(b);
	}

	@Override
	public void write(int arg0) throws IOException {
		_inner.write(arg0);
	}

}
