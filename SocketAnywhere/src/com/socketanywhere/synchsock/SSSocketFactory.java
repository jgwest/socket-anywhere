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

package com.socketanywhere.synchsock;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class SSSocketFactory implements ISocketFactory {

	ISocketFactory _innerFactory = new TCPSocketFactory(); // default
	
	
	public SSSocketFactory(ISocketFactory innerFactory) {
		synchronized(SSSocketImpl.LOCK) {
			_innerFactory = innerFactory;
		}
	}

	
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		synchronized(SSSocketImpl.LOCK) {
			return new SSServerSocketImpl(_innerFactory);
		}
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		synchronized(SSSocketImpl.LOCK) {
			return new SSServerSocketImpl(address, _innerFactory);
		}
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		synchronized(SSSocketImpl.LOCK) {
			return new SSSocketImpl(_innerFactory);
		}
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		synchronized(SSSocketImpl.LOCK) { 
			return new SSSocketImpl(address, _innerFactory);
		}
	}

}
