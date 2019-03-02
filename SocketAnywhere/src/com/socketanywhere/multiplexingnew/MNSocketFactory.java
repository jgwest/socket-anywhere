/*
	Copyright 2012, 2014 Jonathan West

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

package com.socketanywhere.multiplexingnew;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** The multiplexing socket factory allows the user to create a number of Socket instances, but have
 * all data that is passed to these sockets actually be sent/received by a single socket connection behind the scenes. 
 * 
 **/
public class MNSocketFactory implements ISocketFactory {

	MNConnectionBrain _connectionBrain;

//	private final MNOptions _options;

	private MNSocketFactory(ISocketTL socket, MNOptions options) {
//		if(options == null) { options = new MNOptions(); /* default*/ }
//		
//		_options = options;
		 _connectionBrain = new MNConnectionBrain(socket, options);
		 _connectionBrain.start();
	}
	
	private MNSocketFactory(MNConnectionBrain brain/*, MNOptions options*/) {
//		if(options == null) { options = new MNOptions(); /* default*/ }
		
//		_options = options;
		_connectionBrain = brain;
	}
	
	public static MNSocketFactory createFactory(ISocketTL socket, MNOptions options) {
		MNSocketFactory result = new MNSocketFactory(socket, options);
		return result;
	}


	public static MNSocketFactory createServerSocketFactory(TLAddress addr, ISocketFactory factory, MNOptions options) {
		MNSocketFactory result = new MNSocketFactory(MNConnectionBrain.createConnectionBrainOnServSocket(addr, factory, options)/*, options*/);		
		return result;
	}

	public static MNSocketFactory createClientSocketFactory(TLAddress addr, ISocketFactory factory, MNOptions options) {
		MNSocketFactory result = new MNSocketFactory(MNConnectionBrain.createConnectionBrainOnClientSocket(addr, factory, options)/*, options*/);		
		return result;
	}
	
		
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return new MNMultServerSocket(_connectionBrain);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return new MNMultServerSocket(address, _connectionBrain);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return new MNMultSocket(_connectionBrain);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return new MNMultSocket(address, _connectionBrain);
	}
	
}
