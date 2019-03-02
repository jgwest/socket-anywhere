/*
	Copyright 2012, 2016 Jonathan West

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

package com.socketanywhere.nonbreakingnew;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;
import com.socketanywhere.nonbreakingnew.cmd.CmdFactory;
import com.socketanywhere.util.ManagedThread;

/** Listens for commands on a given thread; each time a socket is opened, a new instance 
 * of this thread is started to read on it. */
public class NBSocketListenerThread extends ManagedThread {
	
	final ConnectionBrain _brain;
	final ConnectionBrainInterface _inter;
	final ISocketTLWrapper _innerSocket;
	final NBOptions _options;
	
	private final NBSocketCommandThread _scThread;

	protected boolean _continueParsing = true;

	public NBSocketListenerThread(ISocketTLWrapper socket, ConnectionBrain brain, NBOptions options) {
		super(NBSocketListenerThread.class.getName(), true);
		setPriority(Thread.NORM_PRIORITY+2);
		
		_innerSocket = socket;
		_brain = brain;
		_inter = brain.getInterface();
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
		boolean joinSeen = false;
		boolean dataSeen = false;
		
		String exitReason = null;
		
		try {
			_continueParsing = true;
			
			while(_continueParsing) {
				
				InputStream is = _innerSocket.getInputStream();
	
				int bytesRead = 0;
				
				// read magic number (4 bytes)
				byte[] magicNumArr = readAndWait(is, 4);
				if(magicNumArr == null) {  _continueParsing = false; exitReason = "No magic number"; break;}
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
					_innerSocket.close(true);
					NBLog.severe("Command received with invalid magic number. Socket closed.");
					exitReason = "Command received with invalid magic number. Socket closed.";
					_continueParsing = false; 
					break;
				}
				bytesRead += 4;
				
				// read cmd id (2 bytes)
				byte[] cmdIdArr = readAndWait(is, 2);
				if(cmdIdArr == null) { _continueParsing = false; exitReason = "readAndWait returned null"; break; }
				bytesRead += 2;
				
				// read command length  (4 bytes)
				byte[] lengthArr = readAndWait(is, 4);
				if(lengthArr == null) { _continueParsing = false; exitReason = "readAndWait returned null"; break; }
				int cmdLength = CmdAbstract.b2i(lengthArr);
				bytesRead += 4;
				
				byte[] innerCmdData = readAndWait(is, cmdLength-bytesRead);
				if(innerCmdData == null) { _continueParsing = false; exitReason = "readAndWait returned null"; break; }
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
					
					_scThread.addCommand(cmd);
					
					
				} catch(Exception e) {
					NBLog.error("Inner exception on command parsing of socket. ["+_continueParsing+"] global-id:"+_innerSocket.getGlobalId());
					exitReason = "Exception occured: "+e.getMessage();
					if(_continueParsing) {
						e.printStackTrace();
					}
					_continueParsing = false;
				}
				
			}
			
		} catch (Exception e) {
			if(_continueParsing) {
				exitReason = "Outer exception occured: "+e.getMessage();
				// If continue parsing is false, that means we stopped the thread, and this exception can be ignored.
				NBLog.debug("Exception in NBSocketListenerThread - socket probably dead. Exception: "+ SoAnUtil.convertStackTrace(e).replace("\n", "").replace("\r", "")+" global-id: "+_innerSocket.getGlobalId(), NBLog.INTERESTING);
			}
		} finally {
			_continueParsing = false;
		}
		
//		try {
//			_innerSocket.close();
//		} catch (IOException e) { }
		
		_inter.eventConnErrDetectedReconnectIfNeeded(_innerSocket);
		
//		NBLog.debug("[jgw] site 1 "+_innerSocket.getGlobalId()+ " "+exitReason, NBLog.INTERESTING);
		
	}

	/** Stops the thread (from parsing) */
	public void stopParsing() { 
		_continueParsing = false;
	}
	
}

/** Pass commands to the connection brain on a separate thread */
class NBSocketCommandThread extends ManagedThread {
	
	/** Ordered list of commands that we are waiting to pass to the brain*/
	private final Queue<CmdAbstract> _queue = new LinkedList<CmdAbstract>();
	private final NBSocketListenerThread _thread;
	
	/** The size (in bytes) of commands in the queue*/
	private long _totalUnproccessedCmdLength = 0;
	
	public NBSocketCommandThread(NBSocketListenerThread thread) {
		super(NBSocketCommandThread.class.getName(), true);
		_thread = thread;
		setPriority(Thread.NORM_PRIORITY+2);
	}
	
	public void addCommand(CmdAbstract cmd) {
	
		NBLog.debug("cmd received: "+cmd.toString()+ " global-id: "+_thread._innerSocket.getGlobalId(), NBLog.INTERESTING);
		
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
					} else {
						contQueueLoop = false;
					}
				}			
				
				if(contQueueLoop) {
					try { Thread.sleep(50); } catch(Exception e) {};
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
					try { _queue.wait(1000); } catch (InterruptedException e) { e.printStackTrace(); }
				}
				if(_queue.size() > 0) {
					c = _queue.poll();
					_totalUnproccessedCmdLength -= c.getParsedCmdLength();
				}
			}
			
			if(c != null) {
				
				NBLog.debug("cmd processed: "+c.toString()+ " global-id: "+_thread._innerSocket.getGlobalId(), NBLog.INTERESTING);
				_thread._inter.eventCommandReceived(_thread._innerSocket, c);
			}
		}

		synchronized(_queue) {
			_queue.clear();
		}
		
	}
}