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

package com.socketanywhere.nonbreakingnew;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantLock;

import com.socketanywhere.nonbreakingnew.cmd.CmdData;

public class NBSocketOutputStream extends OutputStream {

	private final NBSocket _socketImpl;
	private final ConnectionBrainInterface _inter;
//	private final ConnectionBrain _cb;
	
	private final ReentrantLock _lock = new ReentrantLock();
	
	public NBSocketOutputStream(NBSocket socketImpl, ConnectionBrain cb) {
		_socketImpl = socketImpl;
		_inter = cb.getInterface();
//		_cb = cb;
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
//		if(!_socketImpl.internalOutputStreamIsConnected()) throw new IOException("Socket is not open.");
//		if(_socketImpl.internalOutputStreamIsClosed()) throw new IOException("Socket has closed.");

		if(_socketImpl.internalIsConnectionClosingOrClosed()) {
			throw new IOException("Socket is closing or closed.");
		}
		
		
		try {
			_lock.lock();
		
//			while( _inter.entryState(_socketImpl.getInnerSocket()) != Entry.State.CONN_ESTABLISHED) {
//				try {
//					Thread.sleep(50);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
			
			byte[] dataToSend = new byte[len];
			System.arraycopy(b, off, dataToSend, 0, len);
			
			CmdData d = new CmdData(dataToSend, len, _socketImpl.getNextPacketId());
			
			// Add this data to the queue, in case the other party didn't receive
			boolean result = _inter.eventDataSent(_socketImpl, d);
			if(!result) {
				NBLog.debug("Unable to send outputstream data due to socket being closing or closed, cmd: "+d+"  "+_socketImpl.getDebugTriplet(), NBLog.INTERESTING);
				throw new IOException("Connection is closed.");
			}

			NBLog.sent("NBSOS", d, _socketImpl.getDebugTriplet(), null, NBLog.INFO);
			
			Mapper.getInstance().putIntoList( (Triplet)Mapper.getInstance().get(this), d );
			Mapper.getInstance().put(d, true);
			
//			_socketImpl.getInnerSocket().writeCommand(d);
			
			NBLog.dataSent(d, _socketImpl.getDebugTriplet());
			

		} finally {
			_lock.unlock();
		}
		
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
		if(_socketImpl.internalIsConnectionClosingOrClosed()) { return; }
//		if(_socketImpl.internalOutputStreamIsClosed()) { return; }
		
		_socketImpl.close();
	}

	@Override
	public void flush() throws IOException {
		_socketImpl.flushInnerSocket();
	}
	
}
