/*
	Copyright 2012, 2019 Jonathan West

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.socketanywhere.filefactory.lock.IFileLock;
import com.socketanywhere.filefactory.lock.FileLockOnNonlockingFS;
import com.vfile.VFile;

/**
 * 
 * File Tags (see comments below for tag):
 * 
 * BOUND-CONNECTION-ACQUIRED - Point at which a bound connection is acquired. These should all be inside 
 * a synchronization on the BoundConnectionThreadResult, with a check on _acquireConnIfAvailable inside 
 * (and before the actual lock is acquired).
 *
 * 
 * File Messages:
 *
 * When a bound connection is active, the following file should exist in the directory
 * - filetl-listening-on-[(bound connection name)]-bounduuid[(bound connection's uuid]
 * 
 * Test whether a bound connection is active:
 * - filetl-test-listen-on-connection[(bound connection)]-bounduuid[(uuid of bound connection)]-testuuid[(random uuid of the tester)]
 * - Response, if active: filetl-test-listen-active-connection[(bound connection)]-bounduuid[(uuid if bound connection)]-testuuid[(random uuid of the tester)]
 *
 */

class BoundConnectionManager extends Thread {
	
	public static void zout(String str) {
		System.out.println(Thread.currentThread().getId() + "> "+str);
	}
	
	private static BoundConnectionManager _instance = new BoundConnectionManager();
	boolean _connectionManagerThreadRunning = false;
	
	/** A list of all connections that this process is currently bound to */
	List<BoundConnEntry> _boundConnections = new ArrayList<BoundConnEntry>();
	
	private static final long LOCK_POLL_INTERVAL = 500;
	
	private static String extractBoundUUIDFromFilename(String filename) {
		return FileTLUtil.extractField("bounduuid", filename);
	}
	
	
	protected static BoundConnectionManager getInstance() {
		return _instance;
	}
	
	private BoundConnectionManager() {
		super(BoundConnectionManager.class.getName());
		_connectionManagerThreadRunning = true;
		this.setDaemon(true);
		this.start();
	}
	
	private BoundConnEntry createAndAddBoundConnection(VFile dir, String name, String connUUID) {
		BoundConnEntry bce = new BoundConnEntry();
		bce._connDirectory = dir;
		bce._connName = name;
		bce._connUUID = connUUID;
		
		synchronized(_boundConnections) {
			_boundConnections.add(bce);
		}
		return bce;
	}
	
	private void removeBoundConnection(BoundConnEntry be) {
		synchronized(_boundConnections) {
			_boundConnections.remove(be);
		}
		VFile f = new VFile(be._connDirectory.getPath() + VFile.separator + "listening-on-["+be._connName+"]-bounduuid["+be._connUUID+"]");
		QueueManager.queueDeleteFile(f);
	}
	
	public void run() {

		while(_connectionManagerThreadRunning) {

			synchronized (_boundConnections) {
				
				
				// Caches directory listing: Directory -> List of files in directory
				Map<VFile, VFile[]> dirList = new HashMap<VFile, VFile[]>();
			
				for(BoundConnEntry be : _boundConnections) {
					VFile dir = be._connDirectory;
					
					// If the dirList is empty, list the directory and store it in dirList
					VFile[] fileList;
					if(dirList.get(dir) == null) {
						fileList = dir.listFiles();
//						fileList = dir.listFiles(FileTLUtil.FILE_TL_FILE_FILTER);
						dirList.put(dir, fileList);
					} else {
						fileList = dirList.get(dir);
					}
					
					// If anyone is testing the bound connection entry, it will take this form
					String lockTestFormat = "filetl-test-listen-on-connection["+be._connName+"]-bounduuid["+be._connUUID+"]-testuuid[";
					
					for(VFile f : fileList) {
						
						// We have found a matching file...
						if(f.getName().startsWith(lockTestFormat)) {
							
							// ... so reply
							String testUUID = FileTLUtil.extractField("testuuid", f.getName());
							
							String responseName = "filetl-test-listen-active-connection["+be._connName+"]-bounduuid["+be._connUUID+"]-testuuid["+testUUID+"]";
							VFile responseFile = new VFile(dir.getPath()+VFile.separator+responseName);
							if(!responseFile.exists()) {
								final int TIME_TO_WAIT_BEFORE_DELETE = 60000;
								FileTLUtil.writeEmptyMessageFile(responseFile);

								TimedDeleteManager.getInstance().deleteFile(responseFile, TIME_TO_WAIT_BEFORE_DELETE, false);

							}
						}
					}
				}
			} // end synchronized
			
			
			FileTLUtil.sleep(LOCK_POLL_INTERVAL);
			
		}
		
	}
	
