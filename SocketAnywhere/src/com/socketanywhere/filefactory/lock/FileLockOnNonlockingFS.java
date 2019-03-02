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

package com.socketanywhere.filefactory.lock;

import com.socketanywhere.filefactory.FileTLRuntimeException;
import com.socketanywhere.filefactory.FileTLUtil;
import com.socketanywhere.filefactory.QueueManager;
import com.socketanywhere.filefactory.TimedDeleteManager;
import com.socketanywhere.net.SoAnUtil;
import com.vfile.VFile;

/** *
 * You must:
 * - Only ever release a Lock from within the thread it was acquired
 * - Do not call acquire() on a lock that has already been released(). 
 *
 * File Messages:

 * Active Lock:
 * - filetl-lock[lockName]-lockuuid[FileTLUtil.generateUUID()]
 * 
 * Attempting to acquire a new (non-existent) lock:
 * - filetl-attempt-acquire-lock[lockName]-acquireuuid[acquireUUID]
 * 
 * Testing an existing active to see if it is still active:
 * - filetl-test-lock[lockName]-lockuuid[lockUUID]-testuuid[uuidComponent]
 * - This class sends this.
 * 
 * The response from an active lock, indicating that it is in fact still active
 * - filetl-test-lock-response-active-lock[lockName]-lockuuid[lockUUID]-testuuid[uuidComponent]
 * - This file waits for this, after sending the above.
 * 
 * Implements on a lock on a filesystem that doesn't natively support file locking. 
 *
 */
public class FileLockOnNonlockingFS implements IFileLock {
	
	public static void zout(String str) {
		System.out.println(Thread.currentThread().getId()+"> "+str);
	}
		
	protected static final int WAIT_INTERVAL = 750;
	protected static final int MAX_WAIT_TIME = 5 * 1000;

	protected static final long UNLIMITED_WAIT_TIME = -1;
	
	boolean _lockActive = false;
	boolean _lockReleased = false;
		
	VFile _lockUUIDFile = null;
	
	VFile _lockDirectory = null;
	String _lockName = null;
	String _lockUUID = null;
	
	public FileLockOnNonlockingFS(VFile directory, String name) {
		_lockDirectory = directory;
		_lockName = name;
	}
	
	public void acquireLock() {
		acquireLock(UNLIMITED_WAIT_TIME);
	}
	
	
	public boolean acquireLock(long maxTimeToWait) {
	
		VFile lockDirectoryFile =  new VFile(_lockDirectory.getPath()+VFile.separator+"filetl-lock["+_lockName+"]");
		
		if(_lockReleased) {
			throw new FileTLRuntimeException("Attempting to acquire a lock that has been released.");
		}
		
		if(_lockActive) {
			throw new FileTLRuntimeException("Attempting to acquire a lock that has not been released.");
		}
		
		// Ensure the lock directory exists
		if(!lockDirectoryFile.exists()) {
			zout("pre lock mkdirs"); 
			boolean mkdirResult = lockDirectoryFile.mkdirs();
			zout("post lock mkdirs"); 
			if(!mkdirResult && !lockDirectoryFile.exists()) {
				throw new FileTLRuntimeException("Unable to create lock directory ["+_lockName+"]");
			}
		}

		long acquireStartTime = System.currentTimeMillis();

		boolean lockAcquired = false;
		boolean loopWait = false;
		
		VFile activeUUIDLock = null;
		
		while(!lockAcquired) {
			zout("pre findActiveUUIDLockFile"); 
			activeUUIDLock = findActiveUUIDLockFile(lockDirectoryFile, _lockName);
			zout("post findActiveUUIDLockFile"); 
			if(activeUUIDLock != null) {
				// Lock already exists... so test
				zout("pre testActiveLock"); 
				boolean testResult = testActiveLock(activeUUIDLock, _lockName, acquireStartTime, maxTimeToWait);
				zout("post testActiveLock"); 
				
				if(testResult) {
					// Lock was active, so keep looping
					loopWait = true;
				} else {
					// Lock was not active, so delete the lock and try to acquire
					FileTLUtil.deleteCrucialFile(activeUUIDLock);
					loopWait = false; 
				}
								
			} else {
				
				// No lock current exists, so acquire
				zout("pre internalAcquireLock"); 
				VFile lock = internalAcquireLock(lockDirectoryFile, _lockName, acquireStartTime, maxTimeToWait);
				zout("post internalAcquireLock");
				if(lock != null) {
					loopWait = false;
					lockAcquired = true;
					
					_lockUUIDFile = lock;
					_lockUUID = extractLockUUIDFromFilename(_lockUUIDFile.getName());
					_lockActive = true;
					
					LockManager.getInstance().addLock(this);
					
				} else {
					// Unable to acquire lock
					loopWait = true;
				}
			}

			if(loopWait && !lockAcquired && maxTimeToWait != UNLIMITED_WAIT_TIME) {
				if(System.currentTimeMillis() - acquireStartTime > maxTimeToWait) {
					return false;
				}
			}
			
			if(loopWait) {
				zout("AL sleeping");
				FileTLUtil.sleep(WAIT_INTERVAL);
			}
			
		}
		
		if(lockAcquired)  {
			return true;
		} else {
			return false;
		}
	}
	

