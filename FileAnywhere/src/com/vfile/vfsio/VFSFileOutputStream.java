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
import java.io.OutputStream;

import com.vfile.interfaces.IFileOutputStream;

public class VFSFileOutputStream extends OutputStream implements IFileOutputStream {
	VFSFile _file;
	OutputStream _outer;
	
	public VFSFileOutputStream(VFSFile file) throws IOException {
		this(file, false);
	}

	public VFSFileOutputStream(VFSFile file, boolean isAppend) throws IOException {
		_file = file;
		_outer = _file._fo.getContent().getOutputStream(isAppend);
	}

	
	@Override
	public void close() throws IOException {
		_outer.close();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		_outer.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		_outer.write(b);
	}

	@Override
	public void write(int n) throws IOException {
		_outer.write(n);
	}

	@Override
	public void flush() throws IOException {
		_outer.flush();
	}

}
