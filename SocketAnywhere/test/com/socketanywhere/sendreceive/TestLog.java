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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

public class TestLog {
	Object testLogLock = new Object();
	
	List<String> errorLog = new Vector<String>();
	
	Queue<TestLogEntry> log = new LinkedList<TestLogEntry>();
	
	long counter = 0; 
	
	public TestLog() {
	}
	
	public Object getTestLogLock() {
		return testLogLock;
	}
	
	public void addEntry(TestLogEntry tle) {
		
		synchronized(testLogLock) {
			log.add(tle);
		}
	}
	public long getNextCounterVal() {
		synchronized(testLogLock) {
			return counter++;
		}
	}
	
	public TestLogEntry removeNextEntry() {
		TestLogEntry result = null;
		while(result == null) {
			synchronized (testLogLock) {
				if(log.size() != 0) {
					result = log.remove();
				}
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		return result;
	}
	
	public void addError(String s) {
		System.out.println("ERROR: "+s);
		errorLog.add(s);
	}
	
	public List<String> getErrorLog() {
		return errorLog;
	}
}
