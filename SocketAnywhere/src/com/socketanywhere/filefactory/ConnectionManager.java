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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.socketanywhere.net.SoAnUtil;
import com.vfile.VFile;

/**
 * 
 * Singleton.
 * 
 * This class serves two purposes:
 * 1. To response to queries from others about whether we are participating in an
 * 	  active connection.
 * 2. To issue queries for stale data in the file directory, for the purpose of clearing
 *    it.
 *    
 * File Messages:
 * 
 * Test Active Connection:
 * filetl-test-active-connection-connuuid[uuid]-testuuid[testuuid]
 * 
 * Response (if active):
 * filetl-test-active-connection-response-connuuid[uuid]-testuuid[testuuid]
 * 
 */
public class ConnectionManager {
	
	private static ConnectionManager _instance = new ConnectionManager();
	
	List<ActiveConnection> _activeConnections = new ArrayList<ActiveConnection>();

	ActiveConnectionListenThread _acListenThread = null;
	
	/** List of directories to search when looking for active connection tests or stale files */
	List<VFile> _directoriesToSearch = new ArrayList<VFile>();

	private ConnectionManager() { }
	
	public static final ConnectionManager getInstance() {
		
		// Start the listen thread if not already started
		synchronized(_instance) {
			if(_instance._acListenThread == null) {
				_instance._acListenThread = new ActiveConnectionListenThread(_instance);
				_instance._acListenThread.start();
			}
		}
		return _instance;
	}
	
	public void addActiveConnection(VFile directory, String uuid) {
		synchronized (_activeConnections) {
			ActiveConnection ac = new ActiveConnection();
			ac._directory = directory;
			ac._uuid = uuid;
			_activeConnections.add(ac);
		}
		
		synchronized(_directoriesToSearch) {
			if(!_directoriesToSearch.contains(directory)) {
				_directoriesToSearch.add(directory);
			}
		}
	}
	
	public void removeActiveConnection(VFile directory, String uuid) {
		synchronized (_activeConnections) {
			
			for(Iterator<ActiveConnection> it = _activeConnections.iterator(); it.hasNext();)  {
				ActiveConnection ac = it.next();
				if(directory.equals(ac._directory) && uuid.equals(ac._uuid)) {
					it.remove();
				}
			}
			
		}		
	}
	
}

class ActiveConnection {
	VFile _directory;
	String _uuid; 
}

class ActiveConnectionListenThread extends Thread {
	ConnectionManager _cm;
	boolean _threadRunning = false;
	
	/** Whether or not a file is already in timeFirstSeen (used by stale file scan)*/
	Map<VFile, Boolean> _fileSeen = new HashMap<VFile, Boolean>();
	
	/** An sorted map, containing the list of files by when they were first seen, in ascending order (used by stale file scan) */
	Map<Long, VFile> _timeFirstSeen = new TreeMap<Long, VFile>();
	
	private static final long TIME_TO_WAIT_TO_DELETE_OUR_TEST_RESPONSE = 10 * 60 * 1000;
	private static final long POLL_TIME = 30 * 1000;
	
	private static final long TIME_TO_WAIT_BEFORE_FILE_IS_STALE = (60 * 60 * 2) * 1000; /** 2 hours */

	
	public ActiveConnectionListenThread(ConnectionManager cm) {
		_cm = cm;
		
		setName(ActiveConnectionListenThread.class.getName());
		setDaemon(true);
	}
	
	@Override
	public void run() {
		_threadRunning = true;
		
		while(_threadRunning) {
			List<ActiveConnection> l = new ArrayList<ActiveConnection>();
			synchronized(_cm._activeConnections) {
				l.addAll(_cm._activeConnections);
			}

			// Rather than performing the same directory list over and over again,
			// we cache our list and reuse it when possible
			Map<VFile /** ac._directory */, VFile[] /* files in the dir */> dirCache = new HashMap<VFile, VFile[]>();
			
			// Look for other processes testing our active connections
			scanForActiveConnTests(l, dirCache);
			
			// Look for stale files, and test their active connections
			scanForStaleFiles(dirCache);
			
			FileTLUtil.sleep(POLL_TIME);
			
		}
	}
	
	/** Scan for, and respond to, active connection tests for connections we ourselves
	 * are connected on. */
	private void scanForActiveConnTests(List<ActiveConnection> l, Map<VFile , VFile[] > dirCache) {

		for(ActiveConnection ac : l) {
			String testName = "filetl-test-active-connection-connuuid["+ac._uuid+"]-";
			
			VFile[] dirList = dirCache.get(ac._directory);
			if(dirList == null) {
				dirList = ac._directory.listFiles();
				dirCache.put(ac._directory, dirList);
			}
			
			for(VFile f : dirList) {
				if(f.getName().startsWith(testName)) {
					
					f.delete();
					
					String connuuid = FileTLUtil.extractField("connuuid", f.getName());
					String testuuid = FileTLUtil.extractField("testuuid", f.getName());

					VFile response = new VFile(ac._directory.getPath()
						+VFile.separator+"filetl-test-active-connection-response-connuuid["+connuuid+"]-testuuid["+testuuid+"]");

					// If it already exists, don't write it again
					if(!response.exists()) {
						FileTLUtil.writeEmptyMessageFile(response);
						
						TimedDeleteManager.getInstance().deleteFile(response, TIME_TO_WAIT_TO_DELETE_OUR_TEST_RESPONSE, false);
					}
					
				}
			}
			
		}
		
	}
	
