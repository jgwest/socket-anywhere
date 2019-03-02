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

package com.socketanywhere.net;

public class CompletableWait {
	
	String _name = null;
	Object _o = new Object();
	boolean _isComplete = false;
	
	public CompletableWait(String name) {
		_name = name;
	}
	
	public void waitForComplete() {
		
		try {
			synchronized(_o) {
				if(_isComplete) return;
				_o.wait();
			}
		} catch(InterruptedException ie) { 
			throw new WaitConditionInterrupted(_name+" was interrupted.", ie); 
		}
	}
	
	public void waitForCompleteTimeout(long msecs) {

		try {
			synchronized(_o) {
				if(_isComplete) return;
				_o.wait(msecs);
			}
		} catch(InterruptedException ie) { 
			throw new WaitConditionInterrupted(_name+" was interrupted.", ie); 
		}
		
	}
	
	public void informComplete() {
		synchronized(_o) {
			_isComplete = true;
			_o.notifyAll();
		}
	}
	
	public boolean getState() {
		return _isComplete;
	}
}

class WaitConditionInterrupted extends RuntimeException {
	private static final long serialVersionUID = -1690061524294318319L;

	public WaitConditionInterrupted() {
		super();
	}

	public WaitConditionInterrupted(String message, Throwable cause) {
		super(message, cause);
	}

	public WaitConditionInterrupted(String message) {
		super(message);
	}

	public WaitConditionInterrupted(Throwable cause) {
		super(cause);
	}
	
}