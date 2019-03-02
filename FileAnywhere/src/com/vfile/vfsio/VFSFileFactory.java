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
import java.net.URI;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFactory;
import com.vfile.interfaces.IFileInputStream;
import com.vfile.interfaces.IFileOutputStream;
import com.vfile.interfaces.IFileReader;
import com.vfile.interfaces.IFileWriter;

/** Implements the generic IFileFactory interface. */
public class VFSFileFactory implements IFileFactory {
	private final boolean DEBUG;

	VFSFileConfig _config;
	
	private static IFile fixFileReference(IFile file) {
		return file;
	}
	
	public VFSFileFactory(VFSFileConfig config) {
		_config = config;
		DEBUG = _config.DEBUG;
		
		
	}
	
	@Override
	public IFile createFile(IFile parent, String child) {
		parent = fixFileReference(parent);
		
		VFSFile p = (VFSFile)parent;
		return new VFSFile(_config, p, child);
		
	}

	@Override
	public IFile createFile(String pathname) {
		return new VFSFile(_config, pathname);
	}

	@Override
	public IFile createFile(String parent, String child) {
		return new VFSFile(_config, parent, child);
	}

	@Override
	public IFile createFile(URI uri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFileInputStream createFileInputStream(IFile file) throws FileNotFoundException {
		VFSFile f = (VFSFile)fixFileReference(file);
		try {
			return new VFSFileInputStream(f);
		} catch (IOException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFileInputStream createFileInputStream(String name) throws FileNotFoundException {
		try {
			return new VFSFileInputStream(new VFSFile(_config, name));
		} catch (IOException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name) throws FileNotFoundException {
		return createFileOutputStream(name, false);
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name, boolean append) throws FileNotFoundException {
		try {
			return new VFSFileOutputStream(new VFSFile(_config, name), append);
		} catch (IOException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}		
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file) throws FileNotFoundException {
		file = fixFileReference(file);
		return createFileOutputStream(file, false);
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file, boolean append) throws FileNotFoundException {
		VFSFile f = (VFSFile)fixFileReference(file);
		try {
			return new VFSFileOutputStream(f, append);
		} catch (IOException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}		
	}

	@Override
	public IFileReader createFileReader(String fileName) throws FileNotFoundException {
		try {
			return new VFSFileReader(new VFSFile(_config, fileName));
		} catch (IOException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFileReader createFileReader(IFile file) throws FileNotFoundException {
		try {
			file =  fixFileReference(file);
			return new VFSFileReader((VFSFile)file);
		} catch (IOException e) {
			if(DEBUG) e.printStackTrace();
			return null;
		}
	}

	@Override
	public IFileWriter createFileWriter(String fileName) throws IOException {
		return createFileWriter(fileName, false);
	}

	@Override
	public IFileWriter createFileWriter(String fileName, boolean append) throws IOException {
		return new VFSFileWriter(new VFSFile(_config, fileName), append);
	}

	@Override
	public IFileWriter createFileWriter(IFile file) throws IOException {
		file =  fixFileReference(file);
		return createFileWriter((VFSFile)file, false);
	}

	@Override
	public IFileWriter createFileWriter(IFile file, boolean append) throws IOException {
		file =  fixFileReference(file);
		return new VFSFileWriter((VFSFile)file, append);
	}

}
