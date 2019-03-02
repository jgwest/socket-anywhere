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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.vfile.VFile;
import com.vfile.VFileInputStream;


/** 
 * Polls
 * 
 * File Messages:
 *
 * Listens for
 *  - filetl-packet-source[_remoteName]-to-dest[listenName]-packetid[_nextFileNum]-connuuid[uuid]-ready
 *  
 */
public class FileTLInputStream extends InputStream {

	String _remoteName = null; // name of the remote connection 
	String _listenName = null; // our name, that we are listening on 
	String _connUUID = null;
	
	/** Used to inform a socket of a close operation*/
	FileSocketImpl _socketImpl = null;
	
	private int _nextFileNum = 1;
	
	private int _currFileNum = -1;
	private VFile _currFile = null;
	boolean _isOpen = false;
	private VFile _directory = null;
	
	/** Whether or not we have been informed that the remote or local host has closed to connection.
	 * isOpen will still remain true, in order to allow to to read the rest of the data
	 * from the buffer. */
	boolean _informedClose = false;
	
	private static final long POLLING_FREQUENCY = 200;
	private static final long WAIT_TIME_BEFORE_PACKET_DELETE_LOCAL = 60 * 1000;
	private static final long WAIT_TIME_BEFORE_PACKET_DELETE_REMOTE = 120 * 1000;
	
	private VFileInputStream _fis = null;

	/**
	 * 
	 * @param socketImpl The file socket on which the input stream is based
	 * @param directory Directory in which files will be read
	 * @param remoteName The string through which we will identify the remote system 
	 * @param listenName The string on which we will listen for data
	 * @throws IOException
	 */
	public FileTLInputStream(FileSocketImpl socketImpl, VFile directory, String remoteName, String listenName, String connUUID) throws IOException {
		
		_directory = directory;
		_remoteName = remoteName;
		_listenName = listenName;
		_connUUID = connUUID;
		_socketImpl = socketImpl;
		
		if(!directory.exists() || !_directory.isDirectory() || !_directory.canRead()) {
			throw(new FileTLIOException("Invalid directory"));
		}
		
		_isOpen = true;
		
		FileTLLogger.inputStreamCreated(this);
				
	}
	
	/** Scan the directory for new file packets to read, and when found, update the file number; 
	 * if none were found, returns -1.*/
	private void updateNextFileNum() {
		
		VFile nextFile 
			= new VFile(_directory.getPath() + VFile.separator + "filetl-packet-source["
					+_remoteName+"]-to-dest["+_listenName+"]-packetid["+_nextFileNum+"]-connuuid["+_connUUID+"]-ready");
				
		if(!nextFile.exists()) {
			_currFileNum = -1;
			return;
		} else {
			
			_currFileNum = _nextFileNum;
			_nextFileNum++;
			
		}
	}
	
	@SuppressWarnings("unused")
	private void oldUpdateNextFileNum() {
		
		
		// TODO: LOWER - We don't need to update this every time.
		
		VFile[] fileList = _directory.listFiles();
		
		if(fileList == null) {
			// Directory doesn't exist, or is inaccessible... so return the lack of found files
			_currFileNum = -1;
			return;
		}
		
		int minimumNum = Integer.MAX_VALUE;
		
		String nameFormat = "filetl-packet-source["+_remoteName+"]-to-dest["+_listenName+"]";
				
		// For each file..
		for(VFile f : fileList) {
			
			if(f.isDirectory()) continue;
			
			String name = f.getName();
			
			// If the file is for us, and is ready
			if(name.startsWith(nameFormat) && name.endsWith("-ready") && !name.endsWith("-close")) {
				
				// The connection UUIDs must match, otherwise this is not our connection
				String connUUID = FileTLUtil.extractField("connuuid", name);
				if(connUUID.equalsIgnoreCase(_connUUID)) {
				
					// Parse the packet id and lower the minimum value if needed
					int num = Integer.parseInt(FileTLUtil.extractField("packetid", name));
					
					if(num < minimumNum) {
						minimumNum = num;
					}					
				
				}
			}
			
		}
		if(minimumNum == Integer.MAX_VALUE) {
			minimumNum = -1;
		}

		_currFileNum = minimumNum;

	}

