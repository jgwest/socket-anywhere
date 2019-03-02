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

package com.socketanywhere.main;

import java.io.File;


public class TestMain {

	public static void main(String[] args) {
		File f = new File("/tmp/m2");
		f.delete();
		
		for(int x = 0; x < 20; x++) {
			m();
		}
	}

	public static void m() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					
				}
				File f = new File("/tmp/m	");
				System.out.println(f.mkdir());
			}
		}.start();
	}
	
}
