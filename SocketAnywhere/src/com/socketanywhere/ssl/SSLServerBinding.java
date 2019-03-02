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

import java.io.IOException;
import java.net.Socket;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;

public class SSLServerBinding {
	
	SSLServerFactory _factory = null;
	
	String _keystoreLocation;
	
	// Debug 
	final static boolean DEBUG = true;

	
	public SSLServerBinding(String keystoreLocation) {
		_keystoreLocation = keystoreLocation;
	}
	

	public SSLSocket convertToSSLSocket(Socket socket) {
		
		SSLSocket sslSocket;
		
		if(socket == null) {
			return null;
		}

		sslSocket = (SSLSocket) _factory.createSocket(socket);
		
		sslSocket.setUseClientMode(false);
		sslSocket.setNeedClientAuth(false);
		sslSocket.setWantClientAuth(false);

		return sslSocket;

	}
	
	public void init() {
		
		String ksPassword = "password";
		
		try {
			_factory = new SSLServerFactory(); // SSLServerFactory.getInstance();

			if(_keystoreLocation != null) {
				SSLServerKeyStore ks = new SSLServerKeyStore(_keystoreLocation, ksPassword.toCharArray());
				ks.loadKeyStore();

				SSLServerKeyManager km = new SSLServerKeyManager(ks.getKeyStore(), ksPassword.toCharArray());
				_factory.setKeyManager(km);

				SSLTrustManager tm = new SSLTrustManager(ks.getKeyStore());
				_factory.setTrustManager(tm);
			} else {

				System.err.println("* Warning: Using TLS/SSL without certificate checking.");
				_factory.setKeyManager(null);
				_factory.setTrustManager(new TrustAllX509TrustManager() );
			}

			
		} catch(NoSuchAlgorithmException nsa) {
			nsa.printStackTrace();
		} catch(KeyStoreException kse) {
			kse.printStackTrace();
		} catch(CertificateException ce) {
			ce.printStackTrace();
		} catch(UnrecoverableKeyException uke) {
			uke.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}		
		
	}
}


class TrustAllX509TrustManager implements X509TrustManager {
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

	public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
	}

	public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
	}

}