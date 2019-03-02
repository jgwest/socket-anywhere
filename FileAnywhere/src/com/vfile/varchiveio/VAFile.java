/* Copyright 2013 Jonathan West

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

package com.vfile.varchiveio;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.java.truevfs.access.TFile;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFilter;
import com.vfile.interfaces.IFilenameFilter;

public class VAFile implements IFile {

	TFile _rootArchive;
	
	TFile _inner;
	
	public VAFile(File pathToJar) {
		_inner = new TFile(pathToJar);
		_rootArchive = _inner;
	}
	
	private VAFile(TFile rootArchive, TFile file) {
		_rootArchive = rootArchive;
		_inner = file;
	}
	
	public VAFile(File pathToJar, String innerPath) {
		String fullPath = pathToJar.getPath();
		
		if(!fullPath.endsWith(File.separator) && !innerPath.startsWith(File.separator)) {
			fullPath += File.separator;
		}
		
		fullPath += innerPath;
		
		_rootArchive = new TFile(pathToJar);
		_inner = new TFile(fullPath);
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
		throw new UnsupportedOperationException();
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
		return new VAFile(_rootArchive, _inner.getAbsoluteFile());
	}

	@Override
	public String getAbsolutePath() {
		return _inner.getAbsolutePath();
	}

	@Override
	public IFile getCanonicalFile() throws IOException {
		return new VAFile(_rootArchive, _inner.getCanonicalFile());
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
		return new VAFile(_rootArchive, _inner.getParentFile());
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
		IFile[] fr = listFiles(filter);
		String[] result = new String[fr.length];
		
		for(int x = 0; x < fr.length; x++) {
			result[x] = fr[x].getName();
		}
		
		return result;
	}

	@Override
	public IFile[] listFiles() {
		TFile[] filelist = _inner.listFiles();
		
		IFile[] result = new IFile[filelist.length];
		
		for(int x = 0; x < result.length; x++) {
			result[x] = new VAFile(_rootArchive, filelist[x]);
		}
		
		return result;
	}

	@Override
	public IFile[] listFiles(IFileFilter filter) {
		IFile[] fileList = listFiles();
		List<IFile> result = new ArrayList<IFile>();
		
		for(IFile f : fileList) {
			if(filter.accept(f)) {
				result.add(f);
			}
		}
		
		return result.toArray(new IFile[result.size()]);
		
	}

	@Override
	public IFile[] listFiles(IFilenameFilter filter) {
		IFile[] fileList = listFiles();
		List<IFile> result = new ArrayList<IFile>();
		
		for(IFile f : fileList) {
			if(filter.accept(f.getParentFile(), f.getName())) {
				result.add(f);
			}
		}
		
		return result.toArray(new IFile[result.size()]);
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
		if(dest instanceof VAFile) {
			return _inner.renameTo(((VAFile) dest)._inner);
		} else {
			throw new UnsupportedOperationException();
		}
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
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		return _inner.setReadable(readable, ownerOnly);
	}

	@Override
	public boolean setReadable(boolean readable) {
		return _inner.setReadable(readable);
	}

	@Override
	public boolean setReadOnly() {
		return _inner.setReadOnly();
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
}
