/*
	Copyright 2012, 2017 Jonathan West

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

package com.socketanywhere.multiplexingnew;

import java.io.IOException;
import java.io.OutputStream;

import com.socketanywhere.multiplexing.CmdDataMultiplex;

public class MNMultSocketOutputStream extends OutputStream {

	MNMultSocket _socketImpl;
	MNConnectionBrain _cb;
	String _connUUID;
	int _connId;
	
	public MNMultSocketOutputStream(MNMultSocket socketImpl, MNConnectionBrain cb, String connUUID, int connId) {
		_socketImpl = socketImpl;
		_cb = cb;
		_connUUID = connUUID;
		_connId = connId;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(!_socketImpl.isConnected()) { throw new IOException("Socket is not open."); }
		if(_socketImpl.isClosed()) { throw new IOException("Socket has closed."); }

		byte[] dataToSend = new byte[len];
		System.arraycopy(b, off, dataToSend, 0, len);
		
		CmdDataMultiplex d = new CmdDataMultiplex(dataToSend, len, _socketImpl.getNextPacketId(), _connUUID, _connId);
		
		try {
			_socketImpl.writeDataCommand(d);
		} catch(IOException e) {
			e.printStackTrace();
			_socketImpl.informRemoteClose(); // This isn't necessary, but can't hurt
			throw e;
		}

		MnLog.dataSent(d);

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
