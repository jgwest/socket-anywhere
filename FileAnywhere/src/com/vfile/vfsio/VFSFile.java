/*
	Copyright 2012, 2019 Jonathan West

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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.Selectors;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFilter;
import com.vfile.interfaces.IFilenameFilter;

/** Maps IFile functionality onto the VFS API */
public class VFSFile implements IFile {
	
	private final boolean DEBUG;
	
	FileObject _fo;
	VFSFileConfig _config;
	
	public VFSFile(VFSFileConfig config, FileObject fo) {
		_fo = fo;
		_config = config;
		DEBUG = _config.DEBUG;		
		
	}
	
	public VFSFile(VFSFileConfig config, String pathname) {
		_config = config;
		DEBUG = _config.DEBUG;
		
		try {
			_fo = _config.getRoot().resolveFile(pathname);
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			_fo = null;
		}
	}
	
	public VFSFile(VFSFileConfig config, String parent, String child ) {
		this(config, parent + separator + child);
	}
	
	public VFSFile(VFSFileConfig config, IFile parent, String child ) {
		this(config, parent.getPath() + separator + child);
	}
	
	@Override
	public boolean canExecute() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canRead() {
		try {
			return _fo.isReadable();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean canWrite() {
		try {
			return _fo.isWriteable();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			return false;
		}
	}

	@Override
	public int compareTo(IFile pathname) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean createNewFile() throws IOException {
		if(_fo.exists()) return false;
		
		_fo.createFile();
		
		return _fo.exists();

	}

	@Override
	public boolean delete() {
		try {
			return _fo.delete();
		} catch (FileSystemException e) {

			if(DEBUG) {
				try {
					// Only print the stack trace if the delete actually failed to delete (rather than simply throwing an exception for not existing)
					if(_fo.exists()) {
						System.out.println("------------------------{{");
						System.out.println(_fo.exists());
						e.printStackTrace();

						System.out.println("cause:"+e.getCause().getMessage());
						System.out.println("}}------------------------");
					}
				} catch (FileSystemException e1) { }
			}
			return false;
		}
		
	}

	@Override
	public void deleteOnExit() {
		// TODO: ARCHITECTURE - How to support deleteOnExit().
	}

	@Override
	public boolean exists() {
		try {
			return _fo.exists();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			return false;
		}
	}

	@Override
	public IFile getAbsoluteFile() {
		return this;
	}

	@Override
	public String getAbsolutePath() {
		// TODO: ARCHITECTURE - Implement
		return getPath();
	}

	@Override
	public IFile getCanonicalFile() throws IOException {
		return this;
	}

	@Override
	public String getCanonicalPath() throws IOException {
		// TODO: ARCHITECTURE - Implement
		return getPath();
	}

	@Override
	public long getFreeSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return _fo.getName().getBaseName();
	}

	@Override
	public String getParent() {
		try {
			return _fo.getParent().getName().getPath();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFile getParentFile() {
		try {
			return new VFSFile(_config, _fo.getParent());
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getPath() {
		return _fo.getName().getPath();
	}

	@Override
	public long getTotalSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getUsableSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		try {
			return _fo.getType() == FileType.FOLDER;
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isFile() {
		try {
			return _fo.getType() == FileType.FILE;
		} catch (FileSystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isHidden() {
		try {
			return _fo.isHidden();
		} catch (FileSystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long lastModified() {
		try {
			return _fo.getContent().getLastModifiedTime();
		} catch (FileSystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long length() {
		try {
			return _fo.getContent().getSize();
		} catch (FileSystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] list() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String[] list(IFilenameFilter filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFile[] listFiles() {
		try {
			FileObject[] farr = _fo.findFiles(Selectors.SELECT_CHILDREN);
			VFSFile[] result = null;
			
			if(farr != null) {
				result = new VFSFile[farr.length];
				int x = 0;
				for(FileObject fot : farr) {
					result[x] = new VFSFile(_config, fot);
					x++;
				}
			}
			
			return result;
			
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();			
			return null;
		}
	}

	@Override
	public IFile[] listFiles(IFileFilter filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFile[] listFiles(IFilenameFilter filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean mkdir() {
		try {
			// TODO: ARCHITECTURE - Unlike Java File IO mkdir, this WILL actually create child dirs 
			if(exists()) return false;
			_fo.createFolder();
			
			return exists();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();			
			return false;
		}
	}

	@Override
	public boolean mkdirs() {
		try {
			if(exists()) return false;
			_fo.createFolder();
			
			return exists();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean renameTo(IFile dest) {
		
		// TODO: ARCHITECTURE - This might permit accidental moves that aren't actually just "renames"
		FileObject foDest = ((VFSFile)dest)._fo;
		
		if(!_fo.canRenameTo(foDest)) return false;
		
		try {
//			System.out.println("------------------------");
//			System.out.println("renameTo>["+_fo+ "] ["+foDest+"] ||| ("+_fo.getName().getPath()+") ("+foDest.getName().getPath()+")");
			_fo.moveTo(foDest);

//			System.out.println("renameTo>"+foDest.exists());
//			System.out.println("renameTo> - threadid: "+Thread.currentThread().getId() + " / " + Thread.currentThread().getName());
			
			
		} catch (FileSystemException e) {
			
			try {
				// If the file no longer exists, then its likely this is what it was
				// complaining about, so just return false;
				if(!_fo.exists()) {
					return false;
				}
			} catch (FileSystemException e1) { }
			
			// Otherwise log it
			if(DEBUG) e.printStackTrace();			
		}
		
		try {
			return foDest.exists();
		} catch (FileSystemException e) { return false; }

		
				
	}

	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setExecutable(boolean executable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setLastModified(long time) {
		try {
			_fo.getContent().setLastModifiedTime(time);
			return true;
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();			
			return false;
		}
	}

	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadable(boolean readable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadOnly() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setWritable(boolean writable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI toURI() {
		try {
			return _fo.getURL().toURI();
		} catch (FileSystemException e) {
			if(DEBUG) e.printStackTrace();			
			return null;
		} catch (URISyntaxException e) {
			if(DEBUG) e.printStackTrace();			
			return null;
		}
	}

}
