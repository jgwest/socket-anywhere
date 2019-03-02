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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public final class RCConnectionBrain implements IRCConnectionBrain {	
	
	private Object _lock = new Object();
		
	/*---*/ List<RCInnerListenEntry> _listenEntries = new ArrayList<RCInnerListenEntry>();
		
//	/*---*/ HopperConnectionThread _connThread;
//	/*---*/ HopperAcceptThread _acceptThread;

	ISocketAcquisition _sockAcq;
	
	
	Object _lockNotifyOfNewListenEntry = new Object(); // .notify() is called on this when a new listen entry
		
	
	
	public RCConnectionBrain(/*ISocketFactory innerFactory,*/ ISocketAcquisition sockAcq) {
//		_innerFactory = innerFactory;
		_sockAcq = sockAcq;
	}


	@Override
	public void debugMsg(String str) {	
		if(DEBUG) {
			System.out.println("("+Thread.currentThread().getId()+") "+str);
		}
	}
	
	/*---*/ List<RCInnerIncomingRequestEntry> _listenEntryRequests = new ArrayList<RCInnerIncomingRequestEntry>();

	@Override
	public void activate() {
		
		_sockAcq.start(this);
		
//		debugMsg("Connection brain started. ");
//		
//		if(_innerConnectAddr != null) {
//			_connThread = new HopperConnectionThread(_innerConnectAddr, _innerFactory, this);
//			_connThread.start();
//			
//		} else if(_innerListenAddr != null) {
//			_acceptThread = new HopperAcceptThread(_innerFactory, _innerListenAddr, this);
//			_acceptThread.start();
//			
//		} else {
//			throw new RuntimeException("User must set either listen address or connect address.");
//		}
//		
		
	}
	
	/** Command received by hopper connect thread by RCSocketReaderThread */
	@Override
	public void eventReceivedConnectCmd(RCGenericCmd cmd, ISocketTL socket) throws IOException {

		debugMsg("Brain received connection cmd: "+cmd);

		// Inform the connection thread to open a new socket; this socket will either be closed (on REJECT), or acquired (on ACCEPT) 
//		_sockAcq._connThread.eventCreateNewConnection();
		_sockAcq.informSocketAcquired();
		
		boolean matched = false;

		synchronized(_lock) {
			
			for(RCInnerListenEntry rcile : _listenEntries) {
				
				if(rcile._listenAddr.equals(cmd.getAddr())) {
					matched = true;
				}
			}
		}

		if(!matched) {
			
			RCGenericCmd rejectCmd = new RCGenericCmd();
			rejectCmd.setAddr(cmd.getAddr());
			rejectCmd.setCommandName(RCGenericCmd.CMD_REJECT_CONNECT);
			rejectCmd.setUuid(cmd.getUuid());
			
			debugMsg("Brain rejected connection cmd, with this:"+rejectCmd);
			
			byte[] msg = rejectCmd.generateCmdBytes();
			socket.getOutputStream().write(msg);
			socket.getOutputStream().flush();
			socket.close();
			return;
			
		} else {
			RCGenericCmd acceptCmd = new RCGenericCmd();
			acceptCmd.setAddr(cmd.getAddr());
			acceptCmd.setCommandName(RCGenericCmd.CMD_ACCEPT_CONNECT);
			acceptCmd.setUuid(cmd.getUuid());
			
			debugMsg("Brain accepted connection cmd, with this:"+acceptCmd);
			
			byte[] msg = acceptCmd.generateCmdBytes();
			socket.getOutputStream().write(msg);
			socket.getOutputStream().flush();
			
		}
		
		
		debugMsg("Brain adding incoming request for cmd: "+cmd);

		RCInnerIncomingRequestEntry e = new RCInnerIncomingRequestEntry();
		e._addr = cmd.getAddr();
		e._socket = socket;
		
		synchronized(_lockNotifyOfNewListenEntry) {
			
			_listenEntryRequests.add(e);
			_lockNotifyOfNewListenEntry.notify();
		}
		
	}
	
	// Called by server socket
	@Override
	public ISocketTL blockUntilAccept(TLAddress addr) {
		debugMsg("Server socket blocking until accept: "+addr);
		ISocketTL result = null;
		
		while(result == null) {
			
			synchronized(_lockNotifyOfNewListenEntry) {
				for(Iterator<RCInnerIncomingRequestEntry> it = _listenEntryRequests.iterator(); it.hasNext();) {
					RCInnerIncomingRequestEntry e = it.next();
					
					if(e._addr.equals(addr)) {
						result = e._socket;
						it.remove();
						break;
					}
					
				}
				
				if(result == null) {
					try { _lockNotifyOfNewListenEntry.wait(1000); } catch(InterruptedException e) {}
				}
			}
			
		}
		
		debugMsg("Server socket returned accept result: "+result);
		return result;
	}
	
	@Override
	public void removeServSockListenAddr(TLAddress addr) throws IOException {
		debugMsg("Server socket unbound from "+addr);
		
		synchronized(_lock) {
			for(Iterator<RCInnerListenEntry> it = _listenEntries.iterator(); it.hasNext();) {
				RCInnerListenEntry e = it.next();
				if(e._listenAddr.equals(addr)) {
					it.remove();
				}
			}
		}
	}
	
	public void addServSockListenAddr(TLAddress addr) throws IOException {
		debugMsg("Server socket bound to  "+addr);
		
		RCInnerListenEntry entry = new RCInnerListenEntry();
		entry._listenAddr = addr;
		
		synchronized(_lock) {
			for(RCInnerListenEntry e : _listenEntries) {
				if(e._listenAddr.equals(addr)) {
					throw new IOException("Another socket of this factory is already listening on this thread.");
				}
			}
			_listenEntries.add(entry);
		}
		
	}
	
	/** Called by RCSocketImpl */
	@Override
	public ISocketTL retrieveAndAcquireSocket() {
		// set s3 thing
		// wait for accept on _innerListenAddr
		// return that socket

		return _sockAcq.retrieveAndAcquireAcceptSocket();
		
//		
//		ISocketTL result = _acceptThread.acquireActiveHopperSocket();
//		
//		
//		assert(result != null);
//		
//		return result;
	}
	
}

class RCInnerIncomingRequestEntry {
	TLAddress _addr;
	ISocketTL _socket;
	
	public RCInnerIncomingRequestEntry() {
	}
}

class RCInnerListenEntry {
	TLAddress _listenAddr;
	
	public RCInnerListenEntry() {
	}
}
