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

package com.socketanywhere.nonbreaking;

import java.io.IOException;
import java.io.InputStream;

import com.socketanywhere.net.ByteHolder;

public class NBSocketInputStream extends InputStream {

	NBSocket _socket;

	ByteHolder _byteHolder = new ByteHolder();
		
	public NBSocketInputStream(NBSocket socket) {
		_socket = socket;
	}
	
	protected void informDataReceived(CmdData d) {
		synchronized(_byteHolder) {
			_byteHolder.addBytes(d.getFieldData(), 0, d.getFieldDataLength());
			_byteHolder.notify();
		}
		
		if(_socket.getOptions().getMaxDataReceivedBuffer() != -1 /*INFINITE*/) {
		
			// Block here if the byteholder has grown too large; wait for it to be read
			boolean byteHolderTooLarge = false;
			do {
				int bhSize = 0;
				
				synchronized(_byteHolder) {
					bhSize = _byteHolder.getContentsSize();
				}
				
				if(bhSize >= _socket.getOptions().getMaxDataReceivedBuffer()) {
					byteHolderTooLarge = true;
					try { Thread.sleep(50); } catch(Exception e) { throw new RuntimeException(e); }
					
				} else {
					byteHolderTooLarge = false;
				}
				
			} while(byteHolderTooLarge);
		}
	}
	
	@Override
	public int available() throws IOException {
		synchronized(_byteHolder) {
			return _byteHolder.getContentsSize();
		}
	}

	@Override
	public void close() throws IOException {
		_socket.close();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int c = this.read(b);
		
		if(c == -1) return -1;
		
		return b[0];
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		byte[] result;
		
		synchronized(_byteHolder) {
			if(available() == 0 && _socket.isClosed()) {
				return -1;
			}
			
			while(available() == 0 && !_socket.isClosed()) {
				try { _byteHolder.wait(100); } catch (InterruptedException e) { throw new RuntimeException(e); }
			}
			if(available() == 0 && _socket.isClosed()) return -1;
			
			int amountToRead = Math.min(len, available());
			result = _byteHolder.extractAndRemove(amountToRead);
		}
		
		System.arraycopy(result, 0, b, off, result.length);
		
		return result.length;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

}
