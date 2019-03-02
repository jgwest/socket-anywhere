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

import java.io.IOException;

/** 
 * 
 * Contains entry data for one specific connection; each IRCSocketImpl will have a single corresponding
 * IRCSocketConnection. There is a 1-1 relationship between the two. 
 * 
 * Class that contains the status of  
 * 
 * If we are connection originator is true, then remoteConnectID will be set
 * if we are connection originator is false, then ourConnectionID will be set
 *
 */
public class IRCSocketConnection {
	
	// If we were connected to (Server)
	IRCSocketAddress _listeningAddress;
	long _ourConnectionID;
	
	// If we originated the connection (client)
	IRCSocketAddress _remoteAddress;
	long _remoteConnectionID;
	
	// General Fields
	String _remoteUUID;
	IRCInputStream _inputStream;
	IRCOutputStream _outputStream;
	
	/** Synonym for are we the client / are we not the server */
	boolean _areWeConnOriginator = false;
	
	// Connection status
	boolean _closed = false;
	
	NodeImpl _node;
	
	Object _objectLock = new Object();
	
	public IRCSocketConnection(NodeImpl node) {
		_node = node;
	}
	
	
	public IRCSocketAddress getListeningAddress() {
		return _listeningAddress;
	}
	public void setListeningAddress(IRCSocketAddress listeningAddress) {
		_listeningAddress = listeningAddress;
	}
	
	public IRCSocketAddress getRemoteAddress() {
		return _remoteAddress;
	}
	public void setRemoteAddress(IRCSocketAddress remoteAddress) {
		_remoteAddress = remoteAddress;
	}
	
	public String getRemoteUUID() {
		return _remoteUUID;
	}
	
	public void setRemoteUUID(String remoteUUID) {
		_remoteUUID = remoteUUID;
	}
	
	public boolean areWeConnOriginator() {
		return _areWeConnOriginator;
	}
	
	public void setAreWeConnOriginator(boolean areWeConnOriginator) {
		_areWeConnOriginator = areWeConnOriginator;
	}
	
	
	public long getOurConnectionID() {
		return _ourConnectionID;
	}
	
	public void setOurConnectionID(long ourConnectionID) {
		_ourConnectionID = ourConnectionID;
	}
	
	public long getRemoteConnectionID() {
		return _remoteConnectionID;
	}
	public void setRemoteConnectionID(long remoteConnectionID) {
		_remoteConnectionID = remoteConnectionID;
	}
	
	
	public void setInputStream(IRCInputStream inputStream) {
		_inputStream = inputStream;
	}
	
	public IRCInputStream getInputStream() {
		return _inputStream;
	}
	
	public void setOutputStream(IRCOutputStream outputStream) {
		_outputStream = outputStream;
	}
	
	public IRCOutputStream getOutputStream() {
		return _outputStream;
	}

	private static boolean areBothNull(Object a, Object b) {
		if(a == null && b == null) return true;
		else return false;
	}
	
	private static boolean isOnlyOneNull(Object a, Object b) {
		if(a == null && b != null) return true;
		if(a != null && b == null) return true;
		return false;
	}
	
	
	@Override
	public boolean equals(Object a) {
		
		if(a == null) return false;
		if(!(a instanceof IRCSocketConnection)) return false;
		IRCSocketConnection b = (IRCSocketConnection)a;
		
		if(!areBothNull(_listeningAddress, b.getListeningAddress())) {
			
			if(isOnlyOneNull(_listeningAddress, b.getListeningAddress())) {
				return false;
			} else {
				if(!_listeningAddress.equals(b.getListeningAddress())){
					return false;
				}
			}
		}
		
		if(areWeConnOriginator() != b.areWeConnOriginator()) {
			return false;
		}

		if(getOurConnectionID() != b.getOurConnectionID()) {
			return false;
		}
		
		if(getRemoteConnectionID() != b.getRemoteConnectionID()) {
			return false;
		}
		
		if(!areBothNull(getRemoteUUID(), b.getRemoteUUID())) {
			if(isOnlyOneNull(getRemoteUUID(), b.getRemoteUUID())) {
				return false;
			} else {
				if(!getRemoteUUID().equals(b.getRemoteUUID())) {
					return false;
				}
			}
		}
		
		if(!areBothNull(_listeningAddress, b.getListeningAddress())) {
			
			if(isOnlyOneNull(_listeningAddress, b.getListeningAddress())) {
				return false;
			} else {
				if(!_listeningAddress.equals(b.getListeningAddress())) {
					return false;
				}
			}
			
		}


		if(!areBothNull(getRemoteAddress(), b.getRemoteAddress())) {
			if(isOnlyOneNull(getRemoteAddress(), b.getRemoteAddress())) {
				return false;
			} else {
				if(!getRemoteAddress().equals(b.getRemoteAddress())) {
					return false;
				}
			}
		}

		return true;
		
	}
	
	@Override
	public String toString() {
		
		String result = "";
		if(_areWeConnOriginator) {
			result += "connected to remote-addr:("+_remoteAddress+") remote-conn-id:("+_remoteConnectionID+")";
		} else {
			result += "connected to our-addr:("+_listeningAddress+") our-conn-id:("+_ourConnectionID+")";
		}
		
		result += " remote-uuid:("+_remoteUUID+")"; 		
		
		return result;
	}
	
	
	public void close() throws IOException {
		close(true);
	}
	
	/** This is called by the IRC listener that listens for !close-socket from remote
	 * hosts. */
	protected void informClose() throws IOException {
		close(false);
	}
	
	private void close(boolean outputCloseToChannel) throws IOException {
		
		synchronized (_objectLock) {
			if(_closed) return;
			else _closed = true;
		}
		
		_inputStream.internalClose();
		_outputStream.internalClose();
		
		_node.closeSocketConnection(this, outputCloseToChannel);
		
		
	}
		
	public boolean isClosed() {
		return _closed;
	}
}