	/** 
	 * In order for a read to complete, a file input stream must exist; this function locates the next
	 * file in the stream (blocking, where required). 
	 * 
	 * This function will thus set _fis if an appropriate stream can be found within the function parameters.
	 * 
	 * @param blockIfEmpty Whether or not to return if the appropriate file was not found
	 * @throws IOException
	 */
	void updateFileInputStream(boolean blockIfEmpty) throws IOException  {
				
		if(!_isOpen) {
			throw new FileTLIOException("Input stream has been closed.");
		}
		
		// If we have a fileinputstream, and it has data, then there is no need to update
		try {
			if(_fis != null && _fis.available() > 0) {
				return;
			} 
		} catch(IOException e) {
			// If something happens to the file we were using, then skip it
			// (the scenario this is meant to avoid is where the file was deleted by
			//  deleteRemainingFiles. This is expected behaviour, we just want to
			//  handle it gracefully here.)
			if(_informedClose) {
				_fis = null;
			} else {
				throw e;
			}
		}
				
		boolean contLoop = true;
				
		while(contLoop) {
			
			if(_fis == null && _currFileNum == -1) {
				
				// If we have no fileinputstream, loop until we do (unless asked to block)
				while(_currFileNum == -1) {
					
					updateNextFileNum();

					if(_currFileNum == -1) {
						// Return if we aren't required to block, or if the stream has been closed
						if(!blockIfEmpty || _informedClose) {
							return;
						} else {
							FileTLUtil.sleep(POLLING_FREQUENCY);
						}	
					}
				}
			}
			
			// At this point, _nextFileNum will necessarily point to the next file to read (and that file will exist)
			
			// If we don't have a fileinputstream we can read from, then locate one
			if(_fis == null) {
				
				String name = "filetl-packet-source["+_remoteName+"]-to-dest["+_listenName+"]-packetid["+_currFileNum+"]-connuuid["+_connUUID+"]-ready";
				
				VFile nextFile = new VFile(_directory.getPath()+VFile.separator+name);
	
				boolean a1 = nextFile.exists();
				boolean a2 = nextFile.canRead();
				if(!a1 || !a2) {
					throw(new FileTLRuntimeException("A packet file that should exist could not be found ["+name+"] "+a1+ " "+a2));
				}

				
				boolean inputStreamAcquired = false;
				int acquireCount = 0;
				while(!inputStreamAcquired && acquireCount < 40) {
					try {
						_fis = new VFileInputStream(nextFile);
						inputStreamAcquired = true;
					} catch(FileNotFoundException e) {
						// Catch the following error: The process cannot access the file because it is being used by another process
						acquireCount++;
						try { Thread.sleep(250); } catch (InterruptedException e1) { }
					}
				}
				
				// TODO: MEDIUM - Throw exception if not able to acquire
				
				_currFile = nextFile;
				
				_currFileNum = -1;
				
			}
			
			// If we've got a fileinputstream, test it and delete if it has 0 bytes 
			if(_fis != null) {
				
				if(_fis.available() == 0) {
					_fis.close();
					QueueManager.queueDeleteFile(_currFile);
					_fis = null;
					_currFile = null;
					_currFileNum = -1;
					contLoop = true;
					
				} else {
					contLoop = false;
					break;
				}		
			} 
		}		
	}
	
	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		return read(b);
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		updateFileInputStream(true);
		
		// TODO: LOWER - The way this is implemented, you'll get AT MOST the size of the next file
		
		/** If there is no more data left, and we have closed, then return -1 */
		if(_fis == null && _informedClose) {
			return -1;
		}
		
		int result = 0;
		try {
			result = _fis.read(b, off, len);
		} catch(IOException e) {
			if(_informedClose == true) {
				// If the socket was closed already, this is expected.
				result = -1;
			} else {
				throw e;
			}
			
		}

		updateFileInputStream(false);
		
		FileTLLogger.readInputData(result, _remoteName, _listenName);
		
