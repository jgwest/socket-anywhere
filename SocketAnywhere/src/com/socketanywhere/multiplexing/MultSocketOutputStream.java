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

package com.socketanywhere.multiplexing;

import java.io.IOException;
import java.io.OutputStream;

public class MultSocketOutputStream extends OutputStream {

	MultSocket _socketImpl;
	MultConnectionBrain _cb;
	String _connUUID;
	int _connId;
	
//	private static ReentrantLock _lock = new ReentrantLock();
	
	public MultSocketOutputStream(MultSocket socketImpl, MultConnectionBrain cb, String connUUID, int connId) {
		_socketImpl = socketImpl;
		_cb = cb;
		_connUUID = connUUID;
		_connId = connId;
	}
	
	@SuppressWarnings("unused")
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(!_socketImpl.isConnected()) throw new IOException("Socket is not open.");
		if(_socketImpl.isClosed()) throw new IOException("Socket has closed.");

		byte[] dataToSend = new byte[len];
		System.arraycopy(b, off, dataToSend, 0, len);
		
		CmdDataMultiplex d = new CmdDataMultiplex(dataToSend, len, _socketImpl.getNextPacketId(), _connUUID, _connId);
		
		boolean reconnectNeeded = false;
		
		try {
			_socketImpl.writeToInnerSocket(d.buildCommand());

			MuLog.wroteCommand(d, _socketImpl);
		} catch(Exception e) {
			e.printStackTrace();
			_socketImpl.informRemoteClose(); // This isn't necessary, but can't hurt
		}

		MuLog.dataSent(d);

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
