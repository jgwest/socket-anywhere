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

package com.socketanywhere.filefactory.atomic;

import java.util.Random;

import com.socketanywhere.filefactory.FileTLUtil;
import com.socketanywhere.filefactory.lock.IFileLock;
import com.socketanywhere.filefactory.lock.FileLockOnNonlockingFS;
import com.vfile.VFile;

/** Test to determine if the FileLock class is in fact working (eg is atomic), for a given file system. */
public class AtomicTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		renameTest(args);
	}
	
	public static void testM1M2() {
		for(int y = 0; y < 10; y++) {
			for(int x = 0; x < 10; x++) {
				m1();
				m2();
			}
			FileTLUtil.sleep(5 * 1000);
		}
	}
	
	public static void renameTest(String[] args) {
		System.out.println("Start.");
		VFile a = new VFile("c:\\temp\\filea");
		VFile b = new VFile("c:\\temp\\fileb");
		a.delete();
		b.delete();
		
		FileTLUtil.writeEmptyMessageFile(a);
		
		renameTestStart();
	}
	
	public static void renameTestStart() { 
		
//		while(true) {
			for( int x = 0; x < 20; x++) {
				Thread t = new Thread() {
					public void run() {
						renameTestThread();
					}
				};
				t.start();
			}
//			FileTLUtil.sleep(2000);
//		}

	}
	
	public static void renameTestThread() {
		VFile a = new VFile("c:\\temp\\filea");
		VFile b = new VFile("c:\\temp\\fileb");
		
		IFileLock l = new FileLockOnNonlockingFS(new VFile("c:\\temp\\lock"), "jgwlock");
		
		l.acquireLock();
		sout("lock acquired");
		boolean result = a.renameTo(b);
		if(!result) {
			System.out.println("Test fails.");
			System.exit(0);
		}
		
		FileTLUtil.sleep(0);

		result = b.renameTo(a);
		if(!result) {
			System.out.println("Test fails.");
			System.exit(0);
		}
		l.releaseLock();
		sout("lock released");
	}
	
	// --------------------------------------------------------------
	
	public static void pulse() {
		Thread t = new Thread() {
			public void run() {
				Random r = new Random();
				
				FileLockOnNonlockingFS l = new FileLockOnNonlockingFS(new VFile("c:\\temp\\lock"), "jgwlock");
				while(true) {
					
					FileTLUtil.sleep(r.nextInt(10000));
					l.acquireLock();
					FileTLUtil.sleep(r.nextInt(10000));
					l.releaseLock();
				}
			}
		};
		t.start();
	}

	
	// --------------------------------------------------------------
	
	public static void m1() {
		Thread t = new Thread() {
			public void run() {
				FileLockOnNonlockingFS l = new FileLockOnNonlockingFS(new VFile("c:\\temp\\lock"), "jgwlock");
				sout("t1 starts");
				l.acquireLock();
				sout("t1 lock acquired ");
				FileTLUtil.sleep(50);
				l.releaseLock();
				sout("t1 lock released");
				sout("t1 ends");
			}
		};
		
		t.start();
	}
	
	public static void m2() {
		Thread t = new Thread() {
			public void run() {
				FileTLUtil.sleep(50);
				sout("t2 starts " );
				
				FileLockOnNonlockingFS l = new FileLockOnNonlockingFS(new VFile("c:\\temp\\lock"), "jgwlock");
				
				l.acquireLock();
				sout("t2 lock acquired ");
				
				l.releaseLock();
				sout("t2 lock released");
				
				sout("t2 ends");
			}
		};
		
		t.start();
	}
	
	private static void sout(String str) {
		System.out.println(str +" ("+Thread.currentThread().getId()+")");
	}

}
