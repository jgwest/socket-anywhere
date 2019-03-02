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

import java.util.ArrayList;
import java.util.List;

public class CmdMultFactory {

	// Static instance
	private static CmdMultFactory _instance = new CmdMultFactory();
	
	// Contents
	List<CmdMultiplexAbstract> _availableCmds = new ArrayList<CmdMultiplexAbstract>();
	
	private CmdMultFactory() {
		_availableCmds.add(new CmdNewConnMultiplex());
		_availableCmds.add(new CmdAckMultiplex());
		_availableCmds.add(new CmdDataMultiplex());
		_availableCmds.add(new CmdCloseConnMultiplex());
		_availableCmds.add(new CmdDataReceived());

	}
	
	public static CmdMultFactory getInstance() {
		return _instance;
	}
	
	public CmdMultiplexAbstract createCommand(byte[] cmdIdArr) {
		for(CmdMultiplexAbstract a :_availableCmds) {
			byte[] aByteId = a.getByteId();
			if(cmdIdArr[0] == aByteId[0] && cmdIdArr[1] == aByteId[1]) {
				return a.instantiateInstance();
			}
		}
		
		return null;
	}
	
	public CmdMultiplexAbstract getCommandById(int id) {
		for(CmdMultiplexAbstract a : _availableCmds) {
			if(a.getId() == id) {
				return a;
			}
		}
		
		return null;
		
	}
	
}
