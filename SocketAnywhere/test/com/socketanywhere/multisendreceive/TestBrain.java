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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.socketanywhere.nonbreakingnew.NBLog;
import com.socketanywhere.nonbreakingnew.NBSocket;

public abstract class TestBrain {

	protected final String _brainUUID = UUID.randomUUID().toString();
	
	/** Our agents that are managed by test brain*/
	private List<TestAgent> _agents = new ArrayList<TestAgent>();
	
	private List<TestAgent> _removedAgents = new ArrayList<TestAgent>();
	
	/** Our server agents that listen on ports and are managed by test brain*/
	private List<ServerAgent> _serverAgents = new ArrayList<ServerAgent>();

	private TestDirector _director;
	private TestLog _testLog;
	
	public TestDirector getDirector() {
		return _director;
	}
	
	public List<TestAgent> getAgents() {
		return _agents;
	}
	
	public List<ServerAgent> getServerAgents() {
		return _serverAgents;
	}
	
	public List<TestAgent> getRemovedAgents() {
		return _removedAgents;
	}
	
	public TestLog getTestLog() {
		return _testLog;
	}
	
	public void addTestAgent(TestAgent a) {
		synchronized(_agents) {
			_agents.add(a);
		}
	}

	public void removeTestAgent(TestAgent a) {
		synchronized(_agents) {
			_agents.remove(a);
			_removedAgents.add(a);
		}
	}

	
	public void addServerAgent(ServerAgent a) {
		synchronized(_serverAgents) {
			_serverAgents.add(a);
		}
	}

	public void removeServerAgent(ServerAgent a) {
		synchronized(_serverAgents) {
			_serverAgents.remove(a);
		}
	}

	
	
		
	public TestBrain(TestDirector director, TestLog testLog) {
		_director = director;
		_testLog = testLog;
	}
	
	protected void sleep(long msecs) {
		try {
			Thread.sleep(msecs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/** Inform the brain that the test has ended*/
	public void informTestEnd() {
		
		synchronized(_serverAgents) {
			for(ServerAgent a : _serverAgents) {
				a.stopListening();
			}
		}
		
		synchronized(_agents) {
			for(TestAgent a : _agents) {
				a.informTestEnd();
			}
		}
		
	}
	
	abstract public void run();
	
	/** Add test agents to our agent list, from those agents that are spawned by connecting to a server agent*/
	public void addServerSpawnedTestAgent(TestAgent agent) {
		synchronized(_agents) {
			if(agent.getSocket() instanceof NBSocket) {
				NBLog.out("addServerSpawnedTestAgent: "+ ((NBSocket)agent.getSocket()).getDebugTriplet() +"  ["+_brainUUID+"] ");
			}
			_agents.add(agent);
		}
	}

	
	/** One of our ServerAgents is now listening on a port, we should communicate which it is and store its value */
	protected void addPortListeningOn(int port) { 
		_director.informListeningOnPort(port);
	}

	/** One of our ServerAgents is no longer listening on a port, we should communicate which it is and store its value.
	 * Called by ServerAgent. */
	protected void removePortListeningOn(int port) {
		_director.informStopListeningOnPort(port);
		
	}

}
