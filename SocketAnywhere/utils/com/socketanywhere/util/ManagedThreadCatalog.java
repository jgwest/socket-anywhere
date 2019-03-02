package com.socketanywhere.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import com.socketanywhere.nonbreakingnew.NBLog;

public final class ManagedThreadCatalog {

	private static final ManagedThreadCatalog _instance = new ManagedThreadCatalog();
	
	private ManagedThreadCatalog() {
		DebugManagedThread dmt = new DebugManagedThread();
		dmt.start();
	}
	
	
	public static ManagedThreadCatalog getInstance() {
		return _instance;
	}
	
	// ------
	
	private final Object _lock = new Object();
	
	private final List<ManagedThread> _threadList = new ArrayList<ManagedThread>();
	
	
	public void addManagedThread(ManagedThread mt) {
		synchronized(_lock) {
			_threadList.add(mt);
		}
		
	}
	
	
	public void removeManagedThread(ManagedThread mt) {
		boolean found = false;

		synchronized(_lock) {
			
			for(Iterator<ManagedThread> it = _threadList.iterator(); it.hasNext();) {
				if(mt == it.next()) {
					it.remove();
					found = true;
					break;
				}
			}
		}
		
		if(!found) {
			NBLog.error("Unable to find managed thread while attempting to remove: "+mt.getClass().getName());
		}
		
	}

	public void printCurrThreads() {
 		List<ManagedThread> localCopy = new ArrayList<ManagedThread>();
 		synchronized(_lock) {
 			localCopy.addAll(_threadList);
 		}
 		
 		Map<String, Integer> threadStats = new HashMap<String, Integer>();
 		
 		for(ManagedThread mt : localCopy) {
 			Integer i = threadStats.get(mt.getClass().getName());
 			if(i == null) {
 				i = 0;
 			}
 			i++;
 			threadStats.put(mt.getClass().getName(), i);
 		}
 		
 		String uuid = UUID.randomUUID().toString();
 		
 		NBLog.debug("["+uuid+"] Curr Threads:", NBLog.INTERESTING);
 		for(Entry<String, Integer> e : threadStats.entrySet()) {
 			
 			NBLog.debug("["+uuid+"] " +e.getKey()+" "+e.getValue(), NBLog.INTERESTING);
 			
 		}
	}
	
	
	private class DebugManagedThread extends Thread {
		
		public DebugManagedThread() {
			super(DebugManagedThread.class.getName());
			setDaemon(false);
		}
		
		@Override
		public void run() {
		 	while(true) {
		 		try {
					Thread.sleep(5 * 60 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

		 		printCurrThreads();
		 	
		 	}
			
			
		}
	}
}

