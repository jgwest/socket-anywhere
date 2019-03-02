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

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.ServerSocketTL;
import com.socketanywhere.net.SocketTL;
import com.socketanywhere.net.SocketTLFactory;
import com.socketanywhere.net.TLAddress;
import com.vfile.VFile;

public class ClientServerTest {

	public static void main(String[] args) {
		SocketTLFactory.setDefaultFactory(new FileTLFactory(new VFile("C:\\temp\\filetl")));
		
		new Thread() {
			@Override
			public void run() {
				client();
			}
		}.start();
		
		new Thread() {
			@Override
			public void run() {
				server();
			}
		}.start();
	}
	
	public static void client() {
		FileTLUtil.sleep(0000);
		
		try {
			ISocketTL s = new SocketTL(new TLAddress("localhost", 10000));
			s.getOutputStream().write(new String("Hi world! ;) \n").getBytes());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void server() {
		try {
			IServerSocketTL ss = new ServerSocketTL(new TLAddress(10000));
			
			ISocketTL s = ss.accept();
			Thread.sleep(2000);
			
			byte[] stuff = new byte[32768];
			s.getInputStream().read(stuff);
			System.out.println("out:"+(new String(stuff)).trim());
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
