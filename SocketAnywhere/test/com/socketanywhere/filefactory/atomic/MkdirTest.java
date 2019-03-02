/*
	Copyright 2012, 2019 Jonathan West

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

package com.socketanywhere.filefactory.atomic;

import java.io.File;

/** Simple test to determine if mkdir(...) operation is atomic on a given platform. */
public class MkdirTest {
	
	static int x = 0;
	static Object lock = new Object();

	public static void main(String[] args) {
		
		while(true) {
			
			boolean spawnThread = false;
			
			synchronized(lock) {
				if(x < 10) {
					spawnThread = true;
				}
			}
			
			if(spawnThread) {
			
				new Thread() {
					@Override
					public void run() {
						runMkdir();
					}
				}.start();
				
			}
			
		}
		
	}
	
	public static void runMkdir() {
		
		synchronized (lock) {
			x++;
		}
		
		File f = getFileName(new File("C:\\temp\\busy"));
		
		int number = Integer.parseInt(f.getName())+1;
		
//		if(f != null) {
			File newFile = new File(f.getParent()+File.separator+number);
			
			boolean r = newFile.mkdir();
			
			System.out.println(f.getName() + " to " + newFile.getName() + " ("+r+")");
//		}
			
		synchronized (lock) {
			x--;
		}
		
		f.delete();

	}
	
	public static File getFileName(File directory) {
		File[] files = directory.listFiles();
		return files[0];
		
//		if(files.length == 0) return null;
//		else  return files[0];
		
//		for(File f : files) {
//			return f;
//		}
//		
//		
//		return null;
	}
}
