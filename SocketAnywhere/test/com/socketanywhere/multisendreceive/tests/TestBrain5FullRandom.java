/*
	Copyright 2012, 2019 Jonathan West

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.socketanywhere.multisendreceive.MultiSendReceiveMain;
import com.socketanywhere.multisendreceive.PortListenUtil;
import com.socketanywhere.multisendreceive.ServerAgent;
import com.socketanywhere.multisendreceive.TestAgent;
import com.socketanywhere.multisendreceive.TestBrain;
import com.socketanywhere.multisendreceive.TestDirector;
import com.socketanywhere.multisendreceive.TestLog;
import com.socketanywhere.net.ByteHolder;
import com.socketanywhere.nonbreakingnew.Mapper;
import com.socketanywhere.nonbreakingnew.Triplet;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;

/** A test brain instance, whose behaviour is determined by an RNG */
public class TestBrain5FullRandom extends TestBrain {
	
	private Random _random;
	private PortListenUtil _portUtil;
	
	// Test Parameters:
	public int INITIAL_LISTEN_PORTS = 20;
	public int INITIAL_TEST_AGENTS = 20;
	
	public int TEST_TIME = 60 * 1000;
	
	public int MAX_AGENT_DATA_TO_SEND_PER_ROUND = 10 * 1024; // 16 passes well, 17 nearly completely fails
	
	public int SLEEP_RAND_BETWEEN_ROUNDS = 100;
	
	public int PRE_AND_POST_TEST_PROCESS_WAIT_TIME = 5000;

	/** % of time agent will disconnect instead of sending data*/
	public double AGENT_DISC_PERCENT = 0.03;

	/** % of time we will start listening on a new port */
	public double LISTEN_ON_NEW_PORT_PERCENT = 0.02;

	/** % of time we will stop listening on a new port */
	public double STOP_SERVER_AGENT_LISTEN_ON_PORT_PERCENT = 0.02;

	/** % of time we will connect to a new port */
	public double CONN_TO_NEW_PORT_PERCENT = 0.02;



	
	public TestBrain5FullRandom(TestDirector director, TestLog testLog) {
		super(director, testLog);
		
		_portUtil = new PortListenUtil();
		_random = new Random();
	}
	
