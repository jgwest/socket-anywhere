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

package com.socketanywhere.sendreceive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.socketanywhere.filefactory.FileTLFactory;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.vfile.VFile;

/** Purpose of this test is to send/receive data simultaneously on a single connection, and verify
 * the data as it is sent/received. Both sides of the connection send/receive at the same time.  */
public class TestRunner extends TestCase { 
	
	private static ISocketFactory _factory = null;
	
	// TODO: MEDIUM - Fix the factory on this test
//	
//	public static ISocketFactory getFactory() {
//		return _factory;
//	}
	
	private static ServerSocketGenerator generateServerSocket() throws IOException {
		ServerSocketGenerator g = new ServerSocketGenerator(_factory, new TLAddress(10012));
		g.start();
		
		return g;
	}
	
	private static ISocketTL generateConnectorSocket() throws IOException {
		ISocketTL s = _factory.instantiateSocket(new TLAddress("jgw", 10012)); 
		return s;
	}
	
	private static void blockUntilServerIsReady(ServerSocketGenerator g) {
		try {
			while(!g.isServerReady()) {
				Thread.sleep(1000);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean waitForErrorOrTimeout(List<TestLog> list, long timeout) {
		
		long start = System.currentTimeMillis();
		
		while(System.currentTimeMillis() - start <= timeout) {
			
			for(TestLog t : list) {
				synchronized(t.getTestLogLock()) {
					if(t.getErrorLog().size() != 0) {
						return false;
					}
				}
			}
			
			try { Thread.sleep(1000); } catch (InterruptedException e) { return false; }
		}
		
		return true;
		
	}
	
	public void testOne() throws IOException {
//		_factory = new IRCSocketFactory();
//		_factory = new SocketSocketFactory();
		_factory = new FileTLFactory(new VFile("c:\\delme\\filetl"));
		
		ServerSocketGenerator g = generateServerSocket();
		blockUntilServerIsReady(g);
		
//		_factory = new IRCSocketFactory();
		
		ISocketTL connectorSock = generateConnectorSocket();
		ISocketTL receiverSock = g.getResult();
		
		TestLog tl1 = new TestLog();
		TestLog tl2 = new TestLog();
		ArrayList<TestLog> al = new ArrayList<TestLog>();
		al.add(tl1); al.add(tl2); 
		
		SingleSocketReceiver r1 = new SingleSocketReceiver(connectorSock, tl1);
		r1.start();
		
		SingleSocketReceiver r2 = new SingleSocketReceiver(receiverSock, tl2);
		r2.start();
		
		SingleSocketSender s1 = new SingleSocketSender(receiverSock, tl1, 100 * 384/3, 2000);
		s1.start();

		SingleSocketSender s2 = new SingleSocketSender(connectorSock, tl2, 100 *384/3, 2000);
		s2.start();
		
		boolean result = waitForErrorOrTimeout(al, 180000);
		
		r1.end();
		r2.end();
		s1.end();
		s2.end();
		
		assertTrue(result);
		
	}


	
}
