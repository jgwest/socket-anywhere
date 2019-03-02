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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


/** Creates a self-contained SSL socket/server socket, which uses default options, and does not do 
 * certificate authentication. */
public class SSLServerFactory {
//	private static SSLServerFactory _instance = null;
	
	private static String _protocol = "TLS";
	 
	private SSLContext _sslContext = null;
	private boolean _sslContextInitialized = false; 
	
	private KeyManager[] _kms = new KeyManager[1];
	private TrustManager[] _tms = new TrustManager[1];

//	public synchronized static SSLServerFactory getInstance() throws NoSuchAlgorithmException {
//		if(_instance == null) {
//			_instance = new SSLServerFactory();
//		}
//
//		return _instance;
//	}

	SSLServerFactory() throws NoSuchAlgorithmException {
		_sslContext = SSLContext.getInstance(_protocol);
	}

	public SSLSocket createSocket(Socket socket) {
		SSLSocketFactory factory;
		SSLSocket sslSocket = null;

		synchronized(_sslContext) {
			if(!_sslContextInitialized) {
				try {
					_sslContext.init(_kms, _tms, null);
					_sslContextInitialized = true;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		factory = _sslContext.getSocketFactory();
		

		try {
			sslSocket = (SSLSocket) factory.createSocket(socket, socket.getInetAddress().getHostName(), socket.getPort(), true);
			
		} catch (IOException e) {
					e.printStackTrace();
		}

		return sslSocket;
	}

	public Socket getClientSocket(String host, int port) throws IOException, KeyManagementException, UnknownHostException {
		Socket socket = null;
		
		synchronized(_sslContext) {
			if(!_sslContextInitialized) {
				_sslContext.init(_kms, _tms, null);
				_sslContextInitialized = true;
			}
		}
		
		SSLSocketFactory factory = _sslContext.getSocketFactory();

		if(factory != null) {
			socket = factory.createSocket(host, port);
		}

		return socket;
	}

	public ServerSocket getServerSocket(int port) throws IOException, KeyManagementException {
		ServerSocket socket = null;
		
		synchronized(_sslContext) {
			if(!_sslContextInitialized) {
				_sslContext.init(_kms, _tms, null);
				_sslContextInitialized = true;
			}
		}
		
		SSLServerSocketFactory factory = _sslContext.getServerSocketFactory();

		if(factory != null) {
			socket = factory.createServerSocket(port);
		}

		return socket;
	}

	public void setKeyManager(KeyManager km) {
		_kms[0] = km;
	}

	public void setTrustManager(TrustManager tm) {
		_tms[0] = tm;
	}

}
