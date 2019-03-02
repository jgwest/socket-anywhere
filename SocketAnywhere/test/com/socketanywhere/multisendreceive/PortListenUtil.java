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

package com.socketanywhere.multisendreceive;

import java.util.HashMap;
import java.util.Random;

public class PortListenUtil {

	private Random _random;
	
	private HashMap<Integer, Long> _lastPortsListenedOn = new HashMap<Integer, Long>();
	
	private Object _lock = new Object();
	
	public PortListenUtil() {
		_random = new Random();
	}
	
	/** Randomly select a port to listen on, but ensure that the port hasn't been listened on
	 * in the last x number of seconds. */
	public int getNextPortToListenOn() {
		synchronized(_lock) {
		
			int result = -1;
			
			do {
				long currTime = System.currentTimeMillis();
				int port = 10000 + (int)(_random.nextDouble()*20000);
								
				Long l = _lastPortsListenedOn.get((Integer)port);
				
				if(l == null || currTime - l > 240 * 1000) {
					result = port;
				}
				
				
			} while(result == -1);
			
			_lastPortsListenedOn.put((Integer)result, (Long)System.currentTimeMillis());
			
			return result;
		}
	}
		
}
