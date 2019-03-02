/*
	Copyright 2012, 2014 Jonathan West

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

package com.socketanywhere.multiplexingnew;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import com.socketanywhere.multiplexing.CmdDataMultiplex;
import com.socketanywhere.multiplexing.CmdMultiplexAbstract;
import com.socketanywhere.net.ISocketTL;

public class MnLog {
	public static final boolean DEBUG = false;

	private static FileWriter fw; 
	
	private static Object outLock = new Object();

	private static int counter = 0;

	static {
		String filename = null;
		
		if(System.getProperty("os.name").toLowerCase().contains("win")) {
			filename = "d:\\delme\\log.log";
		} else {
			filename = null;
		}

		if(filename != null) {
			try {
				File file = new File(filename);
				
				if(file.getParentFile().exists()) {
					fw = new FileWriter(file);
				} else {
					fw = null;
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				fw = null;
			}			
		}
	}

	
	public static void dataSent(CmdDataMultiplex d) {
		
	}
	
	public static void wroteCommand(CmdMultiplexAbstract d, ISocketTL sock) {
		if(DEBUG) {
			debug("Wrote command to inner socket. cmd: "+d + " socket:"+sock);
			if(d instanceof CmdDataMultiplex) {
//				CmdDataMultiplex m = (CmdDataMultiplex)d;
//				debug("This was it's contents:{"+new String(m.getFieldData(), 0, m.getFieldDataLength())+"}\n");
			}
		}		
	}
	public static void wroteCommand(CmdMultiplexAbstract d, MNSocketWriter sock) {
		if(DEBUG) {
			debug("Sent command to writer. cmd: "+d + " writer: "+sock);
			if(d instanceof CmdDataMultiplex) {
//				CmdDataMultiplex m = (CmdDataMultiplex)d;
//				debug("This was it's contents:{"+new String(m.getFieldData(), 0, m.getFieldDataLength())+"}\n");
			}
		}
	}

	
	
	
	public static void socketClosed(MNMultSocket sock) {
		if(DEBUG) {
			try {
				out("Closed: "+sock.toString());
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
		out("[MN]  "+Thread.currentThread().getId()+" ["+time+"]> "+str);		
	}
	
	
	@SuppressWarnings("deprecation")
	public static void debug(String str) {
		// Lazy data printing
		Date d = new Date(System.currentTimeMillis());

//		if(!str.contains("currMessage:")) {
//			return;
//		}
		
		String time = 		d.getHours()+":"+d.getMinutes()+":"+d.getSeconds()+":"+(d.getTime()%1000);
		if(DEBUG) { out("[MN]  "+Thread.currentThread().getId()+" ["+time+"]> "+str); }
	}
	
	@SuppressWarnings("deprecation")
	public static void error(String str) {
		// Lazy data printing
		Date d = new Date(System.currentTimeMillis());
		
		String time = 		d.getHours()+":"+d.getMinutes()+":"+d.getSeconds()+":"+(d.getTime()%1000);
		err("[MN]  "+Thread.currentThread().getId()+" ["+time+"]> "+str);
	}
	
	
	private static void out(String s) {
//		if(!DEBUG) { return; }
		
		System.out.println(s);
		
		synchronized(outLock) {
			String str = s.trim();
			
			if(fw != null) {
				try {
					fw.write(str+"\n");
					
					counter++;
					if(counter % 200 == 0) {
						fw.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void err(String s) {
		
		System.err.println(s);
		
		synchronized(outLock) {
			String str = "[ERR!]    "+s.trim();
			
			
			if(fw != null) {
				try {
					fw.write(str+"\n");
					
					counter++;
					if(counter % 20 == 0) {
						fw.flush();
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
	}

}
