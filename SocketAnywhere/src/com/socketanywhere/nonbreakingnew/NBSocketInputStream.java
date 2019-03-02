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

package com.socketanywhere.nonbreakingnew;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import com.socketanywhere.net.ByteHolderList;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;

public class NBSocketInputStream extends InputStream {

	private final NBSocket _socket;

	private final ByteHolderList _byteHolder = new ByteHolderList();
	
	private final AtomicInteger debugFakePacketIdDelme = new AtomicInteger(0); 
	
	private final ConnectionBrain _connectionBrain;
	
	/** Only applicable if getMaxDataReceiverBuffer() is set: true if we have recently informed the brain that our buffer is full, false if it is longer full, null if we have not informed true or false yet. */
	private Boolean _lastFullSignalSent = null;  
	
	public NBSocketInputStream(NBSocket socket, ConnectionBrain connectionBrain) {
		_socket = socket;
		_connectionBrain = connectionBrain;
	}
	
	protected void informDataReceived(CmdData d) {
		
		synchronized(_byteHolder) {
			_byteHolder.addBytes(d.getFieldData(), 0, d.getFieldDataLength());
			_byteHolder.notify();
			
			int newLength = _byteHolder.getContentsSize();
			
			// If the buffer has a limit, and we're over it..
			if(newLength > _socket.getOptions().getMaxDataReceivedBuffer() && _socket.getOptions().getMaxDataReceivedBuffer() != -1) {
				
				// If we have not already informed the CB that we are over the buffer, then do so (we don't want to keep sending full over and over)
				if(_lastFullSignalSent == null || _lastFullSignalSent == false) {
	 			
					_connectionBrain.getInterface().eventInputStreamIsFull(_socket, true);
					
					_lastFullSignalSent = true;
				}
			}
		}
		
//		if(_socket.getOptions().getMaxDataReceivedBuffer() != -1 /*INFINITE*/) {
//		
//			// Block here if the byteholder has grown too large; wait for it to be read
//			boolean byteHolderTooLarge = false;
//			do {
//				int bhSize = 0;
//				
//				synchronized(_byteHolder) {
//					bhSize = _byteHolder.getContentsSize();
//				}
//				
//				if(bhSize >= _socket.getOptions().getMaxDataReceivedBuffer()) {
//					byteHolderTooLarge = true;
//					try { Thread.sleep(50); } catch(Exception e) { throw new RuntimeException(e); }
//					
//				} else {
//					byteHolderTooLarge = false;
//				}
//				
//			} while(byteHolderTooLarge);
//		}
		
	}
	
	@Override
	public int available() throws IOException {
		synchronized(_byteHolder) {
			return _byteHolder.getContentsSize();
		}
	}

	protected int internalGetContentsSize() {
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
		byte[] result = null;
		
		// This if should not be removed, nor should the order be changed.
//		if(_socket.isClosed() && available() == 0) {
//			return -1;
//		}
		
		// This if should not be removed, nor should the order be changed.
		if(_socket.isInputPipeClosed() && available() == 0) {
//			synchronized(_byteHolder) {
//				int avail = available();
//				if(avail > 0) {
//					NBLog.error(":(");
//				}
//			}
			return -1;
		}

		int available = 0;
				
		do {
			synchronized(_byteHolder) {
				// DO NOT put a _socket call inside a _byteHolder synchronized; there should no blocking IO
				// calls in byteHolder synchronized code.
				available = available();
				
				if(available > 0) {
					int amountToRead = Math.min(len, available);
					result = _byteHolder.extractAndRemove(amountToRead);
				} else {
					try { _byteHolder.wait(100); } catch (InterruptedException e) { throw new RuntimeException(e); }
				}
			
			}
			
		} while(available == 0 && !_socket.isInputPipeClosed());
		
		if(available == 0) {
			// input pipe is necessarily closed here
			synchronized(_byteHolder) {
				if(available () != 0) {
					available = available();
					
					int amountToRead = Math.min(len, available);
					result = _byteHolder.extractAndRemove(amountToRead);
				}
			}
		}
		
		if(available == 0 && _socket.isInputPipeClosed()) {
			return -1;
		}
		
		// here: if result is null, then available = 0
		// yet is here are here then isInputPipeClosed is false
		
		if(result.length == 0) {
			NBLog.error("Result length is 0.");
		}
		
		// Otherwise, result.length must be > 0 here (it should be == available)
		
		System.arraycopy(result, 0, b, off, result.length);

		CmdData fakeData = new CmdData(result, result.length, debugFakePacketIdDelme.getAndIncrement());
		
		Mapper.getInstance().putIntoList((Triplet)Mapper.getInstance().get(this), fakeData);
		Mapper.getInstance().put(fakeData, false);
		
		synchronized(_byteHolder) {
			// If we last informed the brain that the byte buffer was full...
			if(result.length > 0 && _lastFullSignalSent != null && _lastFullSignalSent == true) {
				
				// If the byte buffer has a limit, and it is no longer full, then inform the brain
				if(_byteHolder.getContentsSize() < _socket.getOptions().getMaxDataReceivedBuffer() && _socket.getOptions().getMaxDataReceivedBuffer() != -1) {
					_lastFullSignalSent = false;
					_connectionBrain.getInterface().eventInputStreamIsFull(_socket, false);
				}
			}
		}
		
		
		return result.length;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
}
