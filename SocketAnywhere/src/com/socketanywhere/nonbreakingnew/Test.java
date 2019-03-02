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

package com.socketanywhere.nonbreakingnew;

import java.io.IOException;

import com.socketanywhere.forwarder.ForwarderThread;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.cmd.CmdCloseConn;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Simple test of non-breaking socket factory. Run one main in one process, and the other main in the other;
 * they will connect to each other and the test will proceed.*/
public class Test {

	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		
		
		TCPSocketFactory ssf = new TCPSocketFactory();
		
		NBSocketFactory nbf1 = new NBSocketFactory(new TCPSocketFactory());
		NBSocketFactory nbf2 = new NBSocketFactory(new TCPSocketFactory());
//		TCPSocketFactory nbf = new TCPSocketFactory();
		
		IServerSocketTL listen;
		ForwarderThread ft;
		
		try {
			
			listen = ssf.instantiateServerSocket(new TLAddress(1000));
			ft = new ForwarderThread(listen, nbf1, new TLAddress("hostname", 8002)); 
			ft.start();
			
//			listen = nbf2.instantiateServerSocket(new TLAddress("localhost", 8001));
//			ft = new ForwarderThread(listen, new TCPSocketFactory(), new TLAddress("hostname, 3389));
//			ft.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	
	public static void main2(String[] args) {
//		
//		new Thread() {
//			public void run() {
//				startServer();
//			};
//			
//		}.start();
//		
//		try { Thread.sleep(2000); } catch (InterruptedException e) {}
//		
		startClient();
		System.out.println("Done!");
	}
	
	public static void startClient() {
		try {
			System.out.println("Client socket connecting. ");
			NBSocketFactory factory = new NBSocketFactory(new TCPSocketFactory());
			
			ISocketTL s = factory.instantiateSocket(new TLAddress("hostname", 22006)); 
			
			System.out.println("Client socket established.");
			
			int c = 0;
			while(c < 100) {
				s.getOutputStream().write(("Hi from client!! ["+c+"]\n").getBytes());
				c++;
				
//				if(c == 20) {
//					NBSocketImpl inner = (NBSocketImpl)s;
//					inner._inner.close();
//				}
				
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			}
			s.close();
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void startServer() {
		try {
			NBSocketFactory factory = new NBSocketFactory(new TCPSocketFactory());
			IServerSocketTL ss = factory.instantiateServerSocket(new TLAddress(22006));
			System.out.println("Server socket calling accept.");
			ISocketTL sock = ss.accept();
			System.out.println("Socket accepted.");
			
			while(true) {
				byte[] data = new byte[16384];
				
				try { Thread.sleep(3000); } catch (InterruptedException e) { }
				int c = sock.getInputStream().read(data);
				if(c != -1) {
					System.out.println(new String(data, 0, c));
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
	public static void checkCmd() {
		
		CmdCloseConn c = new CmdCloseConn("lol", 10);
		byte[] cmd = c.buildCommand();
		
		CmdCloseConn c2 = new CmdCloseConn();
		c2.parseCommand(cmd);
		
		System.out.println(c2);
		
	}
}