		return result;
		
	}
	
	@Override
	public int available() throws IOException {
		updateFileInputStream(false);
		
		// After having updated the file stream
		// - Null means nothing available ATM
		// OR, - FIS will return a >0 number
		
		if(_fis == null) {
			return 0;
		} else {
			return _fis.available();
		}
		
	}
	
	@Override
	public void close() throws IOException {
		deleteRemainingFiles(WAIT_TIME_BEFORE_PACKET_DELETE_LOCAL);
		_socketImpl.close();
	}
	
	public void informLocalClose () {
		_informedClose = true;
		
		// This won't be double-called by close(), then informLocalClose(), because of a check in the
		// deleteRemainingFiles() method itself: 
		deleteRemainingFiles(WAIT_TIME_BEFORE_PACKET_DELETE_LOCAL); 
		
		FileTLLogger.inputStreamClosed(this);
	}
	
	public void informRemoteClose() {
		_informedClose = true;
		deleteRemainingFiles(WAIT_TIME_BEFORE_PACKET_DELETE_REMOTE);
		
		FileTLLogger.inputStreamClosed(this);
	}
	
	private void deleteRemainingFiles(long timeToWaitBeforeDelete) {
		if(!_isOpen) return;
		
		_isOpen = false;
		
		FileTLInputStreamTimedDeletionThread t 
			= new FileTLInputStreamTimedDeletionThread(_remoteName, _listenName, _directory, _connUUID, timeToWaitBeforeDelete);
		t.setDaemon(true);
		t.start();
	}

	
	@Override
	public synchronized void mark(int readlimit) {
		throw(new UnsupportedOperationException());
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@Override
	public synchronized void reset() throws IOException {
		throw(new UnsupportedOperationException());	}

	@Override
	public long skip(long n) throws IOException {
		throw(new UnsupportedOperationException());
	}	
}

/**
 * In order to ensure all relevant files are deleted, this thread remains to clear them out.
 */
class FileTLInputStreamTimedDeletionThread extends Thread {
	String _remoteName = null;
	String _listenName = null;
	String _connUUID = null;
	VFile _directory = null;
	long _timeToWaitBeforeDelete = 0;
	
	private static final long TIME_TO_WAIT_ON_REMAINING = 30 * 1000;
	
	public FileTLInputStreamTimedDeletionThread(String remoteName, String listenName, VFile directory, String connUUID, long timeToWaitBeforeDelete) {
		_remoteName = remoteName;
		_listenName = listenName;
		_directory = directory;
		_connUUID = connUUID;
		
		_timeToWaitBeforeDelete = timeToWaitBeforeDelete;

		setName(FileTLInputStreamTimedDeletionThread.class.getName());
		setDaemon(true);
	}

	public void run() {
		
		// Wait x seconds before we destroy the remaining data that still remains to be read
		FileTLUtil.sleep(_timeToWaitBeforeDelete); 
		
		// For each file that contains packets for this input stream, delete it
		String filePattern = "filetl-packet-source["+_remoteName+"]-to-dest["+_listenName+"]-";
		VFile[] fileList = _directory.listFiles();
		for(VFile f : fileList) {
			
			if(f.getName().startsWith(filePattern)
					&& f.getName().contains("-connuuid["+_connUUID+"]")) {
				// We do both to ensure we get all
				f.delete();
				f.deleteOnExit();
			}
		}
		
		
		// Keep trying to delete the remaining files every 30 seconds
		int filesRemaining = 0;
		
		do {
			filesRemaining = 0;
		
			filePattern = "filetl-packet-source["+_remoteName+"]-to-dest["+_listenName+"]-";
			fileList = _directory.listFiles();
			for(VFile f : fileList) {
				
				if(f.getName().startsWith(filePattern) 
						&& f.getName().contains("-connuuid["+_connUUID+"]")) {
					f.delete();
				}
				
				if(f.exists()) {
					filesRemaining++;
				}
			}
		
			if(filesRemaining > 0) FileTLUtil.sleep(TIME_TO_WAIT_ON_REMAINING);
			
		} while(filesRemaining > 0);

	}
}
