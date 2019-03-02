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

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

public class SSLServerKeyManager implements X509KeyManager {
	private X509KeyManager _km = null;

	public SSLServerKeyManager(KeyStore ks, char[] password) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, password);
		KeyManager[] kms = kmf.getKeyManagers();

		if (kms != null) {
			_km = (X509KeyManager) kms[0];
		}
	}

	public PrivateKey getPrivateKey(String s) {
		return _km.getPrivateKey(s);
	}

	public X509Certificate[] getCertificateChain(String s) {
		return _km.getCertificateChain(s);
	}

	public String[] getClientAliases(String keyType, Principal[] issuers) {
		return _km.getClientAliases(keyType, issuers);
	}

	public String[] getServerAliases(String keyType, Principal[] issuers) {
		return _km.getServerAliases(keyType, issuers);
	}

	public String chooseServerAlias(String keyType, Principal[] issuers, Socket sock) {
		return _km.chooseServerAlias(keyType, issuers, null);
	}

	public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket sock) {
		return _km.chooseClientAlias(keyType, issuers, null);
	}
}
