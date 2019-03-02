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

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class ClientX509TrustManagerImpl implements X509TrustManager {
	private X509TrustManager defaultX509tm; 
	
	ClientX509TrustManagerImpl () throws Exception {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
		tmf.init((KeyStore) null);
		
		TrustManager tms[] = tmf.getTrustManagers();
		for (int i=0; i<tms.length; i++) {
			if (tms[i] instanceof X509TrustManager) {
				defaultX509tm = (X509TrustManager) tms[i];
				break;
			}
		}
	}
	
	public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		if (defaultX509tm == null) throw new CertificateException ("Not found.");
		
		defaultX509tm.checkClientTrusted(certs, authType);
	}

	public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
		if (defaultX509tm != null)
			try {
				defaultX509tm.checkServerTrusted(certs, authType);
				return;
			} catch (Exception e) {}
	}

	public X509Certificate[] getAcceptedIssuers() {
		if (defaultX509tm != null) return defaultX509tm.getAcceptedIssuers();
		return null;
	}
}
