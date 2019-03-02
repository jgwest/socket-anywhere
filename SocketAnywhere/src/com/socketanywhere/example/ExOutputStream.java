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

package com.socketanywhere.example;

import java.io.IOException;
import java.io.OutputStream;

public class ExOutputStream extends OutputStream {

	OutputStream _inner;
	ExSocketImpl _ourSocket;
	
	public ExOutputStream(ExSocketImpl ourSocket, OutputStream inner) {
		_inner = inner;
		_ourSocket = ourSocket;
	}
	
	@Override
	public void close() throws IOException {
		_inner.close();
	}

	@Override
	public void flush() throws IOException {
		_inner.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		_inner.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		_inner.write(b);
	}

	@Override
	public void write(int b) throws IOException {
		_inner.write(b);
	}

}
