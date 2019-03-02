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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileFactory;
import com.vfile.interfaces.IFileInputStream;
import com.vfile.interfaces.IFileOutputStream;
import com.vfile.interfaces.IFileReader;
import com.vfile.interfaces.IFileWriter;

/** Implements the IFileFactory interface using S3 calls. */
public class S3FileFactory implements IFileFactory {

	S3HostInfo _hostInfo;
	
	public S3FileFactory(S3HostInfo hostInfo) {
		_hostInfo = hostInfo;
	}
	
	@Override
	public IFile createFile(IFile parent, String child) {
		return new S3File(_hostInfo, (S3File)parent, child);
	}

	@Override
	public IFile createFile(String pathname) {
		return new S3File(_hostInfo, pathname);
	}

	@Override
	public IFile createFile(String parent, String child) {
		return new S3File(_hostInfo, parent, child);
	}

	@Override
	public IFile createFile(URI uri) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IFileInputStream createFileInputStream(IFile file) throws FileNotFoundException {
		return new S3FileInputStream((S3File)file);
	}

	@Override
	public IFileInputStream createFileInputStream(String name) throws FileNotFoundException {
		return new S3FileInputStream(new S3File(_hostInfo, name));
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name) throws FileNotFoundException {
		return new S3FileOutputStream(new S3File(_hostInfo, name));
	}

	@Override
	public IFileOutputStream createFileOutputStream(String name, boolean append) throws FileNotFoundException {
		return new S3FileOutputStream(new S3File(_hostInfo, name), append);
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file) throws FileNotFoundException {
		return new S3FileOutputStream((S3File)file);
	}

	@Override
	public IFileOutputStream createFileOutputStream(IFile file, boolean append) throws FileNotFoundException {
		return new S3FileOutputStream((S3File)file, append);	
	}

	@Override
	public IFileReader createFileReader(String fileName) throws FileNotFoundException {
		return new S3FileReader(new S3File(_hostInfo, fileName));
	}

	@Override
	public IFileReader createFileReader(IFile file) throws FileNotFoundException {
		return new S3FileReader((S3File)file);
	}

	@Override
	public IFileWriter createFileWriter(String fileName) throws IOException {
		return new S3FileWriter(new S3File(_hostInfo, fileName));
	}

	@Override
	public IFileWriter createFileWriter(String fileName, boolean append) throws IOException {
		return new S3FileWriter(new S3File(_hostInfo, fileName), append);
	}

	@Override
	public IFileWriter createFileWriter(IFile file) throws IOException {
		return new S3FileWriter((S3File)file);
	}

	@Override
	public IFileWriter createFileWriter(IFile file, boolean append) throws IOException {
		return new S3FileWriter((S3File)file, append);
	}

}
