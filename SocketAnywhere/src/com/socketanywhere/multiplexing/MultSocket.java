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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** The multiplexing socket factory allows the user to create a number of Socket instances, but have
 * all data that is passed to these sockets actually be sent/received by a single socket connection behind the scenes. 
 * 
 *  The MultSocket handles connection initiation, but otherwise, just does what it is told to by 
 *  the connection brain. */
public class MultSocket implements ISocketTL {

	Object _socketLock = new Object();

	int _nextPacketId = 0;
	ISocketTL _inner;

	/** If we initiated the connection, this is the address of the connection we connected to. */
	TLAddress _remoteAddr;
	
	boolean _isConnected = false; 
	boolean _isClosed = false; 
	
	Object _connectedLock = new Object();
	
	MultConnectionBrain _cb;

	/** Streams for the socket */
	MultSocketInputStream _inputStream;
	MultSocketOutputStream _outputStream;
	
	boolean _didWeInitConnection = false;
	String _connectionNodeUUID = null;
	int _connectionId = -1;
	
	String _debugStr;
	
	public MultSocket(TLAddress remoteAddr, ISocketFactory factory) throws IOException {
		_inner = null;
		_remoteAddr = remoteAddr;
		
		_cb = new MultConnectionBrain();
		_cb.setInnerSocketFactory(factory);
		
		this.connect(remoteAddr);
	}
	

	protected MultSocket(ISocketTL inner) {
		_cb = new MultConnectionBrain();
	
		_inner = inner;
	
		_cb.setInnerSocket(inner);
	}

		
	protected MultSocket(TLAddress addr, MultConnectionBrain cb) throws IOException {
		_cb = cb;
		_remoteAddr = addr;
		this.connect(addr);
		
	}

	
	/** Called by serversocket */
	protected MultSocket(MultConnectionBrain cb) throws IOException {
		_cb = cb;
	}	
	
	
	/** Called by server socket after the above constructor */
	protected void setRemoteAddr(TLAddress remoteAddr) {
		this._remoteAddr = remoteAddr;
	}
	
	
	
	/** Whether or not the socket was created by an accept() call of the server*/
	protected void serverInit(ISocketTL socket, String connUUID, int connId) {
		if(_inner != null && _inner != socket) {
			System.err.println("serverInit called when inner socket was already specified.");
		}
		
		_inner = socket;
		_isConnected = true;
		_inputStream = new MultSocketInputStream(this);
		_outputStream = new MultSocketOutputStream(this, _cb, connUUID, connId);
		
		_didWeInitConnection = false;
		_connectionNodeUUID = connUUID;
		_connectionId = connId;
		
		
	}
	
//	protected void closeInnerSocket() {
//		if(_inner == null || _inner.isClosed()) return;
//
//		try {
//			_inner.close();
//		} catch (IOException e) { /** No need to worry about this. */}
//	}
	
	/** Called when the connection is terminated by the remote host */
	protected void informRemoteClose() {
		MuLog.dbg("MultSocket.informRemoteClose() called - current isClosed:"+_isClosed+" this:"+this);
		
		_isClosed = true;
		_isConnected = false;
		_inputStream.informConnectionClosed();
		MuLog.socketClosed(this);
	}

		
	@Override
	public void close() throws IOException {

		MuLog.dbg("MultSocket.close() called - current isClosed:"+_isClosed);
		
		if(_isClosed) return;
		
		_isClosed = true;
		
		_isConnected = false;
		_inputStream.informConnectionClosed();
		
		MuLog.socketClosed(this);
		_cb.eventLocalInitClose(this);
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Support timeout.
		this.connect(endpoint);
	}
	
