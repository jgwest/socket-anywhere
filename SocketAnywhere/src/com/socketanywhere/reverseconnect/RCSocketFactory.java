/*
	Copyright 2013 Jonathan West

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

package com.socketanywhere.reverseconnect;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class RCSocketFactory implements ISocketFactory {

	private IRCConnectionBrain _brain;
	private boolean _isServerSocketFactory = false;
	
	private RCSocketFactory(ISocketAcquisition sockAcq) {
//		_brain = new RCConnectionBrain(sockAcq);
		_brain = new RCConnectionBrainNew(sockAcq);
	}

	/** Requires a socket acquisition with an inner listen addr. This factory only creates Sockets. */
	public static RCSocketFactory createClientSocketFactory(ISocketAcquisition sockAcq) {
		RCSocketFactory f = new RCSocketFactory(sockAcq);
		f._brain.activate();
		f._isServerSocketFactory = true;
		return f;
	}
	
	/** Requires a socket acquisition with an inner connect addr. This factory only creates ServerSockets.  */
	public static RCSocketFactory createServerSocketFactory(ISocketAcquisition sockAcq) {
		RCSocketFactory f = new RCSocketFactory(sockAcq);
		f._brain.activate();
		f._isServerSocketFactory = false;
		return f;
	}
	
	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		if(_isServerSocketFactory) { throw new UnsupportedOperationException("Server socket not supported by server socket factory."); }
		
		return new RCServerSocketImpl(_brain);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		if(_isServerSocketFactory) { throw new UnsupportedOperationException("Server socket not supported by server socket factory."); }
		
		return new RCServerSocketImpl(address, _brain);
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		if(!_isServerSocketFactory) { throw new UnsupportedOperationException("Socket connection not supported by client socket factory."); }
		
		return new RCSocketImpl(_brain);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		if(!_isServerSocketFactory) { throw new UnsupportedOperationException("Socket connection not supported by client socket factory."); }		
		return new RCSocketImpl(address, _brain);
	}

}
