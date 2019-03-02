/*
	Copyright 2012, 2019 Jonathan West

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

package com.socketanywhere.nonbreakingnew;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckCloseDataRequest;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckDataRequestOnReconnect;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckJoinCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckJoinConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckNewConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckReadyToCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdAckReadyToJoin;
import com.socketanywhere.nonbreakingnew.cmd.CmdCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdCloseDataRequestNew;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataReceived;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataRequest;
import com.socketanywhere.nonbreakingnew.cmd.CmdDataRequestOnReconnect;
import com.socketanywhere.nonbreakingnew.cmd.CmdJoinCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdJoinConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdNewConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdReadyToCloseConn;
import com.socketanywhere.nonbreakingnew.cmd.CmdReadyToJoin;

public class NBLog {
	
	
	// CONSTANTS
	
	public static final int DISABLED = 0;
	public static final int INFO = 1;
	public static final int INTERESTING = 2;
	
	// SETTINGS
	public static boolean DEBUG = false;	
	public static int CURR_DEBUG_LEVEL = INTERESTING;
	public static boolean ERROR = true;
	
	private static FileWriter fw; 
	
	private static int counter = 0;
	
	public static NBLogLogListener logListener = new StdOutLogListener();
	
	static {
//		try {
//			fw = new FileWriter("d:\\delme\\log.log");
//		} catch (IOException e) {
//			e.printStackTrace();
//			fw = null;
//		}
		fw = null;
	}

	
	private static Object outLock = new Object();
	
	public static void out(String s) {
		out(s, 9);
	}
	
    public static String getTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        return dateFormat.format(calendar.getTime());
    }

	
	private static void out(String s, int level) {
		if(!DEBUG) { return; }
		
		if(level >= CURR_DEBUG_LEVEL) {
			String enterTime = getTime();
			synchronized(outLock) {
				String str = s.trim();
				
//				if(level >= INTERESTING) {
				logListener.printOut("[NB]  [OUT"+level+"]   "+ str);
				logListener.flushOut();
//				}
				
				if(fw != null) {
					try {
						fw.write("["+enterTime+"]  [NB] [OUT"+level+"]   "+ str+"\n");
						
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
	}
	
	public static void err(String s) {
		if(!ERROR) { return; }
		
		synchronized(outLock) {
			String str = "["+getTime()+"]  [NB] [ERR!]    "+s.trim();
			
			logListener.printErr(str);
			logListener.flushErr();

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
	
	// --------------------------------------------------------
	
	public static void connectionJoined(Triplet t) {
//		out("connectionJoined - triplet: "+t, INTERESTING);
	}

//	public static void connectionJoinClosedSent(Triplet t) {
//		out("connectionJoinClosed - triplet: "+t, INTERESTING);
//	}

	public static void connectionInitateExisting(Triplet t) {
//		out("connectionInitateExisting - triplet: "+t, INTERESTING);
	}
	
	public static void connectionCreated(Triplet t) {
//		out("connectionCreated - triplet: "+t, INTERESTING);
	}
	
	public static void connectionClosed(Triplet t) { 
//		out("connectionClosed - triplet: "+t, INTERESTING);
	}
	
	public static void dataSent(CmdData d, Triplet t) {
//		out("dataSent - "+d.toString()+" from "+t, INFO);
	}

	public static void dataReceived(CmdData d, Triplet t) {
//		out("dataReceived - "+d.toString() + " to "+t, INFO);
	}
	
	public static void dataAckSent(CmdDataReceived d, Triplet t) {
//		out("dataAckSent, cmd:"+d+ " from "+t,  INFO);
	}
	
	public static void dataAckReceived(CmdDataReceived d, Triplet t) {
//		out("dataAckReceived by "+t, INFO);
	}
	
	public static void dataRequestedOnReconnectConnectee(CmdDataRequestOnReconnect d, Triplet t) {
//		out("dataRequestedOnReconnect "+d.toString()+" - Connectee  "+t, INTERESTING);
	}
	
	public static void dataRequested(CmdDataRequest d, Triplet t) {
//		out("General data request - "+d+"  "+t, INTERESTING);
	}

	public static void readyToCloseSent(CmdReadyToCloseConn d, Triplet t) {
//		out("Ready to close sent - "+d+ "  "+t, INTERESTING);
	}
	
	public static void closeDataRequestSent(CmdCloseDataRequestNew d, Triplet t) {
//		out("close data request sent - "+d+ "  "+t, INTERESTING);
	}
	
	
	public static void receivedCmd(CmdAckReadyToJoin cartj, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(cartj, t, wrapper, val); }
	
	public static void receivedCmd(CmdReadyToJoin crtj, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(crtj, t, wrapper, val); }
	
	public static void receivedCmd(CmdAckDataRequestOnReconnect adror, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(adror, t, wrapper, val); }
	
	public static void receivedCmd(CmdDataRequestOnReconnect dror, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(dror, t, wrapper, val); }
	
	public static void receivedCmd(CmdDataRequest dr, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(dr, t, wrapper, val); }
	
	public static void receivedCmd(CmdDataReceived dr, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(dr, t, wrapper, val); }
	
	public static void receivedCmd(CmdAckReadyToCloseConn artcc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(artcc, t, wrapper, val); }
	
	public static void receivedCmd(CmdReadyToCloseConn rtcc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(rtcc, t, wrapper, val); }
	public static void receivedCmd(CmdAckCloseDataRequest acdr, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(acdr, t, wrapper, val); }
	
	public static void receivedCmd(CmdAckJoinConn ajc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(ajc, t, wrapper, val); }
	public static void receivedCmd(CmdAckNewConn acn, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(acn, t, wrapper, val); }
	public static void receivedCmd(CmdJoinConn cj, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(cj, t, wrapper, val); }
	
	public static void receivedCmd(CmdAckCloseConn ack, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(ack, t, wrapper, val); }
	public static void receivedCmd(CmdData abs, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(abs, t, wrapper, val); }
	
	public static void receivedCmd(CmdNewConn nc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(nc, t, wrapper, val); }
	
	public static void receivedCmd(CmdCloseConn cc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(cc, t, wrapper, val); }
	public static void receivedCmd(CmdJoinCloseConn jcc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(jcc, t, wrapper, val); }
	
	
	public static void receivedCmd(CmdAckJoinCloseConn ajcc, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(ajcc, t, wrapper, val); }
	
	public static void receivedCmd(CmdCloseDataRequestNew cdrn, Triplet t, ISocketTLWrapper wrapper, int val) { receivedInner(cdrn, t, wrapper, val); }
	
	
	// TODO: Add DEBUG check to callers of this method
	private static void receivedInner(CmdAbstract abs, Triplet t, ISocketTLWrapper wrapper, int val) {
//		String outVal = "Command received: "+abs.toString()+"  "+t+" global-id:"+wrapper.getGlobalId();
		
//		out(outVal, val);
		
		String tripletStr = t != null ? t.toString()+"@"+wrapper.getGlobalId() : "";
		
		String result = "[new] ";
		
		if(t.areWeConnector()) {
			result += format(utilLeftWidth(tripletStr, 20) + empty(4)+ "<-", abs.toString(), "" );
		} else {
			
			result +=  format("", abs.toString(), utilLeftWidth("->", 4)+tripletStr );
		}
		
		out(result, val);
		
	}
	
	private static String format(String left, String center, String right) {
		return utilLeftWidth(left, 25) + utilLeftWidth(center, 120)+empty(4)+utilLeftWidth(right, 15); 
	}


	
	
	
	
	public static void sent(CmdAckReadyToJoin nc, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, nc, t, sock, val); }
	
	public static void sent(CmdNewConn nc, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, nc, t, sock, val); }
	public static void sent(CmdReadyToJoin data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdAckCloseDataRequest data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	
	public static void sent(CmdAckReadyToCloseConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	
	public static void sent(CmdAckDataRequestOnReconnect data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdAckJoinCloseConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdAckCloseConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdAckNewConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdAckJoinConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	
	public static void sent(CmdDataRequest data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdReadyToCloseConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	
	public static void sent(CmdData data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(String debug, CmdData data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(debug, data, t, sock, val); }
	
	public static void sent(CmdJoinConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdJoinCloseConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdCloseConn data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdCloseDataRequestNew data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	public static void sent(CmdDataReceived data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	
	public static void sent(CmdDataRequestOnReconnect data, Triplet t, ISocketTLWrapper sock, int val) { sentInner(null, data, t, sock, val); }
	
	private static void sentInner(String debug, CmdAbstract abs, Triplet t, ISocketTLWrapper wrapper, int val) {

		String globalId = "";
		if(wrapper != null) {
			globalId = "@"+wrapper.getGlobalId();
		}
//		
//		if(debug != null) {
//			out("Command sent ["+debug+"]: "+abs.toString()+"  "+t+globalId, val);
//		} else {
//			out("Command sent: "+abs.toString()+"  "+t+globalId, val);			
//		}

		String tripletStr = t != null ? t.toString()+globalId : "";
		
		
		String result = "[new] ";
		
		if(t.areWeConnector()) {
			result += format(utilLeftWidth(tripletStr, 20) + empty(4)+ "->", abs.toString(), "");
		} else {
			
			result +=  format("", abs.toString(), utilLeftWidth("<-", 4)+tripletStr );
		}
		
		out(result, val);

		
	}
	
	public static void stateChange(NBSocket sock, Triplet t, Entry.State from, Entry.State to) {		
//		NBLog.debug("[nbsock "+sock.getDebugId() + "] triplet: "+t+"   ["+sock.isDebugFromServSock()+"] State "+from.name()+" to "+to.name(), NBLog.INTERESTING);
		
		String tripletStr = t != null ? t.toString() : "";
		
		String fromToStr = "State "+from.name()+" to "+to.name(); 
		
		String result = "[new] ";
		
		if(t != null) {
		
			if(t.areWeConnector()) {
				result += format(utilLeftWidth(tripletStr, 15), fromToStr, "");
			} else {
				result += format("", fromToStr, empty(4)+tripletStr);
			}
		} else {
			result += format("", fromToStr, "");
		}
		
		debug(result, INTERESTING);
		
	}
	
	private static String empty(int width) {
		String result = "";
		
		while(result.length() < width) {
			result += " ";
		}
		
		return result;
		
	}
	
	private static String utilLeftWidth(final String str, int width) {
		String result = str;
		while(result.length() < width) {
			result += " ";
		}
		
		return result;
	}

	@SuppressWarnings("unused")
	private static String utilRightWidth(final String str, int width) {
		String result = str;
		while(result.length() < width) {
			result = " " + result;
		}
		
		return result;
	}
	

	// --------------------------------------------------------

	public static void debug(String s, int level) {
		out(s, level);
	}

	
	public static void error(String s) {
		
		String stackTrace = SoAnUtil.convertStackTrace(new Throwable());
		err("[thread "+Thread.currentThread().getId()+"] "+ s+ " "+stackTrace);
	}
	
	
	/** Definition of severe: if a severe occurs it implies a problem with the code itself, as in "this error condition could should never happen." */
	public static void severe(String s) {
//		while(true) {
//			try {
				err("Severe: " +s);
				logListener.printErr(s);
				logListener.flushErr();
				
//				try {
//					Thread.sleep(5 * 1000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				
//				System.exit(0);
				
//				SoAnUtil.makeNoise();
//				
//				Thread.sleep(2 * 1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			
//		}
	}
	
	
	
	public static class StdOutLogListener implements NBLogLogListener {

		@Override
		public void printErr(String logStr) {
			System.err.println(logStr);
		}

		@Override
		public void printOut(String logStr) {
			System.out.println(logStr);
		}

		@Override
		public void flushErr() {
			System.err.flush();
		}

		@Override
		public void flushOut() {
			System.out.flush();
		}
		
	}
	
	public static interface NBLogLogListener {
		
		public void printErr(String logStr);
		public void printOut(String logStr);
		
		public void flushErr();
		public void flushOut();
	}
}
