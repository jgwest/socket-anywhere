/*
	Copyright 2013 Jonathan West

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
package com.socketanywhere.nonbreakingnew;

public class NBOptions {
	
	/** Optional Values that are used by the recovery thread that affect how quickly the recovery thread reconnects */
	int _recoveryThreadTimeToWaitForJoinSuccess = -1; // Defaults to using recovery thread's default
	int _recoveryThreadTimeToWaitBtwnFailures = -1;	// Defaults to using recovery thread's default
	
	/** Optional value that specifies how large to allow the data received buffer(s) to grow before blocking on read(), to prevent
	 * them from growing larger. Defaults to infinite. */	
	int _maxDataReceivedBuffer = -1; // Defaults to infinite.

	public NBOptions() {
	}

	public int getRecoveryThreadTimeToWaitForJoinSuccess() {
		return _recoveryThreadTimeToWaitForJoinSuccess;
	}

	public void setRecoveryThreadTimeToWaitForJoinSuccess(
			int recoveryThreadTimeToWaitForJoinSuccess) {
		this._recoveryThreadTimeToWaitForJoinSuccess = recoveryThreadTimeToWaitForJoinSuccess;
	}

	public int getRecoveryThreadTimeToWaitBtwnFailures() {
		return _recoveryThreadTimeToWaitBtwnFailures;
	}

	public void setRecoveryThreadTimeToWaitBtwnFailures(
			int recoveryThreadTimeToWaitBtwnFailures) {
		this._recoveryThreadTimeToWaitBtwnFailures = recoveryThreadTimeToWaitBtwnFailures;
	}

	public int getMaxDataReceivedBuffer() {
		return _maxDataReceivedBuffer;
	}

	public void setMaxDataReceivedBuffer(int maxDataReceivedBuffer) {
		this._maxDataReceivedBuffer = maxDataReceivedBuffer;
	}
	
	

}
