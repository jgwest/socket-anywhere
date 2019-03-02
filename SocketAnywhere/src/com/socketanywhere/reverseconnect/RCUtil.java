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

package com.socketanywhere.reverseconnect;

import java.io.IOException;
import java.io.InputStream;

public class RCUtil {

	public static byte[] readAndWait(InputStream is, int bytesWanted) throws IOException{
		byte[] result = new byte[bytesWanted];
		
		int currPos = 0;
		int bytesRemaining = bytesWanted;
		while(bytesRemaining > 0) {

			int bytesRead = is.read(result, currPos, bytesRemaining);
			if(bytesRead == -1) {
				return null;
			} else {
				bytesRemaining -= bytesRead;
				currPos += bytesRead;
			}
		}
		
		return result;
	}

	
	

}
