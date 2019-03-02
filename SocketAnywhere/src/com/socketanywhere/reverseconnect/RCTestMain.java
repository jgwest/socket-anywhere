/*
	Copyright 2013, 2019 Jonathan West

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

package com.socketanywhere.reverseconnect;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class RCTestMain {

	public static void main(String[] args) {

		RCTestThread1 t1 = new RCTestThread1();
		t1.start();

		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		RCTestThread2 t2 = new RCTestThread2();
		t2.start();
		
	}
}

class RCTestThread1 extends Thread {

	@Override
	public void run() {
		
		HopperSocketAcquisition hsa = new HopperSocketAcquisition(new TCPSocketFactory());
		hsa.setInnerListenAddr(new TLAddress("localhost", 15678));
		
		ISocketFactory factory = RCSocketFactory.createClientSocketFactory(hsa);

		try {
			ISocketTL sock = factory.instantiateSocket(new TLAddress("host", 12));
			
			sock.getOutputStream().write("Hi!".getBytes());
			
			System.out.println("wrote.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}
}

class RCTestThread2 extends Thread {
	@Override
	public void run() {
		HopperSocketAcquisition hsa = new HopperSocketAcquisition(new TCPSocketFactory());
		hsa.setInnerConnectAddr(new TLAddress("localhost", 15678));
		
		ISocketFactory factory = RCSocketFactory.createServerSocketFactory(hsa);
		
		try {
			IServerSocketTL s = factory.instantiateServerSocket(new TLAddress("host", 12));
			ISocketTL sock = s.accept();
			
			byte[] barr = new byte[2048];
			sock.getInputStream().read(barr);
			
			System.out.println("["+new String(barr).trim()+"]");
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

				
	}
}