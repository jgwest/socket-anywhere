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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.UUID;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.vfile.s3io.S3File;
import com.vfile.s3io.S3FileInputStream;
import com.vfile.s3io.S3FileOutputStream;

public class S3SocketAcquisition implements  ISocketAcquisition {

	IRCConnectionBrain _brain;
	S3File _sharedFile;
	
	RCS3ListenerThread _s3ListenerThread;
	
	private TLAddress _innerConnectAddr;
	
	private TLAddress _innerListenAddr;
	
	final private ISocketFactory _factory;
	
	public S3SocketAcquisition( S3File sharedFile, ISocketFactory factory) {

		_sharedFile = sharedFile;
		_factory = factory;
		
	}
	
	public void setInnerListenAddr(TLAddress innerListenAddr) {
		this._innerListenAddr = innerListenAddr;
	}
	
	public void setInnerConnectAddr(TLAddress innerConnectAddr) {
		this._innerConnectAddr = innerConnectAddr;
	}
	
	@Override
	public void start(IRCConnectionBrain brain) {
		_brain = brain;
		
		if(_innerConnectAddr != null) {		
			_s3ListenerThread = new RCS3ListenerThread(this, _sharedFile);
			_s3ListenerThread.start();
		}
	}

	@Override
	public ISocketTL retrieveAndAcquireAcceptSocket() {
		S3File f = _sharedFile;
		S3FileOutputStream fos = new S3FileOutputStream(f);
		
		String newRequestUUID = null;
		
		IServerSocketTL servSock = null;
		try {
			servSock = getServSocketForClient();
		
			newRequestUUID = UUID.randomUUID().toString();
			
			_brain.debugMsg("Writing new request to S3, uuid:"+newRequestUUID);
			
			fos.write((newRequestUUID+"\n").getBytes());
			fos.close();
			fos = null;
			
			ISocketTL sock = servSock.accept();
			_brain.debugMsg("S3 socket accept created a new connection: "+sock);
			
			f.delete();
			
			return sock;
			
		} catch (IOException e) {
			_brain.debugMsg("Error occurred while writing S3 request, uuid:"+newRequestUUID);
			// TODO: EASY - Add exception to debug message.
			e.printStackTrace();
		} finally {
			try { servSock.close(); } catch(Exception e) {} 
			try { fos.close(); } catch(Exception e) {}
		}

		// error condition
		return null;
	}

	@Override
	public void informSocketAcquired() {
		// This is not used for s3.
	}
	
	protected void openNewSocket() throws IOException {
		ISocketTL socket = _factory.instantiateSocket(_innerConnectAddr);
		
		_brain.debugMsg("S3 connection thread opened a new connection to "+_innerConnectAddr);
		
		RCSocketReaderThread readerThread = new RCSocketReaderThread(socket, _brain);
		readerThread.start();
		
	}
	
	private IServerSocketTL getServSocketForClient() throws IOException {
		IServerSocketTL servSock = null;
		servSock = _factory.instantiateServerSocket(_innerListenAddr);
		return servSock;
		
	}

}

class RCS3ListenerThread extends Thread {
	
	LinkedList<String> _uuidsServiced = new LinkedList<String>();
	
	S3File _sharedFile;
	
	S3SocketAcquisition _parent;
	
	boolean _continueRunning = true;
	
	public RCS3ListenerThread(S3SocketAcquisition parent, S3File sharedFile) {
		super(RCS3ListenerThread.class.getName());
		setDaemon(true);
		_sharedFile = sharedFile;
		_parent = parent;
		
	}
	
	@Override
	public void run() {
		
		while(_continueRunning) {
			
			BufferedReader br = null;
			try {
				
				if(_sharedFile.exists()) {
					S3FileInputStream fis = new S3FileInputStream(_sharedFile);
					
					br = new BufferedReader(new InputStreamReader(fis));
					
					String str;
					while(null != (str = br.readLine())) {
						
						boolean matchFound = false;
						for(String serviced : _uuidsServiced) {
							if(serviced.equalsIgnoreCase(str)) {
								matchFound = true;
							}
							
						}
						
						if(!matchFound) {
							if(_uuidsServiced.size() > 100) {
								_uuidsServiced.remove();
							}
							
							_uuidsServiced.add(str.toUpperCase());
							try {
								_parent.openNewSocket();
							} catch(Throwable t) { t.printStackTrace(); }
							_sharedFile.delete();
							
							// This break ensures only one new connection per 120 seconds
							break;
						}
					}
				}
				
			} catch(IOException e) {
				_parent._brain.debugMsg("Exception thrown while trying to connect:");
				e.printStackTrace();
			} finally {
				try {br.close(); } catch(Exception e) {}
			}
			
			try { Thread.sleep(120 * 1000); } catch(InterruptedException e) { }
			
		}
		
	}
	
}

