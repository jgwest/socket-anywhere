/*
	Copyright 2012, 2013 Jonathan West

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

package com.socketanywhere.nonbreaking;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import com.socketanywhere.net.ISocketTL;

/** Listens for commands on a given thread; each time a socket is opened, a new instance 
 * of this thread is started to read on it. */
public class NBSocketListenerThread extends Thread {
	
	ConnectionBrain _brain;
	ISocketTL _innerSocket;
	boolean _continueParsing = true;
	NBOptions _options;
	
	NBSocketCommandThread _scThread;

	public NBSocketListenerThread(ISocketTL socket, ConnectionBrain brain, NBOptions options) {
		super(NBSocketListenerThread.class.getName());
		setPriority(Thread.NORM_PRIORITY+2);
		setDaemon(true);
		_innerSocket = socket;
		_brain = brain;
		_options = options;
		
		_scThread = new NBSocketCommandThread(this);
		_scThread.start();
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
			
			while(_continueParsing) {
				
				InputStream is = _innerSocket.getInputStream();
	
				int bytesRead = 0;
				
				// read magic number (4 bytes)
				byte[] magicNumArr = readAndWait(is, 4);
				if(magicNumArr == null) { _continueParsing = false; break; }
				boolean matchMagicNumber = true;
				if(magicNumArr.length != CmdAbstract.MAGIC_NUMBER.length) {
					matchMagicNumber = false;
				} else {
					for(int x = 0; x < magicNumArr.length; x++) {
						if(magicNumArr[x] != CmdAbstract.MAGIC_NUMBER[x]) {
							matchMagicNumber = false; 
							break;
						}
					}
				}
				
				if(!matchMagicNumber) {
					// Magic number doesn't match, so kill the socket and reconnect
					_innerSocket.close();
					NBLog.debug("Command received with invalid magic number. Socket closed.");
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
				int cmdLength = CmdAbstract.b2i(lengthArr);
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
				
				CmdAbstract cmd = CmdFactory.getInstance().createCommand(cmdIdArr);
				
				try {
					cmd.parseCommand(fullCmdData);
					
//					_brain.eventCommandReceived(_innerSocket, cmd);
					_scThread.addCommand(cmd);
					
					
				} catch(Exception e) {
					NBLog.error("Exception on command parsing of socket. ["+_continueParsing+"]");
					if(_continueParsing) {
						e.printStackTrace();
					}
					_continueParsing = false;
				}
				
			}
			
		} catch (Exception e) {
			if(_continueParsing) {
				// If continue parsing is false, that means we stopped the thread, and this exception can be ignored.
				NBLog.debug("Exception in NBSocketListenerThread - socket probably dead.");
			}
		} 
		
//		try {
//			_innerSocket.close();
//		} catch (IOException e) { }
		
		_brain.eventConnErrDetectedReconnectIfNeeded(_innerSocket);
		
	}

	/** Stops the thread (from parsing) */
	public void stopParsing() { 
		_continueParsing = false;
	}
	
}


class NBSocketCommandThread extends Thread {
	
	Queue<CmdAbstract> _queue = new LinkedList<CmdAbstract>();
	NBSocketListenerThread _thread;
	long _totalUnproccessedCmdLength = 0;
	
	public NBSocketCommandThread(NBSocketListenerThread thread) {
		super(NBSocketCommandThread.class.getName());
		_thread = thread;
		setPriority(Thread.NORM_PRIORITY+2);
	}
	
	public void addCommand(CmdAbstract cmd) {
	
		synchronized(_queue) {
			_queue.offer(cmd);
			_totalUnproccessedCmdLength += cmd.getParsedCmdLength();
			
			if(_queue.size() == 1) {
				_queue.notify();
			}
			
		}
		
		if(_thread._options.getMaxDataReceivedBuffer() != -1 /* INFINITE*/ ) {
		
			// If we aren't clearing the queue fast enough, then block
			boolean contQueueLoop = false;		
			do {
				synchronized(_queue) {
					if(_totalUnproccessedCmdLength >= _thread._options.getMaxDataReceivedBuffer()) {
						contQueueLoop = true;
						try { Thread.sleep(50); } catch(Exception e) {}
					} else {
						contQueueLoop = false;
					}
				}			
				
			} while(contQueueLoop);
		}
		
		
	}
	
	
	@Override
	public void run() {
		
		
		while(_thread._continueParsing) {
			CmdAbstract c = null;
			synchronized(_queue) {
				while(_queue.size() == 0 && _thread._continueParsing) {
					try { _queue.wait(); } catch (InterruptedException e) { e.printStackTrace();
					}
				}
				if(_queue.size() > 0) {
					c = _queue.poll();
					_totalUnproccessedCmdLength -= c.getParsedCmdLength();
				}
			}
			
			if(c != null) {
				_thread._brain.eventCommandReceived(_thread._innerSocket, c);
			}
		}

		synchronized(_queue) {
			_queue.clear();
		}
		
	}
}