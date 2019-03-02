/*
	Copyright 2012, 2013 Jonathan West

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

package com.socketanywhere.nonbreaking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Wraps an inner socket, and has the ability to detect when that socket dies; if it dies,
 * this factory will attempt to re-establish it ASAP. The fact that the underlying socket died 
 * will not be made visible to the caller of the NBSocket; it will be as if it never happened. 
 * 
 * In this way, potentially unreliable underyling socket layers can be made reliable using NBSocket.
 * */
public class NBSocket implements ISocketTL {

	Object _socketLock = new Object();

	int _nextPacketId = 0;
	ISocketTL _inner;

	/** If we initiated the connection, this is the address of the connection we connected to. */
	TLAddress _remoteAddr;
	
	boolean _isConnected = false; 
	boolean _isClosed = false; 
	
	ConnectionBrain _cb;
	ISocketFactory _factory;

	/** Streams for the socket */
	NBSocketInputStream _inputStream;
	NBSocketOutputStream _outputStream;
	
	
	NBOptions _options;
	
	String _debugStr;
	
	/** Create a socket; uses default factory (TCP sockets). */
	public NBSocket(TLAddress remoteAddr) throws IOException {
		this(remoteAddr, new TCPSocketFactory());
	}
	
	public NBSocket(TLAddress remoteAddr, ISocketFactory factory) throws IOException {
		_remoteAddr = remoteAddr;
		_factory = factory;
		_options = new NBOptions();
		_cb = new ConnectionBrain(factory, _options);
		this.connect(remoteAddr);
	}

	
	
	public NBSocket(TLAddress remoteAddr, ConnectionBrain cb, ISocketFactory factory, NBOptions options) throws IOException {
		_remoteAddr = remoteAddr;
		_cb = cb;
		_factory = factory;
		_options = options;
		
		this.connect(remoteAddr);
		
	}
	
	protected NBSocket(ConnectionBrain cb, ISocketFactory factory, NBOptions options) {
		_cb = cb;
		_factory = factory;
		_options = options;
	}

	
	/** Called by server socket after the above constructor */
	protected void setRemoteAddr(TLAddress remoteAddr) {
		this._remoteAddr = remoteAddr;
	}
	
	
	
	/** Whether or not the socket was created by an accept() call of the server*/
	protected void serverInit(ISocketTL socket, boolean addSocketListener) {
		_inner = socket;
		_isConnected = true;
		_inputStream = new NBSocketInputStream(this);
		_outputStream = new NBSocketOutputStream(this, _cb);

		if(addSocketListener) {
			NBSocketListenerThread socketListenerThread = new NBSocketListenerThread(_inner, _cb, _options);
			socketListenerThread.start();
		}
		
	}
	
	protected void closeInnerSocket() {
		if(_inner == null || _inner.isClosed()) return;

		try {
			_inner.close();
		} catch (IOException e) { /** No need to worry about this. */}
	}
	
	/** Called when the connection is terminated by the remote host */
	protected void informRemoteClose() {
		_isClosed = true;
		_isConnected = false;
	}
	
	@Override
	public void close() throws IOException {
		_isClosed = true;
		_isConnected = false;
		
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
		_inner = _factory.instantiateSocket(endpoint);
		
		
		_remoteAddr = endpoint;
		_inputStream = new NBSocketInputStream(this);
		_outputStream = new NBSocketOutputStream(this, _cb);

		NBSocketListenerThread socketListenerThread = new NBSocketListenerThread(_inner, _cb, _options);
		socketListenerThread.start();

		_cb.initiateConnection(this);

		// Block until the connection is fully connected.		
		while(!_isConnected)  { 
			_isConnected = _cb.isConnectionEstablished(this);
			if(!_isConnected) {
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			} // TODO: MEDIUM - Infinite close? Might also need to be, && !isClosed()
		}
		
		_isConnected = true;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return _inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return new NBSocketOutputStream(this, _cb);
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
			return _nextPacketId++;
		}
	}
	
	public NBOptions getOptions() {
		return _options;
	}
		
	protected void newSocketTransferredFromServerSocket(ISocketTL s) {
		synchronized(_socketLock) {
			_inner = s;
		}
		
	}
	
	protected void newSocketEstablishedFromRecoveryThread(ISocketTL s) {
		synchronized(_socketLock) {
			_inner = s;
		}
				
	}	
	
	protected void flushInnerSocket() throws IOException {
		try {
			synchronized(_socketLock) {
				_inner.getOutputStream().flush();
			}
		} catch(Exception e) {
			NBLog.debug("Exception on flushInnerSocket() - "+e);
			_cb.eventConnErrDetectedReconnectIfNeeded(_inner);
		}
	}

	
	protected void writeToInnerSocket(byte[] b) throws IOException {
		try {
			synchronized(_socketLock) {
				synchronized(_inner) {
					_inner.getOutputStream().write(b);
				}
			}
		} catch(IOException e) {
			NBLog.debug("Exception on writeToInnerSocket() - "+e);
			_cb.eventConnErrDetectedReconnectIfNeeded(_inner);
			throw(e);
		}
	}

	protected void eventReceivedDataCmdFromRemoteConn(CmdData d) {
		_inputStream.informDataReceived(d);
	}
	
	protected boolean isUnderlyingSocketConnected() {
		if(_inner != null) {
			return _inner.isConnected() && !_inner.isClosed(); // JGW: Changed this August 2015.
		} else {
			return false;
		}
	}
	
	
	@Override
	public String toString() {
		return "cb-uuid:"+_cb._ourUuid+" next-packet-id:"+_nextPacketId + " inner-hashcode:"+_inner.hashCode()+" remote-addr:"+_remoteAddr;
	}

	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}

	@Override
	public String getDebugStr() {
		return _debugStr;
	}
}
