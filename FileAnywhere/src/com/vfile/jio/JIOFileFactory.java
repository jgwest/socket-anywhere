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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFactory;
import com.vfile.interfaces.IFileInputStream;
import com.vfile.interfaces.IFileOutputStream;
import com.vfile.interfaces.IFileReader;
import com.vfile.interfaces.IFileWriter;

/** Implements a vfile factory using the standard Java IO file interface. */
public class JIOFileFactory implements IFileFactory {

	public JIOFileFactory() {
	}
	
	@Override
	public IFile createFile(IFile parent, String child) {
		return new JIOFile(parent, child);
	}

	@Override
	public IFile createFile(String pathname) {
		return new JIOFile(pathname);		
	}

	@Override
	public IFile createFile(String parent, String child) {
		return new JIOFile(parent, child);
	}

	@Override
	public IFile createFile(URI uri) {
		return new JIOFile(uri);
	}

	@Override
	public IFileInputStream createFileInputStream(IFile file) throws FileNotFoundException {
		return new JIOFileInputStream(file);
	}

	@Override
	public IFileInputStream createFileInputStream(String name) throws FileNotFoundException {
		return new JIOFileInputStream(name);
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name) throws FileNotFoundException {
		return new JIOFileOutputStream(name);
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name, boolean append) throws FileNotFoundException {
		return new JIOFileOutputStream(name, append);
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file) throws FileNotFoundException {
		return new JIOFileOutputStream(file);
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file, boolean append) throws FileNotFoundException {
		return new JIOFileOutputStream(file, append);
	}

	@Override
	public IFileReader createFileReader(String fileName) throws FileNotFoundException {
		return new JIOFileReader(fileName);
	}

	@Override
	public IFileReader createFileReader(IFile file) throws FileNotFoundException {
		return new JIOFileReader(file);
	}

	@Override
	public IFileWriter createFileWriter(String fileName) throws IOException {
		return new JIOFileWriter(fileName);
	}

	@Override
	public IFileWriter createFileWriter(String fileName, boolean append) throws IOException {
		return new JIOFileWriter(fileName, append);
	}

	@Override
	public IFileWriter createFileWriter(IFile file) throws IOException {
		return new JIOFileWriter(file);
	}

	@Override
	public IFileWriter createFileWriter(IFile file, boolean append) throws IOException {
		return new JIOFileWriter(file, append);
	}

}
