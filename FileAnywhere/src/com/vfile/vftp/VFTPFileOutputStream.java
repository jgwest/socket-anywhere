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
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTP;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileOutputStream;

public class VFTPFileOutputStream extends OutputStream implements IFileOutputStream {
	VFTPClient _host = null;
	OutputStream _os = null;
	String _path = null;

	public VFTPFileOutputStream(VFTPClient host, IFile file) throws FileNotFoundException, IOException {
		_host = host.cloneConnectionForStream();
		_host.setFileType(FTP.BINARY_FILE_TYPE);
		_path = file.getPath();
		openFile(false);
	}

	public VFTPFileOutputStream(VFTPClient host, IFile file, boolean append) throws FileNotFoundException, IOException {
		_host = host.cloneConnectionForStream();
		_host.setFileType(FTP.BINARY_FILE_TYPE);
		_path = file.getPath();
		openFile(append);
	}

	public VFTPFileOutputStream(VFTPClient host, String name) throws FileNotFoundException, IOException {
		_host = host.cloneConnectionForStream();
		_host.setFileType(FTP.BINARY_FILE_TYPE);
		_path = name;
		openFile(false);
	}

	public VFTPFileOutputStream(VFTPClient host, String name, boolean append) throws FileNotFoundException, IOException {
		_host = host.cloneConnectionForStream();
		_host.setFileType(FTP.BINARY_FILE_TYPE);
		_path = name;
		openFile(append);
	}
	
	private void openFile(boolean append) throws FileNotFoundException, IOException {		
		if(append) {
			_os = _host.appendFileStream(_path);
		} else {
			_os = _host.storeFileStream(_path);
		}		
	}
	
	@Override
	public void close() throws IOException {
		_os.close();
		_host.completePendingCommand();
		_host.logout();
		_host.disconnect();
	}

	@Override
	public void flush() throws IOException {
		_os.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		_os.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		_os.write(b);
	}

	@Override
	public void write(int arg0) throws IOException {
		_os.write(arg0);
	}

}