	// filetl-attempt-acquire-lock[lockName]-acquireuuid[uuidComponent]

	// active lock: filetl-lock[lockName]-lockuuid[uuid]
	
	private static VFile internalAcquireLock(VFile directory, String lockName, long acquireStartTime, long acquireMaxWaitTime) {
		final int TIME_TO_WAIT_TO_CLEAR_ATTEMPT_ACQ = 11000;
		final int TIME_TO_WAIT_BEFORE_LOCK_ACQ = 5000;
		
		// list for files - looking for 'attempt acquire' or already existing locks
		VFile[] fileList = directory.listFiles();
		for(VFile f : fileList) {
			String fn = f.getName();
			
			if(fn.startsWith("filetl-attempt-acquire-lock")) {
				// Someone appears to be attempting to acquire a lock, so we will
				// delete it in 11 seconds to keep it from blocking us, in case it is stale
				TimedDeleteManager.getInstance().deleteFile(f, TIME_TO_WAIT_TO_CLEAR_ATTEMPT_ACQ, true); 
				return null;
			}
			
			if(fn.startsWith("filetl-lock[")) {
				// Someone else completed acquisition, so return
				return null;
			}
					
		}
		
		// At this point, there are no existing acquired locks, and we have not
		// seen anyone attempt to acquire this lock yet.
		
		String acquireUUID = FileTLUtil.generateUUID();
		VFile attemptAcquireFile = new VFile(directory.getPath()+VFile.separator+"filetl-attempt-acquire-lock["+lockName+"]-acquireuuid["+acquireUUID+"]");
		FileTLUtil.writeEmptyMessageFile(attemptAcquireFile);
		
		boolean lockAcquired = false;
		boolean waiting = true;
		long scanStartTime = System.currentTimeMillis();
		
		do {
			
			// If we have waited the necessary amount of time to acquire the lock...
			if(System.currentTimeMillis() - scanStartTime > TIME_TO_WAIT_BEFORE_LOCK_ACQ) { 
				lockAcquired = true;
				waiting = false;
			}
			
			// TODO: ARCHITECTURE - This acquireMaxTime must be greater than the 5 second wait time for acq, else it will never complete  
			if(waiting && acquireMaxWaitTime != UNLIMITED_WAIT_TIME) {
				if(System.currentTimeMillis() - acquireStartTime > acquireMaxWaitTime) {
					FileTLUtil.deleteCrucialFile(attemptAcquireFile);
					return null;
				}
			}

			// I considered adding a if(waiting) to the file list block below, but
			// I believe leaving it out adds reliability, as this forces a timed out
			// attempt acquire (from the first if in the loop) to look one more time to 
			// see if anyone else has attempt acq-ed, or locked. 
			
			// File list block begin {{
			fileList = directory.listFiles();
			
			// Look to see if the lock has already been acquired by someone else
			for(VFile f : fileList) {
				String fn = f.getName();
				
				if(fn.startsWith("filetl-lock[")) {
					// Someone else completed acquisition, so return					
					lockAcquired = false;
					waiting = false;
					FileTLUtil.deleteCrucialFile(attemptAcquireFile);
					return null;
				}
			}
			
			// Look to see if anyone else is attempting to acquire on this lock
			for(VFile f : fileList) {
				String fn = f.getName();
				
				if(fn.startsWith("filetl-attempt-acquire-lock") 
						&& !fn.equalsIgnoreCase(attemptAcquireFile.getName())) {

					// ... someone is.
					
					String theirUUID = FileTLUtil.extractField("acquireuuid", fn);
					
					// Person with the lower UUID wins
					if(theirUUID.compareTo(acquireUUID) < 0 ) {
						// Someone has beaten us to it, so return, and delete the file
						// in some number of seconds to prevent stale attempt acqs
						TimedDeleteManager.getInstance().deleteFile(f, 11000, true);
						FileTLUtil.deleteCrucialFile(attemptAcquireFile);
						lockAcquired = false;
						waiting = false;
						return null;
					} else {
						// We win, the other process should stop trying as soon
						// as they see our attempt
					}
					
				}
			}
			// }} File list block end
			
			
			if(waiting) {
				FileTLUtil.sleep(WAIT_INTERVAL);
			}
			
		} while(waiting);

		if(lockAcquired) {
			VFile newLock = new VFile(directory.getPath()+VFile.separator+"filetl-lock["+lockName+"]-lockuuid["+FileTLUtil.generateUUID()+"]");
			FileTLUtil.writeEmptyMessageFile(newLock);
			FileTLUtil.deleteCrucialFile(attemptAcquireFile);
			return newLock;
		} else {
			return null;
		}
	
	}
	
