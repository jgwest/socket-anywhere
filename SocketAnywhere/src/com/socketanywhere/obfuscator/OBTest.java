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

package com.socketanywhere.obfuscator;

import java.util.Random;

public class OBTest {

	public static void main(String[] args) {
//		SimpleDataTransformer dt = new SimpleDataTransformer();
//		
//		byte[] data = new String("Hi!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!").getBytes();
//		
//		dt.encrypt(data, 0, data.length);
//		dt.decrypt(data, 0, data.length);
//
//		dt.encrypt(data, 0, data.length);
//		dt.decrypt(data, 0, data.length);
//		
//		System.out.println(new String(data));
		
		Random r = new Random(12);

		for(int x = 0 ; x < 4; x++) {
			byte[] b = new byte[1];
			r.nextBytes(b);
			System.out.println(b[0]);
		}
		
		
		System.out.println("-------------------------");
		
		r = new Random(12);
		
		byte[] d1 = new byte[4];
		r.nextBytes(d1);
		
		for(int x = 0; x < d1.length; x++) {
			System.out.println(d1[x]);
		}
		
		
		
		
		
		
	}
}
