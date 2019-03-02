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
import java.io.InputStream;

import com.socketanywhere.net.ByteHolder;

/** Input stream for the IRCSocket; data is populated into this InputStream by
 * the PacketManager. Received data is placed into the byteholder, and calling read
 * returns data from the byteholder (or blocks, if not available). */
public class IRCInputStream extends InputStream {

	NodeImpl _node;
	IRCSocketConnection _connection;
	
//	boolean _dataAvailable = true;
	ByteHolder _bh = new ByteHolder();
	Object _dataMonitor = new Object();
	boolean _closed = false;
	
	public IRCInputStream(NodeImpl node, IRCSocketConnection conn) {
		_node = node;
		_connection = conn;
	}

	private void waitForData() throws IOException {
		synchronized(_dataMonitor) {
			if(_bh.getContentsSize() == 0) {
				assertNotClosed();
				
				try {
					_dataMonitor.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void assertNotClosed() throws IOException {
		synchronized(_dataMonitor) {
			if(_closed && _bh.getContentsSize() == 0) {
				throw new IOException("Stream is closed");
			}
		}
	}
	
	@Override
	public int read() throws IOException {
		assertNotClosed();
		
		synchronized(_dataMonitor) {
			waitForData();
			int i = (int)_bh.extractAndRemove(1)[0];

			return i;
		}
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		assertNotClosed();
		
		synchronized(_dataMonitor) {
			waitForData();
			int extractAmount = len > _bh.getContentsSize() ? _bh.getContentsSize() : len;
			
			byte[] result = _bh.extractAndRemove(extractAmount);

			int l = result.length;
			
			System.arraycopy(result, 0, b, off, l);
			return l;
		}
	}
	
	protected void informBytes(byte[] moreData) {
		if(moreData == null || moreData.length == 0) {
			return;
		}
		
		synchronized(_dataMonitor) {
			_bh.addBytes(moreData);
			_dataMonitor.notifyAll();
		}		
	}
	
	@Override
	public void close() throws IOException {
		synchronized(_dataMonitor) {
			if(_closed) return;
			else _closed = true;
		}
		
		_connection.close();
	}
	
	protected void internalClose() {
		synchronized(_dataMonitor) {
			if(_closed) return;
			else _closed = true;
		}
	}

}
