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
public class NewMkdirTest {
	
	static int x = 0;
	static Object lock = new Object();
	
	static int setNumber = 0;
	static int atomicAct = 0;
	static int numTrues = 0;
	static Object numTruesLock = new Object();
	
	static File directory = new File("C:\\temp\\busy");

	public static void main(String[] args) {
		
		for(int x = 0; x < 5; x++) {
			new MkingThread().start();
		}
		
		new SettingThread().start();
		
//		while(true) {
//			
//			boolean spawnThread = false;
//			
//			synchronized(lock) {
//				if(x < 5) {
//					spawnThread = true;
//				}
//			}
//			
//			if(spawnThread) {
//			
//				new MkingThread().start();
//				
//			}
//			
//		}
		
	}
	
	static class SettingThread extends Thread {
		
		@Override
		public void run() {
			
			while(true) {
				
				atomicAct = 0;
				numTrues = 0;
				setNumber = (int)(Math.random()* 1000);
				
				atomicAct = 1;
				
				try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); }
				
				(new File(directory + File.separator + setNumber)).delete();
				
				if(numTrues > 1) {
					System.out.println("Failed.  ("+numTrues+")");
					System.exit(0);
				} else {
					System.out.println("Passed ("+numTrues+")");
				}
				
			}
			
		}
	}
	
	static class MkingThread extends Thread {
		
		@Override
		public void run() {
			while(true) {
				
				if(atomicAct == 1) {
					File f = new File(directory + File.separator + setNumber);
					boolean r = f.mkdir();

					if(r) { 
						synchronized(numTruesLock) {
							numTrues++;
						}
					}
					
				}
				
			}

		}
	}
}
