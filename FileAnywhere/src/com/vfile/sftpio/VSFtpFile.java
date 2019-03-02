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

package com.vfile.sftpio;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;

import com.vfile.interfaces.IFile;
import com.vfile.vfsio.VFSFile;

public class VSFtpFile extends VFSFile{

	public VSFtpFile(VSFtpHostInfo host, FileObject fo) {
		super(host.getVfsConfig(), fo);
	}

	public VSFtpFile(VSFtpHostInfo host, IFile parent, String child) {
		super(host.getVfsConfig(), parent, child);
	}

	public VSFtpFile(VSFtpHostInfo host, String parent, String child) {
		super(host.getVfsConfig(), parent, child);
	}

	public VSFtpFile(VSFtpHostInfo host, String pathname) {
		super(host.getVfsConfig(), pathname);
	}

	
	static FileObject connect(String host, String user, String password, String remotePath) throws FileSystemException {
		
		// we first set strict key checking off
		FileSystemOptions fsOptions = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
		
		// now we create a new filesystem manager
		DefaultFileSystemManager fsManager = (DefaultFileSystemManager) VFS.getManager();

		// the url is of form sftp://user:pass@host/remotepath/
		String uri = "sftp://" + user + ":" + password + "@" + host + "/" + remotePath;
		
		// get file object representing the local file
		FileObject fo = fsManager.resolveFile(uri, fsOptions);

		return fo;
		
	}

}
