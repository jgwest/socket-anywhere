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
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

public class ClientSecureConnectionImpl {
	
    public static SSLSocket initSSL (Socket socket) throws IOException {
    	if (socket == null || !socket.isConnected()) return null;
    	
		TrustManager tms[] = null;
		try {
			ClientX509TrustManagerImpl tm = new ClientX509TrustManagerImpl();
			tms = new TrustManager[1];
			tms[0] = tm;
		} catch (Exception e) {}
		
		SSLContext sslCtx;
		try {
			sslCtx = SSLContext.getInstance("TLS");
			sslCtx.init(null, tms, new SecureRandom());
		}
		catch(Exception e) {
			sslCtx = null;
		}
		
		if (sslCtx == null) return null;
		
		String host = socket.getInetAddress().getHostAddress();
		int port = socket.getPort();
		
		SSLSocket sslSocket = (SSLSocket) sslCtx.getSocketFactory().createSocket(socket, host, port, true);
		sslSocket.setUseClientMode(true);

		sslSocket.startHandshake();

		if (sslSocket.getSession() == null) return null;
		
		return sslSocket;
    }

}