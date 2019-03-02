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

package com.vfile.s3io;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Object;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFilter;
import com.vfile.interfaces.IFilenameFilter;

/** Mapping IFile interface to jetset S3 API */
public class S3File implements IFile {
	public static final String SLASH = "/";
	
	public static final String DIRECTORY_METADATA_KEY = "vfile-s3-file-type";
	public static final String DIRECTORY_METADATA_VALUE_DIR = "directory";
	S3HostInfo _host;
	
	/** The full file name, including both the path and the name of the file itself. 
	 * The root path is stored as "/", even though this is not valid in S3 itself. */
	String _s3Path = null;
	
	String _userVisiblePath = null;
	
	
	/** Just the name of the file, excluding the path*/
	String _name = null;
	
	/** The path split into component parts, delimited by '/'; the root path is stored
	 * as a list with 0 elements (an empty list). */
	List<String> _pathComponents = new ArrayList<String>();

	S3File(S3HostInfo host, IFile parent, String child)  {
		this(host, parent.getPath(), child);
	}
	
	public S3File(S3HostInfo host, String pathname)  {
		construct(host, pathname);
	}
	
	S3File(S3HostInfo host, String parent, String child)  {
		child = utilStripLeadingSlash(child);
		
		String path = utilStripLeadingSlash(parent);
		
		if(!path.endsWith(SLASH)) {
			path += SLASH;
		}
		path += child;
	
		construct(host, path);
	}
	
	private void construct(S3HostInfo host, String path) {
		_host = host;
		_s3Path = utilStripLeadingSlash(utilStripTrailingSlash(path));
		
		_pathComponents = parsePath(_s3Path);

		utilHandleRelativePath(_pathComponents);
		
		// Reconstruct S3 Path without relative paths
		_s3Path  = "";
		for(String s : _pathComponents) {
			_s3Path += s + SLASH;
		}
		_s3Path = utilStripTrailingSlash(_s3Path);
		
		
		if(!isRootDir()) {
			_name = _pathComponents.get(_pathComponents.size()-1);
		} else {
			_name = "";
		}
		
		if(isRootDir()) {
			_s3Path = "";
			_userVisiblePath = "/";
		} else {
			_userVisiblePath = SLASH+_s3Path;
		}
		
	}
	
//	private boolean isParentRootDir() {
//		if(_pathComponents.size() == 1) {
//			return true;
//		} else {
//			return false;
//		}
//	}
	
	private boolean isRootDir() {
		if(_pathComponents.size() == 0) {
			return true;
		} else {
			return false;
		}
	}

	
//	private String getParentPath() {
//		String result = "";
//		
//		if(isParentRootDir()) {
//			return "/";
//		}
//		
//		// Strip out the last item on the list
//		for(int x = 0; x < (_pathComponents.size()-1); x++) {
//			result += SLASH+_pathComponents.get(x);
//		}
//		return result;	
//	}
	
	private static List<String> parsePath(String pathStr) {
		List<String> list = new ArrayList<String>();
		
		String[] arr = pathStr.split(SLASH);
		
		for(String s : arr) {
			if(s.trim().length() > 0) {
				list.add(s);
			}
		}
		
		return list;
	}
	
	
	protected S3Object getS3ObjectFull() {
		try {
			S3Object o = _host.getS3Service().getObject(_host.getS3Bucket(), _s3Path);
			
			return o;
		} catch (S3ServiceException e) {
			if(e.getResponseCode() == 404) {
				S3Log.debug("404 on call to getS3ObjectFull().");
				return null;
			} else {
				S3Log.err("S3ServiceException on getS3Object()" + e);
			}
			return null;
		}
		
	}
	
	protected S3Object getS3Object() {
		try {
			S3Object o = _host.getS3Service().getObjectDetails(_host.getS3Bucket(), _s3Path);
			
			return o;
		} catch (S3ServiceException e) {
			if(e.getResponseCode() == 404) {
				S3Log.debug("404 returned on call to getS3Object() - (note: this is usually due to an exists(...) or delete(...) call, where the s3object doesn't exist)");
			} else {
				S3Log.err("S3ServiceException on getS3Object()" + e);
			}
			return null;
		}
		
	}
	
