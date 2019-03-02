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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.socketanywhere.multiplexing.Entry.State;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;

/** 
 * Nearly all of the multiplexing logic is handled here; commands/requests are sent/received, data is
 * passed to the appropriate InputStreams, and written to the appropriate output streams. Connection
 * states are all managed here.  
 * 
 * Each brain instance will have a single connection from which it will send/receive multiplex commands/data.  
 **/
public class MultConnectionBrain {

	private Object _mapLock = new Object();
	
	/** Socket => Entry */
	Map<MultSocket, Entry> _multSocketToEntrymap = new HashMap<MultSocket, Entry>();


	/** UUID+Connection Id => Entry */
	Map<MultPairUUIDConnID, Entry> _uuidMap = new HashMap<MultPairUUIDConnID, Entry>();

	/** Address(only contains port) => server socket listener for that port */
	Map<TLAddress, MultServerSocketListener> _servSockListenerMap = new HashMap<TLAddress, MultServerSocketListener>(); 
	

	
	/** The UUID for this node (specific to this ConnectionBrain) */
	String _ourUuid;
	
	/** The next connection created will use this ID. */
	int _nextConnId = 0;
	Object _nonMapLock = new Object();
	
	
	// These variables handle our connection with the inner socket ------------------
	
	/** connectInnerFactory(...) synchronizes on this lock to prevent multiple connection attempts from multiple thread calls.*/
	private Object _innerSocketLock = new Object();
	
	/** The socket that we channel all our multiplexed data through */
	private ISocketTL _innerSocket;
	
	/** The user can provide us with either a socket (above), or with a factory and (either a connect address, or a listen address).
	 * */
	private ISocketFactory _innerSocketFactory;
	
	/** Address to connect to a listening server socket on; the listening server sock must be another
	 * MultConnectionBrain that is listening on listenAddr below. Once connected, we will store
	 * this socket on innerSocket, and multiplex data over this socket.  */
	TLAddress _connectAddr;
	
	/** Address to wait for another MultConnectionBrain to attach to us; we will accept() this socket
	 * and then use it to multiplex data on. */
	TLAddress _listenAddr;
	
	// End inner socket variables --------------------------------------------------
	
	
	
	
	protected MultConnectionBrain() {
		_ourUuid = UUID.randomUUID().toString();
	}


