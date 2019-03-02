package com.socketanywhere.nonbreakingnew;

import java.util.ArrayList;
import java.util.HashMap;

public class Mapper {
	
	private static final boolean ENABLED = false;
	

	private final static Mapper _instance = new Mapper();
	
	private Mapper() {
	}
	
	public static Mapper getInstance() {
		return _instance;
	}
	
	// ---------------------------------------------
	
	private final HashMap<Object, Object> map = new HashMap<Object, Object>();
	
	public Object get(Object key) {
		if(!ENABLED) { return null; }
		
		if(key == null) {throw new IllegalArgumentException(); }
		
		synchronized(map) {
			return map.get(key);
		}
	}

	public void put(Object key, Object value) {
		if(!ENABLED) { return; }
		
		if(key == null) {throw new IllegalArgumentException(); }
		synchronized(map) {
			map.put(key, value);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void putIntoList(Object key, Object thing) {
		if(!ENABLED) { return; }
		
		if(key == null) {throw new IllegalArgumentException(); }
	
		synchronized(map) {
			ArrayList<Object> val = (ArrayList<Object>)map.get(key);
			if(val == null) {
				val = new ArrayList<Object>();
				map.put(key, val);
			}
			val.add(thing);
		}
	}
	
	public HashMap<Object, Object> unsafeGetMap() {
		if(!ENABLED) { return null; }
		return map;
	}
	
}