	public void run() {

//		System.out.println(Thread.currentThread().getId() + "> 1");
		
		TestDirector td = getDirector();
		
		// Listen on INITIAL_LISTEN_PORTS random ports
		for(int x = 0; x < INITIAL_LISTEN_PORTS; x++) {
			int port = _portUtil.getNextPortToListenOn();
			ServerAgent sa = new ServerAgent(td.getSocketFactory(), td, this, getTestLog(), td.getLocalAddress(), port);
			sa.startListening();
			
			while(sa.getListenOnPortResult() == null) {
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			}
			// Only add server agents to the SA list if they were able to successfully listen on their port			
			if(sa.getListenOnPortResult() == true) {
				addServerAgent(sa);				
			}
		}
		
		sleep(PRE_AND_POST_TEST_PROCESS_WAIT_TIME);

		// Create INITIAL_TEST_AGENTS agents, and have them randomly connect to available remote ports
		int agentsCreated = 0;
		while(agentsCreated < INITIAL_TEST_AGENTS) {
			List<Integer> ports = td.getPortsAvailableToConnectTo();
		
			if(ports != null && ports.size() > 0) {
				Integer port = ports.get((int)(ports.size() * _random.nextDouble()));
				
				TestAgent a = new TestAgent(td.getSocketFactory(), td.getNextAgentId(), getTestLog(), false);
				a.connect(td.getRemoteAddress(), port);
				addTestAgent(a);
				
				agentsCreated++;
				MultiSendReceiveMain.println("agents created ["+_brainUUID+"]");
			}
			sleep(250);
		}
		
		long startTime = System.currentTimeMillis();
		
		
		byte[] bytearr = new byte[MAX_AGENT_DATA_TO_SEND_PER_ROUND]; 
		while(System.currentTimeMillis() - startTime <= TEST_TIME) {
			// Test lasts at most TEST_TIME seconds

//			NBLog.out("maybe: new round. ["+_brainUUID+"]");
			
			List<TestAgent> agentsToRemove = new ArrayList<TestAgent>();
			
			// For each of the agents...
			for(int x = 0; x < getAgents().size(); x++) {
//			for(TestAgent ta : getAgents()) {
				TestAgent ta = getAgents().get(x);
				
//				NBSocket nb = (NBSocket)ta.getSocket();
				
//				NBLog.out(" sending data maybe: " + nb.getTriplet()+"  ta.isDisconnected:"+ta.isDisconnected() + " from-serv-sock:"+ta.isServerSpawned()+ " ["+_brainUUID+"]");
				
				if(ta.getSocket().isClosed() && !ta.isFinalDisconnectCalled()) {
					ta.setFinalDisconnectCalled(true);
					ta.finalDisconnect();
				}
				
				if(!ta.isDisconnected()) {
				
					// Disconnect 3% of the time, otherwise, send a random amount of data
					
					double d = _random.nextDouble();
					d -= AGENT_DISC_PERCENT;
					if(d < 0) {
						// Only disconnect agents that have been connected for at least 10 seconds
						if((ta.getAgentStartTime() != null && System.currentTimeMillis() - ta.getAgentStartTime() >= 10 * 1000 ) || ta.getAgentStartTime() == null) {
							ta.disconnect();
							agentsToRemove.add(ta);
//							removeTestAgent(ta);
						}
					} else {
						_random.nextBytes(bytearr);
						int bytesToSend = _random.nextInt(bytearr.length);
						ta.send(bytearr, 0, bytesToSend);					
					}
				}
			}
			
			for(TestAgent ta : agentsToRemove) {
				removeTestAgent(ta);
			}
			
			for(TestAgent ta : getRemovedAgents()) {
				if(ta.getSocket().isClosed() && !ta.isFinalDisconnectCalled()) {
					ta.setFinalDisconnectCalled(true);
					ta.finalDisconnect();
				}
			}
			
			double d = _random.nextDouble();
			
			d -= LISTEN_ON_NEW_PORT_PERCENT;
			if(d < 0) { // 2% of the time...
				// listen on a random new port
				int port = _portUtil.getNextPortToListenOn();
				final ServerAgent sa = new ServerAgent(td.getSocketFactory(), td, this, getTestLog(), td.getLocalAddress(), port);
				sa.startListening();
				
				// Start a new thread to briefly listen on the server agent thread to see if it was 
				// able to listen on its port; if it was, add it to our server agent list.
				new Thread() {
					public void run() {
						while(sa.getListenOnPortResult() == null) {
							try { Thread.sleep(1000); } catch (InterruptedException e) { }
						}

						// Only add server agents to the SA list if they were able to successfully listen on their port
						if(sa.getListenOnPortResult() == true) {
							addServerAgent(sa);				
						}						
					}
				}.start();
			}
			
			d -= STOP_SERVER_AGENT_LISTEN_ON_PORT_PERCENT;
			if(d < 0) { // 2% of the time...
				// Stop server agent from listening on whatever port it is currently listening 
				List<ServerAgent> lsa = getServerAgents();
				synchronized(lsa) {
					if(lsa.size() > 0) {
						int r = (int)(_random.nextDouble() * lsa.size());
						ServerAgent sa = (ServerAgent)lsa.get(r);
						sa.stopListeningOnPort();
						removeServerAgent(sa);
					}					
				}
			
			}
			
			d -= CONN_TO_NEW_PORT_PERCENT;
			if(d < 0) { // 2% of the time...
				// New connection - Connect to server port
				List<Integer> ports = getDirector().getPortsAvailableToConnectTo();
				
				if(ports != null && ports.size() > 0) {
					Integer port = ports.get((int)(ports.size() * _random.nextDouble()));

					TestAgent a = new TestAgent(td.getSocketFactory(), td.getNextAgentId(), getTestLog(), false);
					a.connect(td.getRemoteAddress(), port);

					addTestAgent(a);
					agentsCreated++;
				}
			}
			
			// random sleep
			sleep(_random.nextInt(SLEEP_RAND_BETWEEN_ROUNDS));
		} // end while

		MultiSendReceiveMain.println("Sleeping X seconds to give time to communicate remaining data");
		sleep(PRE_AND_POST_TEST_PROCESS_WAIT_TIME);

		
		// Disconnect remaining agents
		for(int x = 0; x < getAgents().size(); x++) {
			TestAgent ta = getAgents().get(x);
			ta.disconnect();
		}
		for(int x = 0; x < getServerAgents().size(); x++) {
			ServerAgent sa = getServerAgents().get(x);
			sa.stopListening();
		}
		
		MultiSendReceiveMain.println("Sleeping X seconds to give time to communicate disconnects");
		sleep(PRE_AND_POST_TEST_PROCESS_WAIT_TIME);
		
		for(int x = 0; x < getAgents().size(); x++) {
				TestAgent ta = getAgents().get(x);
				
				if(ta.getSocket().isClosed() && !ta.isFinalDisconnectCalled()) {
					ta.setFinalDisconnectCalled(true);
					ta.finalDisconnect();
				}
				
				System.out.println(TestAgent.getAgentDebugStr(ta.getSocket())+" --> "+ta.isFinalDisconnectCalled());
		}
		
		for(TestAgent ta : getRemovedAgents()) {
			if(ta.getSocket().isClosed() && !ta.isFinalDisconnectCalled()) {
				ta.setFinalDisconnectCalled(true);
				ta.finalDisconnect();
			}
		}
		
		sleep(PRE_AND_POST_TEST_PROCESS_WAIT_TIME);
		
		

		
		MultiSendReceiveMain.println("test brain complete");
		
		getDirector().informTestBrainComplete();
				
		try {
			Thread.sleep(7 * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(debugDelme.getAndIncrement() == 0) {
			System.out.println("starting.");
			
			analyzeData();
			
//			AnalyzeEntries.analyzeConnections();
			
			
		}
		
		
	}

	
	
	private static void analyzeData() {
		List<Triplet> triplets = new ArrayList<Triplet>();
		
		Set<Object> s = Mapper.getInstance().unsafeGetMap().keySet();
		
//		Mapper.getInstance().unsafeGetMap().clear();
		
		for(Object o : s) {
			
			if(o instanceof Triplet) {
				if(!triplets.contains(o)) {
					triplets.add((Triplet)o);
				}
			}
			
		}
		
		List<Triplet> connectors = getConnectors(triplets);

		Comparator<CmdData> comp = new Comparator<CmdData>() {

			@Override
			public int compare(CmdData one, CmdData two) {
				return one.getFieldPacketId() - two.getFieldPacketId();
			}
			
		};
		
		for(Triplet one : connectors) {
			
			System.out.println("----------------------");
			
			Triplet two = getPartner(one, triplets);
			
			
			if(two != null) {
			
				List<CmdData> twoData = (List<CmdData>)Mapper.getInstance().get(two);
				List<CmdData> oneData = (List<CmdData>)Mapper.getInstance().get(one);

				Collections.sort(oneData, comp);
				Collections.sort(twoData, comp);

				List<CmdData> oneDataSend = splitIntoStream(oneData, true);
				List<CmdData> oneDataRecv = splitIntoStream(oneData, false);
				
				
				List<CmdData> twoDataSend = splitIntoStream(twoData, true);
				List<CmdData> twoDataRecv = splitIntoStream(twoData, false);
			
				compareList(oneDataSend, twoDataRecv, "SEND:"+ one.toString(), "RECEIVED:"+two.toString());
				compareList(twoDataSend, oneDataRecv, "RECEIVED:"+one.toString(), "SENT:"+two.toString());
				
			} else {
				System.err.println("Unable to find partner for "+one);
			}
			
			
//			removeDupes(oneData);
//			removeDupes(twoData);
			
		}
	
	}
	
	private static void compareList(List<CmdData> oneData, List<CmdData> twoData, String one, String two) {
		ByteHolder bhone = new ByteHolder();
		ByteHolder bhtwo = new ByteHolder();
		
		long lastId = -1;
		long oneLength = 0;
		for(CmdData d : oneData) {
			bhone.addBytes(d.getFieldData());
			if(d.getFieldPacketId() != lastId+1) {
				System.err.println("1) packet diff at "+d.getFieldPacketId()+ " "+bhone.getContentsSize());
//				throw new RuntimeException("Error");
			} else {
				lastId++;
			}
//			oneLength += d.getFieldData().length;
		}
		oneLength = bhone.getContentsSize();
		
		lastId = -1;
		long twoLength = 0;
		for(CmdData d : twoData) {
			bhtwo.addBytes(d.getFieldData());
//			twoLength += d.getFieldData().length;
			if(d.getFieldPacketId() != lastId+1) {
				System.err.println("2) packet diff at "+d.getFieldPacketId()+ " "+bhtwo.getContentsSize());
			} else {
				lastId++;
			}
			
		}
		twoLength = bhtwo.getContentsSize();
		
		long matchFailPos = -1;
		boolean match = true;
		
		if(oneLength != twoLength) {
			match = false;
		}
			
		for(int x = 0; x < oneLength; x++) {
		
			if(bhone.getContents().length <= x  || bhtwo.getContents().length <= x) {
				matchFailPos = x;
				break;
			}
			
			
			if( bhone.getContents()[x] != bhtwo.getContents()[x]) {
				matchFailPos = x;
				match = false;
				break;
			}
			
		}			
		
		if(match) {
			System.out.println(one + " + " + two+" => "+oneLength + " " + twoLength+ " "+(match ? "MATCH! " : ("NO MATCH! fail pos:"+matchFailPos)  ));	
		} else {
			System.err.println(one + " + " + two+" => "+oneLength + " " + twoLength+ " "+(match ? "MATCH! " : ("NO MATCH! fail pos:"+matchFailPos)  ));
		}
		
		
		
	}
	
	private static List<CmdData> splitIntoStream(List<CmdData> list, boolean isSender) {
		List<CmdData> result = new ArrayList<CmdData>();
		
		for(CmdData d : list) {
		
			Object b = Mapper.getInstance().get(d);
			if(b != null) {
				if((Boolean)b == isSender) {
					result.add(d);
				}
				
			} else {
				System.err.println("null obj for: "+d);
			}
			
		}
		
		return result;
		
		
	}
	
	/** input must be a sorted list */
	private static void removeDupes(List<CmdData> sortedList) {
		
		long nextId = 0;
		
		for(int x = 0; x < sortedList.size(); x++) {
			
			CmdData curr = sortedList.get(x);
			if(curr.getFieldPacketId() != nextId) {
				System.err.println("Missing packet id: "+nextId);
				return;
			} else {
				nextId++;
			}
			
			innerfor: for(int y = x+1; y < sortedList.size(); y++) {
				CmdData nextY = sortedList.get(y);
				if(nextY.getFieldPacketId() == curr.getFieldPacketId()) {
					
					if(!areByteArraysEqual(curr.getFieldData(), nextY.getFieldData())) {
						System.err.println("contents don't match: "+x + " "+y);
						areByteArraysEqual(curr.getFieldData(), nextY.getFieldData());
						return;
					} else {
						sortedList.remove(y);
						y--;
					}
					
				} else {
					break innerfor;
				}
			}
			
		}
		
	}
	
	private static boolean areByteArraysEqual(byte[] one, byte[] two) {
		if(one.length != two.length) {
			return false;
		}
		
		for(int x = 0; x < one.length; x++) {
			if(one[x] != two[x]) {
				return false;
			}
		}
		
		return true;
		
	}
	
	
	private static List<Triplet> getConnectors(List<Triplet> triplets) {
		List<Triplet> result = new ArrayList<Triplet>();
		
		for(Triplet t : triplets) {
			if(t.areWeConnector()) {
				result.add(t);
			}
			
		}
		
		
		return result;
	}
	
	private static Triplet getPartner(Triplet one, List<Triplet> triplets) {
		for(Triplet t : triplets) {
			if(t.getConnectorId() == one.getConnectorId() 
					&& t.getConnectorUuid().equals(one.getConnectorUuid()) 
					&& one.areWeConnector() != t.areWeConnector()) {
				return t;
			}
		}
		
		return null;
	
	}
	
		
	private static final AtomicLong debugDelme = new AtomicLong(0);
}
