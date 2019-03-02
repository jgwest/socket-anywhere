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

package com.vfile.vftp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Establishing file connects takes time, as does retrieving file data; this class caches some of that data for each client. 
 * Default cache flush time is fairly quick, to reduce/eliminate old data. */
public class FileCache {
	
	// TODO: LOWER - ARCHITECTURE - One thing this file cache doesn't do is cache the non existence of files... e.g. if I list a dir and I don't see a file, if I test for the existence of that file it is not cache. 

	public static final int FIELD_FTPFILE = 1;

	static Map<VFTPClient, FileCache> _clientToCacheMap = Collections.synchronizedMap(new HashMap<VFTPClient, FileCache>());
	
	VFTPClient _client = null;
	
	Map<String, Map<Integer, AttributeValue>> _fileToAttributesMap = new HashMap<String, Map<Integer, AttributeValue>>();
	
	public static final long DEFAULT_CACHE_EXPIRE_TIME = 5000;
	
	long _cacheExpireTime = DEFAULT_CACHE_EXPIRE_TIME;

	private FileCache() {
		
	}
	
	public static void setCacheExpireTime(VFTPClient client, long cacheExpirationTimeInMsecs) {
		FileCache fc = getFileCache(client);
		fc._cacheExpireTime = cacheExpirationTimeInMsecs;
	}
	
	public synchronized static FileCache getFileCache(VFTPClient client) {
		FileCache result = _clientToCacheMap.get(client);
		
		if(result == null) {
			result = new FileCache();
			_clientToCacheMap.put(client, result);
		}
		
		return result;
	}
	
	public void putAttribute(VFTPFile file, int type, Object value) {
		synchronized(_fileToAttributesMap) {
			Map<Integer, AttributeValue> map = _fileToAttributesMap.get(file.getPath());
			
			if(map == null) {
				map = new HashMap<Integer, AttributeValue>();
				_fileToAttributesMap.put(file.getPath(), map);
			}
			
			synchronized(map) {
				AttributeValue v = new AttributeValue();
				v._value = value;
				v._timestamp = System.currentTimeMillis();
				
				map.put(type, v);
			}
		}
	}
	
	public Object getAttribute(VFTPFile file, int type) {
		synchronized(_fileToAttributesMap) {
			Map<Integer, AttributeValue> map = _fileToAttributesMap.get(file.getPath());
			
			if(map == null) {
				return null;
			}
			
			synchronized(map) {
				AttributeValue v = map.get(type);
				
				if(v == null) {
					return null;
				}
				
				if(System.currentTimeMillis() - v._timestamp > _cacheExpireTime) {
					return null;
				}
				
				return v._value;
			}
			
		}
	}
}

class AttributeValue {
	Object _value = null;
	long _timestamp = -1;
}
