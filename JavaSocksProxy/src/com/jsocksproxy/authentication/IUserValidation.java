/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.authentication;

import java.net.Socket;

import com.socketanywhere.net.ISocketTL;

/**
 * Interface which provides for user validation, based on user name password and where it connects from.
 */
public interface IUserValidation {
	/**
	 * Implementations of this interface are expected to use some or all of the information provided plus any
	 * information they can extract from other sources to decide whether given user should be allowed access to SOCKS
	 * server, or whatever you use it for.
	 * 
	 * @return true to indicate user is valid, false otherwise.
	 * @param username
	 *            User whom implementation should validate.
	 * @param password
	 *            Password this user provided.
	 * @param connection
	 *            Socket which user used to connect to the server.
	 */
	boolean isUserValid(String username, String password, Socket connection);
	
	boolean isUserValid(String username, String password, ISocketTL connection);
}
