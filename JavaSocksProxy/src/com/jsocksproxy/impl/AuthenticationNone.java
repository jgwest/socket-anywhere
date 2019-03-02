/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.impl;

import java.net.Socket;

/**
 * SOCKS5 none authentication. Dummy class does almost nothing.
 */
public class AuthenticationNone implements IAuthentication {

	public SocksAuthEntry doSocksAuthentication(int methodId, Socket proxySocket) throws java.io.IOException {

		if (methodId != 0)
			return null;

		return new SocksAuthEntry ( proxySocket.getInputStream(), proxySocket.getOutputStream() );
	}
}
