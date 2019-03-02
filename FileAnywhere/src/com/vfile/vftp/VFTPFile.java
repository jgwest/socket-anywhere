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
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFilter;
import com.vfile.interfaces.IFilenameFilter;

/** Central class to access file data for vftp file; maps IFile API to corresponding apache ftp api. */
public class VFTPFile implements IFile {
	
	/** Note that path includes the filename at the end, just like with normal Java getPath() result */
	String _path = null;
	String _name = null;
	List<String> _pathComponents = new ArrayList<String>();
	VFTPClient _host = null;
	FileCache _fileCache = null;
	
	public static final String SLASH = "/";	
	
	VFTPFile(VFTPClient host, IFile parent, String child) {
		this(host, parent.getName(), child);
	}
	
	VFTPFile(VFTPClient host, String parent, String child) { 
		String path = parent;
		if(!path.endsWith(SLASH)) {
			path += SLASH;
		}
		path += child;
		
		construct(host, path);
	}
	
	public VFTPFile(VFTPClient host, String path) {
		construct(host, path);
	}
	
	private void construct(VFTPClient host, String path) {
		_host = host;
		_path = path;
		_pathComponents = parsePath(path);
		
		if(_pathComponents.size() == 0) {
			_name = "";
		} else {
			_name = _pathComponents.get(_pathComponents.size()-1);
		}
		
		_fileCache = FileCache.getFileCache(_host);
	}
	
	
	private VFTPFile(VFTPClient host, List<String> parentPathComponents, String parentPath, String name, FTPFile attributes) {
		_host = host;
		
		List<String> l = new ArrayList<String>();
	
		l.addAll(parentPathComponents);
		l.add(name);
		
		_pathComponents = l;

		if(!parentPath.endsWith(SLASH)) {
			_path = parentPath + SLASH + name;
		} else {
			_path = parentPath + name;
		}
				
		_name = name;
		
		_fileCache = FileCache.getFileCache(_host);
		
		_fileCache.putAttribute(this, FileCache.FIELD_FTPFILE, attributes);

	}
	
