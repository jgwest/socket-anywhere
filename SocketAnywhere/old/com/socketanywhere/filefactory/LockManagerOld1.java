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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LockManagerOld1 {

	private static LockManagerThread _lockManagerThread = null;
	private static LockManagerOld1 _instance = new LockManagerOld1();
	private static boolean _lockManagerThreadRunning = false;
	
	private List<LockEntry> _waitingForActiveLocks = new ArrayList<LockEntry>();
	private List<LockEntry> _waitingToRemoveLocks = new ArrayList<LockEntry>();
	private List<LockEntry> _activeLocks = new ArrayList<LockEntry>();
	
	Object _activeLockFlush = new Object();
	
	private List<LockEntry> _readOnlyActiveLocks 
		= Collections.synchronizedList(new ArrayList<LockEntry>());
	
	private static final long LOCK_POLL_INTERVAL = 1000;
	
	private LockManagerOld1() {}
	
	public static synchronized LockManagerOld1 getInstance() {
		if(_lockManagerThread == null) {
			_lockManagerThreadRunning = true;
			_lockManagerThread = _instance.new LockManagerThread();
			_lockManagerThread.setDaemon(true);
			_lockManagerThread.start();
		}
		return _instance;
	}
	
	public void addLock(LockOld0 lock) {
		synchronized(_activeLockFlush) {
			synchronized(_waitingForActiveLocks) {
				if(!_waitingForActiveLocks.contains(lock)) {
//					System.out.println("adding lock" + lock.getName() + " ("+Thread.currentThread().getId()+")");
					LockEntry le = new LockEntry();
					le._lock = lock;
					_waitingForActiveLocks.add(le);
				}
			}
		}
	}
	
	/**
	 * Get active lock. 
	 * @param directory
	 * @param lockName
	 * @return
	 */
//	public File getLock(File directory, String lockName) {
//		
//		LockEntry matchingEntry = null;
//		
//		synchronized(_activeLockFlush) {
//			// This combination of synchronized keywords ensures that the following is always true: 
//			// _waitingForActiveLocks + _readOnlyActiveLocks == _activeLocks
//
//			synchronized(_waitingToRemoveLocks) {
//				
//				// Scan through the read only lock list looking for a matching entry
//				// (but cannot be one that is queued to be removed)
//				synchronized(_readOnlyActiveLocks) {
//						
//					for(LockEntry le : _readOnlyActiveLocks) {
//						if(le._lockName.equalsIgnoreCase(lockName) &&
//								!_waitingToRemoveLocks.contains(le)) {
//							matchingEntry = le;
//							System.out.println("matched from roal");
//						}
//					}
//				}
//				
//				// Scan through the waiting for active lock list looking for a matching entry
//				// (but cannot be one that is queued to be removed)	
//				synchronized(_waitingForActiveLocks) {
//					if(matchingEntry == null) {
//						for(LockEntry le : _waitingForActiveLocks) {
//							if(le._lockName.equalsIgnoreCase(lockName) &&
//									!_waitingToRemoveLocks.contains(le)) {
//								matchingEntry = le;
//								System.out.println("matched from wfal");
//							}
//						}	
//					}	
//				}
//				
//			}
//		}
//		
//		return matchingEntry == null ? null : matchingEntry._lock;
//		
//	}
	
	public void removeLock(LockOld0 lock) {
		synchronized(_activeLockFlush) { 
			synchronized (_waitingToRemoveLocks) { 
//				System.out.println("removing lock" + lock.getName() + " ("+Thread.currentThread().getId()+")");
				LockEntry le = new LockEntry();
				le._lock = lock;
				if(!_waitingToRemoveLocks.contains(le)) {
					_waitingToRemoveLocks.add(le);
				}
			}
		}
	}
	
	private class LockEntry  {
		LockOld0 _lock;

		public boolean equals(Object obj) {
			if(!(obj instanceof LockEntry)) return false;
			
//			return ((LockEntry)obj)._lock.getName().equalsIgnoreCase(this._lock.getName());
			
			return ((LockEntry)obj)._lock.equals(this._lock);
			
		}
	}
	
	private class LockManagerThread extends Thread {

		public void run() {

			while(_lockManagerThreadRunning) {
				
				synchronized (_activeLocks) {
					
					Map<File, File[]> dirList = new HashMap<File, File[]>();
					
					for(LockEntry lock : _activeLocks) {
						File dir = lock._lock.getLockDirectoryFile().getParentFile();
						
						File[] fileList;
						if(dirList.get(dir) == null) {
							fileList = dir.listFiles(FileTLUtil.FILE_TL_FILE_FILTER);
							dirList.put(dir, fileList);
						} else {
							fileList = dirList.get(dir);
						}
						
						String lockTestFormat = "filetl-test-lock["+lock._lock.getName()+"]-";
						
						for(File f : fileList) {

							if(f.getName().startsWith(lockTestFormat)) {
								String testUUID = LockOld0.extractTestUUIDFromFilename(f.getName());
								
								String responseName = "filetl-test-lock-response-active-lock["+lock._lock.getName()+"]-testuuid["+testUUID+"]";
								File responseFile = new File(dir.getPath()+File.separator+responseName);
								if(!responseFile.exists()) {
									FileTLUtil.writeEmptyMessageFile(responseFile);
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
									System.out.println("BWAHHHHHHHHHHHHHHHHH");
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
