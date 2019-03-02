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
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileInputStream;

/** Input stream for a file that is being downloaded through vftp. */
public class VFTPFileInputStream extends InputStream implements IFileInputStream {
	
	VFTPFile _file = null;
	VFTPClient _host = null;
	InputStream _fileInputStream = null;
	
	// TODO: LOWER - By default this will NOT reuse the existing connection for the file transfer

	public VFTPFileInputStream(VFTPClient host, IFile file) throws FileNotFoundException, IOException {
		_host = host.cloneConnectionForStream();
		_host.setFileType(FTP.BINARY_FILE_TYPE);
		_file = new VFTPFile(_host, file.getPath());
		openFile();
	}

	public VFTPFileInputStream(VFTPClient host, String name) throws FileNotFoundException, IOException {
		_host = host.cloneConnectionForStream();
		_file = new VFTPFile(_host, name);
		openFile();
	}
	
	private void openFile() throws FileNotFoundException, IOException {
		if(!_file.exists()) {
			try {
				_host.logout();
				_host.disconnect();
			} catch(Exception e) {}
			throw new FileNotFoundException();
		}
		
		_fileInputStream = _host.retrieveFileStream(_file.getPath());
		
	}
	
	@Override
	public int available() throws IOException {
		return _fileInputStream.available();
	}

	@Override
	public void close() throws IOException {
		_fileInputStream.close();
		_host.completePendingCommand();
		_host.logout();
		_host.disconnect();
	}

	@Override
	public void mark(int readlimit) {
		_fileInputStream.mark(readlimit);
		
	}

	@Override
	public boolean markSupported() {
		return _fileInputStream.markSupported();
	}

	@Override
	public int read() throws IOException {
		int result = _fileInputStream.read();
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int result = _fileInputStream.read(b, off, len);
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int result = _fileInputStream.read(b);
		return result;
	}

	@Override
	public void reset() throws IOException {
		_fileInputStream.reset();
	}

	@Override
	public long skip(long arg0) throws IOException {
		return _fileInputStream.skip(arg0);
	}

}
