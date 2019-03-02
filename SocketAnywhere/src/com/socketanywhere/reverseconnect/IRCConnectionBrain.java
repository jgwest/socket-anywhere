package com.socketanywhere.reverseconnect;

import java.io.IOException;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public interface IRCConnectionBrain {

	public final static long TIMEOUT_ON_CONNECTION_FAIL_IN_MSECS = 60 *1000l;
	public final static long TIMEOUT_ON_ACCEPT_FAIL_IN_MSECS = 60 * 1000;

	public final static boolean DEBUG = false;
	
	public ISocketTL retrieveAndAcquireSocket();
	
	public void debugMsg(String str);
	

	public void addServSockListenAddr(TLAddress addr) throws IOException;
	
	public void removeServSockListenAddr(TLAddress addr) throws IOException;
	
	public ISocketTL blockUntilAccept(TLAddress addr);
	
//	public TLAddress getInnerConnectAddr();
//	
//	public TLAddress getInnerListenAddr();
	
	public void eventReceivedConnectCmd(RCGenericCmd cmd, ISocketTL socket) throws IOException;
	
	public void activate();
}
