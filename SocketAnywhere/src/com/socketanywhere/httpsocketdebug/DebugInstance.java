package com.socketanywhere.httpsocketdebug;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DebugInstance {

	private static final DebugInstance _instance = new DebugInstance();

	Object _lock = new Object();
	
	List<MonitoredObject> _objects = new ArrayList<MonitoredObject>();
	
	int _nextObjId = 0;
	
	private DebugInstance() {

		Thread t = new Thread() {
			public void run() {
				int port = 8080+(int)(Math.random()*10);
				DebugServer ds = new DebugServer(port);
				ServerRunner.executeInstance(ds);
			};
		};
		
		t.start();
		
	
	}
	
	public static DebugInstance getInstance() {
		return _instance;
	}
	

	public void fastAddObject(Object o) {
		synchronized(_lock) {
			MonitoredObject mo = new MonitoredObject();
			mo.id = _nextObjId++;
			mo.object = new WeakReference<Object>(o);
			_objects.add(mo);
		}
		
	}
	
	public void addObject(Object o) {
		synchronized(_lock) {
			
			for(Iterator<MonitoredObject> it = _objects.iterator(); it.hasNext();) {
				MonitoredObject mo = it.next();
				
				if(mo.object.get() == null) {
					it.remove();					
				} else {
					Object o1 = mo.object.get();
					
					if(o1 == o) {
						return;
					}
					
				}
				
			}
			
			fastAddObject(o);
		}
	}
	
	public List<MonitoredObject> getMonitoredObjects() {
		System.gc();
		
		List<MonitoredObject> result = new ArrayList<MonitoredObject>();
		
		synchronized(_lock) {
			for(Iterator<MonitoredObject> it = _objects.iterator(); it.hasNext();) {
				MonitoredObject mo = it.next();
				
				if(mo.object.get() == null) {
					it.remove();
					
				} else {
					result.add(mo);
				}
				
			}
			
		}
		
		return result;
		
	}

	public int getIdOfObject(Object o) {
		synchronized(_lock) {
			for(Iterator<MonitoredObject> it = _objects.iterator(); it.hasNext();) {
				MonitoredObject mo = it.next();
		
				if(mo.object.get() == null) {
					it.remove();
					
				} else {
					if(mo.object.get() == o) {
						return mo.id;
					}
					
				}
				
				
			}
		}
		
		return -1;
	}
}