	private void scanForStaleFiles(Map<VFile, VFile[]> dirCache) {
		
		synchronized(_cm._directoriesToSearch) {  
			for(VFile dir : _cm._directoriesToSearch) {  
				
				if(!dir.exists()) continue;
	
				VFile[] dirList = dirCache.get(dir);
				if(dirList == null) {
					dirList = dir.listFiles();
					dirCache.put(dir, dirList);
				}
				
				if(dirList == null || dirList.length == 0) continue;
				
				long time = System.currentTimeMillis();
				
				// Add files to our map of possible stale files
				for(VFile f : dirList) {
	
					// Skip files we've already seen				
					Boolean c = _fileSeen.get(f); 
					if(c != null && c == true) continue;
					
					String fn = f.getName();
					if(fn.startsWith("filetl-test-active-connection-connuuid") || 
							fn.startsWith("filetl-test-active-connection-response-connuuid") || 
							fn.startsWith("filetl-packet-source")) {
						
						_timeFirstSeen.put(time, f);
						_fileSeen.put(f, true);
					}
									
				}
			
			}
		}
		
		// Connection UUIDs to test for whether they are active
		Map<String, List<VFile>> connUUIDToTest = new HashMap<String, List<VFile>>();
		
		// Iterate through the list of files (sorted by seen time), looking for those
		// that are older than 2 hours.
		long currTime = System.currentTimeMillis(); 
		for(Iterator<Entry<Long, VFile>> it = _timeFirstSeen.entrySet().iterator(); it.hasNext();) {
			
			Entry<Long, VFile> e = (Entry<Long, VFile>)it.next();
			
			/** fileDeleteTime is x minutes after the file was first seen by this process*/
			long fileDeleteTime = e.getKey() + TIME_TO_WAIT_BEFORE_FILE_IS_STALE;
			
			if(currTime > fileDeleteTime) {
				// Time to delete
				String connUUID = FileTLUtil.extractField("connuuid", e.getValue().getName());
				
				// Remove from the maps
				it.remove();
				_fileSeen.remove(e.getValue());

				// Add file to connUUID to test list
				List<VFile> lf = connUUIDToTest.get(connUUID);
				if(lf == null) {
					lf = new ArrayList<VFile>();
					connUUIDToTest.put(connUUID, lf);
				}
				lf.add(e.getValue());
				
			} else {
				
				// The iterator is sorted, so if one file is not ready to be delete, then
				// all files after that will not be ready, so we break
				break;
			}
			
		}
		
		if(connUUIDToTest.size() > 0) {
		
			// Pass the connection uuids to test to the test thread
			ActiveConnectionTestThread t = new ActiveConnectionTestThread(connUUIDToTest);
			t.start();
		
		}
				
	}
}

class ActiveConnectionTestThread extends Thread {
	Map<String, List<VFile>> _connUUIDs;
	private static final long POLL_TIME = 30 * 1000;
	private static final long TIME_TO_WAIT_FOR_RESPONSE = 5 * 60 * 1000;
	
	public ActiveConnectionTestThread(Map<String, List<VFile>> map) {
		setDaemon(true);
		setName(ActiveConnectionTestThread.class.getName());
		_connUUIDs = map;
		
	}

	@Override
	public void run() {
		
		List<ActiveConnectionTestEntry> tests = new ArrayList<ActiveConnectionTestEntry>();
		
		for(String connUUID : _connUUIDs.keySet()) {
			ActiveConnectionTestEntry a = new ActiveConnectionTestEntry();
			a._connUUID = connUUID;
			a._files = _connUUIDs.get(connUUID);
			
			VFile parentDir = a._files.get(0).getParentFile(); 
			
			a._responseTestUUID = SoAnUtil.generateUUID().toString();
			a._testFile = new VFile(parentDir.getPath() + VFile.separator 
					+  "filetl-test-active-connection-connuuid" +
					"["+a._connUUID+"]-testuuid["+a._responseTestUUID+"]");
			
			a._responseFile = new VFile(parentDir.getPath() + VFile.separator 
					+  "filetl-test-active-connection-response-connuuid" +
							"["+a._connUUID+"]-testuuid["+a._responseTestUUID+"]");
			
			tests.add(a);
		}
		
		// Write the test files
		for(ActiveConnectionTestEntry a : tests) {
			FileTLUtil.writeEmptyMessageFile(a._testFile);
		}
		
		long startTime = System.currentTimeMillis();
		int responsesExpected = tests.size();
		boolean contLoop = true;
		do {
			
			// Look for the expected response file for each connection
			for(ActiveConnectionTestEntry a : tests) {
				if(!a._responseSeen) {
					if(a._responseFile.exists()) {
						a._responseSeen = true;
						responsesExpected--;
					}
				}
			}
			
			contLoop = responsesExpected > 0 && System.currentTimeMillis()-startTime < TIME_TO_WAIT_FOR_RESPONSE; 
			
			if(contLoop) {
				FileTLUtil.sleep(POLL_TIME);
			}
			
		} while(contLoop); 
		
		// For the tests we didn't see a response for, we delete the files
		for(ActiveConnectionTestEntry a : tests) {
			if(!a._responseSeen) {
				for(VFile f : a._files) {
					if(f.exists() && !f.delete()) {
						f.deleteOnExit();
					}
				}
			}
		}
		
	}
	
	class ActiveConnectionTestEntry {
		String _connUUID;
		boolean _responseSeen = false;
		VFile _responseFile;
		VFile _testFile;
		String _responseTestUUID;
		List<VFile> _files;
	}
	
}