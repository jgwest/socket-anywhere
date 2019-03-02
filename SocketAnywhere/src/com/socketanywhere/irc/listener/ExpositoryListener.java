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

package com.socketanywhere.irc.listener;

import com.socketanywhere.irc.IRCConnection;

public class ExpositoryListener implements IJoinLeaveListener, IMsgListener, IQuitListener {

	@Override
	public void userJoined(String channel, String nickname, String address) {
		System.out.println("["+channel+"] * "+nickname+"("+address+") joined.");
	}

	@Override
	public void userLeft(String channel, String nickname, String address) {
		System.out.println("["+channel+"] * "+nickname+"("+address+") left.");
	}

	@Override
	public void userMsg(String nickname, String address, String destination, String message, IRCConnection connection) {
		System.out.println("["+destination+"] * "+nickname+"("+address+") said -- "+message);
	}
	
	@Override
	public void userQuit(String nickname, String address, String quitMsg) {
		System.out.println("* "+nickname+"("+address+") quit -- "+quitMsg);
		
	}

}
