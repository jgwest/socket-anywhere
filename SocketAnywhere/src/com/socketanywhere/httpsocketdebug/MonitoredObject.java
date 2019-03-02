package com.socketanywhere.httpsocketdebug;

import java.lang.ref.WeakReference;

public class MonitoredObject {
	WeakReference<Object> object;
	int id;
	
	@Override
	public int hashCode() {
		if(object.get() != null) {
			return object.get().hashCode();
		} else {
			return 0;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		MonitoredObject mo = (MonitoredObject)o;
		
		if(mo == this) { return true; }
		
		if(mo.object == object) { return true; }
		
		if(mo.object.get() == object.get()) { return true; }
		
		if(mo.object.get() == null || object.get() ==  null) {
			return false;
		}
		
		return mo.object.get().equals(object.get());
		 
	}
	
}