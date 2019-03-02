/*
	Copyright 2016 Jonathan West

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

package com.socketanywhere.multiplexingnew;

public class MNOptions {

	private final int _maxReceiveBufferInBytes;
	
	private final int _maxSendBufferPerSocketInBytes;
	
	public MNOptions() {
		_maxReceiveBufferInBytes = -1;
		_maxSendBufferPerSocketInBytes = -1;
	}
	public MNOptions(int maxReceiveBufferInBytes, int maxSendBufferPerSocketInBytes) {
		_maxReceiveBufferInBytes = maxReceiveBufferInBytes;
		_maxSendBufferPerSocketInBytes = maxSendBufferPerSocketInBytes;
	}
	
	public int getMaxReceiveBufferInBytes() {
		return _maxReceiveBufferInBytes;
	}
	
	public int getMaxSendBufferPerSocketInBytes() {
		return _maxSendBufferPerSocketInBytes;
	}
	
//	public void setMaxReceiveBufferInBytes(int maxReceiveBufferInBytes) {
//		this.maxReceiveBufferInBytes = maxReceiveBufferInBytes;
//	}
	
	
}
