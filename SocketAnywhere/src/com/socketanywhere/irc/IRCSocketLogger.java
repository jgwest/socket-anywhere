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

package com.socketanywhere.irc;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;

/** IRC specific logging class, that will write either to the console, or two a file, and with
 * variosu log levels. */
public class IRCSocketLogger {
	
	public static final int LOG_LEVEL_ERROR = 0;
	public static final int LOG_LEVEL_WARNING = 1;
	public static final int LOG_LEVEL_INFO = 2;
	public static final int LOG_LEVEL_DEBUG = 3;
	
	private static int _logLevel = LOG_LEVEL_DEBUG;
	
	private static Object writeLock = new Object();
	
	private static boolean _writeToConsole = false;
	private static boolean _writeToFile = true;
	
	private static String getTimestamp() {
		String str = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date());
		
		// Lazy time zone removal for East
		str = str.replaceAll(" EST", "");
		str = str.replaceAll(" EDT", "");
		return "["+str+"]  ";
	}

	private static void print(String message) {
		
		message = getTimestamp() + message;
		
		if(_writeToConsole) {
			synchronized(writeLock) {
				System.out.println(message);
			}
			
		} else if(_writeToFile) {
			
			try {

				synchronized(writeLock) {
					File f = new File("c:\\sa-irc-debug.log");
					FileWriter fw = new FileWriter(f, true);
					fw.write(message);
					
					fw.write("\r\n");
					
					fw.flush();
					fw.close();
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
						
	}

	
	public static void logError(String s) {
		
		print("ERROR: " + s);
		
	}
	
	public static void logWarning(String s) {
		if(_logLevel == LOG_LEVEL_ERROR) return;
		
		print("WARNING: "+s);
	}
	
	public static void logInfo(String s) {
		if(_logLevel == LOG_LEVEL_ERROR || _logLevel == LOG_LEVEL_WARNING) return;

		print("INFO: " + s);
	}
	
	public static void logDebug(String s) {
		if(_logLevel == LOG_LEVEL_DEBUG) {
			print("DEBUG: " + s);
		}
	}
	
	// Additional param supports
	
	public static void logWarning(Object o, String s) {
		logWarning(o.getClass(), s);
	}
	
	public static void logWarning(Class<? extends Object> c, String s) {
		logWarning(s+" [in "+c.getName()+"]");
	}
	
	public static void logError(Object o, String s) {
		logError(o.getClass(), s);
	}
	
	public static void logError(Class<? extends Object> c, String s) {
		logError(s+" [in "+c.getName()+"]");
	}
	
	public static void logInfo(Object o, String s) {
		logInfo(o.getClass(), s);
	}
	
	public static void logInfo(Class<? extends Object> c, String s) {
		logInfo(s+" [in "+c.getName()+"]");
	}
	
	public static void logDebug(Object o, String s) {
		logDebug(o.getClass(), s);
	}
	
	public static void logDebug(Class<? extends Object> c, String s) {
		logDebug(s+" [in "+c.getName()+"]");
	}
	
}
