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

public class HopperSocketAcquisition implements ISocketAcquisition {
	
	IRCConnectionBrain _brain;
	
	private /*---*/ HopperConnectionThread _connThread;
	private /*---*/ HopperAcceptThread _acceptThread;

	private TLAddress _innerConnectAddr;
	
	private TLAddress _innerListenAddr;
	
	private final ISocketFactory _factory;
	
	
//	private static int sharedDebugNum = 0;
//	private int debugNum = 0;
	
	public HopperSocketAcquisition(ISocketFactory factory) {
		_factory = factory;
		
	}
	
	public void setInnerConnectAddr(TLAddress innerConnectAddr) {
//		debugNum = sharedDebugNum++;
		_innerConnectAddr = innerConnectAddr;
	}

	public void setInnerListenAddr(TLAddress innerListenAddr) {
//		debugNum = sharedDebugNum++;
		this._innerListenAddr = innerListenAddr;
	}
	
	@Override
	public void start(IRCConnectionBrain brain) {
		
		_brain = brain;
		_brain.debugMsg("Connection brain started. ");
		
		if(_innerConnectAddr != null) {
			_connThread = new HopperConnectionThread(_innerConnectAddr, _factory, _brain);
			_connThread.start();
			
		} else if(_innerListenAddr != null) {
			_acceptThread = new HopperAcceptThread(_factory, _innerListenAddr, _brain);
			_acceptThread.start();
			
		} else {
			throw new RuntimeException("User must set either listen address or connect address.");
		}

	}
	
	/** Called by RCSocketImpl */
	@Override
	public ISocketTL retrieveAndAcquireAcceptSocket() {
		
		ISocketTL result = _acceptThread.acquireActiveHopperSocket();
		
		
		assert(result != null);
		
		return result;
		
	}
	
	@Override
	public void informSocketAcquired() {
		_connThread.eventCreateNewConnection();		
	}
	

}




class HopperConnectionThread extends Thread {
	
//	private static final boolean DEBUG = true;

	private final IRCConnectionBrain _brain;
	
	private final Object _lock = new Object();

		private final TLAddress _innerConnectAddr;
		
		private ISocketTL _socket = null;
		
		private final ISocketFactory _connectFactory;
		
		private boolean _continueRunning = true;
		
	
	public HopperConnectionThread(TLAddress innerConnectAddr, ISocketFactory factory, IRCConnectionBrain brain) {
		super(HopperConnectionThread.class.getName());
		setDaemon(true);
		
		_innerConnectAddr = innerConnectAddr;
		_connectFactory = factory;
		_brain = brain;
	}
	
	@Override
	public void run() {
		
		while(_continueRunning) {
			
			synchronized(_lock) {
				
				while(_socket != null && _socket.isConnected() && !_socket.isClosed() && _continueRunning) {
					
					try { _lock.wait(200); } catch(InterruptedException e) {}
				}
			}
			
			if(!_continueRunning) { return; } 
			
			try {
				synchronized(_lock) {
					ISocketTL socket = _connectFactory.instantiateSocket(_innerConnectAddr);
					
					_brain.debugMsg("Hopper connection thread opened a new connection to "+_innerConnectAddr);
					
					_socket = socket;
					
					RCSocketReaderThread readerThread = new RCSocketReaderThread(_socket, _brain);
					readerThread.start();
					
					_lock.notify(); // why is this here?
				}
			} catch(IOException e) {
				if(IRCConnectionBrain.DEBUG) {
					e.printStackTrace();
				}
				// Try again after a few seconds
				try { Thread.sleep(IRCConnectionBrain.TIMEOUT_ON_CONNECTION_FAIL_IN_MSECS); } catch(Exception e2) { }
			}
		}
			
		
	}
	
	public void eventCreateNewConnection() {
		_brain.debugMsg("eventCreateNewConnection.");
		// Inform the hopper connection thread that a new thread should be created.
		synchronized(_lock) {
			_socket = null;
			_lock.notify();
		}
		
	}
	
}

class HopperAcceptThread extends Thread {

	Object _lock = new Object();
		
		ISocketTL _socket;
		
		private final ISocketFactory _listenFactory;
		