	boolean unbindOnNames(VFile directory, List<String> namedAddresses) throws FileTLIOException {
		
		synchronized(_boundConnections) {
			List<BoundConnEntry> l = new ArrayList<BoundConnEntry>();
			List<String> namedAddrList = new ArrayList<String>();
			
			namedAddrList.addAll(namedAddresses);
			
			// Located all BoundConnEntry objects that match the given name
			for(BoundConnEntry e : _boundConnections) {
				
				// For each of the named addresses to unbind from
				for(String s : namedAddresses) {
					
					if(e._connName.equalsIgnoreCase(s)) {
						l.add(e);
						namedAddrList.remove(s);
					}
				}
			}
			
			if(namedAddrList.size() != 0) {
				throw new FileTLIOException("Not all bound addresses were found");
			}
			
			// All bound addresses were found, so remove them one by one			
			for(BoundConnEntry e : l) {
				removeBoundConnection(e);
			}
			
			FileTLLogger.unbindOnNames(namedAddresses);
			
			return true;
			
		}
	}
	
	boolean bindOnNames(VFile directory, List<String> namedAddresses) {
		final long TIME_TO_WAIT_FOR_BIND = 1000 * 60;
		
		// The threads will store their results in here
		BoundConnectionThreadResult[] mtr = new BoundConnectionThreadResult[namedAddresses.size()];
		
		for(int x = 0; x < mtr.length; x++) {
			mtr[x] = new BoundConnectionThreadResult();
			mtr[x]._connAcquired = false;
			mtr[x]._acquireConnIfAvailable = true;
		}
		
		// Start a thread for each name we want to bind to 
		int x = 0;
		for(String addr : namedAddresses) {
			mtr[x]._address = addr;
			BoundConnectionThread thread = new BoundConnectionThread(mtr[x], directory, addr);
			thread.setDaemon(true);
			thread.start();
			x++;
		}
		
		long start = System.currentTimeMillis();
		
		boolean contLoop = true;
		boolean timeout = false;
		boolean bindComplete = false;
		
		
		// This loops exits on either timeout, or acquisition of all addresses
		while(contLoop) { 
			
			// Have all the threads acquired connections?
			boolean allComplete = true;
			for(x = 0; x < mtr.length; x++) {
				synchronized(mtr[x]) {
					if(!mtr[x]._connAcquired) {
						allComplete = false;
					}
				}
			}
			
			// They have!
			if(allComplete) {
				bindComplete = true;
				contLoop = false;
				break;
			}
			
			// If we have timed out the wait, then exit the loop
			if(System.currentTimeMillis() - start > TIME_TO_WAIT_FOR_BIND) {
				timeout = true;
				bindComplete = false;
				contLoop = false;
				break;
			}
			
			if(contLoop) {
				FileTLUtil.sleep(LOCK_POLL_INTERVAL);
			}
		}

		// Inform any threads that are still running that they should no longer acquire bound connections 
		for(x = 0; x < mtr.length; x++) {
			synchronized (mtr[x]) {
				// This method of terminating all the threads works because all lock acquisition is synchronized on a BoundConnectionThreadResult, 
				// and inside the synch the _acquireConnIfAvailable variable is checked before the lock itself is created.
				mtr[x]._acquireConnIfAvailable = false;
			}
		}
		
		// Necessarily, no new connections will be created after the above loop has run.

		// We could not acquire all the locks in time, so release
		if(timeout) {	
			for(x = 0; x < mtr.length; x++) {
				synchronized(mtr[x]) {
					if(mtr[x]._connAcquired) {
						BoundConnectionManager.getInstance().removeBoundConnection(mtr[x]._be);

						FileTLUtil.deleteCrucialFile(mtr[x]._connFile);						
					} else {
						FileTLLogger.bindFailed(mtr[x]._address);
					}
				}
			}
		}
		
		if(bindComplete) {
			FileTLLogger.bindOnNames(namedAddresses);
			return true;
		}
		return false;
				
	}


