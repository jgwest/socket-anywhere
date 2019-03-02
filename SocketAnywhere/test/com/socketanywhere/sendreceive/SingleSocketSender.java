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

import java.util.Random;

import com.socketanywhere.net.ISocketTL;

public class SingleSocketSender extends Thread {

	TestLog _log;
	ISocketTL _socket;
	int _dataMax;
	long _delayTime;
	
	boolean _continueRunning = true;
	
	public SingleSocketSender(ISocketTL socket, TestLog log, int dataMax, long delayTime) {
		setName(SingleSocketSender.class.getName());
		_socket = socket;
		_log = log;
		
		_dataMax = dataMax;
		_delayTime = delayTime;
	}
	
	private static byte[] generateRandomData(int length) {
		Random r = new Random();
		byte[] result = new byte[length];
		
		r.nextBytes(result);
		return result;
		
	}
	
	@Override
	public void run() {
		
		try {
			Random r = new Random();
			
			while(_continueRunning) {
				// Generate random data to send
				byte[] b = generateRandomData(r.nextInt(_dataMax));
				
				TestLogEntry e = new TestLogEntry();
				
				// Add the data and info to the test log
				synchronized(_log.getTestLogLock()) {
					e.data = b;
					e.orderSent = _log.getNextCounterVal();
					e.received = false;
					e.timeSent = System.currentTimeMillis();
					
					_log.addEntry(e);
					
					// Send the data
					_socket.getOutputStream().write(b);
				}
				
				Thread.sleep(_delayTime);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
				
	}
	
	public void end() {
		_continueRunning = false;
	}
}
