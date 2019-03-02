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
 * The Authentication interface provides for performing method specific authentication for SOCKS5 connections.
 */
public interface IAuthentication {
	/**
	 * This method is called when SOCKS5 server have selected a particular authentication method, for whch an
	 * implementation have been registered.
	 * 
	 * <p>
	 * This method should return an array {inputstream,outputstream [,UDPEncapsulation]}. The reason for that is that
	 * SOCKS5 protocol allows to have method specific encapsulation of data on the socket for purposes of integrity or
	 * security. And this encapsulation should be performed by those streams returned from the method. It is also
	 * possible to encapsulate datagrams. If authentication method supports such encapsulation an instance of the
	 * UDPEncapsulation interface should be returned as third element of the array, otherwise either null should be
	 * returned as third element, or array should contain only 2 elements.
	 * 
	 * @param methodId
	 *            Authentication method selected by the server.
	 * @param proxySocket
	 *            Socket used to connect to the proxy.
	 * @return Two or three element array containing Input/Output streams which should be used on this connection. Third
	 *         argument is optional and should contain an instance of UDPEncapsulation. It should be provided if the
	 *         authentication method used requires any encapsulation to be done on the datagrams.
	 */
	SocksAuthEntry doSocksAuthentication(int methodId, Socket proxySocket) throws java.io.IOException;
}


class SocksAuthEntry {

	InputStream _in;
	OutputStream _out;
	
	public SocksAuthEntry(InputStream in, OutputStream out) {
		this._in = in;
		this._out = out;
	}

}