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

package com.socketanywhere.forwarder.util;

import java.io.IOException;

import com.socketanywhere.forwarder.ForwarderThread;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class EntryForwarder {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TCPSocketFactory localEntryFactory = new TCPSocketFactory();
		ISocketFactory localExitFactory = EntryExitForwarderFactory.createFactory(true); 
		
		IServerSocketTL listen;
		ForwarderThread ft;
		
		try {
			
			listen = localEntryFactory.instantiateServerSocket(new TLAddress(1079));
			ft = new ForwarderThread(listen, localExitFactory, new TLAddress("localhost", 8000));
			ft.start();
						
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
