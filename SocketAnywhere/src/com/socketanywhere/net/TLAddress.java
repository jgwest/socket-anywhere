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

public class TLAddress {
	public final static int UNDEFINED_PORT = -1;
	private final String _hostname; // = null;
	private final int _port; // = UNDEFINED_PORT;
	
	public TLAddress(int port) {
		_port = port;
		_hostname = null;
	}

	
	public TLAddress(String hostname) {
		_hostname = hostname;
		_port = UNDEFINED_PORT;
	}
	
	public TLAddress(String hostname, int port) {
		_hostname = hostname;
		_port = port;
	}
	
	public String getHostname() {
		return _hostname;
	}
	
//	public void setHostname(String hostname) {
//		this._hostname = hostname;
//	}
	
	public int getPort() {
		return _port;
	}
	
//	public void setPort(int port) {
//		this._port = port;
//	}
	
	@Override
	public String toString() {
		if(_port == -1) {
			return "["+_hostname +":N/A]";
		} else {
			return "["+_hostname +":"+_port+"]";	
		}
		
	}
	
	@Override
	public int hashCode() {
		if(_hostname != null) {
			return (_hostname.hashCode()% 1000) * 1000 + _port;
		} else {
			return _port;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof TLAddress)) {
			return false;
		}
		
		TLAddress t = (TLAddress)obj;
		
		if(t._port != _port) {
			return false;
		}
		
		if(t._hostname == _hostname) {
			return true;
		}
		
		// This means one is null and the other is not, therefore not equal
		if(t._hostname == null || _hostname == null) {
			return false;
		}
		
		// Neither are null
		return t._hostname.equals(_hostname);
	}
}
