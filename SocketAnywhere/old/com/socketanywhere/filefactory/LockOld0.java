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

import java.io.File;

import com.socketanywhere.net.SoAnUtil;

public class LockOld0 {

	protected static final int WAIT_INTERVAL = 1000;
	protected static final int MAX_WAIT_TIME = WAIT_INTERVAL * 10;

	boolean _lockActive = false;
	
	/** This variable is a convenience variable, and refers to the name of the directory 
	 * that represents lock acquisition (this value is set by the constructor and does not change)*/
	File _lockDirectoryFile = null;
	
	File _directory = null;
	String _name = null;
	
	LockOld0(File directory, String name) {
		_directory = directory;
		_name = name;
		_lockDirectoryFile =  new File(_directory.getPath()+File.separator+"filetl-lock["+_name+"]");
	}
	
	public void acquireLock() {
		if(_lockActive) {
			throw new FileTLRuntimeException("Attempting to acquire a lock that has not been released.");
		}
		
		boolean mkdirResult = false;
		
		while(mkdirResult == false) {
			
			if(_lockDirectoryFile.exists()) {
				testExistingLock(_directory, _name, _lockDirectoryFile);
			}
			
			mkdirResult = _lockDirectoryFile.mkdir();
						
			if(!mkdirResult) {
				FileTLUtil.sleep(WAIT_INTERVAL);
			}
			
		}
		_lockActive = true;
		LockManagerOld1.getInstance().addLock(this);

	}
	
	public void releaseLock() {
		if(!_lockActive) {
			throw new FileTLRuntimeException("Attempting to release a lock that has not been acquired.");
		}
		
		boolean result = _lockDirectoryFile.delete();
		if(!result) {
			throw new FileTLRuntimeException("Unable to delete directory, likely already removed");
		} 
		_lockActive = false;
		LockManagerOld1.getInstance().removeLock(this);
		
	}
	
	private static class WriteTestLockFileReturn {
		File testLockFile = null;
		File expectedResponseFile = null;
	}

	
	/** This function will block until:
	 * o Our test lock times out
	 * o The lock no longer exists (implying it is no longer in use)
	 * Thus, when this function returns, it is safe to assume the lock can now be written.*/
	private static void testExistingLock(File directory, String lockName, File activeLock) {
		
		WriteTestLockFileReturn ret = null;		
		
		while(activeLock.exists()) {
		
			long startTime = System.currentTimeMillis();			
			boolean cont = true;
			boolean writeNewTestLock = true;
			boolean lockTimedOut = false;
			do {
				
				// If we've been asked to write a new test lock (because the old one got a response, then do it)
				if(writeNewTestLock) {
//					if(ret != null) {
//						if(ret.expectedResponseFile.exists()) {
//							ret.expectedResponseFile.delete();
//						}
//					}
					writeNewTestLock = false;
					ret = writeTestLockFile(directory, lockName);
				}
				
				// Lock no longer exists, so we can write it
				if(!activeLock.exists()) {
					QueueManager.queueDeleteFile(ret.testLockFile);
					QueueManager.queueDeleteFile(ret.expectedResponseFile);
//					ret.testLockFile.delete();
//					if(ret.expectedResponseFile.exists()) {
//						ret.expectedResponseFile.delete();						
//					}
					cont = false;
					break;
				}
				
				// If we've timed out
				if(System.currentTimeMillis() - startTime > (MAX_WAIT_TIME)) {
					QueueManager.queueDeleteFile(ret.testLockFile);
					QueueManager.queueDeleteFile(ret.expectedResponseFile);
					cont = false;
					lockTimedOut = true;
					break;
				}
				
				// We received a response from another process holding the lock, so keep looping
				if(ret.expectedResponseFile.exists()) {
//					ret.testLockFile.delete();
//					ret.expectedResponseFile.delete();
					QueueManager.queueDeleteFile(ret.testLockFile);
					QueueManager.queueDeleteFile(ret.expectedResponseFile);
					startTime = System.currentTimeMillis();
					writeNewTestLock = true;
				}
	
				FileTLUtil.sleep(WAIT_INTERVAL);
				
			} while(cont);

			if(ret.testLockFile.exists()) {
				QueueManager.queueDeleteFile(ret.testLockFile);
			}

			if(ret.expectedResponseFile.exists()) {
				QueueManager.queueDeleteFile(ret.expectedResponseFile);
			}
			
			if(lockTimedOut) {
				activeLock.delete();
				if(activeLock.exists()) {
					throw new RuntimeException("Unable to delete inactive lock.");
				}
			}
		}
		
		return;
	}

	
	/** Write a test lock message to the directory, and returns the expected response message file name, as well as the test lock file name */
	private static WriteTestLockFileReturn writeTestLockFile(File directory, String lockName) {
		
		String uuidComponent = SoAnUtil.generateUUID().toString();
		
		String testLockFilename = "filetl-test-lock["+lockName+"]-testuuid["+ uuidComponent+"]";
		
		File testLockFile = new File(directory.getPath()+File.separator+testLockFilename);
		
		String expectedResponseName = "filetl-test-lock-response-active-lock["+lockName+"]-testuuid["+ uuidComponent+"]";
		File expectedResponseFile = new File(directory.getPath()+File.separator+expectedResponseName);
		FileTLUtil.writeEmptyMessageFile(testLockFile);
		
		WriteTestLockFileReturn ret = new WriteTestLockFileReturn();
		ret.testLockFile = testLockFile;
		
		ret.expectedResponseFile = expectedResponseFile;
		
		return ret;
	}

	public String getName() {
		return _name;
	}

	public File getLockDirectoryFile() {
		return _lockDirectoryFile;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof LockOld0)) {
			return false;
		}
		LockOld0 l = (LockOld0)obj;
		return super.equals(l);
//		l.
	}
	
	protected static String extractTestUUIDFromFilename(String filename) {
		int start = filename.indexOf("-testuuid[") + "-testuuid[".length();
		int end = filename.indexOf("]", start);
		
		String result = filename.substring(start, end);
		
		return result;
		
	}


	
}
