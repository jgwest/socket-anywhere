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
import java.io.FileWriter;
import java.io.IOException;

import com.socketanywhere.net.SoAnUtil;

public class LockOld2 {
	
	protected static final int WAIT_INTERVAL = 1000;
	protected static final int MAX_WAIT_TIME = WAIT_INTERVAL * 10;
	
	private static class WriteTestLockFileReturn {
		File testLockFile = null;
		File expectedResponseFile = null;
	}
	
	protected static String extractLockNameFromFilename(String filename) {
		int start = filename.indexOf("filetl-lock[") + "filetl-lock[".length();
		int end = filename.indexOf("]", start);
		
		String result = filename.substring(start, end);
		
		return result;
	}
	
	protected static String extractLockUUIDFromFilename(String filename) {
		int start = filename.indexOf("-lockuuid[") + "-lockuuid[".length();
		int end = filename.indexOf("]", start);
		
		String result = filename.substring(start, end);
		
		return result;
	}
	
	protected static String extractTestUUIDFromFilename(String filename) {
		int start = filename.indexOf("-testuuid[") + "-testuuid[".length();
		int end = filename.indexOf("]", start);
		
		String result = filename.substring(start, end);
		
		return result;
		
	}

	/** Write a test lock message to the directory, and returns the expected response message file name, as well as the test lock file name */
	private static WriteTestLockFileReturn writeTestLockFile(File directory, String lockName, String lockUUID) {
		
		String uuidComponent = SoAnUtil.generateUUID().toString();
		
		String testLockFilename = "filetl-test-lock["+lockName+"]-lockuuid["+lockUUID+"]-testuuid["+ uuidComponent+"]";
		
		File testLockFile = new File(directory.getPath()+File.separator+testLockFilename);
		
		String expectedResponseName = "filetl-test-lock-response-active-lock["+lockName+"]-lockuuid["+lockUUID+"]-testuuid["+ uuidComponent+"]";
		File expectedResponseFile = new File(directory.getPath()+File.separator+expectedResponseName);
		FileTLUtil.writeEmptyMessageFile(testLockFile);
		
		WriteTestLockFileReturn ret = new WriteTestLockFileReturn();
		ret.testLockFile = testLockFile;
		
		ret.expectedResponseFile = expectedResponseFile;
		
		return ret;
	}
	
	/** 
	 * Scans a directory for a lock with a UUID. If found, this File is returned. If not, null is returned.
	 */
	private static File findUUIDLockFile(File directory, String lockName) {
		File[] fileList = directory.listFiles();
		File result = null;
		
		String fullLockName = "filetl-lock["+lockName+"]-lockuuid[";
		
		for(File f : fileList) {
			if(f.isDirectory()) continue;
			if(!f.getName().contains("filetl")) continue;
			
			if(f.getName().startsWith(fullLockName)) {
				result = f;
				break;
			}
		}
		return result;
		
	}
	
	/** This function will block until:
	 * o Our test lock times out
	 * o The lock no longer exists (implying it is no longer in use)
	 * Thus, when this function returns, it is safe to assume the lock can now be written.*/
	private static void testExistingLock(File directory, String lockName, File activeLock) {
		
		WriteTestLockFileReturn ret = null;		
		
		while(activeLock != null) {
		
			long startTime = System.currentTimeMillis();			
			boolean cont = true;
			boolean writeNewTestLock = true;
			boolean lockTimedOut = false;
			do {
				
				// If we've been asked to write a new test lock (because the old one got a response, then do it)
				if(writeNewTestLock) {
					writeNewTestLock = false;
					ret = writeTestLockFile(directory, lockName, extractLockUUIDFromFilename(activeLock.getName()));
				}
				
				// Lock no longer exists, so we can write it
				if(!activeLock.exists()) {
					ret.testLockFile.delete();
					cont = false;
					break;
				}
				
				// If we've timed out
				if(System.currentTimeMillis() - startTime > (MAX_WAIT_TIME)) {
					cont = false;
					lockTimedOut = true;
					break;
				}
				
				// We received a response from another process holding the lock, so keep looping
				if(ret.expectedResponseFile.exists()) {
					ret.testLockFile.delete();
					ret.expectedResponseFile.delete();
					startTime = System.currentTimeMillis();
					writeNewTestLock = true;
				}
	
				FileTLUtil.sleep(WAIT_INTERVAL);
				
			} while(cont);

			if(ret.testLockFile.exists()) {
				ret.testLockFile.delete();
			}

			if(ret.expectedResponseFile.exists()) {
				ret.expectedResponseFile.delete();
			}
			
			if(lockTimedOut) {
				activeLock.delete();
				if(activeLock.exists()) {
					throw new RuntimeException("Unable to delete inactive lock.");
				}
			}
						
			activeLock = findUUIDLockFile(directory, lockName);
		}
		
		return;
	}

	public static void acquireLock(File directory, String lockName) {

		String lockFileName = "filetl-lock["+lockName+"]-hold";
		File lockFile = new File(directory.getPath()+File.separator+lockFileName);
		
		String renameToName = "filetl-lock["+lockName+"]-lockuuid["+SoAnUtil.generateUUID().toString()+"]";
		File renameToNameFile = new File(directory.getPath() + File.separator + renameToName);
		
		boolean lockAcquired = false;
		
		while(!lockAcquired) {
			
			boolean uuidLockExists = true;
		
			while(uuidLockExists) {
			
				File uuidLockFile = findUUIDLockFile(directory, lockName);
				
				if(uuidLockFile != null) {
					testExistingLock(directory, lockName, uuidLockFile);
				}
							
				if(!lockFile.exists()) {
					try {
						FileWriter fw = new FileWriter(lockFile);
						fw.write("\n");
						fw.flush();
						fw.close();
					} catch(IOException e) {
						// If we are unable to write, it may be another active process is holding the lock
						// so we don't really sweat it.
					}
				}
				
				if(lockFile.exists()) {
					// Having written the file, we need check if a uuid lock exists
					uuidLockFile = findUUIDLockFile(directory, lockName);
					
					if(uuidLockFile != null) {
						lockFile.delete();
						continue;
					} else {
						uuidLockExists = false;
						break;
					}
					
				} else {
					FileTLUtil.sleep(WAIT_INTERVAL);
				}
			}
			
			// We've asked the inner while loop, therefore we have successfully
			// written our hold lock (or someone else wrote it for us)
			
			lockFile.renameTo(renameToNameFile);
			
			if(renameToNameFile.exists()) {
				// Lock acquired
				lockAcquired = true;
				LockManagerOld.getInstance().addLock(renameToNameFile);
			} else {
				// Missed our lock
				lockAcquired = false;
			}
			
		}
		
	}
	
	public static void releaseLock(File directory, String lockName) {
		File uuidLockFile = LockManagerOld.getInstance().getLock(directory, lockName);
		
		
		
		if(uuidLockFile != null) {
			uuidLockFile.delete();
			LockManagerOld.getInstance().removeLock(uuidLockFile);
		} else {
			System.err.println("uuid not found?");
		}
	}
	
}
