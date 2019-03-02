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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

public class RCSocketImpl implements ISocketTL {

	IRCConnectionBrain _brain;
	TLAddress _addr;
	
	ISocketTL _socket;
	
	InputStream _inputStream;
	OutputStream _outputStream;
	
	String _debugStr;
	
	
	public RCSocketImpl(IRCConnectionBrain brain) throws IOException {
		_brain = brain;
		
	}

	public RCSocketImpl(TLAddress address, IRCConnectionBrain brain) throws IOException {
		_addr = address;
		_brain = brain;
		connect(address);
	}
	
	// Called by server-socket only
	protected RCSocketImpl(ISocketTL socket, IRCConnectionBrain brain) throws IOException {
		_brain = brain;
		_socket = socket;
		_addr = _socket.getAddress();
	
		_inputStream = new RCInputStream(this, _socket.getInputStream());
		_outputStream = new RCOutputStream(this, _socket.getOutputStream());			
	}


	
	@Override
	public void close() throws IOException {
		_socket.close();		
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Implement timeout
		_addr = endpoint;
		connect(endpoint);
	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
		_addr = endpoint;
		
		// We are the only connection who will ever be using this socket, as we have 
		// acquired it from brain, and now OWN it
		ISocketTL socket = _brain.retrieveAndAcquireSocket();
		
		String uuidStr = UUID.randomUUID().toString();
		
		RCGenericCmd cmd = new RCGenericCmd();
		cmd.setAddr(endpoint);
		cmd.setCommandName(RCGenericCmd.CMD_CONNECT);
		cmd.setUuid(uuidStr);

		byte[] msg = cmd.generateCmdBytes();
		
		OutputStream os = socket.getOutputStream();
		
		os.write(msg);
		os.flush();
		
		_brain.debugMsg(getClass().getSimpleName()+" sent connection command");
		
		
		byte[] response = RCUtil.readAndWait(socket.getInputStream(), RCGenericCmd.SIZE_OF_CMD_IN_BYTES);
		
		String str = new String(response).trim();
		
		RCGenericCmd readCmd = new RCGenericCmd();
		readCmd.parseCmd(str);
		
		if(readCmd.getAddr().equals(endpoint) && readCmd.getUuid().equalsIgnoreCase(uuidStr)) {
			if(readCmd.getCommandName().equalsIgnoreCase(RCGenericCmd.CMD_ACCEPT_CONNECT)) {
				_socket = socket;
				_inputStream = new RCInputStream(this, _socket.getInputStream());
				_outputStream = new RCOutputStream(this, _socket.getOutputStream());
				
			} else {
				throw new IOException("Unable to connect.");
			}
			
		} else {
			throw new IOException("Socket connection received a response for a request it did not initiate.");
		}
		
	}

	@Override
	public InputStream getInputStream() throws IOException {
		
		return _inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {

		return _outputStream;
		
	}

	@Override
	public boolean isClosed() {
		boolean result;
		
		if(_socket == null) {
			result = false;
		} else {
			result = _socket.isClosed();
		}
		return result;
	}

	@Override
	public boolean isConnected() {
		
		boolean result;
		
		if(_socket == null) {
			result = false;
		} else {
			result = _socket.isConnected();
		}
		return result;
	}

	@Override
	public int hashCode() {
		synchronized(_socket) {
			return _socket.hashCode();
		}
	}
	
	@Override
	public TLAddress getAddress() {
		synchronized(_socket) {
			TLAddress result = _addr; 
			return result;
		}
	}

	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}

	@Override
	public String getDebugStr() {
		return _debugStr;
	}
	
	
}
