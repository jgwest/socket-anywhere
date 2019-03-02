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

package com.socketanywhere.irc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Standalone IRC test */
public class IRCTest {

	public static Object listening = new Object();
	public static boolean isReady = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Thread() {
			public void run() {
				server();
			}
		}.start();
		
		client();
		try { Thread.sleep(10000); } catch (InterruptedException e) { }
	}
	
	public static void server() {
		ISocketFactory f = new IRCSocketFactory();
		
		try {
			IServerSocketTL ss = f.instantiateServerSocket(new TLAddress(10002));
			
			synchronized(listening) {
				isReady = true;
				IRCTest.listening.notifyAll();
			}

			ISocketTL s = ss.accept();
			
			InputStream is = s.getInputStream();
			
			while(true) {
				byte[] result = new byte[8192];
				System.out.println("Reading.....");
				int x = is.read(result);
				String resultStr = new String(result, 0, x);
				System.out.println("["+resultStr+"]");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void client() {
		ISocketFactory f = new IRCSocketFactory();
		
		try {

			if(!isReady) {
				synchronized(IRCTest.listening) {
					try {
						System.out.println("waiting");
						IRCTest.listening.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			ISocketTL s = f.instantiateSocket();
			
			s.connect(new TLAddress("hostname", 10002)); 
			
			int count = 0;
			Random r = new Random();
			while(true) {
				int x = r.nextInt(2 * 1000 * 1000 * 1000);
				
				System.out.println("writing:"+System.currentTimeMillis() + " ("+count+++")");
				s.getOutputStream().write(("number "+x).getBytes());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
					
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
