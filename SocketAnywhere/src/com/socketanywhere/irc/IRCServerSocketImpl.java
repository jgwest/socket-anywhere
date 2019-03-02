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
import java.util.LinkedList;
import java.util.Queue;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Waits for someone to connect to us; if they do, inform the node, and then pass the
 * connection information to a new IRCSocket. */
public class IRCServerSocketImpl implements IServerSocketTL, ISocketQueueReceiver {

	boolean _isBound;
	boolean _isClosed;
	NodeImpl _node;
	int _port;
	
	// Debug variables
	boolean _isFactoryCreated = false;
	
	Queue<IRCServerSocketQueueEntry> _sockAcceptQueue = new LinkedList<IRCServerSocketQueueEntry>();
	
	Object _objectLock = new Object();

	/** This constructor is used by irc socket factory to pass nodeImpl */
	protected IRCServerSocketImpl(NodeImpl node) throws IOException {
		_isBound = false;
		_isClosed = false;
		_node = node;
		_isFactoryCreated = true;
	}

	protected IRCServerSocketImpl(NodeImpl node, int port) throws IOException {
		_node = node;
		_isBound = false;
		_isClosed = false;
		serveInit(port);
		_isFactoryCreated = true;
	}

	
	public IRCServerSocketImpl() throws IOException {
		_node = new NodeImpl();
		_isBound = false;
		_isClosed = false;
		_isFactoryCreated = false;
	}
	
	public IRCServerSocketImpl(int port) throws IOException {
		_node = new NodeImpl();		
		_isBound = false;
		_isClosed = false;
		_isFactoryCreated = false;
		serveInit(port);
		
	}
	
	private void serveInit(int port) throws IOException {
		
		// TODO: MEDIUM - LOWER - port of 0 zero pick random port
		
		_port = port;
		// Ensure the Node is initialized
		_node.connectToServers();
		
		boolean result = _node.listenOnPort(port, this);
		
		if(!result) throw new IOException("Unable to listen on port "+port);
				
		_isBound = true;
	}
	
	private void assertValid() throws IOException {
		if(_isClosed) throw new IOException("Socket is closed.");
	}

	/** Waits for someone to connect to us; if they do, inform the node, and then pass the
	 * connection information to a new IRCSocket. */
	@Override
	public ISocketTL accept() throws IOException {
		assertValid();
				
		try {
			
			synchronized (_sockAcceptQueue) {
				if(_sockAcceptQueue.size() == 0) {
					_sockAcceptQueue.wait();
				}
				
				IRCServerSocketQueueEntry e = _sockAcceptQueue.remove();
				
				_node.acceptSocket(e.ic, e.ircConn, e.destination, e.context);
				
				IRCSocketImpl result = new IRCSocketImpl();
				result.serverSocketInit(_node, e.ic);
				return result;
			}
			
		} catch(InterruptedException e) {}
		
		return null;
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		assertValid();
		if(isBound()) throw new IOException("Socket is already bound.");
		
		serveInit(endpoint.getPort());
	}

	@Override
	public void close() throws IOException {
		synchronized (_objectLock) {
			if(_isClosed) return;
		}
		_isClosed = true;
		_isBound = false;
		
		_node.stopListeningOnPort(_port);
		
	}

	@Override
	public TLAddress getInetAddress() {
		// TODO: MEDIUM - Implement getInetAddress() on IRCServerSocketImpl
		return null;
	}

	@Override
	public boolean isBound() {
		return _isBound;
	}

	@Override
	public boolean isClosed() {
		return _isClosed;
	}
	
	public void addToSockAcceptQueue(IRCServerSocketQueueEntry q){
		synchronized(_sockAcceptQueue) {
			_sockAcceptQueue.add(q);
			_sockAcceptQueue.notifyAll();
		}
	}

}

class IRCServerSocketQueueEntry {
	IRCSocketConnection ic;
	IRCConnection ircConn;
	String destination;
	int context;
}

interface ISocketQueueReceiver {
	public void addToSockAcceptQueue(IRCServerSocketQueueEntry q);
}