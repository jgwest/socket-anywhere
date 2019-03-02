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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class OBOutputStream extends OutputStream {

	OutputStream _outputStream;
	List<IDataTransformer> _dataTransformerList;
		
	public static byte[] cloneData(byte[] data) {
		byte[] result = new byte[data.length];
		System.arraycopy(data, 0, result, 0, data.length);
		return result;
	}
	
	public OBOutputStream(OutputStream outputStream, List<IDataTransformer> dataTransformer) {
		_outputStream = outputStream;
		_dataTransformerList = dataTransformer;
	}
	
	@Override
	public void close() throws IOException {
		_outputStream.close();
	}

	@Override
	public void flush() throws IOException {
		_outputStream.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		byte[] b2 = cloneData(b);
		
		// "encrypt" stating from the first in the list and going to the last
		for(IDataTransformer dt : _dataTransformerList) {
			dt.encrypt(b2, off, len);
		}
		
		_outputStream.write(b2, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	@Override
	public void write(int c) throws IOException {
		byte[] b = new byte[1];
		b[0] = (byte)(c);
		write(b);
	}

}
