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

package com.socketanywhere.irc;

import java.io.IOException;
import java.io.OutputStream;

public class IRCOutputStream extends OutputStream {

	NodeImpl _node;
	IRCSocketConnection _connection;
	IRCOutputStreamDataManager _dataManager;
	boolean _closed = false;
	
	Object _objectLock = new Object(); 
	
//	Object streamLock = new Object();
//	long _nextSeqNum = 0;
//	
	public IRCOutputStream(NodeImpl node, IRCSocketConnection conn) {
		_node = node;
		_connection = conn;
		_dataManager =  node.getOutputDataManager();
	}
	

	private void assertNotClosed() throws IOException {
		synchronized(_objectLock) {
			if(_closed)  throw new IOException("Stream is closed");
		}
	}
	
	@Override
	public void write(int arg0) throws IOException {
		assertNotClosed();
		
		byte b[] = new byte[1];
		b[0] = (byte)arg0;
		_dataManager.sendData(_connection, b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		assertNotClosed();
		
		_dataManager.sendData(_connection, b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		assertNotClosed();
		
		byte[] newResult = new byte[len];
		System.arraycopy(b, off, newResult, 0, len);
		write(newResult);
	}

	@Override
	public void close() throws IOException {
		synchronized (_objectLock) {
			if(_closed) return;
			else _closed = true;
		}
		
		_connection.close();
	}
	
	protected void internalClose() {
		_closed = true;
	}
	
}