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

package com.socketanywhere.nonbreaking;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;

public class NBSocketOutputStream extends OutputStream {

	NBSocket _socketImpl;
	ConnectionBrain _cb;
	
	private ReentrantLock _lock = new ReentrantLock();
	
	public NBSocketOutputStream(NBSocket socketImpl, ConnectionBrain cb) {
		_socketImpl = socketImpl;
		_cb = cb;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(!_socketImpl.isConnected()) throw new IOException("Socket is not open.");
		if(_socketImpl.isClosed()) throw new IOException("Socket has closed.");

		_lock.lock();
		
		byte[] dataToSend = new byte[len];
		System.arraycopy(b, off, dataToSend, 0, len);
				
		CmdData d = new CmdData(dataToSend, len, _socketImpl.getNextPacketId());

		boolean reconnectNeeded = false;
		
		try {
			_socketImpl.writeToInnerSocket(d.buildCommand());
		} catch(Exception e) {
			NBLog.debug("Exception received on writeToInnerSocket of write() in output stream: "+e);
			reconnectNeeded = true;
		}
		
		if(reconnectNeeded) {
			_cb.eventConnErrDetectedReconnectIfNeeded(_socketImpl._inner);
		}
		
		NBLog.dataSent(d);
		
		// Add this data to the queue, in case the other party didn't receive
		_cb.eventDataSent(_socketImpl, d);
		
		_lock.unlock();
		
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		this.write(b, 0, b.length);
	}

	@Override
	public void write(int b) throws IOException {
		byte[] barr = new byte[1];
		barr[0] = (byte)b;
		this.write(barr);
	}

	@Override
	public void close() throws IOException {
		if(_socketImpl.isClosed()) return;
		
		_socketImpl.close();
	}

	@Override
	public void flush() throws IOException {
		_socketImpl.flushInnerSocket();
	}
	
}
