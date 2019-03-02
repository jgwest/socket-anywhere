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

package com.socketanywhere.multiplexing;

import java.io.IOException;
import java.io.InputStream;

import com.socketanywhere.net.ISocketTL;

/** Listens for commands on a given thread; each time a socket is opened, a new instance 
 * of this thread is started to read on it. */
public class MultSocketListenerThread extends Thread {
	
//	MultSocket _multSocket;
	MultConnectionBrain _brain;
	ISocketTL _innerSocket;
	boolean _continueParsing = true;
	
	public MultSocketListenerThread(ISocketTL socket, MultConnectionBrain brain) {
		super(MultSocketListenerThread.class.getName());
		setPriority(Thread.NORM_PRIORITY+2);
		setDaemon(true);
		_innerSocket = socket;
		_brain = brain;
	}
	
	/** Returns either a byte array with ALL of the requests bytes, or null (or throws IOE) */
	private static byte[] readAndWait(InputStream is, int bytesWanted) throws IOException{
		byte[] result = new byte[bytesWanted];
		
		int currPos = 0;
		int bytesRemaining = bytesWanted;
		while(bytesRemaining > 0) {
//			System.out.println(Thread.currentThread().getId()+" - bytesRemaining:"+bytesRemaining);
			int bytesRead = is.read(result, currPos, bytesRemaining);
			if(bytesRead == -1) {
				return null;
			} else {
				bytesRemaining -= bytesRead;
				currPos += bytesRead;
			}
		}
		
		return result;
	}
	
	@Override
	public void run() {
		try {
			_continueParsing = true;
			
			InputStream is = _innerSocket.getInputStream();
			
			while(_continueParsing) {
	
				int bytesRead = 0;
				
				// read magic number (4 bytes)
				byte[] magicNumArr = readAndWait(is, 4);
				if(magicNumArr == null) { _continueParsing = false; break; }
				boolean matchMagicNumber = true;
				if(magicNumArr.length != CmdMultiplexAbstract.MAGIC_NUMBER.length) {
					matchMagicNumber = false;
				} else {
					for(int x = 0; x < magicNumArr.length; x++) {
						if(magicNumArr[x] != CmdMultiplexAbstract.MAGIC_NUMBER[x]) {
							matchMagicNumber = false; 
							break;
						}
					}
				}
				
				if(!matchMagicNumber) {
					// Magic number doesn't match, so kill the socket
					_innerSocket.close();
					MuLog.error("Command received with invalid magic number. Socket closed.");
					_continueParsing = false; 
					break;
				}
				bytesRead += 4;
				
				// read cmd id (2 bytes)
				byte[] cmdIdArr = readAndWait(is, 2);
				if(cmdIdArr == null) { _continueParsing = false; break; }
				bytesRead += 2;
				
				// read command length  (4 bytes)
				byte[] lengthArr = readAndWait(is, 4);
				if(lengthArr == null) { _continueParsing = false; break; }
				int cmdLength = CmdMultiplexAbstract.b2i(lengthArr);
				bytesRead += 4;
				
				byte[] innerCmdData = readAndWait(is, cmdLength-bytesRead);
				if(innerCmdData == null) { _continueParsing = false; break; }
				bytesRead += innerCmdData.length;
				
				// Copy the entire command into a new array
				byte[] fullCmdData = new byte[bytesRead];
				int currPos = 0;
				System.arraycopy(magicNumArr, 0, fullCmdData, currPos, magicNumArr.length); currPos += magicNumArr.length;
				System.arraycopy(cmdIdArr, 0, fullCmdData, currPos, cmdIdArr.length); currPos += cmdIdArr.length;
				System.arraycopy(lengthArr, 0, fullCmdData, currPos, lengthArr.length); currPos += lengthArr.length;
				System.arraycopy(innerCmdData, 0, fullCmdData, currPos, innerCmdData.length); currPos += innerCmdData.length;
				
				CmdMultiplexAbstract cmd = CmdMultFactory.getInstance().createCommand(cmdIdArr);
				
				try {
					cmd.parseCommand(fullCmdData);
					
					MuLog.debug("\nCommand received on: "+_innerSocket);
					_brain.eventCommandReceived(_innerSocket, cmd);
					
				} catch(Exception e) {
					MuLog.debug("Exception on command parsing of socket. ["+_continueParsing+"]");
					if(_continueParsing) {
						e.printStackTrace();
					}
					_continueParsing = false;
				}
				
			}
			
		} catch (Exception e) {
			if(_continueParsing) {
				// If continue parsing is false, that means we stopped the thread, and this exception can be igonred.
				MuLog.debug("Exception in MultSocketListenerThread - socket probably dead.");
			}
		} 
		
		// TODO: ARCHITECTURE - Socket disconnect. Do we care?
		
	}

	/** Stops the thread (from parsing) */
	public void stopParsing() { 
		_continueParsing = false;
	}
}
