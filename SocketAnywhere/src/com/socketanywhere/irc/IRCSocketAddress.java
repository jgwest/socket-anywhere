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



package com.socketanywhere.irc;


public class IRCSocketAddress implements Comparable<IRCSocketAddress> {
	private String _address;
	private int _port;
	
	// TODO: Replace this class with TLAddress?
	
	public IRCSocketAddress(String addr, int port) {
		_address = addr;
		_port = port;
	}
	
	public String getAddress() {
		return _address;
	}
	public int getPort() {
		return _port;
	}
	
	public void setAddress(String address) {
		_address = address;
	}
	public void setPort(int port) {
		this._port = port;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof IRCSocketAddress)) return false;
		IRCSocketAddress other = (IRCSocketAddress)o;
		
		if(other.getPort() != getPort() || !other.getAddress().equalsIgnoreCase(getAddress())) {
			return false;
		} else {
			return true;
		}
		
	}
	
	@Override
	public int compareTo(IRCSocketAddress o) {
		return toString().compareTo(o.toString());
	}

	public String toString() {
		String str = ""+_address+"-"+_port;
		return str;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
}
