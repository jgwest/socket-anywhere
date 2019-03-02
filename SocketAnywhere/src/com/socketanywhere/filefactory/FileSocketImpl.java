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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.net.TLAddress;
import com.vfile.VFile;

/**
 * File Messages:
 * 
 * Sends:
 *  - filetl-establish-source[listenName]-to-dest[remoteName]-esuuid[uuid]-connuuid[uuid] (sent by FileSocketImpl)
 * Then Waits for:
 *  - filetl-establish-response-dest[myListenName]-from-source[srcEntry]-esuuid[establishUUID]-connuuid[uuid]
 *
 * To close, it sends:
 *  - filetl-packet-source[listenName]-to-dest[remoteName]-connuuid[uuid]-close
 *  
 *  
 *  Notes:
 *  - See close-events.txt
 */
public class FileSocketImpl  implements ISocketTL {
	VFile _directory = null;
	String _listenName = null;
	String _remoteName = null;
	
	String _connUUID = null;
	
	FileTLOutputStream _outputStream;
	FileTLInputStream _inputStream;
	WatchingForCloseThread _closeDataListenThread = null;
	
	boolean _isConnected = false;
	boolean _isClosed = false;
	
	/** The remote address that this socket is connected to */
	TLAddress _remoteAddress = null;
	
	private static final long DEFAULT_CONNECT_ATTEMPT_TIMEOUT = 30000;
	private static final long POLL_FREQUENCY = 333;

	
	String _debugStr;
	
	/**
	 * For internal use only: Called by FileServerSocketImpl, as the FileServerSocketImpl has already done
	 * the connection handshaking (e.g. the hardwork). 
	 */
	protected static FileSocketImpl createSocketForServerSocket(TLAddress remoteAddress, VFile directory, String listenName, String remoteName, String connUUID) throws IOException {
		FileSocketImpl fs = new FileSocketImpl(directory);
		fs._listenName = listenName;
		fs._remoteName= remoteName;
		fs._connUUID = connUUID;
		
		fs._inputStream = new FileTLInputStream(fs, fs._directory, fs._remoteName, fs._listenName, fs._connUUID);
		fs._outputStream = new FileTLOutputStream(fs, fs._directory, fs._listenName, fs._remoteName, fs._connUUID);
		
		fs.startCloseListenThread();
		
		fs._isClosed = false;
		fs._isConnected = true;
		fs._remoteAddress = remoteAddress;
		
		ConnectionManager.getInstance().addActiveConnection(fs._directory, fs._connUUID);
		
		return fs;
	}
	
	private void startCloseListenThread() {
		WatchingForCloseThread thread = new WatchingForCloseThread(this);
		thread.setDaemon(true);
		thread.start();
	}
	
	/** This constructor is equivalent to the Socket() constructor, e.g. a socket that has not yet been instructed to connect (connect() call)*/
	public FileSocketImpl(VFile directory) {
		
		String listenName = null;
		try {
			listenName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			listenName = "unknown-host-"+FileTLUtil.generateUUID();
		}
		_listenName = listenName;
		_directory = directory;
	}
	
	public FileSocketImpl(VFile directory, TLAddress address) throws IOException {
		this(directory);
		
		initConnect(address, DEFAULT_CONNECT_ATTEMPT_TIMEOUT);
	}
	
	private boolean initConnect(TLAddress address, long timeout) throws IOException {
		_remoteAddress = address;
		
		// TODO: LOWER - ARCHITECTURE - Why is this doing what it's doing?
		
		// Converts the address to full hostname, if partial hostname was what
		// was specified.
		List<TLAddress> tl = SoAnUtil.getNameResolver().resolveAddress(address);
		List<String> list = SoAnUtil.convertTLAddressList(tl);
		
		String result = null;
		for(String s : list) {
			if(s.toLowerCase().contains(address.getHostname().toLowerCase())) {
				result = s;
			}
		}
		if(result == null) {
			result = SoAnUtil.convertTLAddress(address);
		}
				
		boolean connResult = initConnect(result, timeout);
		if(!connResult) {
			throw new FileTLIOException("Unable to connect to address");
		}
		return true;
	}
	
