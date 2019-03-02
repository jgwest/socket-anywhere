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

package com.socketanywhere.multisendreceive.tests;

import java.util.List;
import java.util.Random;

import com.socketanywhere.multisendreceive.MultiSendReceiveMain;
import com.socketanywhere.multisendreceive.ServerAgent;
import com.socketanywhere.multisendreceive.TestAgent;
import com.socketanywhere.multisendreceive.TestBrain;
import com.socketanywhere.multisendreceive.TestDirector;
import com.socketanywhere.multisendreceive.TestLog;

/** A test brain instance, whose behaviour is determined by an RNG */
public class TestBrain5Random2 extends TestBrain {

	private Random _random;
	
	public TestBrain5Random2(TestDirector director, TestLog testLog) {
		super(director, testLog);
		_random = new Random();
	}
		
	public void run() {
		TestDirector td = getDirector();
		
		// Listen on 5 random ports
		for(int x = 0; x < 20; x++) {
			int port = 10000 + (int)(_random.nextDouble()*20000);
			ServerAgent sa = new ServerAgent(td.getSocketFactory(), td, this, getTestLog(), td.getLocalAddress(), port);
			sa.startListening();
			addServerAgent(sa);
		}
		
		sleep(5000);

		int agentsCreated = 0;
		while(agentsCreated < 20) {
			List<Integer> ports = td.getPortsAvailableToConnectTo();

			MultiSendReceiveMain.println(""+ports.size());
			
			if(ports != null && ports.size() > 0) {
				Integer port = ports.get((int)(ports.size() * _random.nextDouble()));
				
				TestAgent a = new TestAgent(td.getSocketFactory(), td.getNextAgentId(), getTestLog(), false);
				a.connect(td.getRemoteAddress(), port);
				synchronized(getAgents()) {
					getAgents().add(a);
				}
				agentsCreated++;
				MultiSendReceiveMain.println("agents created");
			}
			sleep(250);
		}
		
		long startTime = System.currentTimeMillis();
		
		byte[] bytearr = new byte[16384]; 
		while(System.currentTimeMillis() - startTime <= 60 * 1000) {
			
			int agentToSendDataOn = (int)(getAgents().size() * _random.nextDouble());
			
			TestAgent ta = getAgents().get(agentToSendDataOn);
			_random.nextBytes(bytearr);
			int bytesToSend = _random.nextInt(bytearr.length);
			
			ta.send(bytearr, 0, bytesToSend);
			
			sleep(_random.nextInt(100));
		}

		MultiSendReceiveMain.println("Sleeping 5 seconds to give time to communicate remaining data");
		sleep(5000);
		
		// Disconnect remaining agents
		for(int x = 0; x < getAgents().size(); x++) {
			TestAgent ta = getAgents().get(x);
			ta.disconnect();
		}
		
		MultiSendReceiveMain.println("Sleeping 5 seconds to give time to communicate disconnects");
		sleep(5000);
				
		MultiSendReceiveMain.println("test brain complete");
		
		getDirector().informTestBrainComplete();
		
	}
	
}
