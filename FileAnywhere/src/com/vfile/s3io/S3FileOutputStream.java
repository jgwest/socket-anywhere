/*
	Copyright 2012, 2013 Jonathan West

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Object;

import com.vfile.interfaces.IFileOutputStream;

/** Handles writing data to the remote s3 object. 
 * Write the data to a temporary local file, then uploads that data. */
public class S3FileOutputStream extends OutputStream implements IFileOutputStream {
	S3File _file;
	FileOutputStream _fos;
	File _tempFile;
	boolean _isAppend;
	
	boolean _isClosed = false;

	boolean _streamInitialized = false;
	
	public S3FileOutputStream(S3File file) {
		this(file, false);
	}

	public S3FileOutputStream(S3File file, boolean isAppend) {
		_file = file;
		_isAppend = isAppend;
	}
	
	private void createFileOutputStreamIfNeeded() throws IOException {
		synchronized(this) {
			if(_streamInitialized) {
				return;
			}
			_streamInitialized = true;
			
			File tempFile = File.createTempFile("s3-file-", "");
			_fos = new FileOutputStream(tempFile);
			_tempFile = tempFile;
			
			if(_isAppend) {
				S3FileInputStream fis = new S3FileInputStream(_file);
				
				boolean ready = true;
				byte[] bytes = new byte[65536];
				while(ready) {
					int c = fis.read(bytes);
					
					if(c == -1) {
						ready = false;
					} else {
						_fos.write(bytes, 0, c);
					}
				}
				fis.close();
				
			}
				
		}
	}

	
	@Override
	public void close() throws IOException {
		assertNotClosed();
		createFileOutputStreamIfNeeded();
		
		_isClosed = true;
		_fos.close();
		
		S3Object so = new S3Object(_file.getHostInfo().getS3Bucket(), _file.getS3Path());
		
		FileInputStream fis = new FileInputStream(_tempFile);
		so.setDataInputStream(fis);
		so.setContentLength(_tempFile.length());
		so.setContentType("binary/octet-stream");

		try {
			_file.getHostInfo().getS3Service().putObject(_file.getHostInfo().getS3Bucket(), so);
		} catch (S3ServiceException e) {
			S3Log.err("S3ServiceException on Put"+e);
			fis.close();
			_tempFile.delete();
			throw new IOException("Error on file write.");
		}
		
		fis.close();
		_tempFile.delete();
		
		
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		assertNotClosed();
		
		createFileOutputStreamIfNeeded();
		_fos.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		assertNotClosed();
		
		createFileOutputStreamIfNeeded();
		_fos.write(b);
	}

	@Override
	public void write(int n) throws IOException {
		assertNotClosed();
		
		createFileOutputStreamIfNeeded();
		_fos.write(n);
	}

	@Override
	public void flush() throws IOException {
		assertNotClosed();
		_fos.flush();
	}

	
	private void assertNotClosed() {
		if(_isClosed) {
			throw new RuntimeException("Output Stream has already been closed.");
		}
	}
}
