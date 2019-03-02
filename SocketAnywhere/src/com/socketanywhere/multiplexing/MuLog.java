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

import java.util.Date;

import com.socketanywhere.net.ISocketTL;


public class MuLog {
	public static final boolean DEBUG = false;
	
	public static void dataSent(CmdDataMultiplex d) {
		
	}
	
	@SuppressWarnings("unused")
	public static void wroteCommand(CmdMultiplexAbstract d, MultSocket sock) {
		if(DEBUG) debug("We wrote command out:"+d + " to socket:"+sock);		
		if(d instanceof CmdDataMultiplex) {
			CmdDataMultiplex m = (CmdDataMultiplex)d;
//			debug("This was it's contents:{"+new String(m.getFieldData(), 0, m.getFieldDataLength())+"}\n");
		}

	}

	public static void wroteCommand(CmdMultiplexAbstract d, ISocketTL sock) {
		if(DEBUG) {
			debug("We wrote command out:"+d + " to socket:"+sock);
			if(d instanceof CmdDataMultiplex) {
				CmdDataMultiplex m = (CmdDataMultiplex)d;
//				debug("This was it's contents:{"+new String(m.getFieldData(), 0, m.getFieldDataLength())+"}\n");
			}
		}
	}

	
	
	
	public static void socketClosed(MultSocket sock) {
		if(DEBUG) {
			try {
				System.out.println("Closed: "+sock.toString());
//				FileWriter fw = new FileWriter(new File("c:\\temp\\errlog"), true);
//				fw.write("* Closed: "+sock.toString()+"\r\n");
//				fw.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void dbg(String str) {
//		Date d = new Date(System.currentTimeMillis());

//		String time = 		d.getHours()+":"+d.getMinutes()+":"+d.getSeconds()+":"+(d.getTime()%1000);
		String time = "";
		System.out.println(Thread.currentThread().getId()+" ["+time+"]> "+str);		
	}
	
	
	@SuppressWarnings("deprecation")
	public static void debug(String str) {
		// Lazy data printing
		Date d = new Date(System.currentTimeMillis());

		String time = 		d.getHours()+":"+d.getMinutes()+":"+d.getSeconds()+":"+(d.getTime()%1000);
		if(DEBUG) System.out.println(Thread.currentThread().getId()+" ["+time+"]> "+str);
	}
	
	@SuppressWarnings("deprecation")
	public static void error(String str) {
		// Lazy data printing
		Date d = new Date(System.currentTimeMillis());
		
		String time = 		d.getHours()+":"+d.getMinutes()+":"+d.getSeconds()+":"+(d.getTime()%1000);
		System.err.println(Thread.currentThread().getId()+" ["+time+"]> "+str);
	}
}
