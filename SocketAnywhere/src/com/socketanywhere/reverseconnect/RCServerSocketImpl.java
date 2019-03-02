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

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class RCServerSocketImpl implements IServerSocketTL {
	
//	ISocketFactory _socketFactory = new TCPSocketFactory();
	
//	IServerSocketTL _serverSocket;
	
	TLAddress _addr;
	IRCConnectionBrain _brain;
	
	Status _currentStatus = Status.UNBOUND;
	
	enum Status { UNBOUND, BOUND, CLOSED };
	
	protected RCServerSocketImpl(IRCConnectionBrain brain) throws IOException{
//		_socketFactory = socketFactory;
		_brain = brain;
		
		// unbound, call bind() to bind
	}

	protected RCServerSocketImpl(TLAddress addr, IRCConnectionBrain brain) throws IOException{
		_addr = addr;
//		_socketFactory = socketFactory;
		_brain = brain;
		
		listenOnAddress(_addr);
		// bound to addr
	}

	
	@Override
	public ISocketTL accept() throws IOException {
		ISocketTL result = _brain.blockUntilAccept(_addr);
		return result;
	}

	@Override
	public void bind(TLAddress endpoint) throws IOException {
		_addr = endpoint;
		listenOnAddress(_addr);
		
	}
	
	private void listenOnAddress(TLAddress addr) throws IOException {
		_brain.addServSockListenAddr(addr);
		_currentStatus = Status.BOUND;
	}

	@Override
	public void close() throws IOException {
		_brain.removeServSockListenAddr(_addr);
		_currentStatus = Status.CLOSED;
//		_serverSocket.close();
	}

	@Override
	public TLAddress getInetAddress() {
		return _addr;
	}

	@Override
	public boolean isBound() {
		return _currentStatus == Status.BOUND;
	}

	@Override
	public boolean isClosed() {
		return _currentStatus == Status.CLOSED;
	}

}
