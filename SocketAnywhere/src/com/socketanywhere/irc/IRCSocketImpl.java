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
import java.io.InputStream;
import java.io.OutputStream;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Creates a new connection using the node, and handles socket events, such data sent/receive and close. */
public class IRCSocketImpl implements ISocketTL {

	NodeImpl _node;
	TLAddress _connectedTo;
	boolean _closed = false;
	boolean _isConnected = false;
	IRCSocketConnection _ircSockConn;
	
	// Debug values
	boolean _isFactoryCreated = false;
	boolean _isServSockCreated = false;
	String _debugStr;
	
	Object _objectLock = new Object();
	
	
	public IRCSocketImpl() {
		_isFactoryCreated = false;
		_node = new NodeImpl();
	}
	
	public IRCSocketImpl(String address, int port) throws IOException {
		_isFactoryCreated = false;
		_node = new NodeImpl();
		connectInit(address, port);
	}

	/** Called by IRC socket factory, passes in its node */
	protected IRCSocketImpl(NodeImpl node) {
		_isFactoryCreated = true;
		_node = node;
	}
	
	/** Called by IRC socket factory, passes in its node */
	protected IRCSocketImpl(NodeImpl node, String address, int port) throws IOException {
		_isFactoryCreated = true;
		_node = node;
		connectInit(address, port);
	}	
	
	protected void serverSocketInit(NodeImpl node, IRCSocketConnection ircSockConn) {
		_node = node;
		_ircSockConn = ircSockConn;
		_closed = false;
		_isConnected = true;
		_connectedTo = null;
		_isServSockCreated = true;
	}
	
	/** Called by socket constructor */
	private void connectInit(String address, int port) throws IOException {
		_connectedTo = new TLAddress(address, port);
		_node.connectToServers();
		_ircSockConn = _node.createConnection(address, port);
		
		if(_ircSockConn != null) {
			_isConnected = true;
		} else {
			_isConnected = false;
			throw new IOException("Unable to connect.");
		}
	}
	
	@Override
	public void close() throws IOException {
		synchronized(_objectLock) {
			if(_closed) return;
			else _closed = true;
		}
		_ircSockConn.close();
		_isConnected = false;
	}
	
	private void checkIfClosed(boolean throwException) throws IOException {
		synchronized(_objectLock) {
			if(!_closed && _ircSockConn != null && _ircSockConn.isClosed()) {
				_closed = true;
			}

			if(_closed && throwException) {
				throw new IOException("Socket is already closed");
			} 
		}
	}
	
	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		checkIfClosed(true);
		if(_isConnected) {
			throw new IOException("Socket is already connected");
		}
		connectInit(endpoint.getHostname(), endpoint.getPort());
	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
		checkIfClosed(true);
		if(_isConnected) {
			throw new IOException("Socket is already connected");
		}

		connectInit(endpoint.getHostname(), endpoint.getPort());
	}

	@Override
	public TLAddress getAddress() {
		if(!_isConnected || _closed) return null;
		
		return _connectedTo;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		checkIfClosed(true);
		if(!_isConnected) {
			throw new IOException("Stream is not connected");
		}
		return _ircSockConn.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {		
		checkIfClosed(true);
		
		if(!_isConnected) {
			throw new IOException("Stream is not connected");
		}
		return _ircSockConn.getOutputStream();
	}

	@Override
	public boolean isClosed() {
		try { checkIfClosed(false); } catch (IOException e) { }
		return _closed;
	}

	@Override
	public boolean isConnected() {
		try { checkIfClosed(false); } catch (IOException e) { }
		
		return _isConnected;
	}
	
	protected IRCSocketConnection getIRCSockConnection() {
		return _ircSockConn;
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
