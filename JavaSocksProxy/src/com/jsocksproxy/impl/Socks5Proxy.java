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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * SOCKS5 Proxy.
 */

public class Socks5Proxy extends Proxy implements Cloneable {

	// Data members
	private Hashtable<Integer, IAuthentication> _authMethods = new Hashtable<Integer, IAuthentication>();
	private int _selectedMethod;

	private boolean _resolveAddrLocally = true;
	
	// Public Constructors
	// ====================

	/**
	 * Creates SOCKS5 proxy.
	 * 
	 * @param p
	 *            Proxy to use to connect to this proxy, allows proxy chaining.
	 * @param proxyHost
	 *            Host on which a Proxy server runs.
	 * @param proxyPort
	 *            Port on which a Proxy server listens for connections.
	 * @throws UnknownHostException
	 *             If proxyHost can't be resolved.
	 */
	public Socks5Proxy(String proxyHost, int proxyPort) throws UnknownHostException {
		super(proxyHost, proxyPort);
		_version = 5;
		setAuthenticationMethod(0, new AuthenticationNone());
	}


	/**
	 * Creates SOCKS5 proxy.
	 * 
	 * @param p
	 *            Proxy to use to connect to this proxy, allows proxy chaining.
	 * @param proxyIP
	 *            Host on which a Proxy server runs.
	 * @param proxyPort
	 *            Port on which a Proxy server listens for connections.
	 */
	public Socks5Proxy(InetAddress proxyIP, int proxyPort) {
		super(proxyIP, proxyPort);
		_version = 5;
		setAuthenticationMethod(0, new AuthenticationNone());
	}


	// Public instance methods
	// ========================

	/**
	 * Whether to resolve address locally or to let proxy do so.
	 * <p>
	 * SOCKS5 protocol allows to send host names rather then IPs in the requests, this option controls whether the
	 * hostnames should be send to the proxy server as names, or should they be resolved locally.
	 * 
	 * @param doResolve
	 *            Whether to perform resolution locally.
	 */
	public void setResolveAddrLocally(boolean doResolve) {
		_resolveAddrLocally = doResolve;	
	}
	
	/**
	 * Get current setting on how the addresses should be handled.
	 * 
	 * @return Current setting for address resolution.
	 * @see Socks5Proxy#resolveAddrLocally(boolean doResolve)
	 */
	public boolean resolveAddrLocally() {
		return _resolveAddrLocally;
	}

	/**
	 * Adds another authentication method.
	 * 
	 * @param methodId
	 *            Authentication method id, see rfc1928
	 * @param method
	 *            Implementation of Authentication
	 * @see IAuthentication
	 */
	public boolean setAuthenticationMethod(int methodId, IAuthentication method) {
		if (methodId < 0 || methodId > 255) {
			return false;
		}
		
		if (method == null) {
			// Want to remove a particular method
			return (_authMethods.remove(new Integer(methodId)) != null);
		} else {// Add the method, or rewrite old one
			_authMethods.put(new Integer(methodId), method);
		}
		
		return true;
	}

	/**
	 * Get authentication method, which corresponds to given method id
	 * 
	 * @param methodId
	 *            Authentication method id.
	 * @return Implementation for given method or null, if one was not set.
	 */
	public IAuthentication getAuthenticationMethod(int methodId) {
		Object method = _authMethods.get(new Integer(methodId));
		if (method == null)
			return null;
		return (IAuthentication) method;
	}


	/**
	 * Creates a clone of this Proxy.
	 */
	@SuppressWarnings("unchecked")
	public Object clone() {
		Socks5Proxy newProxy = new Socks5Proxy(_proxyIP, _proxyPort);
		newProxy._authMethods = (Hashtable<Integer, IAuthentication>) this._authMethods.clone();
		newProxy._directHosts = (InetRange) _directHosts.clone();
		newProxy._resolveAddrLocally = _resolveAddrLocally;
		return newProxy;
	}

	// Public Static(Class) Methods
	// ==============================

	// Protected Methods
	// =================

	protected Proxy copy() {
		Socks5Proxy copy = new Socks5Proxy(_proxyIP, _proxyPort);
		copy._authMethods = this._authMethods; // same Hash, no copy
		copy._directHosts = this._directHosts;
		copy._resolveAddrLocally = this._resolveAddrLocally;
		return copy;
	}

	/**
    *
    *
    */
	protected void startSession() throws SocksException {
		super.startSession();
		IAuthentication auth;
		Socket ps = _proxySocket; // The name is too long

		try {

			byte nMethods = (byte) _authMethods.size(); // Number of methods

			byte[] buf = new byte[2 + nMethods]; // 2 is for VER,NMETHODS
			buf[0] = (byte) _version;
			buf[1] = nMethods; // Number of methods
			int i = 2;

			Enumeration<Integer> ids = _authMethods.keys();
			while (ids.hasMoreElements())
				buf[i++] = (byte) ids.nextElement().intValue();

			_out.write(buf);
			_out.flush();

			int versionNumber = _in.read();
			_selectedMethod = _in.read();

			if (versionNumber < 0 || _selectedMethod < 0) {
				// EOF condition was reached
				endSession();
				throw (new SocksException(SOCKS_PROXY_IO_ERROR, "Connection to proxy lost."));
			}
			if (versionNumber < _version) {
				// What should we do??
			}
			if (_selectedMethod == 0xFF) { // No method selected
				ps.close();
				throw (new SocksException(SOCKS_AUTH_NOT_SUPPORTED));
			}

			auth = getAuthenticationMethod(_selectedMethod);
			if (auth == null) {
				// This shouldn't happen, unless method was removed by other
				// thread, or the server stuffed up
				throw (new SocksException(SOCKS_JUST_ERROR, "Speciefied Authentication not found!"));
			}
			SocksAuthEntry in_out = auth.doSocksAuthentication(_selectedMethod, ps);
			if (in_out == null) {
				// Authentication failed by some reason
				throw (new SocksException(SOCKS_AUTH_FAILURE));
			}
			// Most authentication methods are expected to return
			// simply the input/output streams associated with
			// the socket. However if the auth. method requires
			// some kind of encryption/decryption being done on the
			// connection it should provide classes to handle I/O.

			_in = (InputStream) in_out._in;
			_out = (OutputStream) in_out._out;
//			if (in_out._udpEncapsulation != null)
//				_udpEncapsulation = (IUDPEncapsulation) in_out._udpEncapsulation;

		} catch (SocksException s_ex) {
			throw s_ex;
		} catch (UnknownHostException uh_ex) {
			throw (new SocksException(SOCKS_PROXY_NO_CONNECT));
		} catch (SocketException so_ex) {
			throw (new SocksException(SOCKS_PROXY_NO_CONNECT));
		} catch (IOException io_ex) {
			// System.err.println(io_ex);
			throw (new SocksException(SOCKS_PROXY_IO_ERROR, "" + io_ex));
		}
	}

	protected ProxyMessage formMessage(SocksCmd cmd, InetAddress ip, int port) {
		return new Socks5Message(cmd, ip, port);
	}

	protected ProxyMessage formMessage(SocksCmd cmd, String host, int port) throws UnknownHostException {
		if (_resolveAddrLocally)
			return formMessage(cmd, InetAddress.getByName(host), port);
		else
			return new Socks5Message(cmd, host, port);
	}

	protected ProxyMessage formMessage(InputStream in) throws SocksException, IOException {
		return new Socks5Message(in);
	}

}
