/* Copyright 2013, 2019 Jonathan West

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import net.java.truevfs.access.TFile;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFactory;
import com.vfile.interfaces.IFileInputStream;
import com.vfile.interfaces.IFileOutputStream;
import com.vfile.interfaces.IFileReader;
import com.vfile.interfaces.IFileWriter;

public class VAFileFactory implements IFileFactory {
	
	File _archiveFile;
	
	
	private void verifyFileArchiveMatch(VAFile f) {
		if(_archiveFile != f._rootArchive) {
			throw new UnsupportedOperationException("Unable to mix archive files with different archives.");
		}
	}
	
	public VAFileFactory(File archiveFile) {
		_archiveFile = archiveFile;
	}
	
	@Override
	public IFile createFile(IFile parent, String child) {
		verifyFileArchiveMatch((VAFile)parent);
		
		String fullPath = parent.getPath();
		
		if(!fullPath.endsWith(File.separator) && !child.startsWith(File.separator)) {
			fullPath += File.separator;
		}
		
		fullPath += child;
		
		VAFile newFile = new VAFile(_archiveFile, fullPath);
		
		return newFile;
	}

	@Override
	public IFile createFile(String pathname) {
		TFile t = new TFile(_archiveFile.getPath()+File.separator+pathname);
		return new VAFile(t);
	}

	@Override
	public IFile createFile(String parent, String child) {
		
		TFile t = new TFile(_archiveFile.getPath()+File.separator+parent+File.separator+child);
		return new VAFile(t);
	}

	@Override
	public IFile createFile(URI uri) {
		return null;
	}

	@Override
	public IFileInputStream createFileInputStream(IFile file) throws FileNotFoundException {
		
		verifyFileArchiveMatch((VAFile)file);
		
		try {
			return new VAFileInputStream((VAFile)file);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IFileInputStream createFileInputStream(String name) throws FileNotFoundException {
		
		try {
			return new VAFileInputStream(new VAFile(_archiveFile, name));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IFileOutputStream createFileOutputStream(String name) throws FileNotFoundException {
		try {
			return new VAFileOutputStream(new VAFile(_archiveFile, name), false);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name, boolean append) throws FileNotFoundException {
		
		try {
			return new VAFileOutputStream(new VAFile(_archiveFile, name), append);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file) throws FileNotFoundException {
		verifyFileArchiveMatch((VAFile)file);
		
		try {
			return new VAFileOutputStream((VAFile)file, false);
		} catch (Exception e) {
			throw new RuntimeException(e);			
		}

	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file, boolean append) throws FileNotFoundException {
		verifyFileArchiveMatch((VAFile)file);
		
		try {
			return new VAFileOutputStream((VAFile)file, append);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IFileReader createFileReader(String fileName) throws FileNotFoundException {
		try {
			return new VAFileReader(new VAFile(_archiveFile, fileName));
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IFileReader createFileReader(IFile file) throws FileNotFoundException {
		verifyFileArchiveMatch((VAFile)file);
		
		try {
			return new VAFileReader((VAFile)file);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IFileWriter createFileWriter(String fileName) throws IOException {
		try {
			return new VAFileWriter(new VAFile(_archiveFile, fileName), false);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public IFileWriter createFileWriter(String fileName, boolean append) throws IOException {
		try {
			return new VAFileWriter(new VAFile(_archiveFile, fileName), append);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IFileWriter createFileWriter(IFile file) throws IOException {
		verifyFileArchiveMatch((VAFile)file);
		
		try {
			return new VAFileWriter((VAFile)file, false);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public IFileWriter createFileWriter(IFile file, boolean append) throws IOException {
		verifyFileArchiveMatch((VAFile)file);
		
		try {
			return new VAFileWriter((VAFile)file, append);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}

	}

}
