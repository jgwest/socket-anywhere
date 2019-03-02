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

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** The multiplexing socket factory allows the user to create a number of Socket instances, but have
 * all data that is passed to these sockets actually be sent/received by a single socket connection behind the scenes. 
 * 
 * Each factory instance corresponds to a single MultConnectionBrain instance; each brain instance
 * will have a single connection from which it will send/receive multiplex commands/data.  
 **/
public class MultSocketFactory implements ISocketFactory {

	MultConnectionBrain _connectionBrain = new MultConnectionBrain();

	private MultSocketFactory() {
	}
	
	protected MultSocketFactory(ISocketFactory factory) {
		_connectionBrain.setInnerSocketFactory(factory);
	}
	
	public MultSocketFactory(ISocketTL inner) {
		_connectionBrain.setInnerSocket(inner);		
	}
	
	public static MultSocketFactory createServerSocketFactory(TLAddress addr, ISocketFactory factory) {
		MultSocketFactory result = new MultSocketFactory();
		result._connectionBrain.setListenAddr(addr);
		result._connectionBrain.setInnerSocketFactory(factory);
		return result;
	}

	public static MultSocketFactory createClientSocketFactory(TLAddress addr, ISocketFactory factory) {
		MultSocketFactory result = new MultSocketFactory();
		result._connectionBrain.setConnectAddr(addr);
		result._connectionBrain.setInnerSocketFactory(factory);
		return result;
	}
		
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return new MultServerSocket(_connectionBrain);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return new MultServerSocket(address, _connectionBrain);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return new MultSocket(/*_connectionBrain.getInnerSocket(),*/ _connectionBrain);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return new MultSocket(address, /*_connectionBrain.getInnerSocket(), */_connectionBrain);
	}
	
	public void connect() throws IOException {
		_connectionBrain.connectInnerFactory();
	}

}
