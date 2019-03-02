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

public class NBLog {
	public static boolean DEBUG = false;
	
	private static Object outLock = new Object();
	
	private static void out(String s) {
		if(!DEBUG) return;
		synchronized(outLock) {
			System.out.println("> "+s);
			System.out.flush();
		}
	}
	
	private static void err(String s) {
		if(!DEBUG) return;
		
		synchronized(outLock) {
			System.err.println("!> "+s);
			System.err.flush();
		}
	}
	
	// --------------------------------------------------------
	
	public static void connectionJoined(String uuidOfConnect, int connId) {
		out("connectionJoined - connector: "+uuidOfConnect + " id:"+connId);
	}
	
	public static void connectionCreated(String uuidOfConnect, int connId) {
		out("connectionCreated - connector:"+uuidOfConnect + "  id:"+connId);
	}
	
	public static void connectionClosed(String uuidOfConnector, int connId) { 
		out("connectionClosed - connector:"+uuidOfConnector + "  id:"+connId);
	}
	
	public static void dataSent(CmdData d) {
		out("dataSent - "+d.toString());
	}

	public static void dataReceived(CmdData d) {
		out("dataReceived - "+d.toString());
	}
	
	public static void dataAckSent(CmdDataReceived d) {
		out("dataAckSent, cmd:"+d);
	}
	
	public static void dataAckReceived(CmdDataReceived d) {
//		out("dataAckReceived");
	}
	
	public static void dataRequestedOnReconnectConnectee(CmdDataRequestOnReconnect d) {
		out("dataRequestedOnReconnect - Connectee");
	}

	public static void dataRequestedOnReconnectConnector(CmdAck d) {
		out("dataRequestedOnReconnect - Connector");
	}
	
	public static void dataRequested(CmdDataRequest d) {
		out("General data request - "+d);
	}


	// --------------------------------------------------------
	
	public static void debug(String s) {
		out(s);
	}
	
	public static void error(String s) {
		err(s);
	}
}