	public void eventCommandReceived(ISocketTL s, CmdMultiplexAbstract cmd) throws IOException {
		
		if(!(cmd instanceof CmdDataMultiplex)) {
			MuLog.debug("["+_ourUuid+"] command received:"+cmd);
		}
		
		if(cmd.getId() == CmdNewConnMultiplex.ID) {
			CmdNewConnMultiplex c = (CmdNewConnMultiplex)cmd;
			
			MultSocket newSocket = new MultSocket(this);
			newSocket.setRemoteAddr(s.getAddress()); 
			
			newSocket.serverInit(_innerSocket, c.getCmdConnUUID(), c.getCmdConnectionId());
			
			Entry e = new Entry();
			
			e._state = State.CONN_NEW_CONN_ACK1_SENT_WAITING_FOR_ACK;
			e._uuidOfConnector = c.getCmdConnUUID();
			e._connId = c.getCmdConnectionId();
			e._didWeInitConnection = false;
			
			synchronized(_mapLock) {
				_uuidMap.put(new MultPairUUIDConnID(e._uuidOfConnector, e._connId), e);
				
				_multSocketToEntrymap.put(newSocket,  e);
			}
			
			e._multSock = newSocket;
			
			e._port = c.getFieldServerPort(); // Port of the socket connection
					
			CmdAckMultiplex ackResp = new CmdAckMultiplex(CmdNewConnMultiplex.ID, 1, e._uuidOfConnector, e._connId);
			writeCommand(ackResp, s);
			
			
		} else if(cmd.getId() == CmdCloseConnMultiplex.ID) {
			
			MuLog.debug("Received CmdCloseConnMultiplex, by brain: "+_ourUuid);
			
			CmdCloseConnMultiplex c = (CmdCloseConnMultiplex)cmd;

			Entry e;
			
			synchronized(_mapLock) {
				e = _uuidMap.remove(new MultPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
				if(e != null) {
					_multSocketToEntrymap.remove(e._multSock);
				}				
			}
			
			if(e == null) {
				MuLog.error("Unable to locate the entry specified in the close command.");
				return;
			}
			e._state = State.CONN_CLOSED;

			CmdAckMultiplex ackResp = new CmdAckMultiplex(CmdCloseConnMultiplex.ID, 0, cmd.getCmdConnUUID(), cmd.getCmdConnectionId());
			writeCommand(ackResp, s);
			
			e._multSock.informRemoteClose();
			
		} else if(cmd.getId() == CmdDataMultiplex.ID) {
			CmdDataMultiplex c = (CmdDataMultiplex)cmd;
			
			Entry e;
			synchronized(_mapLock) {
				e = _uuidMap.get(new MultPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
			}
			if(e == null) {
				MuLog.error("Unable to locate the entry specified in data command");
				return;
			}
			
			e._multSock.eventReceivedDataCmdFromRemoteConn(c);
			
			
		} else if(cmd.getId() == CmdAckMultiplex.ID) {
			CmdAckMultiplex c = (CmdAckMultiplex)cmd;
			
			
			Entry e;
			synchronized(_mapLock) {
				e = _uuidMap.get(new MultPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
			}
			if(e == null) {
				MuLog.error("Unable to locate the entry specified in ack command "+c);
				return;
			}

			if(c.getFieldCmdIdOfAckedCmd() == CmdNewConnMultiplex.ID) {

				if(e._state == State.CONN_NEW_CONN_SENT_WAITING_FOR_ACK && c.getFieldIntParam() == 1) {
					// We are now free to send data to connectee
					CmdAckMultiplex ackResp = new CmdAckMultiplex(CmdNewConnMultiplex.ID, 2, cmd.getCmdConnUUID(), cmd.getCmdConnectionId());
					writeCommand(ackResp, s);
					e._state = State.CONN_ESTABLISHED;
					
					// Inform the socket that the connection is ready
					e._multSock.eventSignalConnected();
										
				} else  if(e._state == State.CONN_NEW_CONN_ACK1_SENT_WAITING_FOR_ACK && c.getFieldIntParam() == 2) {
					// We are now free to send data from connectee
					e._state = State.CONN_ESTABLISHED;
					
					MultServerSocketListener l;
					synchronized(_mapLock) {
						l = _servSockListenerMap.get(new TLAddress(e._port));
					}
					if(l == null) {
						MuLog.error("Unable to locate the server socket for the address that is specified.");
						return;
					}
					
					l.informNewSocketAvailable(e._multSock);
					return;
				} else {
					MuLog.error("Unrecognized command/state for received ack command. "+e._state + " " + cmd);
					return;
				}
			
			
			} else if(c.getFieldCmdIdOfAckedCmd() == CmdCloseConnMultiplex.ID) {
				// Received by the person who initially sent the command
				
				synchronized(_mapLock) {
					_uuidMap.remove(new MultPairUUIDConnID(c.getCmdConnUUID(), c.getCmdConnectionId()));
					_multSocketToEntrymap.remove(e._multSock);

				}
				
				e._state = State.CONN_CLOSED;
				

				
			} else {
				MuLog.error("Unrecognized command/state for received command.");
			}
		}
		
		
		
	}

	/** *
	 * 
	 * @param sock
	 * @return connection id
	 * @throws IOException 
	 */
	public int initiateConnection(MultSocket sock) throws IOException {
		Entry e = null;
		
		synchronized(_mapLock) {
			if(sock.getConnectionId()!= -1) { 

				e = _multSocketToEntrymap.get(sock);
			}
		}
		
		if(e != null) {
			MuLog.error("In initiateConnection, MultSocketImpl was already found in the map. ");
			return -1;
		}
		e = new Entry();
		
		e._state = Entry.State.CONN_NEW_CONN_SENT_WAITING_FOR_ACK;
		e._uuidOfConnector = _ourUuid;

		synchronized(_nonMapLock) {
			e._connId = _nextConnId;
			_nextConnId++;
		}
		
		e._port = sock.getAddress().getPort();
		e._didWeInitConnection = true;
		e._multSock = sock;
		

		// TODO: MEDIUM - How to handle these
		sock._didWeInitConnection = true;
		sock._connectionId = e._connId;
		sock.setConnectionNodeUUID(e._uuidOfConnector);

		synchronized(_mapLock) {
			_multSocketToEntrymap.put(sock, e);
			_uuidMap.put(new MultPairUUIDConnID(e._uuidOfConnector ,e._connId), e);
		}
		
		CmdNewConnMultiplex cmd = new CmdNewConnMultiplex(e._uuidOfConnector, e._connId, sock.getAddress().getHostname(), e._port);

		writeCommand(cmd, sock);
		
		return e._connId;

	}

	
	public void removeSockListener(TLAddress address) {
		synchronized(_mapLock) {
			// map key: only the port
			_servSockListenerMap.remove(new TLAddress(address.getPort()));
		}
		
	}
	
	public void addServSockListener(TLAddress address, MultServerSocketListener l) {
		synchronized(_mapLock) {
			// map key: only add the port here
			_servSockListenerMap.put(new TLAddress(address.getPort()), l);
		}
	}

	
	public void eventLocalInitClose(MultSocket sock) {
		
		MuLog.debug("eventLocalInitClose - "+sock);
		
		Entry e;
		synchronized(_mapLock) {
			e = _multSocketToEntrymap.get(sock);
		}
		
		if(e == null) {
			MuLog.error("eventLocalInitClose - Unable to find given socket in socket map: sock.isClosed:"+sock.isClosed()+", "+sock);
			return;
		}
		
		CmdCloseConnMultiplex c = new CmdCloseConnMultiplex(e._uuidOfConnector, e._connId);

		try {
			writeCommand(c, sock);
		} catch (IOException e1) {
			MuLog.debug("IOE on write of close command.");
		}

		e._state = State.CONN_CLOSED;


	}
	
	
	protected void writeCommand(CmdMultiplexAbstract cmd, MultSocket sock) throws IOException {

		try {
			if(MuLog.DEBUG) {
				MuLog.wroteCommand(cmd, sock);
			}
			
			sock.writeToInnerSocket(cmd.buildCommand());
		} catch (IOException e) {
			MuLog.error("Unable to write command to inner socket. exception:"+e);

			throw new IOException("Unable to write command to inner socket. exception:"+e);
		}
		
	}

	
	protected void writeCommand(CmdMultiplexAbstract cmd, ISocketTL sock) throws IOException {
		
		if(MuLog.DEBUG) MuLog.wroteCommand(cmd, sock);
		
		boolean writeFailed = false;
		
		synchronized(sock) {
			try {
				sock.getOutputStream().write(cmd.buildCommand());
			} catch (IOException e) {
				writeFailed = true;
				if(MuLog.DEBUG) {
					e.printStackTrace();
				}
			}				
		}
		
		if(writeFailed) {
			MuLog.error("Unable to write command to ISocket in writeCommand(...) of MCB");
			throw new IOException("Unable to write command to ISocket in writeCommand(...) of MCB");
		}
		
	}

	
	
	public void setConnectAddr(TLAddress _connectAddr) {
		this._connectAddr = _connectAddr;
	}
	
	public void setListenAddr(TLAddress _listenAddr) {
		this._listenAddr = _listenAddr;
	}
		
	public void setInnerSocketFactory(ISocketFactory innerSocketFactory) {
		this._innerSocketFactory = innerSocketFactory;
	}
	
	public ISocketTL connectInnerFactory() throws IOException {
		MuLog.debug("connectInnerFactory - " + this.hashCode() + " " + _ourUuid + " ["+_connectAddr + " | "+_listenAddr+"]");
		
		if(_innerSocketFactory == null) {
			MuLog.error("Socket connection factory not specified; is null.");
			return null;	
		} 
		
		synchronized(_innerSocketLock) {
			if(_innerSocket != null) {
				return _innerSocket;
			}
			
			if(_connectAddr != null) {
				long startedTryingToConnect = System.currentTimeMillis();
				
				// Keep trying to connect 
				while(_innerSocket == null && System.currentTimeMillis() - startedTryingToConnect <= 15 * 1000) {
					
					ISocketTL socket = null;
					try {
						socket = _innerSocketFactory.instantiateSocket(_connectAddr);
						_innerSocket = socket;
						
						SoAnUtil.appendDebugStr(socket, "connectAddress created in connInnerFactory.");
					} catch(IOException e) {
						if(MuLog.DEBUG) {
							MuLog.debug("IOE on attempt to connect, will try to reconnect."+e);
						}
					}
					
					if(_innerSocket == null) {
						try { Thread.sleep(2000); } catch(InterruptedException e) {}
					}
				}

				if(_innerSocket != null) {
					MultSocketListenerThread sockListener = new MultSocketListenerThread(_innerSocket, this);
					sockListener.start();
				}
				return _innerSocket;
				
			} else if(_listenAddr != null) {
				
				IServerSocketTL serverSocket = _innerSocketFactory.instantiateServerSocket(_listenAddr);
				ISocketTL socket = serverSocket.accept();
				_innerSocket = socket;
				
				SoAnUtil.appendDebugStr(socket, "listenAddress created in connInnerFactory.");
				
				MultSocketListenerThread sockListener = new MultSocketListenerThread(_innerSocket, this);
				sockListener.start();
				
				return _innerSocket;
			} else {
				MuLog.error("User did not specfic a connect address or listen, along with the factory");
				return null;
			}
		}
	}
	

		
	public ISocketTL getInnerSocket() {
		return _innerSocket;
	}

	public void setInnerSocket(ISocketTL innerSocket) {
		_innerSocket = innerSocket; 
	}

	
}


class Entry {
	// TODO: LOWER - Consider referencing these through getters/setters. 
	
	enum State {

		CONN_INIT,

		CONN_NEW_CONN_SENT_WAITING_FOR_ACK,
		CONN_NEW_CONN_ACK1_SENT_WAITING_FOR_ACK,
		
		CONN_ESTABLISHED, 	// Underlying socket connection is currently established and ready to send/receive data
		CONN_CLOSED		// Socket has been closed in an orderly fashion
	}
	
	State _state = State.CONN_INIT; 

	MultSocket _multSock; // NBSocket that this entry is used for
	
	String _uuidOfConnector; // UUID of the node that initiated the connection
	int _connId; // Connection id from the node that initiated the connection. 
	boolean _didWeInitConnection; // Whether or not this node was the one that initiated the connection.

	
	int _port; // Port of the socket connection
		
}

class MultPairUUIDConnID {
	
	private String _uuid;
	private int _connId;
	
	
	public MultPairUUIDConnID(String uuid, int connId) {
		this._uuid = uuid;
		this._connId = connId;
	}
	
	
	public String getUuid() {
		return _uuid;
	}
	public int getConnId() {
		return _connId;
	}
	
	
	@Override
	public int hashCode() {
		return _uuid.hashCode()*100 + _connId;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof MultPairUUIDConnID)) {
			return false;
		}
		MultPairUUIDConnID other = (MultPairUUIDConnID)obj;
		
		if(other.getConnId() != getConnId()) {
			return false;
		}
		
		if(!other.getUuid().equals(getUuid())) {
			return false;
		}
		
		return true;
	}
	
}

