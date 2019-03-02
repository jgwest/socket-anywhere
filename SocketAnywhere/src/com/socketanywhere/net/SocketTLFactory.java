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

package com.socketanywhere.net;

import java.io.IOException;

public class SocketTLFactory implements ISocketFactory {
	
	private static ISocketFactory _factoryImpl = null;
	private static SocketTLFactory _selfInstance = new SocketTLFactory();
	
	public static synchronized SocketTLFactory getInstance() {
		return _selfInstance;
	}
	
	public static void setDefaultFactory(ISocketFactory factoryImpl) {
		_factoryImpl = factoryImpl;
	}

	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return _factoryImpl.instantiateServerSocket();
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return _factoryImpl.instantiateServerSocket(address);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return _factoryImpl.instantiateSocket();
	}
	
	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return _factoryImpl.instantiateSocket(address);
	}
	
}
