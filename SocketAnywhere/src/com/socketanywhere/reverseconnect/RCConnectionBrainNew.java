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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import com.socketanywhere.multiplexingnew.MQMessage;
import com.socketanywhere.multiplexingnew.MessageQueue;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public final class RCConnectionBrainNew extends Thread implements IRCConnectionBrain {	

	private static final String EVENT_RECEIVED_CONNECT_CMD = "EVENT_RECEIVED_CONNECT_CMD";
		
	private static final String BLOCK_UNTIL_ACCEPT = "BLOCK_UNTIL_ACCEPT"; 

	protected static String EVENT_RECEIVED_CONNECT_CMD_THREAD_RESPONSE = "EVENT_RECEIVED_CONNECT_CMD_THREAD_RESPONSE";
	
	private static final String REMOVE_SERV_SOCK_LISTENER = "REMOVE_SERV_SOCK_LISTENER";
	private static final String ADD_SERV_SOCK_LISTENER = "ADD_SERV_SOCK_LISTENER";

	private ISocketAcquisition _sockAcq;
	
	private MessageQueue _queue = new MessageQueue(this);

	public RCConnectionBrainNew(ISocketAcquisition sockAcq) {
		super(RCConnectionBrainNew.class.getName());
		setDaemon(true);
		_sockAcq = sockAcq;
	}
	
	@Override
	public void run() {

		/** List of open sockets attempting to connect to us */
		List<RCInnerIncomingRequestEntry> listenEntryRequests = new ArrayList<RCInnerIncomingRequestEntry>();
		
		/** A list of server threads (and their address) that are waiting in accept() calls. */
		Map<TLAddress, List<MessageQueue>> threadsWaitingForAccept = new HashMap<TLAddress, List<MessageQueue>>();
		
		/** Server addresses we are listening on */
		List<RCInnerListenEntryNew> listenEntries = new ArrayList<RCInnerListenEntryNew>();

		
		boolean continueLoop = true;

		Queue<MQMessage> unprocessedMessages = null;
		while(continueLoop) {
			
			if(unprocessedMessages == null || unprocessedMessages.size() == 0) {
				unprocessedMessages = _queue.getNextMessagesBlocking(false);
			}
			
			MQMessage currMessage = unprocessedMessages.poll();

			debugMsg("currMessage: "+currMessage);
			
			if(currMessage.getName().equalsIgnoreCase(BLOCK_UNTIL_ACCEPT)) {
				TLAddress addr = (TLAddress)currMessage.getParam();
				
				List<MessageQueue> l = threadsWaitingForAccept.get(addr);
				if(l == null) {
					l = new ArrayList<MessageQueue>();
					threadsWaitingForAccept.put(addr, l);
				}
				l.add(currMessage.getResponseQueue());
				
				transferOpenSocketToAccept(threadsWaitingForAccept, listenEntryRequests);
				
			} else if(currMessage.getName().equalsIgnoreCase(ADD_SERV_SOCK_LISTENER)) {
				handleAddServSockListener(currMessage, listenEntries); 
				
			} else if(currMessage.getName().equalsIgnoreCase(REMOVE_SERV_SOCK_LISTENER)) {
				handleRemoveServSockListener(currMessage, listenEntries); 
				
			} else if(currMessage.getName().equalsIgnoreCase(EVENT_RECEIVED_CONNECT_CMD_THREAD_RESPONSE)) {
				
				listenEntryRequests.add((RCInnerIncomingRequestEntry)currMessage.getParam());
				
				transferOpenSocketToAccept(threadsWaitingForAccept, listenEntryRequests);
				
				
			} else if(currMessage.getName().equalsIgnoreCase(EVENT_RECEIVED_CONNECT_CMD)) {
				boolean matched = false;

				Object[] params = (Object[]) currMessage.getParam();

				RCGenericCmd cmd = (RCGenericCmd)params[0];
				ISocketTL socket = (ISocketTL)params[1];
				
				for(RCInnerListenEntryNew rcile : listenEntries) {
					
					if(rcile._listenAddr.equals(cmd.getAddr())) {
						matched = true;
					}
				}
				
				EventReceivedConnectCmdThread ercct = new EventReceivedConnectCmdThread(cmd, socket, _sockAcq, matched, _queue);
				ercct.start();

			}
			
		}
	}
	
	private static void transferOpenSocketToAccept(Map<TLAddress, List<MessageQueue>> threadsWaitingForAccept, List<RCInnerIncomingRequestEntry> listenEntryRequests) {

		for(Iterator<RCInnerIncomingRequestEntry> it = listenEntryRequests.iterator(); it.hasNext();) {
			// For each socket attempting to connect to us
			RCInnerIncomingRequestEntry rcii = it.next();
			
			
			// Find a listening server socket that is waiting
			List<MessageQueue> mq = threadsWaitingForAccept.get(rcii._addr);
			if(mq != null ) {
				if(mq.size()  > 0)  {
					it.remove();
					
					MessageQueue targetQueue = mq.remove(0);
					
					MQMessage responseMessage = new MQMessage(null, RCConnectionBrainNew.class, rcii._socket, null);
					
					targetQueue.addMessage(responseMessage);
					
				}
				
			}
			
		}
		
	}
	
	private static void handleRemoveServSockListener(MQMessage msg, List<RCInnerListenEntryNew> listenEntries) {
		
		TLAddress addr = (TLAddress)msg.getParam();
		
		for(Iterator<RCInnerListenEntryNew> it = listenEntries.iterator(); it.hasNext();) {
			RCInnerListenEntryNew e = it.next();
			if(e._listenAddr.equals(addr)) {
				it.remove();
			}
		}

	}
	
	private static void handleAddServSockListener( MQMessage msg, List<RCInnerListenEntryNew> listenEntries) {
		
		TLAddress addr = (TLAddress)msg.getParam();
		
		String errorMsg = null;
		
		for(RCInnerListenEntryNew e : listenEntries) {
			if(e._listenAddr.equals(addr)) {
				errorMsg = "Another socket of this factory is already listening on this thread.";
				break;
			}
		}
		
		if(errorMsg == null) {
			RCInnerListenEntryNew entry = new RCInnerListenEntryNew();
			entry._listenAddr = addr;

			listenEntries.add(entry);
		} else {
			/** Send the reply with the message below: this will cause an exception to be thrown. */
		}
		
		MQMessage response = new MQMessage(null, null, errorMsg, null);
		msg.getResponseQueue().addMessage(response);

	}
	

	@Override
	public void debugMsg(String str) {	
		if(DEBUG) {
			System.out.println("("+Thread.currentThread().getId()+") "+str);
		}
	}
	

	@Override
	public void activate() {
		this.start();
		_sockAcq.start(this);
				
	}
	
	/** Command received by hopper connect thread by RCSocketReaderThread */
	@Override
	public void eventReceivedConnectCmd(RCGenericCmd cmd, ISocketTL socket) throws IOException {

		debugMsg("Brain received connection cmd: "+cmd);
		
		MQMessage msg = new MQMessage(EVENT_RECEIVED_CONNECT_CMD, RCConnectionBrainNew.class, new Object[] { cmd,  socket }, null);

		_queue.addMessage(msg);
		
	}
	
	
	// Called by server socket
	@Override
	public ISocketTL blockUntilAccept(TLAddress addr) {
		debugMsg("Server socket blocking until accept: "+addr);
		ISocketTL result = null;
		
		
		MessageQueue responseQueue = new MessageQueue(BLOCK_UNTIL_ACCEPT);
		
		MQMessage mqm = new MQMessage(BLOCK_UNTIL_ACCEPT, null, addr, responseQueue);
		
		_queue.addMessage(mqm);
		
		MQMessage responseMessage = responseQueue.getNextMessageBlocking();
	
		
		result = (ISocketTL)responseMessage.getParam();
		
		debugMsg("Server socket returned accept result: "+result);
		return result;
	}
	
	@Override
	public void removeServSockListenAddr(TLAddress addr) throws IOException {
		debugMsg("Server socket unbound from "+addr);
				
		MQMessage msg = new MQMessage(REMOVE_SERV_SOCK_LISTENER, RCConnectionBrainNew.class, addr, null);
	
		_queue.addMessage(msg);
		
	}
	
	
	public void addServSockListenAddr(TLAddress addr) throws IOException {
		debugMsg("Server socket bound to  "+addr);
		
		
		MessageQueue responseQueue = new MessageQueue(ADD_SERV_SOCK_LISTENER);
		
		MQMessage msg = new MQMessage(ADD_SERV_SOCK_LISTENER, null, addr, responseQueue);
	
		_queue.addMessage(msg);
		
		MQMessage reply = responseQueue.getNextMessageBlocking();
	
		if(reply.getParam() != null) {
			// An error occured.
			throw new RuntimeException((String)reply.getParam());
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

class EventReceivedConnectCmdThread extends Thread {
	
	final private boolean _matched;
	final private RCGenericCmd _cmd;
	final private ISocketTL _socket;
	final private ISocketAcquisition _sockAcq;
	
	final private MessageQueue _responseQueue;
	
	public EventReceivedConnectCmdThread(RCGenericCmd cmd, ISocketTL socket, ISocketAcquisition sockAcq, boolean matched, MessageQueue responseQueue) {
		super(EventReceivedConnectCmdThread.class.getName());
		this._matched = matched;
		this._cmd = cmd;
		this._socket = socket;
		this._sockAcq = sockAcq;
		this._responseQueue = responseQueue;
		setDaemon(true);
	}
	
	@Override
	public void run() {
		try {
			innerRun();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void innerRun() throws IOException {
		// Inform the connection thread to open a new socket; this socket will either be closed (on REJECT), or acquired (on ACCEPT) 
//		_sockAcq._connThread.eventCreateNewConnection();

		_sockAcq.informSocketAcquired();
		
		if(!_matched) {
			
			RCGenericCmd rejectCmd = new RCGenericCmd();
			rejectCmd.setAddr(_cmd.getAddr());
			rejectCmd.setCommandName(RCGenericCmd.CMD_REJECT_CONNECT);
			rejectCmd.setUuid(_cmd.getUuid());
			
			System.out.println("Brain rejected connection cmd, with this:"+rejectCmd);
			
			byte[] msg = rejectCmd.generateCmdBytes();
			_socket.getOutputStream().write(msg);
			_socket.getOutputStream().flush();
			_socket.close();
			return;
			
		} else {
			RCGenericCmd acceptCmd = new RCGenericCmd();
			acceptCmd.setAddr(_cmd.getAddr());
			acceptCmd.setCommandName(RCGenericCmd.CMD_ACCEPT_CONNECT);
			acceptCmd.setUuid(_cmd.getUuid());
			
			System.out.println("Brain accepted connection cmd, with this:"+acceptCmd);
			
			byte[] msg = acceptCmd.generateCmdBytes();
			_socket.getOutputStream().write(msg);
			_socket.getOutputStream().flush();
			
		}
		
		System.out.println("Brain adding incoming request for cmd: "+_cmd);

		RCInnerIncomingRequestEntry e = new RCInnerIncomingRequestEntry();
		e._addr = _cmd.getAddr();
		e._socket = _socket;
		
		MQMessage response = new MQMessage(RCConnectionBrainNew.EVENT_RECEIVED_CONNECT_CMD_THREAD_RESPONSE,EventReceivedConnectCmdThread.class , e, null);

		_responseQueue.addMessage(response);
		
//		synchronized(_lockNotifyOfNewListenEntry) {
//			
//			_listenEntryRequests.add(e);
//			_lockNotifyOfNewListenEntry.notify();
//		}
	}
}

//class BlockUntilAcceptThread extends Thread {
//	
//	private TLAddress _addr;
//	
//	public BlockUntilAcceptThread(TLAddress addr) {
//		super(BlockUntilAcceptThread.class.getName());
//		_addr = addr;
//	}
//	
//	@Override
//	public void run() {
//		ISocketTL result = null;
//				
//		while(result == null) {
//			
//			for(Iterator<RCInnerIncomingRequestEntry> it = _listenEntryRequests.iterator(); it.hasNext();) {
//				RCInnerIncomingRequestEntry e = it.next();
//				
//				if(e._addr.equals(_addr)) {
//					result = e._socket;
//					it.remove();
//					break;
//				}
//				
//			}
//			
//			if(result == null) {
//				try { _lockNotifyOfNewListenEntry.wait(1000); } catch(InterruptedException e) {}
//			}
//			
//		}
//		
//	}
//	
//}

//class RCInnerIncomingRequestEntry {
//	TLAddress _addr;
//	ISocketTL _socket;
//	
//	public RCInnerIncomingRequestEntry() {
//	}
//}
//
class RCInnerListenEntryNew {
	TLAddress _listenAddr;
//	ISocketTL _socket;
	
	public RCInnerListenEntryNew() {
	}
}
