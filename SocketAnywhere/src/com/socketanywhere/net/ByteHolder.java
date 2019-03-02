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

/** 
 * Byte holder is not thread-safe.
 * */
public class ByteHolder {
	// TODO: LOWER - This is a simplistic implementation of byte holder

	byte[] _contents = new byte[0];

	public ByteHolder() {
	}
	
	public void addBytes(byte[] value) {
		addBytes(value, 0, value.length);
	}
	
	public void addBytes(byte[] value, int offset, int length) {
		byte[] result = new byte[_contents.length + (length)];
		
		System.arraycopy(_contents, 0, result, 0, _contents.length);
		
		System.arraycopy(value, offset, result, _contents.length, (length));
		
		_contents = result;
		
	}
	
	public int getContentsSize() {
		return _contents.length;
	}
	
	public byte[] getContents() {
		return _contents;
	}
	
	private void remove(int startPosOfNewArray) {
		byte[] result = new byte[_contents.length- startPosOfNewArray];
		
		System.arraycopy(_contents, startPosOfNewArray, result, 0, _contents.length - startPosOfNewArray);
		_contents = result;
	}
	
	public byte[] extractAndRemove(int length) {
		byte[] result = new byte[length];
		
		System.arraycopy(_contents, 0, result, 0, length);
		remove(length);
		
		return result;
	}
	
}
