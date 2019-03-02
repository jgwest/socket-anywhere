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

package com.socketanywhere.filefactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketTest {
	
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Listening on 1004");

		
		ServerSocket ss = new ServerSocket(1004);
		System.out.println(ss.getReceiveBufferSize());
		
		ss.accept();
		
		boolean continueLoop = true;
		while(continueLoop) {
//			System.out.println("st");
			Thread.sleep(1000);
		}
		
		ss.close();
	}

}

class M1 {
	public static void main(String[] args) {
		try {
			Thread.sleep(5000);
			Socket s = new Socket("127.0.0.1", 1004);
			
			System.out.println("Connected.");
			
			byte thing[] = new byte[32768];
			for(int x =0; x < thing.length; x++) {
				thing[x] = (byte)x;
			}
			
			OutputStream os = s.getOutputStream();
			
			boolean continueLoop = true;
			while(continueLoop) {
				os.write(thing);
			}
			
			s.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}