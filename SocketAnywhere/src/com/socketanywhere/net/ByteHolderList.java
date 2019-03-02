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

package com.socketanywhere.net;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 
 * Byte holder is not thread-safe.
 * */
public class ByteHolderList {

//	byte[] _contents = new byte[0];
	
	List<byte[]> _contents = new ArrayList<byte[]>();
	
	private int _size = 0;

	private String _debugStr;
	
	public ByteHolderList() {
	}
	
	public void addBytes(byte[] value) {
		addBytes(value, 0, value.length);
	}
	
	public void setDebugStr(String debugStr) {
		_debugStr = debugStr;
	}
	
	public void addBytes(byte[] value, int offset, int length) {
//		System.out.println("add bytes: "+length);

		byte[] result = new byte[length];

		System.arraycopy(value, offset, result, 0, length);

		_contents.add(result);
		_size += length;
	
		
		if(_size >= 1024 * 1024 * 5) {
			System.out.println("size is up to: "+_size+" "); // +_debugStr);
		}
		
//		System.out.println("add bytes out.");
	}
	
	public int getContentsSize() {
//		int size = 0;
//		for(Iterator<byte[]> it = _contents.iterator(); it.hasNext();) {
//			
//			size += it.next().length;
//		}
//		return size;
		return _size;
	}
	
//	public byte[] getContents() {
//		return _contents;
//	}
	
//	private byte[] remove(int bytesToRemove) {
	public byte[] extractAndRemove(int bytesToRemove) {
		
//		System.out.println("remove bytes:" +bytesToRemove+ " "+_contents.size());
		
		List<byte[]> currResult = new ArrayList<byte[]>();		

		// Extract >= x bytes from current contents, and store in currResult
		{
			int currBytes = 0;
			
			for(Iterator<byte[]> it = _contents.iterator(); it.hasNext();) {
				
				byte[] curr = it.next();
				
				currBytes += curr.length;
				currResult.add(curr);
				
				it.remove();
				
				if(currBytes >= bytesToRemove) {
//					System.out.println("breaking: "+currBytes+" "+bytesToRemove);
					break;
				}
				
			}
			
			if(currBytes < bytesToRemove) {
				throw new RuntimeException("Less than the expected number of bytes were returned. "+currBytes+" "+bytesToRemove);
			}
		}
		
//		System.out.println("remove bytes2: "+bytesToRemove);
		
		
		byte[] result = new byte[bytesToRemove];
		
		int resultIndex = 0;
		for(int x = 0; x < currResult.size(); x++) {
			byte[] barr = currResult.get(x);
			
			int bytesUnfilledInResult = result.length - resultIndex;
			
			int bytesToCopy = Math.min(bytesUnfilledInResult, barr.length);
			
			System.arraycopy(barr, 0, result, resultIndex, bytesToCopy);
			
			resultIndex += bytesToCopy;
			
			// If this is the last barr in the list, and it still has bytes left that are not being returned, then we need to add it back
			if(barr.length > bytesToCopy) {
//				System.out.println("triggered fine if.");
				
				// Must be the last item in the barr list, otherwise it's an error
				if(x+1 != currResult.size()) {
					throw new RuntimeException("The array may only be larger if it is the last array in the list.");
				}
				
				// There are still bytes left in barr, so add them back to the front of the list
				byte[] newBarr = new byte[barr.length-bytesToCopy];
				System.arraycopy(barr, bytesToCopy, newBarr, 0, newBarr.length);
//				currResult.add(0, newBarr);
				_contents.add(0, newBarr);
				
//				// We break here to keep from continuing to loop (if we did not break the loop would continue as a new entry was just added to the list)
//				break;
			}
			
		}

		_size -= result.length;
		
//		System.out.println("size is down to: "+_size);
		
//		System.out.println("remove bytes out. "+bytesToRemove);
		
		return result;
		
	}
	
//	public byte[] extractAndRemove(int length) {
//		byte[] result = new byte[length];
//		
//		System.arraycopy(_contents, 0, result, 0, length);
//		remove(length);
//		
//		return result;
//	}
	
}
