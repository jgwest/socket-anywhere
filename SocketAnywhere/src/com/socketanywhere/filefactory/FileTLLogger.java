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

package com.socketanywhere.filefactory;

import java.util.List;

/** Logger logs various events that occur during normal operation. Very few
 * of the methods in this class log errors, just events. */
public class FileTLLogger {

	public static final boolean DEBUG = false;
	
	public static void out(String str) {
		System.out.println("* " +str);
	}

	
	public static void userClose(String from, String to) {
		if(!DEBUG) return;
		out("User called close() on socket. socket from "+from+" to "+to);
	}
	
	public static void receivedRemoteClose(String from, String to) {
		if(!DEBUG) return;
		out("Received remote close from "+from+" to "+to);
	}
	
	public static void writeOutputData(int len, String from, String to) {
		if(!DEBUG) return;
		out("Wrote "+len+ " bytes from "+from+" to "+to);
	}
	
	public static void readInputData(int bytes, String from, String to) {
		if(!DEBUG) return;
		out("Read "+bytes+ " bytes from "+from+" to "+to);
		
	}
	
	public static void unbindOnNames(List<String> addresses) {
		if(!DEBUG) return;
		
		String str = "Unbound on addresses: ";
		
		for(int x = 0; x < addresses.size()-1; x++) {
			str += addresses.get(x) + ", ";
		}
		
		str += addresses.get(addresses.size()-1);
		
		out(str);
		
	}
	
	public static void bindOnNames(List<String> addresses) {
		if(!DEBUG) return;
		
		String str = "Bound on addresses: ";
		
		for(int x = 0; x < addresses.size()-1; x++) {
			str += addresses.get(x) + ", ";
		}
		
		str += addresses.get(addresses.size()-1);
		
		out(str);
				
	}
	
	public static void outputStreamCreated(FileTLOutputStream s) {
		if(!DEBUG) return;
		out("Output stream created - remote["+s._remoteName+"] source["+s._sourceName+"]");		
	}

	public static void outputStreamClosed(FileTLOutputStream s) {
		if(!DEBUG) return;
		out("Output stream created - remote["+s._remoteName+"] source["+s._sourceName+"]");		
	}

	public static void inputStreamCreated(FileTLInputStream fs) {
		if(!DEBUG) return;
		out("Input stream created - remote["+fs._remoteName+"] listen["+fs._listenName+"]");		
	}
	
	public static void inputStreamClosed(FileTLInputStream fs) {
		if(!DEBUG) return;
		out("Input stream closed - remote["+fs._remoteName+"] listen["+fs._listenName+"]");		
	}
	
	public static void bindFailed(String addr) {
		if(!DEBUG) return;
		out("Bind failed on address "+addr);
	}
	 
}
