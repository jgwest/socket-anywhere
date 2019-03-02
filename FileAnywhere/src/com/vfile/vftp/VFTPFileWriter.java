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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileWriter;

public class VFTPFileWriter extends Writer implements IFileWriter {
	
	OutputStreamWriter _osw = null;


	public VFTPFileWriter(VFTPClient host, String fileName) throws IOException {
		OutputStream o = new VFTPFileOutputStream(host, fileName);
		_osw = new OutputStreamWriter(o);
	}
	
	public VFTPFileWriter(VFTPClient host,String fileName, boolean append) throws IOException {
		OutputStream o = new VFTPFileOutputStream(host, fileName, append);
		_osw = new OutputStreamWriter(o);
	}

	public VFTPFileWriter(VFTPClient host, IFile file) throws IOException {
		OutputStream o = new VFTPFileOutputStream(host, file);
		_osw = new OutputStreamWriter(o);
	}
	
	public VFTPFileWriter(VFTPClient host, IFile file, boolean append) throws IOException {
		OutputStream o = new VFTPFileOutputStream(host, file, append);
		_osw = new OutputStreamWriter(o);
	}
	
	@Override
	public void close() throws IOException {
		_osw.close();
	}

	@Override
	public void flush() throws IOException {
		_osw.flush();
	}

	@Override
	public void write(char[] cbuff, int off, int len) throws IOException {
		_osw.write(cbuff, off, len);
	}

	@Override
	public String getEncoding() {
		return _osw.getEncoding();
	}

}
