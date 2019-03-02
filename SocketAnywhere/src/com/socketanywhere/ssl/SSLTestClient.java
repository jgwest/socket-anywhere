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

package com.socketanywhere.ssl;
import java.io.IOException;
import java.net.UnknownHostException;

import com.socketanywhere.net.TLAddress;



public class SSLTestClient extends Thread {

	@Override
	public void run() {
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			SSLSocketImpl s = new SSLSocketImpl(new TLAddress("localhost", 1556), SSLTest._factory2, "C:\\temp\\ks\\samplekeystore"); 
			
			while(true) {
				byte[] b = new byte[1024];
				int c = s.getInputStream().read(b);
				System.out.print(new String(b, 0, c));
			}
			
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
}
