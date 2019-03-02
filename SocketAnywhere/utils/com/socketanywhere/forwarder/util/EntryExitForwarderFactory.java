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

import com.socketanywhere.multiplexing.MultSocketFactory;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.passthrough.PTSocketFactory;
import com.socketanywhere.socketfactory.TCPSocketFactory;

public class EntryExitForwarderFactory {


	public static ISocketFactory createFactory(boolean isEntry) {
		ISocketFactory rFactory1;
		
//		ArrayList<IDataTransformer> transformers = new ArrayList<IDataTransformer>();
//		
//		transformers.add(new OneTimePadFileTransformer(new File("c:\\cpsweb.log")));
//		transformers.add(new PseudorandomDataTransformer());
//		transformers.add(new SimpleShiftDataTransformer(12));
		
//		ISocketFactory obFactory1 = new OBSocketFactory(transformers, new TCPSocketFactory());
		
		ISocketFactory obFactory1 = new TCPSocketFactory();
		
		
//		ISocketFactory sslFactory1 = new SSLSocketFactory(obFactory1);
		
//		ISocketFactory nbFactory1 = new NBSocketFactory(sslFactory1);

		if(isEntry) {
			rFactory1 = MultSocketFactory.createClientSocketFactory(new TLAddress("localhost", 5555), obFactory1);
		} else {
			rFactory1 = MultSocketFactory.createServerSocketFactory(new TLAddress("localhost", 5555), obFactory1);
		}
		
		rFactory1 = new PTSocketFactory(rFactory1, isEntry ? "entry" : "exit");
		
//		rFactory1 = nbFactory1;
		
		return rFactory1;
		
	}

	
}
