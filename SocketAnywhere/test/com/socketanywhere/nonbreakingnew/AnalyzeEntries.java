package com.socketanywhere.nonbreakingnew;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.socketanywhere.nonbreakingnew.ConnectionBrain.ThreadState;

public class AnalyzeEntries {

	public static void analyzeConnections() {
		List<ConnectionBrain>  l = (List)Mapper.getInstance().unsafeGetMap().get("CB");
		
		Map<Entry, Boolean> entriesMap = new HashMap<Entry, Boolean>();
		
			for(ConnectionBrain cb : l) {
			ThreadState st = cb.getInterface().getThreadStateAndStop();
			
			for(Entry e : st.nbSocketToEntryMapInternal.values()) {
				entriesMap.put(e, true);
			}
			
			for(Entry e : st.uuidMapInternal.values()) {
				entriesMap.put(e, true);
			}
		}
			
		List<Entry> entries = new ArrayList<Entry>();
		entries.addAll(entriesMap.keySet());

		for(Entry e : entries) {
			System.out.println(e.getTriplet() + " " + e.getState() + " sent-data-packets: "+  e._sentDataPackets.size());
		}
		
	}

}
