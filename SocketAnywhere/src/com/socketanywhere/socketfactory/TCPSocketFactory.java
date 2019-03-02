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

package com.socketanywhere.socketfactory;

import java.io.IOException;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class TCPSocketFactory implements ISocketFactory {

	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		TCPServerSocketImpl impl = new TCPServerSocketImpl();
		return impl;
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		TCPServerSocketImpl impl = new TCPServerSocketImpl(address);
		return impl;
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		TCPSocketImpl impl = new TCPSocketImpl();
		return impl;
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		TCPSocketImpl impl = new TCPSocketImpl(address);
		return impl;
	}
	
}
