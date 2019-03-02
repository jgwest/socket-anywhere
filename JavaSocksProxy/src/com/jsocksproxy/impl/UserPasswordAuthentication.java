/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * SOCKS5 User Password authentication scheme
 */
public class UserPasswordAuthentication implements IAuthentication {

	/** SOCKS ID for User/Password authentication method */
	public final static int METHOD_ID = 2;

	String _userName, _password;
	byte[] _request;

	/**
	 * Create an instance of UserPasswordAuthentication.
	 * 
	 * @param userName
	 *            User Name to send to SOCKS server.
	 * @param password
	 *            Password to send to SOCKS server.
	 */
	public UserPasswordAuthentication(String userName, String password) {
		this._userName = userName;
		this._password = password;
		formRequest();
	}

	/**
	 * Get the user name.
	 * 
	 * @return User name.
	 */
	public String getUser() {
		return _userName;
	}

	/**
	 * Get password
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return _password;
	}

	/**
	 * Does User/Password authentication as defined in rfc1929.
	 * 
	 * @return An array containing in, out streams, or null if authentication fails.
	 */
	public SocksAuthEntry doSocksAuthentication(int methodId, Socket proxySocket) throws java.io.IOException {

		if (methodId != METHOD_ID)
			return null;

		InputStream in = proxySocket.getInputStream();
		OutputStream out = proxySocket.getOutputStream();

		out.write(_request);
		int version = in.read();
		
		if (version < 0) {
			return null; // Server closed connection
		}
		
		int status = in.read();
		
		if (status != 0) {
			return null; // Server closed connection, or auth failed.
		}

		return new SocksAuthEntry ( in, out );
	}

	// Private methods
	// ////////////////

	/** Convert UserName password in to binary form, ready to be send to server */
	private void formRequest() {
		byte[] user_bytes = _userName.getBytes();
		byte[] password_bytes = _password.getBytes();

		_request = new byte[3 + user_bytes.length + password_bytes.length];
		_request[0] = (byte) 1;
		_request[1] = (byte) user_bytes.length;
		System.arraycopy(user_bytes, 0, _request, 2, user_bytes.length);
		_request[2 + user_bytes.length] = (byte) password_bytes.length;
		System.arraycopy(password_bytes, 0, _request, 3 + user_bytes.length, password_bytes.length);
	}
}
