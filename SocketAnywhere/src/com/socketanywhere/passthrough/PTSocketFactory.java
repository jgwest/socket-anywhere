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

package com.socketanywhere.passthrough;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** The purpose of the passthrough socket factory is for debugging purposes; the passthrough factory can be
 * placed at exit point of a socket factory stack, for both the sender/receiver, in order to ensure
 * that data that is transmitted by one is received by the other, and vice versa. */

public class PTSocketFactory implements ISocketFactory {

	ISocketFactory _innerFactory = new TCPSocketFactory(); // default
	
	String _dbgNodeIdentifier;
	
	public void setDbgNodeIdentifier(String _dbgNodeIdentifier) {
		this._dbgNodeIdentifier = _dbgNodeIdentifier;
	}
	
	
	public PTSocketFactory(ISocketFactory innerFactory, String dbgNodeIdentifier) {
		_innerFactory = innerFactory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
	}

	
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return new PTServerSocketImpl(_innerFactory, _dbgNodeIdentifier);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return new PTServerSocketImpl(address, _innerFactory, _dbgNodeIdentifier);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return new PTSocketImpl(_innerFactory, _dbgNodeIdentifier);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return new PTSocketImpl(address, _innerFactory, _dbgNodeIdentifier);
	}

}
