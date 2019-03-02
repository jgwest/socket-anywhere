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

package com.socketanywhere.forwarder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.socketanywhere.multisendreceive.TestAgent;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.SoAnUtil;

/** Given two sockets, bind them together, but only in one direction: data received on
 * src's inputstream will be written to dest's outputstream. */
public class UnidirectionalConnectionBindThread extends Thread {
	private static final boolean DEBUG = false;
	
	private final String _srcDebug;
	
	private final String _destDebug;
	
	
	private final ISocketTL _src;
	private final ISocketTL _dest;
	
	boolean _threadRunning = true;
	
	Object _lock = new Object();
	
	private final long _threadId;
	
	public UnidirectionalConnectionBindThread(ISocketTL src, ISocketTL dest) {
//		super();
		setName(UnidirectionalConnectionBindThread.class.getName()+"-"+getId());
		_src = src;
		_dest = dest;
		_srcDebug = src.getClass().getSimpleName()+"-"+TestAgent.getAgentDebugStr(src); // TODO: Move these to a shared utility class
		_destDebug = dest.getClass().getSimpleName()+"-"+TestAgent.getAgentDebugStr(dest);
		_threadRunning = true;
		_threadId = getId();
	}
	
	public void closeBoth() {
		if(DEBUG) { out("closeBoth() called - "+_srcDebug+" "+_destDebug);  }
		
		_threadRunning = false;
		try {
			if(!_src.isClosed()) {
				_src.close();
			}
		} catch (IOException e) {
		}
		
		try {
			if(!_dest.isClosed()) {
				_dest.close();
			}
		} catch (IOException e) {
		}
		
	}
	
	
	private boolean threadRunning() {
		synchronized(_lock) {
			return _threadRunning;
		}
	}
	
	private void out(String str) {
		System.out.println("[thr"+_threadId+"] "+str);
		
	}
	
	@Override
	public void run() {
		
		try {
			innerRun();
		} finally {
			if(DEBUG) { out("Thread complete - "+_srcDebug+" "+_destDebug);  }
		}
	}
	
	
	private void innerRun() {
		InputStream srcStream = null;
		OutputStream destStream = null;

		if(DEBUG) { out("UCBT run() - source socket ["+_srcDebug+" ("+_src.isConnected()+"| "+!_src.isClosed()+" )], dest socket ["+_destDebug+"]"); }
		
		try {
			srcStream = _src.getInputStream();
			destStream = _dest.getOutputStream();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			closeBoth();
			return;
		}
		
		while(threadRunning()) {
			try {
				byte[] bytearr = new byte[32768];
//				if(_src.isClosed()) {
//					closeBoth();
//					return;
//				}

				int size = 0;
				try {
					size = srcStream.read(bytearr);
				} catch(IOException ioe) {
					if(!ioe.getMessage().contains("Socket closed") && !ioe.getMessage().contains("Connection reset")) {
						ioe.printStackTrace();
					} else {
						out("UCBT - Source socket closed. closed socket:["+_srcDebug+"]");
					}
					closeBoth();
					return;
				}
				
				if(size == -1 || !threadRunning()) {
					closeBoth();
					return;
				}
				
				if(size > 0) {
					
					if(DEBUG) {
						out("");
						out("* Read "+size+ " bytes on UCB thread. Read from:"+_srcDebug + "."); //  data: "+ sanitizeString(new String(bytearr, 0, size)));
//						out("-----------------------------");
					}
					
					if(threadRunning()) {
						destStream.write(bytearr, 0, size);

						if(DEBUG) {
							out("* Wrote "+size+ " bytes on UCB thread. Wrote to:"+_destDebug+ "."); //  data: "+ sanitizeString(new String(bytearr, 0, size)));
//							out("-----------------------------");
						}
						
						destStream.flush(); // ADDED DEC 2014
						
					}
					
				}				
				
			} catch(IOException ioe) {
				if(DEBUG) {
					String stackTrace = SoAnUtil.convertStackTrace(ioe);
					stackTrace = stackTrace.replace("\r", "");
					String[] str = stackTrace.split(Pattern.quote("\n"));
					for(String curr : str) {
						out("[err] "+curr);
					}
					
				}
				closeBoth();
				return;
			}
			
		}
		
	}

	private static String sanitizeString(String str) {
		StringBuilder sb = new StringBuilder();
		for(int x = 0; x < str.length(); x++) {
			char c = str.charAt(x);
			if(c >= 32 && c <= 126) {
				sb.append(c);
			}
			
		}
		
		return sb.toString();
	}
	
	
	public void endThread() {
		synchronized(_lock) {
			_threadRunning = false;
		}
	}
	
	
	private static String convertBinaryByteArrayToString(byte[] barr, int len) {
		StringBuilder sb = new StringBuilder();
		for(int x = 0; x < len; x++) {
			
			char c = (char)barr[x];
			if(Character.isJavaIdentifierPart(c)) {
				sb.append(c);
			} else {
				sb.append("-");
			}
			
		}
		
		return sb.toString();
	}
}
