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

package com.socketanywhere.unreliable;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class URSocketFactory implements ISocketFactory {

	private ISocketFactory _innerFactory = new TCPSocketFactory(); // default
	
	private String _dbgNodeIdentifier;
	
	public void setDbgNodeIdentifier(String _dbgNodeIdentifier) {
		this._dbgNodeIdentifier = _dbgNodeIdentifier;
	}
	
	private final URCentralInfo _centralInfo;
	
	
	public URSocketFactory(ISocketFactory innerFactory, String dbgNodeIdentifier, URCentralInfo centralInfo) {
		_innerFactory = innerFactory;
		_dbgNodeIdentifier = dbgNodeIdentifier;
		_centralInfo = centralInfo;
	}

	
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return new URServerSocketImpl(_innerFactory, _dbgNodeIdentifier, _centralInfo);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return new URServerSocketImpl(address, _innerFactory, _dbgNodeIdentifier, _centralInfo);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return new URSocketImpl(_innerFactory, _dbgNodeIdentifier, _centralInfo);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return new URSocketImpl(address, _innerFactory, _dbgNodeIdentifier, _centralInfo);
	}

}
