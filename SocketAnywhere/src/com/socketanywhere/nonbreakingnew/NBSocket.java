/*
	Copyright 2012, 2016 Jonathan West

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.cmd.CmdData;

/** Wraps an inner socket, and has the ability to detect when that socket dies; if it dies,
 * this factory will attempt to re-establish it ASAP. The fact that the underlying socket died 
 * will not be made visible to the caller of the NBSocket; it will be as if it never happened. 
 * 
 * In this way, potentially unreliable underlying socket layers can be made reliable using NBSocket.
 * */
public class NBSocket implements ISocketTL {

	private final Object _socketLock = new Object();

	private int _nextPacketId = 0;
		
	private Triplet _debugTriplet; // TODO: CURR - Make this debug only.

	/** If we initiated the connection, this is the address of the connection we connected to. */
	private TLAddress _remoteAddr;
	
	private final ConnectionBrainInterface _cbi;
	
	private final ConnectionBrain _cb;
//	private final ISocketFactory _factory;

	/** Streams for the socket */
	private NBSocketInputStream _inputStream;
//	private NBSocketOutputStream _outputStream;
	
	
	private static final AtomicLong nextDebugId = new AtomicLong();

	private final long _debugId;
	private final boolean _debugFromServSock;
	
	private final NBOptions _options;
	
	String _debugStr;
	
	// Socket only
	protected NBSocket(TLAddress remoteAddr, ConnectionBrain cb, NBOptions options) throws IOException {
		_remoteAddr = remoteAddr;
		_cb = cb;
		_cbi = cb.getInterface();
//		_factory = factory;
		_options = options;
		_debugId = nextDebugId.getAndIncrement();
		
		this.connect(remoteAddr);
		_debugFromServSock = false;
		
	}
	
	// ServerSocket only
	protected NBSocket(Triplet triplet, ConnectionBrain cb, NBOptions options, boolean fromServerSock) {
		_cb = cb;
		_cbi = cb.getInterface();
//		_factory = factory;
		_options = options;
		_debugId = nextDebugId.getAndIncrement();
		_debugTriplet = triplet;
		
		_debugFromServSock = fromServerSock;
	}

	
	/** Called by server socket after the above constructor */
	protected void setRemoteAddr(TLAddress remoteAddr) {
		this._remoteAddr = remoteAddr;
	}
	
	
	
	/** Whether or not the socket was created by an accept() call of the server*/
	protected void serverInit() {
		_inputStream = new NBSocketInputStream(this, _cb);
	}
	
	@Override
	public void close() throws IOException {
		_cbi.eventLocalInitClose(this);	
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Support timeout.
		this.connect(endpoint);
	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
//		ISocketTLWrapper inner = new ISocketTLWrapper(_factory.instantiateSocket(endpoint), false, _cb);
//		
		_remoteAddr = endpoint;
		_inputStream = new NBSocketInputStream(this, _cb);
//		_outputStream = new NBSocketOutputStream(this, _cb);

//		NBSocketListenerThread socketListenerThread = new NBSocketListenerThread(inner, _cb, _options);
//		socketListenerThread.start();

		Triplet triplet = _cbi.initConnection(this);
		_debugTriplet = triplet;

		long printMessageTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(20, TimeUnit.SECONDS);

		boolean isConnected = false;
		
		// Block until the connection is fully connected.
		while(!isConnected)  { 
			isConnected = _cbi.isConnectionEstablishedOrClosing(this);
			if(!isConnected) {
				if(System.nanoTime() > printMessageTimeInNanos) {
					NBLog.severe("Unable to initially connect within 20 seconds. triplet: "+_debugTriplet);
					printMessageTimeInNanos = Long.MAX_VALUE;
				}
				
				try { Thread.sleep(50); } catch (InterruptedException e) { }
			} // TODO: MEDIUM - Infinite close? Might also need to be, && !isClosed()
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if(_debugTriplet != null) {
			Mapper.getInstance().put(_inputStream, _debugTriplet);
		}
		
		return _inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		NBSocketOutputStream os = new NBSocketOutputStream(this, _cb);
		Mapper.getInstance().put(os, _debugTriplet);
		return os;
	}
	
	protected boolean internalIsConnectionClosingOrClosed() {
		return _cbi.isConnectionClosingOrClosed(this);
	}
	
	
	@Override
	public boolean isClosed() {
		return internalIsConnectionClosingOrClosed();
	}

	@Override
	public boolean isConnected() {
		return _cbi.isConnectionEstablishedOrEstablishing(this);
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
	
	protected void flushInnerSocket() throws IOException {
		_cbi.eventFlushSocket(this);		
	}

	
	protected void eventReceivedDataCmdFromRemoteConn(CmdData d) {
		if(_debugTriplet != null) {
			Mapper.getInstance().put(_inputStream, _debugTriplet);
		}
		_inputStream.informDataReceived(d);
	}
	
	protected NBSocketInputStream internalGetInputStream() {
		return _inputStream;
	}
	
	// If true is returned, no more data will be added to the inputstream
	protected boolean isInputPipeClosed() {
		return _cbi.isInputPipeClosed(this);
	}
	
	@Override
	public String toString() {
		return "next-packet-id:"+_nextPacketId + " remote-addr:"+_remoteAddr+ " triplet:"+_debugTriplet;
	}

	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}

	@Override
	public String getDebugStr() {
		return _debugStr;
	}
	
	public long getDebugId() {
		return _debugId;
	}
	
	public boolean isDebugFromServSock() {
		return _debugFromServSock;
	}
	
	public Triplet getDebugTriplet() {
		return _debugTriplet;
	}
	
	@Override
	public boolean equals(Object paramObject) {
		if(!(paramObject instanceof NBSocket)) {
			return false;
		}
		
		NBSocket other = (NBSocket)paramObject;

		return other.getDebugId() == getDebugId();
	}
	
	@Override
	public int hashCode() {
		return (int)_debugId;
	}
}
