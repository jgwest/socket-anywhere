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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLTrustManager implements X509TrustManager {
	private X509TrustManager _tm = null;
	private X509Certificate[] _certs = null;
	private X509Certificate[] _incommingCerts = null;
	private boolean _isCheckServerTrusted = true;

	public SSLTrustManager(KeyStore ks) throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		TrustManager[] tms = tmf.getTrustManagers();
		for (int i = 0; i < tms.length; i++) {
			if (tms[i] instanceof X509TrustManager) {
				_tm = (X509TrustManager) tms[i];
				_certs = ((X509TrustManager) tms[i]).getAcceptedIssuers();
				break;
			}
		}
		if (_tm == null) {
			_certs = new X509Certificate[] {};
		}
	}

	public X509Certificate[] getAcceptedIssuers() {
		return _certs;
	}

	public X509Certificate[] getIncommingCertificates() {
		return _incommingCerts;
	}

	public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException {
	}

	public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException {
		if(_isCheckServerTrusted) {
			if (_tm != null) {
				try {
					_tm.checkServerTrusted(cert, authType);
				} catch(Exception e) {
					if(e instanceof RuntimeException) {
						_incommingCerts = cert;
						throw (RuntimeException)e;
					}
					if(e instanceof CertificateException) {
						throw (CertificateException)e;
					}
				}
			}
		}
	}

	public boolean isCheckServerTrusted() {
		return _isCheckServerTrusted;
	}

	public void setCheckServerTrusted(boolean isCheckServerTrusted) {
		_isCheckServerTrusted = isCheckServerTrusted;
	}

}
