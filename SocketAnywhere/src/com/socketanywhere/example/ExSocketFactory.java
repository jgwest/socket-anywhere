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

package com.socketanywhere.example;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class ExSocketFactory implements ISocketFactory {

	ISocketFactory _innerFactory = new TCPSocketFactory(); // default
	
	public ExSocketFactory(ISocketFactory innerFactory) {
		_innerFactory = innerFactory;
	}

	
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return new ExServerSocketImpl(_innerFactory);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return new ExServerSocketImpl(address, _innerFactory);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return new ExSocketImpl(_innerFactory);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return new ExSocketImpl(address, _innerFactory);
	}

}
