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

import com.socketanywhere.net.TLAddress;

/*
 * We can receive these commands:
 * CONNECT addr:[addr] port:[port] uuid:[uuid]
 * REJECT-CONNECT addr:[] port:[] uuid:[uuid]
 * ACCEPT-CONNECT addr:[] port:[]  uuid:[uuid]*/
public class RCGenericCmd {

	public static final String CMD_CONNECT = "CONNECT";
	public static final String CMD_REJECT_CONNECT = "REJECT-CONNECT";
	public static final String CMD_ACCEPT_CONNECT = "ACCEPT-CONNECT";
	public static final String CMD_KEEP_ALIVE = "KEEP-ALIVE";
	
	public static final String[] CMD_LIST = new String[] { CMD_CONNECT, CMD_REJECT_CONNECT, CMD_ACCEPT_CONNECT, CMD_KEEP_ALIVE };
	
	public static final int SIZE_OF_CMD_IN_BYTES = 1024;
	
	String _commandName;
	
	TLAddress _addr;
	
	String _uuid;
	
	public RCGenericCmd() {
		
	}

	public void parseCmd(String str) {
		
		String cmd = null;
		
		for(String a : RCGenericCmd.CMD_LIST) {
			if(str.startsWith(a+" ")) {
				cmd = a;
			}
		}
		
		if(cmd != null && !cmd.equals(CMD_KEEP_ALIVE)) {
			// Parse fields from all commands but keep alive
			
			String addr = extractField("addr", str);
			int port = Integer.parseInt(extractField("port", str));
			String uuid = extractField("uuid", str);

			setAddr(new TLAddress(addr, port));
			setUuid(uuid);
		
		}
		
		setCommandName(cmd);
	}
	
	public byte[] generateCmdBytes() {
		byte[] barr = new byte[SIZE_OF_CMD_IN_BYTES];
		
		String str = generateCmdString();
		
		byte[] cmdBytes = str.getBytes();
		
		System.arraycopy(cmdBytes, 0, barr, 0, cmdBytes.length);
		
		return barr;
		
		
	}
	
	public String generateCmdString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(_commandName).append(" ");
		
		if(_addr != null) {
			sb.append("addr[").append(_addr.getHostname()).append("] ");
			
			sb.append("port[").append(_addr.getPort()).append("] ");
		}
		
		if(_uuid != null) {
		
			sb.append("uuid[").append(_uuid).append("]");
		}
		
		
		
		return sb.toString();
	}
	
	public String getCommandName() {
		return _commandName;
	}

	public void setCommandName(String command) {
		this._commandName = command;
	}

	public TLAddress getAddr() {
		return _addr;
	}

	public void setAddr(TLAddress addr) {
		this._addr = addr;
	}

	public String getUuid() {
		return _uuid;
	}

	public void setUuid(String uuid) {
		this._uuid = uuid;
	}
	

	public static String extractField(String field, String str) {
		String fullField = " "+field+"[";
		int start = str.indexOf(fullField) + fullField.length();
		int end = str.indexOf("]", start);
		return str.substring(start, end);
	}
	
	@Override
	public String toString() {
		return generateCmdString();
	}
	
	
}
