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

package com.vfile;

import java.io.IOException;
import java.net.URI;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFilter;
import com.vfile.interfaces.IFilenameFilter;

/** Wraps an inner file that implements the IFile interface, and passes method
 * calls to its inner object. */
public class VFile implements IFile {
	private static boolean DEBUG = false;
	
	IFile _inner = null;
	
	public VFile(VFile parent, String child)  {
		_inner = DefaultFileFactory.getDefaultInstance().createFile(parent.getInnerFile(), child);
	}
	
	public VFile(String pathname)  {
		_inner = DefaultFileFactory.getDefaultInstance().createFile(pathname);
	}
	
	public VFile(String parent, String child)  {
		_inner = DefaultFileFactory.getDefaultInstance().createFile(parent, child);
	}
	
	public VFile(URI uri) {
		_inner = DefaultFileFactory.getDefaultInstance().createFile(uri);
	}
	
	public IFile getInnerFile() {
		return _inner;
	}
	
	private VFile(IFile inner) {
		_inner = inner;
		
	}
	
	private VFile[] convertList(IFile[] list) {
		VFile[] result = new VFile[list.length];
		for(int x = 0; x < result.length; x++) {
			result[x] = convert(list[x]);
		}
		return result;
	}
	
	private VFile convert(IFile file) {
		return new VFile(file);
	}
	
	public boolean canExecute() {
		return _inner.canExecute();
	}

	public boolean canRead() {
		return _inner.canRead();
	}

	public boolean canWrite() {
		return _inner.canWrite();
	}

	public int compareTo(VFile pathname) {
		return _inner.compareTo(pathname.getInnerFile());
	}

	public boolean createNewFile() throws IOException {
		return _inner.createNewFile();
	}

	public boolean delete() {
		if(DEBUG) System.out.println("deleting "+_inner.getPath() +" | "+Thread.currentThread().getId() + " / " + Thread.currentThread().getName());
		return _inner.delete();
	}


	public void deleteOnExit() {
		_inner.deleteOnExit();
	}


	public boolean exists() {
		return _inner.exists();
	}


	public VFile getAbsoluteFile() {
		return convert(_inner.getAbsoluteFile());
	}


	public String getAbsolutePath() {
		return _inner.getAbsolutePath();
	}


	public VFile getCanonicalFile() throws IOException {
		return convert(_inner.getCanonicalFile());
	}


	public String getCanonicalPath() throws IOException {
		return _inner.getCanonicalPath();
	}


	public long getFreeSpace() {
		return _inner.getFreeSpace();
	}


	public String getName() {
		return _inner.getName();
	}


	public String getParent() {
		return _inner.getParent();
	}


	public VFile getParentFile() {
		return convert(_inner.getParentFile());
	}


	public String getPath() {
		return _inner.getPath();
	}


	public long getTotalSpace() {
		return _inner.getTotalSpace();
	}


	public long getUsableSpace() {
		return _inner.getUsableSpace();
	}


	public boolean isAbsolute() {
		return _inner.isAbsolute();
	}


	public boolean isDirectory() {
		return _inner.isDirectory();
	}


	public boolean isFile() {
		return _inner.isFile();
	}


	public boolean isHidden() {
		return _inner.isHidden();
	}


	public long lastModified() {
		return _inner.lastModified();
	}


	public long length() {
		return _inner.length();
	}


	public String[] list() {
		return _inner.list();
	}


	public String[] list(IFilenameFilter filter) {
		return _inner.list(filter);
	}


	public VFile[] listFiles() {
		IFile[] list = _inner.listFiles();
		return convertList(list);
	}


	public VFile[] listFiles(IFileFilter filter) {
		return convertList(_inner.listFiles(filter));
	}


	public VFile[] listFiles(IFilenameFilter filter) {
		return convertList(_inner.listFiles(filter));
	}


	public boolean mkdir() {
		return _inner.mkdir();
	}


	public boolean mkdirs() {
		return _inner.mkdirs();
	}

	public boolean renameTo(IFile dest) {
		if(DEBUG) {
			System.out.println("["+Thread.currentThread().getId()+"] Renaming "+this.getPath()+" to "+dest.getPath());
		}
		return _inner.renameTo(((VFile)dest).getInnerFile());
	}


	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		return _inner.setExecutable(executable, ownerOnly);
	}


	public boolean setExecutable(boolean executable) {
		return _inner.setExecutable(executable);
	}


	public boolean setLastModified(long time) {
		return _inner.setLastModified(time);
	}


	public boolean setReadOnly() {
		return _inner.setReadOnly();
	}


	public boolean setReadable(boolean readable, boolean ownerOnly) {
		return _inner.setReadable(readable, ownerOnly);
	}


	public boolean setReadable(boolean readable) {
		return _inner.setReadable(readable);
	}

	public boolean setWritable(boolean writable, boolean ownerOnly) {
		return _inner.setWritable(writable, ownerOnly);
	}

	public boolean setWritable(boolean writable) {
		return _inner.setWritable(writable);
	}

	public URI toURI() {
		return _inner.toURI();
	}

	@Override
	public int compareTo(IFile pathname) {
		return _inner.compareTo(pathname);
	}

	@Override
	public boolean equals(Object arg0) {
		return _inner.equals(arg0);
	}

	@Override
	public int hashCode() {
		return _inner.hashCode();
	}

	@Override
	public String toString() {
		// TODO: ARCHITECTURE - This actually seems that it should return the path
		return _inner.toString();
	}

}