		private final TLAddress _listenFactoryAddr;
		
		boolean _continueRunning = true;
		
		private final IRCConnectionBrain _brain;
		
		HopperAcceptThreadKeepAlive _keepAliveThread;
		
	
	public HopperAcceptThread(ISocketFactory factory, TLAddress listenFactoryAddr, IRCConnectionBrain brain) {
		super(HopperAcceptThread.class.getName());
		setDaemon(true);
		
		_listenFactory = factory;
		_listenFactoryAddr = listenFactoryAddr;
		_brain = brain;
	}
	
	@Override
	public void run() {
		
		IServerSocketTL servSock = null;
		try {
			servSock = _listenFactory.instantiateServerSocket(_listenFactoryAddr);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		while(_continueRunning) {
			
			try {
				
				_brain.debugMsg("Hopper accept thread listening on "+_listenFactoryAddr);
	
				while(_continueRunning) {
					
					_brain.debugMsg("Hopper inside accept.");
					ISocketTL sock = servSock.accept();
					_brain.debugMsg("Hopper accept thread accepted a new connection: "+sock);

					synchronized(_lock) {
						_socket = sock;
						_lock.notify();
						if(_keepAliveThread != null) {
							_keepAliveThread.invalidateSocket();
						}
						_keepAliveThread = new HopperAcceptThreadKeepAlive(_lock, _socket);
						_keepAliveThread.start();
					}
					
				}
				
			} catch(IOException e) {
				e.printStackTrace();
				
				try { Thread.sleep(IRCConnectionBrain.TIMEOUT_ON_ACCEPT_FAIL_IN_MSECS); } catch(InterruptedException ie) {}
			} 
		}
		
		try { if(servSock != null) { servSock.close(); } } catch(Exception e) {}
		
	}
	
	public ISocketTL acquireActiveHopperSocket() {
		
		_brain.debugMsg("acquireActiveHopperSocket waiting for active hopper socket.");
		
		ISocketTL result = null;
		
		try {
			
			synchronized(_lock) {
				
				while(_socket == null && _continueRunning) {
					_lock.wait(100);
				}
				
				if(_socket != null) {
					result = _socket;
					_socket = null;
					_keepAliveThread.invalidateSocket();
					_keepAliveThread = null;
					_lock.notify();
				}
			}
		
		} catch(InterruptedException e) {
			result = null;
		}
		
		_brain.debugMsg("acquireActiveHopperSocket acquired active hopper socket.");
		
		return result;
		
	}
		
}

class HopperAcceptThreadKeepAlive extends Thread {
	
	public static final long MAX_RAND_KEEP_ALIVE_TIME_TO_WAIT_MSECS = 120 * 1000;
	private final Object _lock;
	private ISocketTL _socket;
	
	public HopperAcceptThreadKeepAlive(Object lock, ISocketTL socket) {
		super(HopperAcceptThreadKeepAlive.class.getName());
		setDaemon(true);
		_lock = lock;
		_socket = socket;
	}
	
	@Override
	public void run() {
		
		boolean contLoop = true;
		
		while(contLoop) {
		
			synchronized(_lock) {
				if(_socket == null) {
					contLoop = false;
				} else {
					RCGenericCmd cmd = new RCGenericCmd();
					cmd.setAddr(null);
					cmd.setCommandName(RCGenericCmd.CMD_KEEP_ALIVE);
					cmd.setUuid(null);
					byte[] msg = cmd.generateCmdBytes();
					
					try { 
						_socket.getOutputStream().write(msg);
					} catch (IOException e) {
						e.printStackTrace();
						contLoop = false;
					}
					
				}
				
			} // end lock
			
			if(contLoop) {			
				long timeToWait = (long)(Math.random() * MAX_RAND_KEEP_ALIVE_TIME_TO_WAIT_MSECS);
				if(timeToWait < 10 * 1000) {
					timeToWait = 10 * 1000;
				}
				
				try { Thread.sleep(timeToWait); } catch (InterruptedException e) { }
			}

			
		}
	}
	
	public void invalidateSocket() {
		synchronized(_lock) {
			_socket = null;
		}
	}
	
}
