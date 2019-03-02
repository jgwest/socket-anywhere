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

import com.socketanywhere.irc.listener.ConnectionListener;
import com.socketanywhere.irc.listener.DataConnectionListener;
import com.socketanywhere.irc.listener.SingleRespCommandListener;
import com.socketanywhere.net.CompletableWait;
import com.socketanywhere.net.SoAnUtil;

/** This thread is spun up in order to create a new tcp socket connection to an IRC socket;
 * it adds listeners, performs the log-in, then informs the node and terminates. */
public class IRCServerConnectionThread extends Thread {

	NodeImpl _node;
	String _host;
	int _port;
	String _channel;
	String _channelPassword;
	
	boolean _connected = false;
	
	/** Complete is defined as either the connection has now succeeded, or it has now failed (e.g. if this is true it must either have succeeded or failed) */
	boolean _connectionComplete = false;
	
	IRCConnection _result;
	
	String _nicknamePrefix;
	CompletableWait _waiter;
	
	public IRCServerConnectionThread(NodeImpl node, String host, int port, String channel, String channelPassword, String nickPrefix) {
		super(IRCServerConnectionThread.class.getName());
		_node = node;
		_host = host;
		_port = port;
		_channel = channel;
		_channelPassword = channelPassword;
		_nicknamePrefix = nickPrefix;
		_waiter = new CompletableWait(IRCServerConnectionThread.class.getName());
	}
	
	@Override
	public void run() {
		
		IRCConnection ic = new IRCConnection(_node);
		
		// Add various listeners to the socket; these listeners are used to implement
		// the functionality of the node in the IRC channel. 
		
		// Add the node state command response listener
		SingleRespCommandListener l1 = new SingleRespCommandListener(_node);
		ic.addMsgListener(l1);
		
		ConnectionListener l2 = new ConnectionListener(_node);
		ic.addMsgListener(l2);
		
		DataConnectionListener l3 = new DataConnectionListener(_node);
		ic.addMsgListener(l3);
						
		try {
			
			ic.setNicknamePrefix(_nicknamePrefix);
			
			ic.connect(_host, _port);
			if(_channelPassword != null) {
				ic.joinChannel(_channel, _channelPassword);
			} else {
				ic.joinChannel(_channel);
			}
			
			_connected = true;
			_result = ic;
			
		} catch(IOException e) {
			IRCSocketLogger.logError(this, "IOException on connection "+SoAnUtil.convertStackTrace(e));
			_connected = false;
		}
		
		_connectionComplete = true;
		_waiter.informComplete();
	}
	
	public boolean isConnected() {
		return _connected;
	}
	
	public CompletableWait getResponseWait() {
		return _waiter;
	}

	public IRCConnection getResult() {
		return _result;
	}
	
	public boolean isConnectionComplete() {
		return _connectionComplete;
	}
}
