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

package com.socketanywhere.nonbreaking;

import java.util.ArrayList;
import java.util.List;

public class CmdFactory {

	// Static instance
	private static CmdFactory _instance = new CmdFactory();
	
	// Contents
	List<CmdAbstract> _availableCmds = new ArrayList<CmdAbstract>();
	
	 
	
	private CmdFactory() {
		_availableCmds.add(new CmdNewConn());
		_availableCmds.add(new CmdData());
		_availableCmds.add(new CmdDataReceived());
		_availableCmds.add(new CmdJoinConn());
		_availableCmds.add(new CmdCloseConn());
		_availableCmds.add(new CmdAck());
		_availableCmds.add(new CmdDataRequest());
		_availableCmds.add(new CmdDataRequestOnReconnect());

	}
	
	public static CmdFactory getInstance() {
		return _instance;
	}
	
	public CmdAbstract createCommand(byte[] cmdIdArr) {
		for(CmdAbstract a :_availableCmds) {
			byte[] aByteId = a.getByteId();
			if(cmdIdArr[0] == aByteId[0] && cmdIdArr[1] == aByteId[1]) {
				return a.instantiateInstance();
			}
		}
		
		return null;
	}
	
	
}
