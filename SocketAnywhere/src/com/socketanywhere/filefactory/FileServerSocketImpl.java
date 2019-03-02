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

package com.socketanywhere.filefactory;

import java.io.IOException;
import java.util.List;

import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.net.ThreadAssert;
import com.vfile.VFile;

/**
 * File Messages
 * 
 * Waits for:
 *  - filetl-establish-source[listenName]-to-dest[remoteName]-esuuid[uuid]-connuuid[uuid] (sent by FileSocketImpl)
 *  
 *  Responds with:
 *   - filetl-establish-response-dest[myListenName]-from-source[srcEntry]-esuuid[establishUUID]-connuuid[uuid]
 *
 */
public class FileServerSocketImpl implements IServerSocketTL {
	
	private VFile _directory = null;
	private List<TLAddress> _addresses = null;
	private List<String> _namedAddresses = null;
	
	private int _nextPipeNum = 1;
	
	private boolean _boundToConnections = false;
	private boolean _connectionClosed = false;
	
	private static final long POLL_FREQUENCY = 1000;
	

	FileServerSocketImpl(VFile directory) throws IOException {
		_directory = directory;

		if(!_directory.exists() || !_directory.canRead()) {
			throw(new FileTLIOException("Unable to read directory"));
		}
		
		_boundToConnections = false;

	}
	
	FileServerSocketImpl(VFile directory, TLAddress listenAddr) throws IOException {
		_addresses = SoAnUtil.getNameResolver().resolveAddress(listenAddr);
		
		_namedAddresses = SoAnUtil.convertTLAddressList(_addresses);
		
		_directory = directory;
		
		if(!_directory.exists() || !_directory.canRead()) {
			throw(new FileTLIOException("Unable to read directory ["+directory.getPath()+"]"));
		}
		
		boolean bind = BoundConnectionManager.getInstance().bindOnNames(_directory, _namedAddresses);
		if(!bind) {
			throw new FileTLIOException("Unable to bind on all addresses");
		}
		_boundToConnections = true;
		// This constructor should bind, but not accept
	}
	
	// TODO: LOWER - ARCHITECTURE - I seem to have gotten rid of the -ready idea in much of the low level stuff (handshaking, atomic locking, bound connections). Is this correct?
	
	@Override
	public ISocketTL accept() throws IOException {
		assertOpen();
		ThreadAssert.assertSingleThreadCaller(this);
		
		String srcEntry = null;
		String destEntry = null;
		String establishUUID = null;
		String connUUID = null;
		
		try {
			
			// Scan the directory looking for any filetl-establish files from peers
			// Blocks until a connection is found, or interrupt is called on the thread
			while(srcEntry == null) {
				VFile[] fileList = _directory.listFiles();
				
				for(VFile f : fileList) {
					if(f.isDirectory()) continue;
					
					String filename = f.getName();
					
					if(!filename.contains("filetl-establish")) {
						continue;
					}
					
					// If found (and this will match everybody, not just ours...)
					for(String name : _namedAddresses ) {

						if(filename.startsWith("filetl-establish-source[") && filename.contains("-to-dest["+name+"]")) {
							
							srcEntry = FileTLUtil.extractField("source", filename);
							destEntry = FileTLUtil.extractField("dest", filename);
							establishUUID = FileTLUtil.extractField("esuuid", filename);
							connUUID = FileTLUtil.extractField("connuuid", filename);
							
							f.delete();
							
							break;
						}

					}
					
				}
				
				if(srcEntry == null) {
					Thread.sleep(POLL_FREQUENCY);
				}
			}
		} catch (InterruptedException e) {
			return null;
		}
		
		// We have found someone to connect with, so post the response		
		String myListenName = destEntry+"("+_nextPipeNum+")";
		
		String responseFilename = _directory.getPath()+VFile.separator+"filetl-establish-response-dest["+myListenName+"]-from-source["+srcEntry+"]-esuuid["+establishUUID+"]-connuuid["+connUUID+"]";
		
		_nextPipeNum++;

		VFile respFile = new VFile(responseFilename);

		FileTLUtil.writeEmptyMessageFile(respFile);
		
//		FileTLUtil.writeAndRenameEmptyMessageFile(respFile);
		
		FileSocketImpl result = FileSocketImpl.createSocketForServerSocket(new TLAddress(srcEntry, -1), _directory, myListenName, srcEntry, connUUID);
		
		return result;
	}

	@Override
	public void bind(TLAddress listenAddr) throws IOException {
		if(_boundToConnections) {
			throw new FileTLIOException("ServerSocket already bound to an address");
		}
				
		_addresses = SoAnUtil.getNameResolver().resolveAddress(listenAddr);
		
		_namedAddresses = SoAnUtil.convertTLAddressList(_addresses);
				
		boolean bind = BoundConnectionManager.getInstance().bindOnNames(_directory, _namedAddresses);
		if(!bind) {
			throw new FileTLIOException("Unable to bind on all addresses");
		}
		_boundToConnections = true;
	}

	@Override
	public void close() throws IOException {
		assertOpen();
		BoundConnectionManager.getInstance().unbindOnNames(_directory, _namedAddresses);
		_connectionClosed = true;
	}

	@Override
	public TLAddress getInetAddress() {
		// TODO: LOWER - Implement FileServerSocketImpl.getInetAddress();
		return null;
	}

	@Override
	public boolean isBound() {
		return _boundToConnections;
	}

	@Override
	public boolean isClosed() {
		return _connectionClosed;
	}
	
	private void assertOpen() {
		if(!_boundToConnections || _connectionClosed) {
			throw new FileTLRuntimeException("Server socket not bound/closed");
		}
	}

}