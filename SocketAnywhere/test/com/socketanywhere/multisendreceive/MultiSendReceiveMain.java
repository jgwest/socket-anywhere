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

package com.socketanywhere.multisendreceive;

import java.util.Map;

import com.socketanywhere.multisendreceive.tests.TestBrain5FullRandom;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ITaggedSocketTL;
import com.socketanywhere.nonbreakingnew.NBLog;
import com.socketanywhere.nonbreakingnew.NBSocketFactory;
import com.socketanywhere.socketfactory.TCPSocketFactory;
import com.socketanywhere.unreliable.URCentralInfo;
import com.socketanywhere.unreliable.URCentralInfo.INukeConnection;
import com.socketanywhere.unreliable.URInputStream;
import com.socketanywhere.unreliable.UROutputStream;
import com.socketanywhere.unreliable.URSocketFactory;

/** Launcher for the multisendreceive test. */
public class MultiSendReceiveMain {

	public static final boolean DEBUG = false;
	
	public static void println(String s) {
		System.out.println(s);
//		TestAgent.fp(s);
	}
	
	
	public static void main(String[] args) {
		
//		ISocketFactory socketFactory = new TCPSocketFactory();
//		ISocketFactory socketFactory = new FileTLFactory(new VFile("c:\\temp\\shared"));

//		ISocketFactory nbFactory1 = new NBSocketFactory();
//		ISocketFactory nbFactory2 = new NBSocketFactory();

//		ISocketFactory nbFactory1 = new SSLSocketFactory();
//		ISocketFactory nbFactory2 = new SSLSocketFactory();
//
	
		ISocketFactory rFactory1 = null;
		ISocketFactory rFactory2 = null;
		
		try {
		
//			ArrayList<IDataTransformer> transformers = new ArrayList<IDataTransformer>();
//			
//			transformers.add(new OneTimePadFileTransformer(new File("c:\\cpsweb.log")));
////			transformers.add(new PseudorandomDataTransformer());
//			transformers.add(new SimpleShiftDataTransformer(12));


//			ISocketFactory obFactory1 = new TCPSocketFactory();
//			ISocketFactory obFactory2 = new TCPSocketFactory();
			
//			ISocketFactory obFactory1 = new OBSocketFactory(transformers, new TCPSocketFactory());
//			ISocketFactory obFactory2 = new OBSocketFactory(transformers, new TCPSocketFactory());
			
//			ISocketFactory obFactory1 = new PTSocketFactory(new TCPSocketFactory());
//			ISocketFactory obFactory2 = new PTSocketFactory(new TCPSocketFactory());
			
//			ISocketFactory sslFactory1 = new SSLSocketFactory(obFactory1);
//			ISocketFactory sslFactory2 = new SSLSocketFactory(obFactory2);
//			
//			ISocketFactory nbFactory1 = new NBSocketFactory(sslFactory1);
//			ISocketFactory nbFactory2 = new NBSocketFactory(sslFactory2);

//		

			// --------------------------
			
//			rFactory1 = MultSocketFactory.createClientSocketFactory(new TLAddress("localhost", 22006), new TCPSocketFactory());
//			rFactory2 = MultSocketFactory.createServerSocketFactory(new TLAddress("localhost", 22006), new TCPSocketFactory());
//
//			rFactory1 = new PTSocketFactory(rFactory1, "rFactory1");
//			rFactory2 = new PTSocketFactory(rFactory2, "rFactory2");
			

			// --------------------------
//			rFactory1 = nbFactory1;
//			rFactory2 = nbFactory2;

//			TMSocketFactory tmSocketFactory1 = new TMSocketFactory(new TCPSocketFactory());
//			TMSocketFactory tmSocketFactory2 = new TMSocketFactory(new TCPSocketFactory());
//			
//			rFactory1 = tmSocketFactory1;
//			rFactory2 = tmSocketFactory2;
			
//			rFactory1 = new TCPSocketFactory();
//			rFactory2 = new TCPSocketFactory();

			URCentralInfo urci = new URCentralInfo(new INukeConnection() {
				
				
				
				@Override
				public void eventReceivedData(URInputStream stream, byte[] data, int off, int len) {
					if(Math.random() <= 0.1) {
						try {
							Long id = null;
							if(stream.getOurSocket() instanceof ITaggedSocketTL) {
								Map<String, Object> m = ((ITaggedSocketTL)stream.getOurSocket()).getTagMap();
								synchronized(m) {
									id = (Long)m.get("id");
								}
							}
							NBLog.err("!!!!!!!!!!!!!!!!!!!!!!!!!!!! Nuking "+id+" !!!!!!!!!!!!!!!!!!!!!!!!	");
							stream.getOurSocket().getInnerSocket().close();
						} catch (Exception e) {
//							e.printStackTrace();
						}
					}
				}
				
				@Override
				public void eventAboutToSendData(UROutputStream stream, byte[] data, int off, int len) {
					if(Math.random() <= 0.1) {
						try {
							Long id = null;
							if(stream.getOurSocket() instanceof ITaggedSocketTL) {
								Map<String, Object> m = ((ITaggedSocketTL)stream.getOurSocket()).getTagMap();
								synchronized(m) {
									id = (Long)m.get("id");
								}
							}

							NBLog.err("!!!!!!!!!!!!!!!!!!!!!!!!!!!! Nuking "+id+" !!!!!!!!!!!!!!!!!!!!!!!!	");
							stream.getOurSocket().getInnerSocket().close();
						} catch (Exception t) {
//							e.printStackTrace();
						}
					}
					
				}
			});
			
			URSocketFactory urf = new URSocketFactory(new TCPSocketFactory(), "urf", urci);
			
			
			NBSocketFactory nbFactory1 = new NBSocketFactory(urf);
			NBSocketFactory nbFactory2 = new NBSocketFactory(urf);
			
			nbFactory1.getOptions().setRecoveryThreadTimeToWaitBtwnFailures(0);
			nbFactory1.getOptions().setMaxDataReceivedBuffer(1024);
			nbFactory2.getOptions().setRecoveryThreadTimeToWaitBtwnFailures(0);
			nbFactory2.getOptions().setMaxDataReceivedBuffer(1024);
			
			rFactory1 = nbFactory1;
			rFactory2 = nbFactory2;
			
			
//			rFactory1 = MNSocketFactory.createServerSocketFactory(new TLAddress(11080), rFactory1);
//			rFactory2 = MNSocketFactory.createClientSocketFactory(new TLAddress("localhost", 11080), rFactory2);
			
			
		} catch(Exception e) { 
			e.printStackTrace();
		}
		
		
		// Setup first tester to listen on 9999
		TestLog testLogOne = new TestLog();
		final TestDirector one = new TestDirector(rFactory1, testLogOne, "localhost", "localhost");
		testLogOne.setLocalDirectorId(one.getLocalDirectorId());
		one.setDirectorSocketFactory(new TCPSocketFactory());
		
		TestBrain5FullRandom brainOne = new TestBrain5FullRandom(one, testLogOne);
		testLogOne.setBrain(brainOne);
		brainOne.INITIAL_LISTEN_PORTS = 5;
		brainOne.INITIAL_TEST_AGENTS = 5;
		brainOne.MAX_AGENT_DATA_TO_SEND_PER_ROUND = 2048;
		brainOne.TEST_TIME = 2 * 60 * 1000;
		brainOne.SLEEP_RAND_BETWEEN_ROUNDS = 50;
		brainOne.PRE_AND_POST_TEST_PROCESS_WAIT_TIME = 30 * 1000;
		
		brainOne.AGENT_DISC_PERCENT = 0.1;
		brainOne.LISTEN_ON_NEW_PORT_PERCENT = 0.00;
		brainOne.STOP_SERVER_AGENT_LISTEN_ON_PORT_PERCENT = 0.00;
		brainOne.CONN_TO_NEW_PORT_PERCENT = 0.1;

		
		one.setTestBrain(brainOne);
		new Thread() {
			public void run() {
				one.directorListenOnPort(9999);
			}
		}.start();
		
		// Setup second tester to connect to first
		TestLog testLogTwo = new TestLog();
		TestDirector two = new TestDirector(rFactory2, testLogTwo, "localhost", "localhost");
		testLogTwo.setLocalDirectorId(two.getLocalDirectorId());
		two.setDirectorSocketFactory(new TCPSocketFactory());
		TestBrain5FullRandom brainTwo = new TestBrain5FullRandom(two, testLogTwo);
		testLogTwo.setBrain(brainTwo);
		brainTwo.INITIAL_LISTEN_PORTS = 5;
		brainTwo.INITIAL_TEST_AGENTS = 5;
		brainTwo.MAX_AGENT_DATA_TO_SEND_PER_ROUND = 2048;
		brainTwo.TEST_TIME = 2 * 60 * 1000;
		brainTwo.SLEEP_RAND_BETWEEN_ROUNDS = 50;
		brainTwo.PRE_AND_POST_TEST_PROCESS_WAIT_TIME = 30 * 1000;

		brainTwo.AGENT_DISC_PERCENT = 0.1;
		brainTwo.LISTEN_ON_NEW_PORT_PERCENT = 0.0;
		brainTwo.STOP_SERVER_AGENT_LISTEN_ON_PORT_PERCENT = 0.0;
		brainTwo.CONN_TO_NEW_PORT_PERCENT = 0.1;

		two.setTestBrain(brainTwo);
		
		two.directorConnect(two.getRemoteAddress(), 9999);
	}
}
