/*
	Copyright 2013 Jonathan West

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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.socketanywhere.net.ISocketTL;

public class RCSocketReaderThread extends Thread {
	
	ISocketTL _innerSocket;
	IRCConnectionBrain _brain;
	
	public RCSocketReaderThread(ISocketTL s, IRCConnectionBrain brain) {
		super(RCSocketReaderThread.class.getName());
		setDaemon(true);
		_innerSocket = s;
		_brain = brain;
	}
	
	public void run() {
		
		try {
			
			boolean contLoop = true;
			
			_brain.debugMsg(this.getClass().getSimpleName()+" waiting for data. ");

			InputStream is = _innerSocket.getInputStream();
			
			while(contLoop) {
				
				byte[] barr = RCUtil.readAndWait(is, RCGenericCmd.SIZE_OF_CMD_IN_BYTES);
				
				// we can receive these commands, here:
				// CONNECT addr:[addr] port:[port] uuid:[uuid]
				
				BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(barr)));
				
				String str = br.readLine();
				
				String cmd = null;
				
				for(String a : RCGenericCmd.CMD_LIST) {
					if(str.startsWith(a+" ")) {
						cmd = a;
					}
				}
				
				if(cmd == null) {
					_brain.debugMsg("Error: Received null command in "+getClass().getSimpleName());
					contLoop = false;
				} else {
					boolean errorOccured = false;
				
					if(str != null) {
						
						if(cmd.equals(RCGenericCmd.CMD_CONNECT)) {
						
							RCGenericCmd genCmd = new RCGenericCmd();
							genCmd.parseCmd(str);
							
							_brain.debugMsg(getClass().getSimpleName()+" received command:"+genCmd);
							
							_brain.eventReceivedConnectCmd(genCmd, _innerSocket);
							
							// We have let the brain know of this connection, so close the thread
							contLoop = false;
							
						} else if(cmd.equals(RCGenericCmd.CMD_KEEP_ALIVE)) {

							RCGenericCmd genCmd = new RCGenericCmd();
							genCmd.parseCmd(str);
							
							_brain.debugMsg(getClass().getSimpleName()+" received command:"+genCmd);
							
							// Don't close the thread, keep it rolling....
							
						} else {
							errorOccured = true;
						}
						
					} else {
						errorOccured = true;
					}
					
					if(errorOccured) {
						// error
						contLoop = false;
						System.err.println("Error occured in "+getClass().getSimpleName());
					}
				}
			
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			
			try { _innerSocket.close(); } catch (IOException e1) { }
			
//			_brain.reestablishHopperConnection();
		}
		
	};

	public static String extractField(String field, String str) {
		String fullField = " "+field+"[";
		int start = str.indexOf(fullField) + fullField.length();
		int end = str.indexOf("]", start);
		return str.substring(start, end);
	}

	
}

