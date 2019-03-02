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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.impl.DefaultFileSystemManager;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;

import com.vfile.DefaultFileFactory;
import com.vfile.VFile;
import com.vfile.VFileWriter;
import com.vfile.interfaces.IFile;

/** Simple standalone test of our VFS code. */
public class VFSTest {

	public static void main(String[] args) {
		
		try {
			
			FileObject o = connect("hostname", "user", "password", "/");
			VFSFileConfig c = new VFSFileConfig();
			c.setRoot(o);

			DefaultFileFactory.setDefaultFactory(new VFSFileFactory(c));
						
			VFileWriter fw = new VFileWriter(new VFile("/tmp/test"));
			fw.write("test!\n");
			fw.close();
			
		} catch (FileSystemException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
//			try { Thread.sleep(5000); } catch (InterruptedException e) { }
//			System.exit(1);
		}

	}
	
	public static void main2() { 
		try {
			
			FileObject o = connect("hostname", "user", "password", "/");
			VFSFileConfig c = new VFSFileConfig();
			c.setRoot(o);

			VFSFile tmp = new VFSFile(c, "/tmp");
			
			VFSFile f2 = new VFSFile(c, "/tmp/m2");
			
			try {
				boolean r = f2.createNewFile();
				System.out.println(r);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			for(IFile f : tmp.listFiles()) {
				System.out.println(f.getPath());
			}
			
			System.exit(1);
			
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
		
	}
	
	public static FileObject connect(String host, String user, String password, String remotePath) throws FileSystemException {
		
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