	private boolean isParentRootDir() {
		if(_pathComponents.size() == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isRootDir() {
		if(_pathComponents.size() == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	private FTPFile getFTPFile() {

		if(isRootDir()) {
			return null;
		}
		
		// TODO: LOWER - Log cache hits/misses
		
		FTPFile cachedFile = (FTPFile)_fileCache.getAttribute(this, FileCache.FIELD_FTPFILE);
		
		if(cachedFile != null) {
			return cachedFile;
		}

		try {
			synchronized(_host) {
				if(!_host.printWorkingDirectory().equalsIgnoreCase(getParentPath())) {

					boolean result = _host.changeWorkingDirectory(getParentPath());
					if(!result) {
						throw new RuntimeException("Unable to change directory.");
					}
				}
				FTPFile[] files = _host.listFiles();
				
				for(FTPFile f : files) {
					if(f.getName().equals(_name)) {
						return f;
					}
				}
			}
			return null;
		} catch(IOException ioe) {
			return null;
		}
	}
	
	private String getParentPath() {
		String result = "";
		
		if(isParentRootDir()) {
			return "/";
		}
		
		// Strip out the last item on the list
		for(int x = 0; x < (_pathComponents.size()-1); x++) {
			result += SLASH+_pathComponents.get(x);
		}
		return result;	
	}
	
	private static List<String> parsePath(String pathStr) {
		List<String> list = new ArrayList<String>();
		
		String[] arr = pathStr.split("/");
		
		for(String s : arr) {
			if(s.trim().length() > 0) {
				list.add(s);
			}
		}
		
		return list;
	}
	
	
	@Override
	public boolean canExecute() {
		throw new UnsupportedOperationException();
//		// TODO: LOWER - UNSUPPORTED - What is below may be correct, however.
//		synchronized(_host) {
//			FTPFile file = getFTPFile();
//			if(file == null) {
//				return false;
//			}
//			
//			boolean result = file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION);
//			if(!result) {
//				result = file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION);
//			}
//
//			if(result) {
//				return true;
//			}
//		}
//		
//		return false;
	}

	@Override
	public boolean canRead() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canWrite() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(IFile pathname) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean createNewFile() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete() {
		if(isRootDir()) {
			return false;
		}
		
		try {
			synchronized(_host) {
				FTPFile file = getFTPFile();
				if(file == null) {
					return false;
				}
				
				if(file.isDirectory()) {
					return _host.removeDirectory(_path);
				} else {
					return _host.deleteFile(_path);
				}				
			}
		} catch(IOException ioe) { }
		
		return false;
		
	}

	@Override
	public void deleteOnExit() {
		throw new UnsupportedOperationException("Unsupported.");
	}

	@Override
	public boolean exists() {
		if(isRootDir()) {
			return true;
		}
		
		synchronized(_host) {
			FTPFile file = getFTPFile();
			if(file == null) {
				return false;
			} else {
				return true;
			}
		}
	}

	@Override
	public IFile getAbsoluteFile() {
		return this;
	}

	@Override
	public String getAbsolutePath() {
		return _path;
	}

	@Override
	public IFile getCanonicalFile() throws IOException {
		return this;
	}

	@Override
	public String getCanonicalPath() throws IOException {
		return _path;
	}

	@Override
	public long getFreeSpace() {
		throw new UnsupportedOperationException("Unsupported operation");
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public String getParent() {
		return getParentPath();
	}

	@Override
	public IFile getParentFile() {
		if(isRootDir()) {
			return null;
		}
		return new VFTPFile(_host, getParentPath());
	}

	@Override
	public String getPath() {
		return _path;
	}

	@Override
	public long getTotalSpace() {
		throw new UnsupportedOperationException("Unsupported operation");
	}

	@Override
	public long getUsableSpace() {
		throw new UnsupportedOperationException("Unsupported operation");
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		if(isRootDir()) {
			return true;
		}
		
		FTPFile file = getFTPFile();
		if(file != null && file.isDirectory()) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean isFile() {
		if(isRootDir()) {
			return false;
		}
		
		FTPFile file = getFTPFile();
		if(file != null && file.isFile()) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean isHidden() {
		
		if(isRootDir()) {
			return false;
		}

		FTPFile file = getFTPFile();
		if(file != null && file.isDirectory()) {
			return true;
		}
		
		return false;
	}

	@Override
	public long lastModified() {
		if(isRootDir()) {
			return 0L;
		}
		
		FTPFile file = getFTPFile();
		if(file == null) return 0L;
		
		return file.getTimestamp().getTimeInMillis();
	}

	@Override
	public long length() {
		FTPFile file = getFTPFile();
		if(file == null) return 0L;
		
		return file.getSize();
	}

	@Override
	public String[] list() {
		return list((IFilenameFilter)null);
	}

	@Override
	public String[] list(IFilenameFilter filter) {

		IFile[] files = listFiles();
		ArrayList<String> result = new ArrayList<String>();
		
		for(int x = 0; x < files.length; x++) {
			if(filter != null) {
				if(!filter.accept(files[x].getParentFile(), files[x].getName())) {
					continue;
				}
			}
			result.add(files[x].getName());
		}
		
		return result.toArray(new String[result.size()]);
	}

	@Override
	public IFile[] listFiles() {
		List<IFile> result = new ArrayList<IFile>();
		
		synchronized(_host) {
			
			try {
				
				if(!exists()) {
					return null;
				}
				
				FTPFile[] files = _host.listFiles(_path);
				if(files == null) {
					return null;
				}
				
				for(FTPFile f : files) {
					VFTPFile fr = new VFTPFile(_host, _pathComponents, _path, f.getName(), f);
					result.add(fr);
				}
				
				return result.toArray(new IFile[result.size()]);
				
			} catch (IOException e) {
				return null;
			}
		}
	}

	@Override
	public IFile[] listFiles(IFileFilter filter) {
		IFile[] files = listFiles();
		ArrayList<IFile> result = new ArrayList<IFile>();
		
		for(int x = 0; x < files.length; x++) {
			if(filter != null) {
				if(!filter.accept(files[x])) {
					continue;
				}
			}
			result.add(files[x]);
		}
		
		return result.toArray(new IFile[result.size()]);
	}

	@Override
	public IFile[] listFiles(IFilenameFilter filter) {
		IFile[] files = listFiles();
		ArrayList<IFile> result = new ArrayList<IFile>();
		
		for(int x = 0; x < files.length; x++) {
			if(filter != null) {
				if(!filter.accept(files[x].getParentFile(), files[x].getName())) {
					continue;
				}
			}
			result.add(files[x]);
		}
		
		return result.toArray(new IFile[result.size()]);
	}

	@Override
	public boolean mkdir() {
		synchronized(_host) {
			try {
				boolean result = _host.makeDirectory(_path);
				return result;
			} catch(IOException ioe) {
				return false;
			}
		}
	}

	@Override
	public boolean mkdirs() {
		try {
			synchronized(_host) {
				String path = "";
				for(String str : _pathComponents) {
					path += SLASH + str;
					
					VFTPFile tmpFile = new VFTPFile(_host, path);
					
					if(!tmpFile.exists()) {
						boolean result = _host.makeDirectory(path);
						if(!result) {
							return false;
						}
					}
				}
				return true;
				
			}
		} catch(IOException ioe) {
			return false;
		}
	}

	@Override
	public boolean renameTo(IFile dest) {
		
		try {
			synchronized(_host) {
				return _host.rename(_path, dest.getPath());
			}
		} catch(IOException ioe) {
			return false;
		}
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
		if(isRootDir()) {
			return false;
		}
		
		FTPFile f = getFTPFile();
		
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(time);
		
		f.setTimestamp(c);
		
		return true;
		
	}

	@Override
	public boolean setReadOnly() {
		throw new UnsupportedOperationException();
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
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setWritable(boolean writable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public URI toURI() {
		throw new UnsupportedOperationException();
	}

}
