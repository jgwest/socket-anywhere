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
import java.io.Writer;

import net.java.truevfs.access.TFileWriter;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileWriter;

public class VAFileWriter extends Writer implements IFileWriter {

	TFileWriter _inner;
	
	public VAFileWriter(IFile file, boolean append) throws IOException {
		_inner = new TFileWriter(((VAFile)file)._inner, append);
	}
	
	@Override
	public String getEncoding() {
		return _inner.getEncoding();
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
	public void write(char[] cbuf, int off, int len) throws IOException {
		_inner.write(cbuf, off, len);
	}

}
