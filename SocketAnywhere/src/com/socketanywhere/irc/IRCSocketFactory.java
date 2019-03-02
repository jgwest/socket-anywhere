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

package com.socketanywhere.irc;

import java.io.IOException;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

// TODO: MEDIUM - Nodes should be able to connect to themselves

/** Each IRCSocketFactory will contains it's own NodeImpl, which will be used by all connections
 * that are created through the factory instance.*/
public class IRCSocketFactory implements ISocketFactory {
	
	NodeImpl _node;
	
	public IRCSocketFactory() {
		_node = new NodeImpl();
		
		final String[] IRC_SERVER_LIST = { "irc.log2x.nu", "irc.absurd-irc.net", "irc.wikkedwire.com", 
				"irc.ambernet.se", "irc.AbleNET.org" };
		
		_node.addIRCHostToConnectList(IRC_SERVER_LIST[0]);

	}

	@Override
	public IServerSocketTL instantiateServerSocket() throws IOException {
		return new IRCServerSocketImpl(_node);
	}

	@Override
	public IServerSocketTL instantiateServerSocket(TLAddress address) throws IOException {
		return new IRCServerSocketImpl(_node, address.getPort());
	}

	@Override
	public ISocketTL instantiateSocket() throws IOException {
		return new IRCSocketImpl(_node);
	}

	@Override
	public ISocketTL instantiateSocket(TLAddress address) throws IOException {
		return new IRCSocketImpl(_node, address.getHostname(), address.getPort());
	}

}
