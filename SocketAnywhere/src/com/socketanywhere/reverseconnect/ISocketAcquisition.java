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

import com.socketanywhere.net.ISocketTL;

public interface ISocketAcquisition {
	
	public void start(IRCConnectionBrain brain);

	
	/** [the thread that implements this method is used listenAddr] Called by RCSocketImpl */
	public ISocketTL retrieveAndAcquireAcceptSocket();

	
	/** Inform the class that it needs to open another client socket, because the current one is now used.
	 * [the thread that implements this method is using connectAddr]*/
	public void informSocketAcquired();

}