	protected S3HostInfo getHostInfo() {
		return _host;
	}
	
	@Override
	public boolean canExecute() {
		throw new UnsupportedOperationException();
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
		// This requires atomic creation; S3 cannot guarantee.
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete() {
		if(!exists()) return false;
		
		try {
			_host.getS3Service().deleteObject(_host.getS3Bucket(), _s3Path);
			
			if(!exists()) return true;
		} catch (S3ServiceException e) {
			S3Log.err("S3ServiceException on delete()" + e);
		}

		return false;
	}

	@Override
	public void deleteOnExit() {
		// TODO: LOWER - ARCHITECTURE - How to support.		
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean exists() {
		if(getS3Object() == null) {
			return false;
		}
		return true;
	}

	@Override
	public IFile getAbsoluteFile() {
		// No relative path support on S3
		return this;
	}

	@Override
	public String getAbsolutePath() {
		return _userVisiblePath;
	}

	@Override
	public IFile getCanonicalFile() throws IOException {
		// No relative path support on S3
		return this;
	}

	@Override
	public String getCanonicalPath() throws IOException {
		return _userVisiblePath;
	}

	@Override
	public long getFreeSpace() {
		// Is it accurate? Find out for yourself :)
		return Long.MAX_VALUE;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public String getParent() {
		String result = SLASH;
		for(int x = 0; x < _pathComponents.size()-1; x++) {
			result += _pathComponents.get(x) + SLASH;
		}
		
		if(result.length() > 1 && result.endsWith(SLASH)) { 
			// remove the trailing slash
			result = result.substring(0, result.length()-1);
		}
		
		return result;
	}

	@Override
	public IFile getParentFile() {
		return new S3File(_host, getParent());
	}

	@Override
	public String getPath() {
		return _userVisiblePath;
	}
	
	protected String getS3Path() {
		return _s3Path;
	}
	

	@Override
	public long getTotalSpace() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getUsableSpace() {
		return Long.MAX_VALUE;
	}

	@Override
	public boolean isAbsolute() {
		return true;
	}

	@Override
	public boolean isDirectory() {
		String s = (String)getS3Object().getMetadata(DIRECTORY_METADATA_KEY);
		if(s == null || !s.equals(DIRECTORY_METADATA_VALUE_DIR)) {
			return false;			
		}
		return true;
	}

	@Override
	public boolean isFile() {
		return exists() && !isDirectory();

	}

	@Override
	public boolean isHidden() {
		// No hidden files
		return false;
	}
	
	@Override
	public long lastModified() {
		if(!exists()) return 0;
		return getS3Object().getLastModifiedDate().getTime();
	}

	@Override
	public long length() {
		if(!exists()) return 0;
		return getS3Object().getContentLength();
	}

	@Override
	public String[] list() {
		IFile[] fileList = listFiles();
		if(fileList == null) return null;
		
		String[] result = new String[fileList.length];
		
		for(int x = 0; x < fileList.length; x++) {
			result[x] = fileList[x].getName();
		}
		
		return result;
	}

	@Override
	public String[] list(IFilenameFilter filter) {
		IFile[] resultArr = this.listFiles(filter);
		if(resultArr == null) return null;
		
		String[] result = new String[resultArr.length];
		for(int x = 0; x < resultArr.length; x++) {
			result[x] = resultArr[x].getName();
		}
		
		return result;
	}

	
	@Override
	public IFile[] listFiles() {
		if(!isRootDir() && !isDirectory()) {
			S3Log.err("listFiles called on non-directory.");
			return null; 
		}
		if(!exists()) {
			S3Log.err("listFiles() called on non-existent directory."); 
			return null;
		}
		
		S3Service service = _host.getS3Service();
		try {
			
			S3Object[] os = null;
			if(isRootDir()) {
				os = service.listObjects(_host.getS3Bucket(), "", "/");
			} else {
				os = service.listObjects(_host.getS3Bucket(), 
							utilStripTrailingSlash(getS3Path())+SLASH, null);
			}
			
			if(os == null) {
				return null;
			}
			
			IFile[] result = new IFile[os.length];
			for(int x = 0; x < os.length; x++) {
				S3Object o = os[x];
				result[x] = new S3File(_host, o.getKey());
			}
			
			return result;
			
			
		} catch (S3ServiceException e) {
			S3Log.err("Exception thrown in listFiles() "+e);
			return null;
		}
	}

	@Override
	public IFile[] listFiles(IFileFilter filter) {
		IFile[] r1 = listFiles();
		if(r1 == null) return null;
		
		List<IFile> resultList = new ArrayList<IFile>();
		
		for(IFile f : r1) {
			if(filter.accept(f)) {
				resultList.add(f);
			}
		}
		
		IFile[] result = new IFile[resultList.size()];
		for(int x = 0; x < result.length; x++) {
			result[x] = resultList.get(x);
		}
		
		return result;
	}

	@Override
	public IFile[] listFiles(IFilenameFilter filter) {
		IFile[] r1 = listFiles();
		if(r1 == null) return null;
		
		List<IFile> resultList = new ArrayList<IFile>();
		
		for(IFile f : r1) {
			if(filter.accept(f.getParentFile(), f.getName())) { 
				resultList.add(f);
			}
		}
		
		IFile[] result = new IFile[resultList.size()];
		for(int x = 0; x < result.length; x++) {
			result[x] = resultList.get(x);
		}
		
		return result;
	}

	@Override
	public boolean mkdir() {
		if(exists()) return false;
		
		S3Object object = new S3Object(_s3Path);
		try {
			object.addMetadata(DIRECTORY_METADATA_KEY, DIRECTORY_METADATA_VALUE_DIR);
			object = _host.getS3Service().putObject(_host.getS3Bucket(), object);
			
		} catch (S3ServiceException e) {
			S3Log.err("Exception thrown in mkdir() "+e);
			return false;
		}
		
		return true;
	}

	@Override
	public boolean mkdirs() {
		String path = "";
		if(_pathComponents == null) return false;
		
		for(String str : _pathComponents) {
			path += SLASH+str;
			
			S3File file = new S3File(_host, path);
			if(file.exists() && !isDirectory()) {
				return false;
			}
			if(!file.mkdir()) {
				// Unable to make, so returning
				return false;
			}
			
		}
		return true;
	}

	@Override
	public boolean renameTo(IFile dest) {
		if(!exists()) return false;
		if(dest == null) throw new NullPointerException("dest parameter is null.");
		
		try {
			_host.getS3Service().renameObject(_host.getS3Bucket().getName(), _s3Path, ((S3File)dest).getS3Object());
			
			if(dest.exists()) return true;
			else return false;
			
		} catch (S3ServiceException e) {
			S3Log.err("Exception thrown in renameTo() "+e);
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
		if(time < 0) throw new IllegalArgumentException();
		if(!exists()) return false;
		
		getS3Object().setLastModifiedDate(new Date(time));
		
		return getS3Object().getLastModifiedDate().getTime() == time;
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

	private static String utilStripTrailingSlash(String str) {
		if(str.endsWith(SLASH)) {
			return str.substring(0, str.length()-1);
		}
		return str;
	}
	
	private static String utilStripLeadingSlash(String str) {
		if(str.startsWith(SLASH)) {
			return str.substring(1);
		}
		return str;
			
	}
	
	private static void utilHandleRelativePath(List<String> componentPath) {
		boolean contLoop = true;
		while(contLoop) {
			contLoop = false;
			
			for(int x = 0; x < componentPath.size(); x++) {
				String str = componentPath.get(x);
				
				if(str.equalsIgnoreCase(".")) {
					componentPath.remove(x);
					contLoop = true;
					continue;
				}
				
				if(str.trim().equalsIgnoreCase("..")) {
					if(x == 0) {
						S3Log.err("Too many '..' occurences in path");
						return;
					} else {
						componentPath.remove(x); // Remove the '..'
						componentPath.remove(x-1); // Remove the item before the ..
						contLoop = true;
						continue;

					}
				}
			}
		}
		
		
	}
}
