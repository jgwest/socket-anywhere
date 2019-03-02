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

import java.util.concurrent.atomic.AtomicLong;

public class ByteHolderDuo {
	
	private ByteHolder one;
	private ByteHolderList two;
	
	private static final AtomicLong al = new AtomicLong(0);
	
	private long id;
	
	public ByteHolderDuo() {
		synchronized(al) {
			id = al.getAndIncrement();
		}
		
		one = new ByteHolder();
		two = new ByteHolderList();
	}

	public void addBytes(byte[] value) {
		
		assertSize("ab1");
		
		one.addBytes(value);
		two.addBytes(value);
		
		assertSize("ab2");
		
	}
	
	public void addBytes(byte[] value, int offset, int length) {
		
		assertSize("aba1");
		
		one.addBytes(value, offset, length);
		two.addBytes(value, offset, length);
		
		assertSize("aba2");
	}
	
	private void assertSize(String debug) {
		int x = one.getContentsSize();
		int y = two.getContentsSize();
		
		if(x != y) {
			System.out.println("("+Thread.currentThread().getId()+") not fine: "+debug + " ["+id+"] "+x +" "+y);
			System.exit(0);
//			System.err.println("hi!");
		} else {
//			System.out.println("("+Thread.currentThread().getId()+") fine: "+debug + " ["+id+"] "+x +" "+ y);
		}
	
	}
	
	public int getContentsSize() {
		assertSize("gcs");
		
		int x = one.getContentsSize();
		int y = two.getContentsSize();
		
		if(x != y) {
			System.out.println("("+Thread.currentThread().getId()+") get contents size mismatch: ["+id+"] "+x +" "+y);
			System.exit(0);
		} else {
//			System.out.println("("+Thread.currentThread().getId()+") get contents size is fine: ["+id+"] "+x +" "+y);
		}
		
		return x;
		
	}
	
//	public byte[] getContents() {
//	}
	
	public byte[] extractAndRemove(int length) {
		
		try {
			assertSize("ear1");
			
			byte[] x = one.extractAndRemove(length);
			byte[] y = two.extractAndRemove(length);
			
			assertSize("ear2");

			
			if(x.length != y.length) {
				System.err.println("extract and remove failed.");
				System.exit(0);
			}
			
			compareBytes(x, y);
			
			return x;

		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(0);
			return null;
		}
		
	}
	
	
	private static void compareBytes(byte[] one, byte[] two) {
		
		boolean fail = false;
		
		if(one.length != two.length) {
			fail = true;
		}
		
		if(!fail) {
			
			for(int x =0; x < one.length;x ++) {
				byte a = one[x];
				byte b = two[x];
				
				if(a != b) {
					fail = true;
					break;
				}
				
			}
			
		}
		
		if(fail) {
			System.err.println("Compare bytes failed.");
			System.exit(0);
		}
		
	}
	

}
