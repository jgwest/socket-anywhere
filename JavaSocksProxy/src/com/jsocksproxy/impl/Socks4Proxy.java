/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Proxy which describes SOCKS4 proxy.
 */

public class Socks4Proxy extends Proxy implements Cloneable {

	// Data members
	private String _user;
	
	public String getUser() {
		return _user;
	}

	// Public Constructors
	// ====================

	/**
	 * Creates the SOCKS4 proxy
	 * 
	 * @param p
	 *            Proxy to use to connect to this proxy, allows proxy chaining.
	 * @param proxyHost
	 *            Address of the proxy server.
	 * @param proxyPort
	 *            Port of the proxy server
	 * @param user
	 *            User name to use for identification purposes.
	 * @throws UnknownHostException
	 *             If proxyHost can't be resolved.
	 */
	public Socks4Proxy(String proxyHost, int proxyPort, String user) throws UnknownHostException {
		super(proxyHost, proxyPort);
		this._user = new String(user);
		_version = 4;
	}

	/**
	 * Creates the SOCKS4 proxy
	 * 
	 * @param p
	 *            Proxy to use to connect to this proxy, allows proxy chaining.
	 * @param proxyIP
	 *            Address of the proxy server.
	 * @param proxyPort
	 *            Port of the proxy server
	 * @param user
	 *            User name to use for identification purposes.
	 */
	public Socks4Proxy(InetAddress proxyIP, int proxyPort, String user) {
		super(proxyIP, proxyPort);
		
		this._user = new String(user);
		_version = 4;
	}

	// Public instance methods
	// ========================

	/**
	 * Creates a clone of this proxy. Changes made to the clone should not affect this object.
	 */
	public Object clone() {
		Socks4Proxy newProxy = new Socks4Proxy(_proxyIP, _proxyPort, _user);
		newProxy._directHosts = (InetRange) _directHosts.clone();
		return newProxy;
	}

	// Public Static(Class) Methods
	// ==============================

	// Protected Methods
	// =================

	protected Proxy copy() {
		Socks4Proxy copy = new Socks4Proxy(_proxyIP, _proxyPort, _user);
		copy._directHosts = this._directHosts;
		return copy;
	}

	protected ProxyMessage formMessage(SocksCmd cmd, InetAddress ip, int port) {
		
		if(cmd.equals(SocksCmd.SOCKS_CMD_CONNECT)) {
			cmd = SocksCmd.createSocksCmd(Socks4Message.REQUEST_CONNECT);
			
		} else {
			return null;
		}
				
		return new Socks4Message(cmd, ip, port, _user);
	}

	protected ProxyMessage formMessage(SocksCmd cmd, String host, int port) throws UnknownHostException {
		return formMessage(cmd, InetAddress.getByName(host), port);
	}

	protected ProxyMessage formMessage(InputStream in) throws SocksException, IOException {
		return new Socks4Message(in, true);
	}

}
