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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vfile.VFile;

/** We needed a mechanism to ensure that files would be deleted once they were no longer
 * in use; though, some files take longer to free up than they should. 
 * 
 * The solution was to implement this class, which maintains a list of files that
 * should be deleted, and keeps attempting to delete them until they are deleted.
 * 
 * We don't stop trying to delete until it no longer exists. 
 * */
public class QueueManager {
	
	static List<VFile> _waitingFilesToDelete = new ArrayList<VFile>();
	static List<VFile> _filesToDelete = new ArrayList<VFile>();
	
//	static Object _waitingToFlushToMain = new Object();
	
	static boolean _threadRunning = false;
	static QueueManagerThread _qmThread = null;
	
	private static synchronized void initializeIfNeeded() {
		if(_qmThread == null) {
			_threadRunning = true;
			_qmThread = new QueueManagerThread();
//			_qmThread.setDaemon(true);
			
			_qmThread.start();
		}
	}
	
	/** Works with files or directories (directory must be empty, however) */
	public static void queueDeleteFile(VFile f) {
		initializeIfNeeded();

		// We try to delete it immediately, but still add it to the queue even if delete appears to succeed
		f.delete();
		
		synchronized (_waitingFilesToDelete) {
			if(!_waitingFilesToDelete.contains(f)) {
				_waitingFilesToDelete.add(f);
			}
			
		}
		
	}
	
	private static class QueueManagerThread extends Thread {
		private static final long POLL_INTERVAL = 2000;
		
		public QueueManagerThread() {
			setName(QueueManagerThread.class.getName());
			setDaemon(true);
		}
		
		public void run() {
			while(_threadRunning) {
				
				synchronized(_filesToDelete) {
					synchronized(_waitingFilesToDelete) {
						
						for(Iterator<VFile> i = _waitingFilesToDelete.iterator(); i.hasNext();) {
							VFile f = i.next();
							
							if(!_filesToDelete.contains(f)) {
								_filesToDelete.add(f);
							}
							
							i.remove();
						}
					}
					
					for(Iterator<VFile> i = _filesToDelete.iterator(); i.hasNext();) {
						
						VFile f = i.next();
						
						if(f.exists()) {
							f.delete();
						}
						
						if(!f.exists()) {
							i.remove();
						}
					}
					
				}				
				
				if(_threadRunning) {
					FileTLUtil.sleep(POLL_INTERVAL);
				}
			}
			
		}
	}
}
