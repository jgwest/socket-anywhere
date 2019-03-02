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

package com.socketanywhere.obfuscator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/** Implements the encrypt(...) and decrypt(...) methods; encryption and decryption
 * is just reading from a "one time pad" file, and adding/subtracing the bytes from that file
 * from the data to be transmitted.
 * 
 *  You should ensure your OTP is randomly generated, so that it doesn't contain a bunch of empty
 *  data (0s), which would mean that the underlying data sent/received is not "encrypted/decrypted". */
public class OneTimePadFileTransformer implements IDataTransformer {

	int _bytesSent = 0 ;
	int _bytesReceived = 0;

	File _otpFile;
	
	InputStream _encryptStream;
	InputStream _decryptStream;
	
	enum Stream { ENCRYPT, DECRYPT };
	
//	static Object _lock = new Object();
	
//	private OneTimePadFileTransformer() { }
	
	public OneTimePadFileTransformer(File otpFile) throws IOException {
		_otpFile = otpFile;
		
		try {
			_encryptStream = new FileInputStream(otpFile);
			_decryptStream = new FileInputStream(otpFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw(e);
		}
		
	}
	
	@Override
	public void encrypt(byte[] data, int off, int len) {
		
		byte[] encData;
		encData = getBytes(Stream.ENCRYPT, len);
		
		 for(int x = off; x < off+len; x++) {
			 data[x] += encData[x-off];
		 }
		 
		_bytesSent += len;
	}

	@Override
	public void decrypt(byte[] data, int off, int len) {
		
		byte[] encData;		
		encData = getBytes(Stream.DECRYPT, len);
			
		 for(int x = off; x < off+len; x++) {
			 data[x] -= encData[x-off];
		 }
		 
		_bytesReceived+= len;
		
	}
	
	
	private byte[] getBytes(Stream stream, int numBytes) {
		byte[] result = new byte[numBytes];
		int currPos = 0;
		int numBytesRemaining = numBytes;

		InputStream is;
		
		if(stream == Stream.ENCRYPT) {
			is = _encryptStream;
		} else {
			is = _decryptStream;
		}
		
		while(numBytesRemaining > 0) {
			
			byte[] ba = new byte[16384];
			int c = 0;
			
			try {
				int bytesRequested = Math.min(numBytesRemaining, ba.length);
				
				while( numBytesRemaining > 0 && (c = is.read(ba, 0, bytesRequested)) != -1) {
					
					System.arraycopy(ba, 0, result, currPos, c);
					currPos += c;
					numBytesRemaining -= c;
					
					bytesRequested = Math.min(numBytesRemaining, ba.length);
				}
			} catch(IOException e) {
				e.printStackTrace();
				return null;
			}
			
			// Always re-up the InputStream if it is empty, even if we have filled our needed bytes.
			if(c == -1) {
				try {
					is.close();

					if(stream == Stream.ENCRYPT ){
						_encryptStream = new FileInputStream(_otpFile);
						is = _encryptStream;
					} else {
						_decryptStream = new FileInputStream(_otpFile);
						is = _decryptStream;
					}

				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				
				
			}
			
		} // end of while loop
		
		
		return result;
		
		
	}
	
	@Override
	public IDataTransformer instantiateDataTransformer() {
		try {
			return new OneTimePadFileTransformer(_otpFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}


//class OTPFileInputStream extends FileInputStream {
//
//	public OTPFileInputStream(File file) throws FileNotFoundException {
//		super(file);
//	}
//	
//	@Override
//	public int read(byte[] b, int off, int len) throws IOException {
//		int c = super.read(b, off, len);
//		
//		return c;
//		
//	}
//	
//	@Override
//	public int read() throws IOException {
//		System.err.println("read1.");
//		System.exit(0);
//		return 0;
//	}
//	
//	@Override
//	public int read(byte[] b) throws IOException {
//		System.err.println("read2.");
//		System.exit(0);
//		return 0;
//	}
//	
//}