	/** Test whether or not existing lock is active. Result is true if active, false otherwise */
	private static boolean testActiveLock(VFile activeLock, String lockName, long acquireStartTime, long acquireMaxWaitTime) {
		if(activeLock == null) {
			return false;
		}
		
		WriteTestLockFileReturn ret = null; // value returned by a call to writeTestLockFile
		
		// The lock's directory
		VFile lockDirectory = activeLock.getParentFile(); 
				
		long startTime = System.currentTimeMillis();		
		
		// Whether the active lock has not timed out yet 
		boolean activeLockIsActive = true;
		
		boolean lockActiveFileDNE = false;
		boolean lockTimedOut = false;

		// Write the file that tests whether the active lock is still active 
		ret = writeTestLockFile(lockDirectory, lockName, extractLockUUIDFromFilename(activeLock.getName()));
		
		do {

			// Lock no longer exists because someone renamed it, so skip to the top of the outermost loop
			if(activeLock == null || !activeLock.exists()) {
				activeLockIsActive = false;
				lockActiveFileDNE = true;
				break;
			}
			
			// If we've timed out... success!!
			if(System.currentTimeMillis() - startTime > (MAX_WAIT_TIME)) {
				activeLockIsActive = false;
				lockTimedOut = true;
				break;
			}
			
			// We received a response from another process holding the lock
			if(ret.expectedResponseFile.exists()) {
				activeLockIsActive = false;
				break;
			}
			
			// If we have exceed the total max time for acquire, leave
			if(activeLockIsActive && acquireMaxWaitTime != UNLIMITED_WAIT_TIME) {
				if(System.currentTimeMillis() - acquireStartTime > acquireMaxWaitTime) {
					activeLockIsActive = false;
					break;
				}
			}

			if(activeLockIsActive) {
				FileTLUtil.sleep(WAIT_INTERVAL);
			}
			
		} while(activeLockIsActive);

		// File cleanup
		if(ret != null) {
			if(ret.testLockFile.exists()) {
				QueueManager.queueDeleteFile(ret.testLockFile);
			}

			if(ret.expectedResponseFile.exists()) {
				QueueManager.queueDeleteFile(ret.expectedResponseFile);
			}
		}
		
		if(lockActiveFileDNE || lockTimedOut) {
			// Lock is not active, can be deleted
			return false;
		} else {
			// Lock is still active
			return true;
		}
	}

	public void releaseLock() {
		
		if(!_lockActive) {
			throw new FileTLRuntimeException("Attempting to release a lock that has not been acquired.");
		}
		
		if(!_lockUUIDFile.exists()) {
			throw new FileTLRuntimeException("Lock file no longer exists.");			
		}
	
		FileTLUtil.deleteCrucialFile(_lockUUIDFile);
		
		_lockReleased = true;
		_lockActive = false;
		_lockUUIDFile = null;
		_lockUUID = null;
				
		LockManager.getInstance().removeLock(this);
		
		// TODO: LOWER - ARCHITECTURE - Add a GMT timestamp to locks, and acquire, then delete, the directories of those that are too old and do not respond. 
	}
	
	/** Write a test lock message to the directory, and returns the expected response message file name, as well as the test lock file name */
	private static WriteTestLockFileReturn writeTestLockFile(VFile directory, String lockName, String lockUUID) {
		
		String uuidComponent = SoAnUtil.generateUUID().toString();
		
		String testLockFilename = "filetl-test-lock["+lockName+"]-lockuuid["+lockUUID+"]-testuuid["+ uuidComponent+"]";
		
		VFile testLockFile = new VFile(directory.getPath()+VFile.separator+testLockFilename);
		
		String expectedResponseName = "filetl-test-lock-response-active-lock["+lockName+"]-lockuuid["+lockUUID+"]-testuuid["+ uuidComponent+"]";
		VFile expectedResponseFile = new VFile(directory.getPath()+VFile.separator+expectedResponseName);
		FileTLUtil.writeEmptyMessageFile(testLockFile);
		
		WriteTestLockFileReturn ret = new WriteTestLockFileReturn();
		ret.testLockFile = testLockFile;
		
		ret.expectedResponseFile = expectedResponseFile;
		
		return ret;
	}
	
	private static VFile findActiveUUIDLockFile(VFile directory, String lockName) {
		VFile existingUUID = null;
		
		VFile[] lockDirFileList = directory.listFiles();
		
		for(VFile f : lockDirFileList) {
			if(f.getName().startsWith("filetl-lock["+lockName+"]-lockuuid[")) {
				existingUUID = f;
			}
		}
		
		return existingUUID;
	}

	private static String extractLockUUIDFromFilename(String filename) {
		return FileTLUtil.extractField("lockuuid", filename);
		
	}
	
	protected static String extractTestUUIDFromFilename(String filename) {
		return FileTLUtil.extractField("testuuid", filename);		
	}
	
	private static class WriteTestLockFileReturn {
		VFile testLockFile = null;
		VFile expectedResponseFile = null;
	}

	public String getName() {
		return _lockName;
	}

	public String getLockUUID() {
		return _lockUUID;
	}

	public VFile getLockUUIDFile()  {
		return _lockUUIDFile;
	}
}
