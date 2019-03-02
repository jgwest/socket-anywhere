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

package com.socketanywhere.trafficmonitor;

import java.io.IOException;
import java.io.OutputStream;

public class TMOutputStream extends OutputStream {

	OutputStream _inner;
	TMSocketImpl _ourSocket;
	TMStatsBrain _brain;
	
	public TMOutputStream(TMSocketImpl ourSocket, OutputStream inner, TMStatsBrain brain) {
		_inner = inner;
		_ourSocket = ourSocket;
		_brain = brain;
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
		checkConstraints();
		
		_inner.write(b, off, len);
		_brain.eventDataSent(len);
		
		checkConstraints();
	}

	@Override
	public void write(byte[] b) throws IOException {
		checkConstraints();
		
		_inner.write(b);
		_brain.eventDataSent(b.length);
		
		checkConstraints();
	}

	@Override
	public void write(int b) throws IOException {
		checkConstraints();
		
		_inner.write(b);
		_brain.eventDataSent(1);
		
		checkConstraints();
	}

	private void checkConstraints() throws IOException {
		if(!_brain.checkConstraints()) {
			try { close(); } catch (IOException e) { }
			throw new IOException("Socket constraints have been met.");
		}
	}

}
