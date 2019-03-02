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

package com.socketanywhere.filefactory.lock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.socketanywhere.filefactory.FileTLUtil;
import com.socketanywhere.filefactory.TimedDeleteManager;
import com.vfile.VFile;

/**
 * File Messages:

 * Testing an existing active to see if it is still active:
 * - filetl-test-lock[lockName]-lockuuid[lockUUID]-testuuid[uuidComponent]
 * 
 * The response from an active lock, indicating that it is in fact still active
 * - filetl-test-lock-response-active-lock[lockName]-lockuuid[lockUUID]-testuuid[uuidComponent]
 *
 */
public class LockManager {

	private static LockManagerThread _lockManagerThread = null;
	private static LockManager _instance = new LockManager();
	private static boolean _lockManagerThreadRunning = false;
	
	private List<LockEntry> _waitingForActiveLocks = new ArrayList<LockEntry>();
	private List<LockEntry> _waitingToRemoveLocks = new ArrayList<LockEntry>();
	private List<LockEntry> _activeLocks = new ArrayList<LockEntry>();
	
	Object _activeLockFlush = new Object();
	
	private List<LockEntry> _readOnlyActiveLocks 
		= Collections.synchronizedList(new ArrayList<LockEntry>());
	
	private static final long LOCK_POLL_INTERVAL = 1000;
	
	private LockManager() {}
	
	public static synchronized LockManager getInstance() {
		if(_lockManagerThread == null) {
			_lockManagerThreadRunning = true;
			_lockManagerThread = _instance.new LockManagerThread();
			_lockManagerThread.setDaemon(true);
			_lockManagerThread.start();
		}
		return _instance;
	}
	
	public void addLock(FileLockOnNonlockingFS lock) {
		synchronized(_activeLockFlush) {
			synchronized(_waitingForActiveLocks) {
				if(!_waitingForActiveLocks.contains(lock)) {
//					System.out.println("adding lock" + lock.getName() + " ("+Thread.currentThread().getId()+")");
					LockEntry le = new LockEntry();
					le._lockUUID = lock.getLockUUID();
					le._lockFile = lock.getLockUUIDFile();
					le._lockName = lock.getName();
					le._lock = lock;
					_waitingForActiveLocks.add(le);
				}
			}
		}
	}
	
	// TODO: LOWER - In light of removing getLock, what else can change?
		
	public void removeLock(FileLockOnNonlockingFS lock) {
		synchronized(_activeLockFlush) { 
			synchronized (_waitingToRemoveLocks) { 
//				System.out.println("removing lock" + lock.getName() + " ("+Thread.currentThread().getId()+")");
				LockEntry le = new LockEntry();
				le._lock = lock;
				if(!_waitingToRemoveLocks.contains(lock)) {
					_waitingToRemoveLocks.add(le);
				}
			}
		}
	}
	
	private class LockEntry  {
		FileLockOnNonlockingFS _lock = null;
		VFile _lockFile = null;
		String _lockUUID = null;
		String _lockName = null;
		
		public boolean equals(Object obj) {
			if(!(obj instanceof LockEntry)) return false;
			
//			return ((LockEntry)obj)._lock.getName().equalsIgnoreCase(this._lock.getName());
			
			return ((LockEntry)obj)._lock.equals(this._lock);
			
		}
	}
	
	private class LockManagerThread extends Thread {

		public LockManagerThread() {
			setName(LockManagerThread.class.getName());
			setDaemon(true);
		}
		
		public void run() {
			final int TIME_TO_WAIT_BEFORE_RESPONSE_DELETE = 60000; 

			while(_lockManagerThreadRunning) {
				
				synchronized (_activeLocks) {
					
					Map<VFile, VFile[]> dirList = new HashMap<VFile, VFile[]>();
					
					for(LockEntry lock : _activeLocks) {
						VFile dir = lock._lockFile.getParentFile();
						
						VFile[] fileList;
						if(dirList.get(dir) == null) {
							fileList = dir.listFiles();
							dirList.put(dir, fileList);
						} else {
							fileList = dirList.get(dir);
						}
						
						String lockTestFormat = "filetl-test-lock["+lock._lockName+"]-lockuuid["+lock._lockUUID+"]-";
						
						for(VFile f : fileList) {

							if(f.getName().startsWith(lockTestFormat)) {
								String testUUID = FileLockOnNonlockingFS.extractTestUUIDFromFilename(f.getName());
								
								String responseName = "filetl-test-lock-response-active-lock["+lock._lockName+"]-lockuuid["+lock._lockUUID+"]-testuuid["+testUUID+"]";
								VFile responseFile = new VFile(dir.getPath()+VFile.separator+responseName);
								if(!responseFile.exists()) {
									FileTLUtil.writeEmptyMessageFile(responseFile);									
									TimedDeleteManager.getInstance().deleteFile(responseFile, TIME_TO_WAIT_BEFORE_RESPONSE_DELETE, false); 
								}
							}
						}
					}
										
				} // end synchronized
				
				synchronized (_activeLocks) {
					
					synchronized(_activeLockFlush) {

						synchronized(_waitingForActiveLocks) {
							// Add any waiting active locks to the active list
							for(Iterator<LockEntry> it = _waitingForActiveLocks.iterator(); it.hasNext();) {
								LockEntry le = it.next();
								
								if(!_activeLocks.contains(le)) {
									_activeLocks.add(le);
								}
								
								it.remove();
							}
						}
			
						synchronized(_waitingToRemoveLocks) {
							// Remove any locks that are waiting to be removed
							for(Iterator<LockEntry> it = _waitingToRemoveLocks.iterator(); it.hasNext();) {
								LockEntry le = it.next();
								boolean removeResult = _activeLocks.remove(le);
								if(removeResult == false) {
									// TODO: LOWER - Log this?
								}
								
								it.remove();
							}
							
						}
								
						synchronized(_readOnlyActiveLocks) {
							_readOnlyActiveLocks.clear();
							_readOnlyActiveLocks.addAll(_activeLocks);

						}
					}
				}
				
				FileTLUtil.sleep(LOCK_POLL_INTERVAL);
				
			}
			
		}
		
	}
	
}
