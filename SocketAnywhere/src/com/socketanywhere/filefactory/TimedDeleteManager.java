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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.vfile.VFile;


/** Singleton */
public class TimedDeleteManager {

	private static TimedDeleteManager _instance = new TimedDeleteManager();
	
	private TimedDeleteManager() {
	}
	
	Map<VFile /** File to del */, TDMMapEntry /** When to delete */> _fileMap = new HashMap<VFile, TDMMapEntry>();
	TimeDeleteManagerThread _delThread = null;
	
	public static TimedDeleteManager getInstance() {
		
		// Start the delete thread
		synchronized(_instance) {
			if(_instance._delThread == null) {
				_instance._delThread = new TimeDeleteManagerThread();
				_instance._delThread.start();
			}
		}
		
		return _instance;
	}
	
	public void deleteFile(VFile f, long timeToWait, boolean isCrucial) {
		synchronized(this) {
			long currTime = System.currentTimeMillis();
			
			if(!_fileMap.containsKey(f)) {
				TDMMapEntry e = new TDMMapEntry();
				e._isCrucial = isCrucial;
				e._whenToDelete = timeToWait+currTime;
				_fileMap.put(f, e);	
			}			
		}
	}


	
}

class TimeDeleteManagerThread extends Thread {
	boolean _threadRunning = true;
	final static long SLEEP_TIME = 1000;
	
	public TimeDeleteManagerThread() {
		setName(TimeDeleteManagerThread.class.getName());
		setDaemon(true);
	}
	
	@Override
	public void run() {
		
		while(_threadRunning) {
			
			synchronized(TimedDeleteManager.getInstance()) {
				
				long currTime = System.currentTimeMillis();
				
				for(Iterator<Entry<VFile, TDMMapEntry>> it = TimedDeleteManager.getInstance()._fileMap.entrySet().iterator(); it.hasNext();) {
					Entry<VFile, TDMMapEntry> e = it.next();
					
					if(currTime > e.getValue()._whenToDelete) {
						if(e.getKey().exists()) {
							if(e.getValue()._isCrucial) {
								FileTLUtil.deleteCrucialFile(e.getKey());
							} else {
								e.getKey().delete();
								e.getKey().deleteOnExit();
							}
						}
						it.remove();
					}

				}						
				
			}
			
			FileTLUtil.sleep(SLEEP_TIME);
			
		}
		
	}
}

class TDMMapEntry {
	Long _whenToDelete;
	boolean _isCrucial;
}