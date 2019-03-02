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

package com.socketanywhere.ssl;

import java.io.File;
import java.io.IOException;

public class SSLServerKeyToolUtil {
	private static String sr = "\\";

	public static void generateSecurityFiles(String pathToKeyTool, String securityHome) {
		//
		// Create the keystore and certificate
		//
		// $KEYTOOL -genkey -alias sample -keyalg RSA -sigalg MD5withRSA -dname
		// "CN=Sample, OU=Sample, O=Sample, C=US" -validity 3650 -keypass
		// $PASSWORD -storetype jks -keystore $KEYSTORE -storepass $PASSWORD
		//
		// $KEYTOOL -export -alias sample -file $CERT -keystore $KEYSTORE
		// -storepass $PASSWORD
		//
		//
		File securityDir = new File(securityHome);
		if (!securityDir.exists()) {
			securityDir.mkdir();
		}

		String[] genCommand = { pathToKeyTool, "-genkey", "-alias", "sample",
				"-keyalg", "RSA", "-sigalg", "MD5withRSA", "-dname",
				"CN=Sample, OU=Sample, O=Sample, C=US", "-validity", "3650",
				"-keypass", "password", "-storetype", "jks", "-keystore",
				securityHome + sr + "samplekeystore", "-storepass", 
				"password" };

		String[] exportCommand = { pathToKeyTool, "-export", "-alias", "sample",
				"-file", securityHome + sr + "sample.cer", "-keystore", 
				securityHome + sr + "samplekeystore", "-storepass", 
				"password" };
		try {
			Process p;
			
			p = Runtime.getRuntime().exec(genCommand);
			p.waitFor();
			
			p = Runtime.getRuntime().exec(exportCommand);
			p.waitFor();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		SSLServerKeyToolUtil.generateSecurityFiles("/path-to-jdk/bin/keytool", "/path-to-store/keystore/");
	}
}
