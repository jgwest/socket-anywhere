/*
	Copyright 2012, 2013 Jonathan West

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

package com.vfile.s3io;

public class S3Log {

	public static final boolean DEBUG = false;
	
	private static final Object _outLock = new Object();
	
	public static void debug(String out) {
		if(DEBUG) {
			synchronized(_outLock) {
				System.out.println("> "+out);
			}
		}
	}
	
	public static void err(String str) {
		synchronized(_outLock) {
			System.err.println("&> " + str);
		}
		
	}
}
