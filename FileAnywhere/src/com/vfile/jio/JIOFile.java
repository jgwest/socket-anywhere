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

package com.vfile.jio;

import java.io.IOException;
import java.net.URI;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFilter;
import com.vfile.interfaces.IFilenameFilter;

/** Implements a vfile using the standard Java IO File interface. */
public class JIOFile implements IFile {
	
	java.io.File _inner = null;
	
	private static IFile[] convertList(java.io.File[] list) {
		IFile[] result = new IFile[list.length];
		
		for(int x = 0; x < result.length; x++) {
			result[x] = convert(list[x]);
		}
		return result;
	}
	
//	private static java.io.File[] convertList(IFile[] list) {
//		java.io.File[] result = new java.io.File[list.length];
//		
//		for(int x = 0; x < result.length; x++) {
//			result[x] = convert(list[x]);
//		}
//		return result;
//	}
	
	protected static java.io.File convert(IFile f) {
		if(f == null) {
			return null;
		} else {
			return new java.io.File(f.getPath());
		}
	}
	
	protected static IFile convert(java.io.File f) {
		if(f == null) {
			return null;
		}
		else {
			return new JIOFile(f);
		}
	}
	
	
	protected JIOFile(java.io.File inner) {
		_inner = inner;
	}
	
	JIOFile(IFile parent, String child)  {		
		_inner = new java.io.File(convert(parent), child);
	}
	
	JIOFile(String pathname)  {
		_inner = new java.io.File(pathname);
		
	}
	
	JIOFile(String parent, String child)  {
		_inner = new java.io.File(parent, child);
	}
	
	JIOFile(URI uri) {
		_inner = new java.io.File(uri);
	}

	@Override
	public boolean canExecute() {
		return _inner.canExecute();
	}

	@Override
	public boolean canRead() {
		return _inner.canRead();
	}

	@Override
	public boolean canWrite() {
		return _inner.canWrite();
	}

	@Override
	public int compareTo(IFile pathname) {
		return _inner.compareTo(convert(pathname));
	}

	@Override
	public boolean createNewFile() throws IOException {
		return _inner.createNewFile();
	}

	@Override
	public boolean delete() {
		return _inner.delete();
	}

	@Override
	public void deleteOnExit() {
		_inner.deleteOnExit();
	}

	@Override
	public boolean exists() {
		return _inner.exists();
	}

	@Override
	public IFile getAbsoluteFile() {
		return convert(_inner.getAbsoluteFile());
	}

	@Override
	public String getAbsolutePath() {
		return _inner.getAbsolutePath();
	}

	@Override
	public IFile getCanonicalFile() throws IOException {
		return convert(_inner.getCanonicalFile());
	}

	@Override
	public String getCanonicalPath() throws IOException {
		return _inner.getCanonicalPath();
	}

	@Override
	public long getFreeSpace() {
		return _inner.getFreeSpace();
	}

	@Override
	public String getName() {
		return _inner.getName();
	}

	@Override
	public String getParent() {
		return _inner.getParent();
	}

	@Override
	public IFile getParentFile() {
		return convert(_inner.getParentFile());
	}

	@Override
	public String getPath() {
		return _inner.getPath();
	}

	@Override
	public long getTotalSpace() {
		return _inner.getTotalSpace();
	}

	@Override
	public long getUsableSpace() {
		return _inner.getUsableSpace();
	}

	@Override
	public boolean isAbsolute() {
		return _inner.isAbsolute();
	}

	@Override
	public boolean isDirectory() {
		return _inner.isDirectory();
	}

	@Override
	public boolean isFile() {
		return _inner.isFile();
	}

	@Override
	public boolean isHidden() {
		return _inner.isHidden();
	}

	@Override
	public long lastModified() {
		return _inner.lastModified();
	}

	@Override
	public long length() {
		return _inner.length();
	}

	@Override
	public String[] list() {
		return _inner.list();
	}

	@Override
	public String[] list(IFilenameFilter filter) {
		return null;
	}

	@Override
	public IFile[] listFiles() {
		return convertList(_inner.listFiles());
	}

	@Override
	public IFile[] listFiles(IFileFilter filter) {
		return convertList(_inner.listFiles(new JIOFileFilter(filter)));
	}

	@Override
	public IFile[] listFiles(IFilenameFilter filter) {
		return convertList(_inner.listFiles(new JIOFilenameFilter(filter)));
	}

	@Override
	public boolean mkdir() {
		return _inner.mkdir();
	}

	@Override
	public boolean mkdirs() {
		return _inner.mkdirs();
	}

	@Override
	public boolean renameTo(IFile dest) {
		return _inner.renameTo(convert(dest));
	}

	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		return _inner.setExecutable(executable, ownerOnly);
	}

	@Override
	public boolean setExecutable(boolean executable) {
		return _inner.setExecutable(executable);
	}

	@Override
	public boolean setLastModified(long time) {
		return _inner.setLastModified(time);
	}

	@Override
	public boolean setReadOnly() {
		return _inner.setReadOnly();
	}

	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		return _inner.setReadable(readable, ownerOnly);
	}

	@Override
	public boolean setReadable(boolean readable) {
		return _inner.setReadable(readable);
	}

	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		return _inner.setWritable(writable, ownerOnly);
	}

	@Override
	public boolean setWritable(boolean writable) {
		return _inner.setWritable(writable);
	}

	@Override
	public URI toURI() {
		return _inner.toURI();
	}
	

	@Override
	public boolean equals(Object obj) {
		return _inner.equals(obj);
	}

	@Override
	public int hashCode() {
		return _inner.hashCode();
	}

	@Override
	public String toString() {
		return _inner.toString();
	}

}
