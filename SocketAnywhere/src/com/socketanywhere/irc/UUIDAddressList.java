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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UUIDAddressList {
	UUID _uuid = null;
	List<IRCSocketAddress> _listeningAddresses = new ArrayList<IRCSocketAddress>();
	
	public UUIDAddressList() {
	}

	public UUID getUUID() {
		return _uuid;
	}

	public void setUUID(UUID uuid) {
		_uuid = uuid;
	}

	public List<IRCSocketAddress> getListeningAddresses() {
		return _listeningAddresses;
	}

	public void setListeningAddresses(List<IRCSocketAddress> listeningAddresses) {
		_listeningAddresses = listeningAddresses;
	}
	
	

}