	/**  Contains information on a connection we are currently bound to */
	private static class BoundConnEntry {
		String _connName = null;
		String _connUUID = null;
		VFile _connDirectory = null;
		
		public boolean equals(Object obj) {
			if(!(obj instanceof BoundConnEntry)) {
				return false;
			}
			BoundConnEntry bce = (BoundConnEntry)obj;
			
			return _connName.equalsIgnoreCase(bce._connName) &&
				_connDirectory.getPath().equals(bce._connDirectory.getPath()) &&
				_connUUID.equalsIgnoreCase(bce._connUUID);
		}
	}
	
	/** This thread attempts to bind to a given connection name, inside the given directory. 
	 * Used by bindOnNames of BoundConnectionManager. */
	private static class BoundConnectionThread extends Thread {
		BoundConnectionThreadResult _result = null;

		VFile _directory = null;
		String _connName = null;
		
		private static long TIME_TO_WAIT_FOR_CONN_TEST = 1000 * 4;
		
		public BoundConnectionThread(BoundConnectionThreadResult result, VFile directory, String connName) {
			super();
			_result = result;
			_directory = directory;
			_connName = connName;
			
			setName(BoundConnectionThread.class.getName());
			setDaemon(true);
		}
		
		public void run() {
			
			zout("BoundConnectionThread started");
			
			VFile[] dirList = null;

			// TODO: EASY - LOWER - Rename filetl-listening-on to filetl-listening-on-addr
			
			String matchName = "filetl-listening-on-["+_connName+"]-";
			
			boolean bcAcquired = false;
			
			// While we haven't acquired the lock, and while BoundConnectionManager hasn't timed us out
			while(!bcAcquired && _result._acquireConnIfAvailable) {

				dirList = _directory.listFiles();
				System.out.println("dirred."); 
				
				// Look to see if anyone else is listening on this connection
				VFile bcMatch = null; 
				for(VFile f : dirList) {
					if(f.getName().startsWith(matchName)) {
						bcMatch = f;
					}
				}
				
				System.out.println("post bcmatch ");
				// If no one is listening on this connection, attempt to acquire it
				if(bcMatch == null) {
					zout("pre createdNewConnFile");
					VFile result = createNewConnectionFile();
					
					zout("createdNewConnFile");
					
					if(result != null) {
						synchronized (_result) {
							BoundConnEntry be = BoundConnectionManager.getInstance().createAndAddBoundConnection(_directory, _connName, extractBoundUUIDFromFilename(result.getName()));
							_result._be = be;
							_result._connFile = result;
							_result._connAcquired = true;							
						}
						bcAcquired = true;
						break;
					} else {
						// Exit the if and continue to the top of the loop again 
					}
					
				} else {
			
					zout("write and wait for bound entered");
					
					// Someone may be presently listening on this connection, so test if it is active
					writeAndWaitForBoundConnectionTest(bcMatch);
					
					zout("write and wait for bound left");
				}
				
				// Sleep if we don't have the lock, and we actually still want it
				if(!bcAcquired && _result._acquireConnIfAvailable) {
					FileTLUtil.sleep(LOCK_POLL_INTERVAL);
				}				
			}
			
		}
		
