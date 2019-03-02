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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SSLServerKeyStore {
	private static String _ksType = "JKS";
	private KeyStore _keyStore;
	private String _filename;
	private char[] _password;

	public SSLServerKeyStore(String filename, char[] password) {
		_filename = filename;
		_password = password;
	}

	public KeyStore getKeyStore() {
		return _keyStore;
	}

	public void loadKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
		boolean ksExist = false;
		
		_keyStore = KeyStore.getInstance(_ksType);
		File ksFile = new File(_filename);
		ksExist = ksFile.exists();
	
		if(ksExist) {
			_keyStore.load(new FileInputStream(_filename), _password);
		} else {
			_keyStore.load(null, _password);
		}
	}

	public void saveKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		FileOutputStream fos = new FileOutputStream(_filename);
		_keyStore.store(fos, _password);
		fos.close();
	}
}
