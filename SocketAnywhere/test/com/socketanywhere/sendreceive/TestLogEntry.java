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

public class TestLogEntry {

	byte[] data;
	long timeSent;
	long orderSent;
	boolean received;
	
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public long getTimeSent() {
		return timeSent;
	}
	public void setTimeSent(long timeSent) {
		this.timeSent = timeSent;
	}
	public long getOrderSent() {
		return orderSent;
	}
	public void setOrderSent(long orderSent) {
		this.orderSent = orderSent;
	}
	public boolean isReceived() {
		return received;
	}

	public void setReceived(boolean received) {
		this.received = received;
	}
	
}
