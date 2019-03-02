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

package com.socketanywhere.sendreceive;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class ServerSocketGenerator extends Thread {
	
	ISocketFactory _factory;
	TLAddress _addr;
	ISocketTL _result;
	boolean _serverIsReady = false;
	
	public ServerSocketGenerator(ISocketFactory factory, TLAddress addr) {
		setName(ServerSocketGenerator.class.getName());
		_factory = factory;
		_addr = addr;
	}
	
	@Override
	public void run() {
		try {
			
			IServerSocketTL serv = _factory.instantiateServerSocket(_addr);
			_serverIsReady = true;
			_result = serv.accept();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public ISocketTL getResult() {
		try {
			while(_result == null) {
					Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return _result;
	}
	
	public boolean isServerReady() {
		return _serverIsReady;
	}
}
