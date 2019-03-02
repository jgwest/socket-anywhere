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

package com.socketanywhere.sendreceive;

import java.io.IOException;

import com.socketanywhere.net.ISocketTL;

/** Part of the send/receive test; this class is a thread that reads from the given socket
 *  and compares what it read, with what we knew the other side of the connection sent. */
public class SingleSocketReceiver extends Thread {
	
	TestLog _log;
	ISocketTL _socket;
	TestLogEntry lastEntry = null;
	
	boolean _continueRunning = true;
	
	long _totalDataReceived = 0;
	long _dataFirstSendReceivedTime = 0;
	
	private int _debugThisReceiverNumber = 0;
	
	private static int _debugReceiverNumber = 0;
	
	
	public SingleSocketReceiver(ISocketTL socket, TestLog log) {
		setName(SingleSocketReceiver.class.getName());
		_log = log;
		_socket = socket;
		_debugThisReceiverNumber = _debugReceiverNumber++;
	}
	
	/** Keep reading until 'length' bytes are received; don't return otherwise. */
	private byte[] readXBytes(int length) throws IOException {
		byte[] b = new byte[length];
		int bytesLeft = length;
		int bytesRead = 0;
		int off = 0;
		
		while(bytesLeft > 0) {
			off += bytesRead;
			bytesRead = _socket.getInputStream().read(b, off, bytesLeft);
			bytesLeft -= bytesRead;
			_totalDataReceived += bytesRead;
			
			// In order to track bytes per second, we need a starting point
			if(_dataFirstSendReceivedTime == 0) {
				_dataFirstSendReceivedTime = System.currentTimeMillis();
			}
		}
				
		return b;
	}
	
	@Override
	public void run() {
		TestLogEntry currEntry = null;
		
		while(_continueRunning) {
			lastEntry = currEntry;
			currEntry = _log.removeNextEntry();
			
			if(lastEntry != null) {
				if(currEntry.getOrderSent() - lastEntry.getOrderSent() != 1) {
					_log.addError("Invalid order detected");
				}
			}
			
			try {
				// Read bytes from the socket
				byte[] r = readXBytes(currEntry.getData().length);
				
				// Compare them to the bytes that were sent
				for(int x = 0; x < r.length; x++) {
					if(r[x] != currEntry.getData()[x]) {
						
						_log.addError("Data contents mismatch in "+SingleSocketReceiver.class.getName());
						return;				
						
					}
				}

				// Did the data match?
				double elapsedTime = (System.currentTimeMillis() - _dataFirstSendReceivedTime)/1000;
				System.out.println("("+_debugThisReceiverNumber+") Data matched ("+r.length+" bytes)  data rate:"+(_totalDataReceived / elapsedTime) + "  latency:("+(System.currentTimeMillis() - currEntry.timeSent)+")");
				
				
			} catch (IOException e) {
				_log.addError("IOException in run() of "+SingleSocketReceiver.class.getName());
				e.printStackTrace();
				return;
			} catch(Exception e) {
				_log.addError("Exception in run() of "+SingleSocketReceiver.class.getName());
				e.printStackTrace();
				return;				
			}
		}
	}
	
	public void end() {
		this._continueRunning = false;
	}
}