	@Override
	public void connect(TLAddress endpoint) throws IOException {
		_isConnected = false;	
		
		_remoteAddr = endpoint;
		_inputStream = new MultSocketInputStream(this);

		if(_cb.getInnerSocket() == null) {
			_inner = _cb.connectInnerFactory();
		
		} else  if(_inner == null) {
			_inner = _cb.getInnerSocket();
		}


		int connId = _cb.initiateConnection(this);
		if(connId == -1) {
			MuLog.error("Unable to connect");
			throw new IOException("Unable to connect.");
		}
//		_didWeInitConnection = true;
//		_connectionId = connId;
//		_connectionNodeUUID = _cb._ourUuid;  
		
		// Block until connect
		
		while(!_isConnected) {
			synchronized(_connectedLock) {
				try {
					_connectedLock.wait(5000);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
		
		_outputStream = new MultSocketOutputStream(this, _cb, _cb._ourUuid, connId);
		
	}

	
	/** Called by connection brain to let us know our socket has connected */
	public void eventSignalConnected() {
		_isConnected = true;
		synchronized(_connectedLock) {
			_connectedLock.notify();
		}
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		return _inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return _outputStream; 
	}

	@Override
	public boolean isClosed() {
		return _isClosed;
	}

	@Override
	public boolean isConnected() {
		return _isConnected;
	}

	@Override
	public TLAddress getAddress() {
		return _remoteAddr;
	}

	protected int getNextPacketId() {
		synchronized(_socketLock) {
			// This is useful for debugging; could easily just be _nextPacketId++; No architectural reason to start at 1000.
			return (_didWeInitConnection ? 0 : 1000) + _nextPacketId++;
		}
	}
	
//	protected void newSocketTransferredFromServerSocket(ISocketTL s) {
//		synchronized(_socketLock) {
//			_inner = s;
//		}
//		
//	}
	
	protected void flushInnerSocket() throws IOException {
		try {
			synchronized(_socketLock) {
				_inner.getOutputStream().flush();
			}
		} catch(Exception e) {
			MuLog.debug("Exception on flushInnerSocket() - "+e);
		}
	}

	
	protected void writeToInnerSocket(byte[] b) throws IOException {
		try {
			synchronized(_socketLock) {
				synchronized(_inner) {
					_inner.getOutputStream().write(b);
				}
			}
		} catch(Exception e) {
			if(MuLog.DEBUG) {
				e.printStackTrace();
			}
			MuLog.debug("Exception on writeToInnerSocket() - "+e);
		}
	}

	protected void eventReceivedDataCmdFromRemoteConn(CmdDataMultiplex d) {
		_inputStream.informDataReceived(d);
	}
	
//	protected boolean isUnderlyingSocketConnected() {
//		if(_inner != null) {
//			return _inner.isConnected();
//		} else {
//			return false;
//		}
//	}
	
	@Override
	public int hashCode() {
		
		try {
			if(_connectionNodeUUID != null) {
				return _connectionNodeUUID.hashCode() + _connectionId;
			} else {
				return 0;
			}
		} catch(Exception e) {
			return 0;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		MultSocket ms = (MultSocket)obj;
				
		// Ignore un-inited connections
		if(ms._connectionId == -1 || _connectionId == -1) return false;

		if(ms._cb != _cb) return false;
		if(ms._didWeInitConnection != _didWeInitConnection) return false;
		if(ms._connectionId != _connectionId) return false;
		if(!ms._connectionNodeUUID.equals(_connectionNodeUUID)) return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		return "[MultSocket] "+(_debugStr != null ? ("dbg-str:.["+_debugStr+"]. ") : "" )+" did-we-init:"+_didWeInitConnection+" uuid:"+_connectionNodeUUID+" conn-id:"+_connectionId +"  , inner:i{"+_inner+"}i ";
	}

	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}

	@Override
	public String getDebugStr() {
		return _debugStr;
	}
	
	public int getConnectionId() {
		return _connectionId;
	}
	
	public String getConnectionNodeUUID() {
		return _connectionNodeUUID;
	}
	
	public void setConnectionNodeUUID(String connectionNodeUUID) {
		this._connectionNodeUUID = connectionNodeUUID;
	}
}