	/** Attempts to establish a connection to a machine with a give name 
	 * Returns true if connection succeeded, false otherwise. */
	private boolean initConnect(String remoteName, long timeout) throws IOException {
		
		if(!_directory.exists() || !_directory.canRead()) {
			throw(new FileTLIOException("Unable to read directory"));
		}
		
		// Write the initial connect request
		
		String esUUID = FileTLUtil.generateUUID();
		String connUUID = FileTLUtil.generateUUID();
		String filename = "filetl-establish-source["+_listenName+"]-to-dest["+remoteName+"]-esuuid["+esUUID+"]-connuuid["+connUUID+"]";
		
		VFile file = new VFile(_directory.getPath()+VFile.separator+filename);
//		FileTLUtil.writeAndRenameEmptyMessageFile(file);
		FileTLUtil.writeEmptyMessageFile(file);
		
		String newDestName = null;
		
		long startTime = System.currentTimeMillis();
		
		// Loop: Scan through the directory, waiting for a response 
		while(newDestName == null) {
			VFile[] fileList = _directory.listFiles();
			
			for(VFile rff : fileList) {
				if(rff.isDirectory()) continue;
				
				String rf = rff.getName();

				if(rf.startsWith("filetl-establish-response-dest") && rf.endsWith("-from-source["+_listenName+"]-esuuid["+esUUID+"]-connuuid["+connUUID+"]")) {
					// We found a response, so extract the new dest value and delete
					newDestName = FileTLUtil.extractField("dest", rf);
					
					QueueManager.queueDeleteFile(rff);
					
					break;
				}
			}
		
			if(System.currentTimeMillis() - startTime > timeout) {
				_isConnected = false;
				return false;
			}
			
			if(newDestName == null) {
				FileTLUtil.sleep(POLL_FREQUENCY);
			}
			
		}			
		
		
		// REMEMBER - Any updates here should also be made in createSocketForServerSocket above
		
		_remoteName = newDestName;
		_connUUID = connUUID;
		_inputStream = new FileTLInputStream(this, _directory,_remoteName, _listenName, _connUUID);
		_outputStream = new FileTLOutputStream(this, _directory, _listenName, _remoteName, _connUUID);

		ConnectionManager.getInstance().addActiveConnection(_directory, connUUID);
		
		startCloseListenThread();
		
		_isConnected = true;
		
		return true;
	}
	
	/** Internal method that is called by the thread that watches for the closing of the connection by the remote host. */
	protected void informRemoteClose() {
		FileTLLogger.receivedRemoteClose(_remoteName, _listenName);
		_outputStream.informRemoteClose();
		_inputStream.informRemoteClose();
		_isConnected = false;
		_isClosed = true;
		
		ConnectionManager.getInstance().removeActiveConnection(_directory, _connUUID);
	}
	
	public void close() throws IOException {
		FileTLLogger.userClose(_remoteName, _listenName);

		_isConnected = false;
		_isClosed = true;
		
		VFile msgFile = new VFile(_directory.getPath()+VFile.separator+"filetl-packet-source["+_listenName+"]-to-dest["+_remoteName+"]-connuuid["+_connUUID+"]-close");
		FileTLUtil.writeEmptyMessageFile(msgFile);
//		FileTLUtil.writeAndRenameEmptyMessageFile(msgFile);

		ConnectionManager.getInstance().removeActiveConnection(_directory, _connUUID);
		
		_inputStream.informLocalClose();
		_outputStream.informLocalClose();
	}

	public void connect(TLAddress address, int timeout) throws IOException {
		initConnect(address, DEFAULT_CONNECT_ATTEMPT_TIMEOUT);
	}

	public void connect(TLAddress address) throws IOException {
		initConnect(address, DEFAULT_CONNECT_ATTEMPT_TIMEOUT);

		// TODO: LOWER - Can I connect using a socket I have already closed? If not, assert.
	}

	public TLAddress getAddress() {
		return _remoteAddress;
	}

	public InputStream getInputStream() throws IOException {
		return _inputStream;
	}

	public OutputStream getOutputStream() throws IOException {
		return _outputStream;
	}

	public boolean isClosed() {
		return _isClosed;
	}

	public boolean isConnected() {
		return _isConnected;
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

class WatchingForCloseThread extends Thread {
	
	public static final long POLL_FREQUENCY = 1000;
	
	FileSocketImpl _socketImpl = null;
	boolean _threadRunning = true;
	
	public WatchingForCloseThread(FileSocketImpl socketImpl) {
		super(WatchingForCloseThread.class.getName());
		_socketImpl = socketImpl;
		setDaemon(true);
	}
	
	@Override
	public void run() {
		
		VFile fileToWatch = new VFile(_socketImpl._directory.getPath()+VFile.separator+"filetl-packet-source["+_socketImpl._remoteName+"]-to-dest["+_socketImpl._listenName+"]-connuuid["+_socketImpl._connUUID+"]-close");

		try {
			while(_threadRunning) {
				
				if(fileToWatch.exists()) {
					_threadRunning = false;
					_socketImpl.informRemoteClose();
					QueueManager.queueDeleteFile(fileToWatch);
					break;
				} else {
					sleep(POLL_FREQUENCY);
				}
			}
		} catch (InterruptedException e) {}

	}
	
}
