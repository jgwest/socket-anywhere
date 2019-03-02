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

package com.socketanywhere.multiplexing;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Simple data multiplexing test */
public class MultTest {

//	private static TCPSocketFactory _tcpFactory = new TCPSocketFactory();
//	private static NBSocketFactory _tcpFactory = new NBSocketFactory();
	
	
	public static void main(String[] args) {
		
		
		new Thread() {
			public void run() {
				startServer();
			};
			
		}.start();
		
		try { Thread.sleep(2000); } catch (InterruptedException e) {}
//		
		startClient();
		System.out.println("Done!");
	}
	
	public static void startClient() {
		try {
			System.out.println("Client socket connecting. ");
						
			MultSocketFactory factory 
				= MultSocketFactory.createClientSocketFactory(new TLAddress("localhost", 22006), new TCPSocketFactory());
			
			ISocketTL s2 = factory.instantiateSocket(new TLAddress("localhost", 22007));
			
			System.out.println("Client socket established.");
			
			int c = 0;
			while(c < 100) {
				s2.getOutputStream().write(("Hi from client!! ["+c+"]\n").getBytes());
				c++;
				
//				if(c == 20) {
//					NBSocketImpl inner = (NBSocketImpl)s;
//					inner._inner.close();
//				}
				
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			}
			s2.close();
			
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void startServer() {
		try {

			MultSocketFactory factory = MultSocketFactory.createServerSocketFactory(new TLAddress(22006), new TCPSocketFactory());
			System.out.println("Listening on mult server socket");
			IServerSocketTL ss2 = factory.instantiateServerSocket(new TLAddress(22007));
			ISocketTL sock2 = ss2.accept();
			System.out.println("Listening on mult server socket accepted");
			
			
			while(true) {
				byte[] data = new byte[16384];
				
				try { Thread.sleep(3000); } catch (InterruptedException e) { }
				int c = sock2.getInputStream().read(data);
				if(c != -1) {
					System.out.println(new String(data, 0, c));
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
}
