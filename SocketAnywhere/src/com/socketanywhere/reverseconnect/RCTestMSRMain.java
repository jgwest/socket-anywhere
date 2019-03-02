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

package com.socketanywhere.reverseconnect;

import com.socketanywhere.multisendreceive.TestDirector;
import com.socketanywhere.multisendreceive.TestLog;
import com.socketanywhere.multisendreceive.tests.TestBrain5FullRandom;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Launcher for the multisendreceive test. */
public class RCTestMSRMain {

	public static final boolean DEBUG = true;
	
	public static void println(String s) {
		System.out.println(s);
	}
	
	
	public static void main(String[] args) {
		
		ISocketFactory outsideListener = null;
		ISocketFactory outsideConnector = null;
		
		try {
			HopperSocketAcquisition outsideListenerHsa = new HopperSocketAcquisition(new TCPSocketFactory());
			outsideListenerHsa.setInnerListenAddr(new TLAddress("localhost", 18909));
			outsideListener = RCSocketFactory.createClientSocketFactory(outsideListenerHsa);
			
			HopperSocketAcquisition outsideConnectorHsa = new HopperSocketAcquisition(new TCPSocketFactory());
			outsideConnectorHsa.setInnerConnectAddr(new TLAddress("localhost", 18909));
			outsideConnector = RCSocketFactory.createServerSocketFactory(outsideConnectorHsa);
			
		} catch(Exception e) { 
			e.printStackTrace();
		}
		
		
		// Setup first tester to listen on 9999
		TestLog testLogOne = new TestLog();
		final TestDirector one = new TestDirector(outsideConnector, testLogOne, "localhost", "localhost");
		testLogOne.setLocalDirectorId(one.getLocalDirectorId());
		one.setDirectorSocketFactory(new TCPSocketFactory());
		
		TestBrain5FullRandom brainOne = new TestBrain5FullRandom(one, testLogOne);
		testLogOne.setBrain(brainOne);
		brainOne.INITIAL_LISTEN_PORTS = 5;
		brainOne.INITIAL_TEST_AGENTS = 0;
		brainOne.MAX_AGENT_DATA_TO_SEND_PER_ROUND = 2048;
		brainOne.TEST_TIME = 60 * 1 * 1000;
		brainOne.SLEEP_RAND_BETWEEN_ROUNDS = 50;
		brainOne.PRE_AND_POST_TEST_PROCESS_WAIT_TIME = 100;
		
		brainOne.AGENT_DISC_PERCENT = 0.0;
		brainOne.LISTEN_ON_NEW_PORT_PERCENT = 0.05;
		brainOne.STOP_SERVER_AGENT_LISTEN_ON_PORT_PERCENT = 0.00;
		brainOne.CONN_TO_NEW_PORT_PERCENT = 0.0;

		
		one.setTestBrain(brainOne);
		new Thread() {
			public void run() {
				one.directorListenOnPort(9999);
			}
		}.start();
		
		// Setup second tester to connect to first
		TestLog testLogTwo = new TestLog();
		TestDirector two = new TestDirector(outsideListener, testLogTwo, "localhost", "localhost");
		testLogTwo.setLocalDirectorId(two.getLocalDirectorId());
		two.setDirectorSocketFactory(new TCPSocketFactory());
		TestBrain5FullRandom brainTwo = new TestBrain5FullRandom(two, testLogTwo);
		testLogTwo.setBrain(brainTwo);
		brainTwo.INITIAL_LISTEN_PORTS = 0;
		brainTwo.INITIAL_TEST_AGENTS = 5;
		brainTwo.MAX_AGENT_DATA_TO_SEND_PER_ROUND = 2048;
		brainTwo.TEST_TIME = 60 * 1 * 1000;
		brainTwo.SLEEP_RAND_BETWEEN_ROUNDS = 50;
		brainTwo.PRE_AND_POST_TEST_PROCESS_WAIT_TIME = 100;

		brainTwo.AGENT_DISC_PERCENT = 0.05;
		brainTwo.LISTEN_ON_NEW_PORT_PERCENT = 0.0;
		brainTwo.STOP_SERVER_AGENT_LISTEN_ON_PORT_PERCENT = 0.0;
		brainTwo.CONN_TO_NEW_PORT_PERCENT = 0.05;

		two.setTestBrain(brainTwo);
		
		two.directorConnect(two.getRemoteAddress(), 9999);
	}
}