		/** Returns null if the bound connection could not be acquired in this cycle, or the file of the new bound connection if it could. */
		private void writeAndWaitForBoundConnectionTest(VFile existingConn) {
			
			// The UUID from the existing lock
			String existLockBoundUUID = extractBoundUUIDFromFilename(existingConn.getName());
		
			// Our own UUID
			String uuid = FileTLUtil.generateUUID();
			
			// This is our test message
			String testFileName = "filetl-test-listen-on-connection["+_connName+"]-bounduuid["+existLockBoundUUID+"]-testuuid["+uuid+"]";
			VFile testFile = new VFile(_directory.getPath() + VFile.separator+testFileName);
			
			// If the lock is alive, this is the response we will expect to see
			String expectedResponseFileName = "filetl-test-listen-active-connection["+_connName+"]-bounduuid["+existLockBoundUUID+"]-testuuid["+uuid+"]";
			VFile expectedRespFile = new VFile(_directory.getPath() + VFile.separator + expectedResponseFileName);
			
			FileTLUtil.writeEmptyMessageFile(testFile);
			
			boolean continueLoop = true;
			
			boolean timeout = false;
			boolean receivedResponse = false;
			boolean fileDNE = false;
			boolean bindUnwanted = false;
			
			long startTime = System.currentTimeMillis();
			
			while(continueLoop) {
				
				// If the existing lock has disappeared
				if(!(existingConn.exists())) {
					continueLoop = false;
					fileDNE = true;
					break;
				}
				
				// If we have received a response to our test message
				if(expectedRespFile.exists()) {
					continueLoop = false;
					receivedResponse = true;
					break;		
				}
				
				// If timeout has occurred
				if(System.currentTimeMillis() - startTime > TIME_TO_WAIT_FOR_CONN_TEST) {
					timeout = true;
					continueLoop = false;
					break;
				}
				
				// If the connection is no longer required then exit the loop
				synchronized(_result) {
					if(!_result._acquireConnIfAvailable) {
						bindUnwanted = true;
						continueLoop = false;
					}
				}
				
				// Otherwise, keep looping
				if(continueLoop) {
					FileTLUtil.sleep(LOCK_POLL_INTERVAL);
				}
			}
			
			QueueManager.queueDeleteFile(testFile);
			QueueManager.queueDeleteFile(expectedRespFile);
			
			// The bind operation is no longer required, so exit
			if(bindUnwanted) {
				return;
			}
			
			// File DNE, meaning either it has closed and we can grab it, or it has closed and someone else has grabbed it already
			if(fileDNE) {
				return;
			}
			
			// Received response, meaning the lock is still active. We'll exit the loop and try again.
			if(receivedResponse) {
				return;
			}
			 
			// Timeout means the other bound connection is dead, and we can attempt to acquire it
			if(timeout) {
				
				FileTLUtil.deleteCrucialFile(existingConn);
				return;
			}
			
			// TODO: LOWER - BoundConnectionManager - Need to -ready this entire file up.
		}
		
		/** No one is listening on this bound connection, so attempt to acquire a lock on the connection */
		private VFile createNewConnectionFile()  {
			VFile myNewConn = new VFile(_directory.getPath()+VFile.separator+"filetl-listening-on-["+_connName+"]-bounduuid["+FileTLUtil.generateUUID()+"]");
			
			zout("pre mkdirs");
			VFile lockDir = new VFile(_directory.getPath() + VFile.separator + "locks");
			lockDir.mkdirs();
			
			zout("post mkdirs");
			
			IFileLock lock = new FileLockOnNonlockingFS(lockDir, "establish-bound-connection-lock-"+_connName);
			
			synchronized(_result) {
				if(!_result._acquireConnIfAvailable) return null;
			}
			
			zout("Attempting to Acquire lock!");
			boolean lockAcquired = lock.acquireLock(30000);
			
			if(!lockAcquired) {
				return null;
			} 
			zout("Acquired locked!");
			
			
			// Now that the lock has been acquired, we must verify that no one else is now listening on the connection
			
			String matchName = "filetl-listening-on-["+_connName+"]-";
			VFile[] dirList = _directory.listFiles();
			// Look to see if anyone else is listening on this connection
			VFile lockMatch = null;
			for(VFile f : dirList) {
				if(f.getName().startsWith(matchName)) {
					lockMatch = f;
				}
			}
			
			VFile result = null;
			
			if(lockMatch == null) {
			
				// No one else bound to connection, so acquire it
				synchronized(_result) {
					if(_result._acquireConnIfAvailable) {
						// BOUND-CONNECTION-ACQUIRED
						FileTLUtil.writeEmptyMessageFile(myNewConn);
						result = myNewConn;
					} else {
						result = null;
					}
				}
			} else {
				// Someone else bound to the connection while we were waiting on the lock, so return emptyhanded
				result = null;
			}
		
			zout("releasing lock");
			lock.releaseLock();
			zout("released lock");
			return result;
		}
		
	}
	
	private static class BoundConnectionThreadResult {
		VFile _connFile = null;
		BoundConnEntry _be = null;
		
		/** The thread will only continue to run while this is true, and many checkpoints through the thread run code also check this and return null if it is false (to ensure faster thread death)*/
		boolean _acquireConnIfAvailable = true;

		boolean _connAcquired = false;
		
		String _address = null;
		
		public BoundConnectionThreadResult() { }
		
	}
}



