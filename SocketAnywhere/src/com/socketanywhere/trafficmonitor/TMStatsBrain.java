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

package com.socketanywhere.trafficmonitor;

import java.io.IOException;

public class TMStatsBrain {
	
	private final static boolean VERBOSE = true;
	
	
	public static final int UNLIMITED = -1;
	
	long _dataSent = 0;
	long _dataReceived = 0;
	long _connectionAttempts = 0;
	long _serverSocketAccepts = 0;
	
	// Restrictions
	long _maxDataTotal = UNLIMITED;
	long _maxDataSent = UNLIMITED;
	long _maxDataReceived = UNLIMITED;
	long _maxConnectionAttempts = UNLIMITED;
	long _maxServerSocketAccepts = UNLIMITED;
	
	
	private Object _lock = new Object();
	
	public TMStatsBrain() {
	}
	
	
	/** Returns true if constraints are fine, false if they have failed. */
	public boolean checkConstraints() {
		synchronized (_lock) {
			
			boolean failed = false;
			
			if(_maxDataTotal != -1) {
				if(_dataSent + _dataReceived > _maxDataTotal) {
					if(VERBOSE) { System.err.println("Failed on data total: "+_maxDataTotal + " / "+(_dataSent+_dataReceived));} 
					failed = true;
				}
			}
			
			if(_maxDataSent != -1) {
				if(_dataSent > _maxDataSent) {
					if(VERBOSE) { System.err.println("Failed on data sent: "+_maxDataSent + " / "+_dataSent);}
					failed = true;
				}
			}
			
			if(_maxDataReceived != -1) {
				if(_dataReceived > _maxDataReceived) {
					if(VERBOSE) { System.err.println("Failed on data received: "+_maxDataReceived + " / "+_dataReceived);}
					failed = true;
				}
			}
			
			if(_maxConnectionAttempts != -1) {
				if(_connectionAttempts > _maxConnectionAttempts) {
					if(VERBOSE) { System.err.println("Failed on max connection attempts:"+_maxConnectionAttempts +" / " + _connectionAttempts);}
					failed = true;
				}
			}
			
			if(_maxServerSocketAccepts != -1) {
				if(_serverSocketAccepts > _maxServerSocketAccepts) {
					if(VERBOSE) { System.err.println("Failed on max server socket accepts:"+_maxServerSocketAccepts+" / " + _serverSocketAccepts);}
					failed = true;
				}
			}
			
			if(failed) {
				String msg = "Socket factory stack has reached its user-defined constraints.";
				System.err.println(msg);
				System.err.flush();
				
				return false;
				
			} else {
				return true;
			}
			
		}
	}
	
	public void eventDataSent(long bytes) throws IOException {
		synchronized(_lock) {
			_dataSent += bytes;
		}
	}

	public void eventDataReceived(long bytes) throws IOException {
		synchronized(_lock) {
			_dataReceived += bytes;
		}
	}

	public void eventConnectionAttempt() throws IOException {
		synchronized (_lock) {
			_connectionAttempts++;
		}
	}
	
	public void eventServerSocketAccept() throws IOException {
		synchronized(_lock) {
			_serverSocketAccepts++;
		}
	}


	public void setMaxDataTotal(long maxDataTotal) {
		if(maxDataTotal < -1) { throw new RuntimeException("Negative number passed to method."); }
		this._maxDataTotal = maxDataTotal;
	}


	public void setMaxDataSent(long maxDataSent) {
		if(maxDataSent < -1) { throw new RuntimeException("Negative number passed to method."); }
		this._maxDataSent = maxDataSent;
	}


	public void setMaxDataReceived(long maxDataReceived) {
		if(maxDataReceived < -1) { throw new RuntimeException("Negative number passed to method."); }
		this._maxDataReceived = maxDataReceived;
	}


	public void setMaxConnectionAttempts(long maxConnectionAttempts) {
		if(maxConnectionAttempts < -1) { throw new RuntimeException("Negative number passed to method."); }
		this._maxConnectionAttempts = maxConnectionAttempts;
	}


	public void setMaxServerSocketAccepts(long maxServerSocketAccepts) {
		if(maxServerSocketAccepts < -1) { throw new RuntimeException("Negative number passed to method."); }
		
		this._maxServerSocketAccepts = maxServerSocketAccepts;
	}
	
	
	

}
