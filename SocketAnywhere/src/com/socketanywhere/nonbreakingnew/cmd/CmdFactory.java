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

package com.socketanywhere.nonbreakingnew.cmd;

import java.util.ArrayList;
import java.util.List;

import com.socketanywhere.nonbreakingnew.NBLog;

public class CmdFactory {

	// Static instance
	private final static CmdFactory _instance = new CmdFactory();
	
	// Contents
	private final List<CmdAbstract> _availableCmds = new ArrayList<CmdAbstract>();
	
	private CmdFactory() {
		_availableCmds.add(new CmdNewConn());
		_availableCmds.add(new CmdData());
		_availableCmds.add(new CmdDataReceived());
		_availableCmds.add(new CmdJoinConn());
		_availableCmds.add(new CmdCloseConn());
		_availableCmds.add(new CmdDataRequest());
		_availableCmds.add(new CmdDataRequestOnReconnect());
		_availableCmds.add(new CmdCloseDataRequestNew());
		_availableCmds.add(new CmdReadyToCloseConn());
		_availableCmds.add(new CmdJoinCloseConn());
		_availableCmds.add(new CmdAckJoinConn());
		_availableCmds.add(new CmdAckNewConn());
		_availableCmds.add(new CmdAckCloseConn());
		_availableCmds.add(new CmdAckJoinCloseConn());
		_availableCmds.add(new CmdAckDataRequestOnReconnect());
		_availableCmds.add(new CmdAckReadyToCloseConn());
		_availableCmds.add(new CmdAckCloseDataRequest());
		_availableCmds.add(new CmdReadyToJoin());
		_availableCmds.add(new CmdAckReadyToJoin());
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
		
		NBLog.error("Unrecognized byte id:" + cmdIdArr[0] + " "+cmdIdArr[1]);
		
		
		return null;
	}

	public String debugGetCommandNameById(int cmdId) {
		for(CmdAbstract a :_availableCmds) {
			if(a.getId() == cmdId) {
				return a.getClass().getName();
			}
		}
		
		return null;
	}
	
}
