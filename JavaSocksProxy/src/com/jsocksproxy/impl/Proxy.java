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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 * Abstract class Proxy, base for classes Socks4Proxy and Socks5Proxy. Defines methods for specifying default proxy, to
 * be used by all classes of this package.
 */

public abstract class Proxy {

	// Data members
	protected InetRange _directHosts = new InetRange();

	protected InetAddress _proxyIP = null;
	protected String _proxyHost = null;
	protected int _proxyPort;
	Socket _proxySocket = null; 

	protected InputStream _in; 
	protected OutputStream _out; 
	
	
	public void setOutputStream(OutputStream out) {
		this._out = out;
	}
	
	public void setInputStream(InputStream in) {
		this._in = in;
	}
	
	public void setProxySocket(Socket proxySocket) {
		this._proxySocket = proxySocket;
	}
	

	protected int _version;

	// Protected static/class variables
	private static Proxy _defaultProxy = null;

	// Constructors
	// ====================

	Proxy(String proxyHost, int proxyPort) throws UnknownHostException {
		this._proxyHost = proxyHost;

		this._proxyPort = proxyPort;
	}

	
	Proxy(InetAddress proxyIP, int proxyPort) {
		this._proxyIP = proxyIP;
		this._proxyPort = proxyPort;
	}


	Proxy(Proxy p) {
		this._proxyIP = p._proxyIP;
		this._proxyPort = p._proxyPort;
		this._version = p._version;
		this._directHosts = p._directHosts;
	}

	// Public instance methods
	// ========================

	/**
	 * Get the port on which proxy server is running.
	 * 
	 * @return Proxy port.
	 */
	public int getPort() {
		return _proxyPort;
	}

	/**
	 * Get the ip address of the proxy server host.
	 * 
	 * @return Proxy InetAddress.
	 */
	public InetAddress getInetAddress() {
		return _proxyIP;
	}

	/**
	 * Adds given ip to the list of direct addresses. This machine will be accessed without using proxy.
	 */
	public void addDirect(InetAddress ip) {
		_directHosts.add(ip);
	}

	/**
	 * Adds host to the list of direct addresses. This machine will be accessed without using proxy.
	 */
	public boolean addDirect(String host) {
		return _directHosts.add(host);
	}

	/**
	 * Adds given range of addresses to the list of direct addresses, machines within this range will be accessed
	 * without using proxy.
	 */
	public void addDirect(InetAddress from, InetAddress to) {
		_directHosts.add(from, to);
	}

	/**
	 * Sets given InetRange as the list of direct address, previous list will be discarded, any changes done previously
	 * with addDirect(Inetaddress) will be lost. The machines in this range will be accessed without using proxy.
	 * 
	 * @param ir
	 *            InetRange which should be used to look up direct addresses.
	 * @see InetRange
	 */
	public void setDirect(InetRange ir) {
		_directHosts = ir;
	}

	/**
	 * Get the list of direct hosts.
	 * 
	 * @return Current range of direct address as InetRange object.
	 * @see InetRange
	 */
	public InetRange getDirect() {
		return _directHosts;
	}

	/**
	 * Check whether the given host is on the list of direct address.
	 * 
	 * @param host
	 *            Host name to check.
	 * @return true if the given host is specified as the direct addresses.
	 */
	public boolean isDirect(String host) {
		return _directHosts.contains(host);
	}

	/**
	 * Check whether the given host is on the list of direct addresses.
	 * 
	 * @param host
	 *            Host address to check.
	 * @return true if the given host is specified as the direct address.
	 */
	public boolean isDirect(InetAddress host) {
		return _directHosts.contains(host);
	}


	/**
	 * Get string representation of this proxy.
	 * 
	 * @returns string in the form:proxyHost:proxyPort \t Version versionNumber
	 */
	public String toString() {
		return ("" + _proxyIP.getHostName() + ":" + _proxyPort + "\tVersion " + _version);
	}

	// Public Static(Class) Methods
	// ==============================

	/**
	 * Sets SOCKS4 proxy as default.
	 * 
	 * @param hostName
	 *            Host name on which SOCKS4 server is running.
	 * @param port
	 *            Port on which SOCKS4 server is running.
	 * @param user
	 *            User name to use for communications with proxy.
	 */
	public static void setDefaultProxy(String hostName, int port, String user) throws UnknownHostException {
		_defaultProxy = new Socks4Proxy(hostName, port, user);
	}

	/**
	 * Sets SOCKS4 proxy as default.
	 * 
	 * @param ipAddress
	 *            Host address on which SOCKS4 server is running.
	 * @param port
	 *            Port on which SOCKS4 server is running.
	 * @param user
	 *            User name to use for communications with proxy.
	 */
	public static void setDefaultProxy(InetAddress ipAddress, int port, String user) {
		_defaultProxy = new Socks4Proxy(ipAddress, port, user);
	}

	/**
	 * Sets SOCKS5 proxy as default. Default proxy only supports no-authentication.
	 * 
	 * @param hostName
	 *            Host name on which SOCKS5 server is running.
	 * @param port
	 *            Port on which SOCKS5 server is running.
	 */
	public static void setDefaultProxy(String hostName, int port) throws UnknownHostException {
		_defaultProxy = new Socks5Proxy(hostName, port);
	}

	/**
	 * Sets SOCKS5 proxy as default. Default proxy only supports no-authentication.
	 * 
	 * @param ipAddress
	 *            Host address on which SOCKS5 server is running.
	 * @param port
	 *            Port on which SOCKS5 server is running.
	 */
	public static void setDefaultProxy(InetAddress ipAddress, int port) {
		_defaultProxy = new Socks5Proxy(ipAddress, port);
	}

	/**
	 * Sets default proxy.
	 * 
	 * @param p
	 *            Proxy to use as default proxy.
	 */
	public static void setDefaultProxy(Proxy p) {
		_defaultProxy = p;
	}

	/**
	 * Get current default proxy.
	 * 
	 * @return Current default proxy, or null if none is set.
	 */
	public static Proxy getDefaultProxy() {
		return _defaultProxy;

	}
	
	public int getVersion() {
		return _version;
	}

	/**
	 * Parses strings in the form: host[:port:user:password], and creates proxy from information obtained from parsing.
	 * <p>
	 * Defaults: port = 1080.<br>
	 * If user specified but not password, creates Socks4Proxy, if user not specified creates Socks5Proxy, if both user
	 * and password are specified creates Socks5Proxy with user/password authentication.
	 * 
	 * @param proxyEntry
	 *            String in the form host[:port:user:password]
	 * @return Proxy created from the string, null if entry was somehow invalid(host unknown for example, or empty
	 *         string)
	 */
	public static Proxy parseProxy(String proxyEntry) {

		String proxyHost;
		int proxyPort = 1080;
		String proxyUser = null;
		String proxyPassword = null;
		Proxy proxy;

		StringTokenizer st = new StringTokenizer(proxyEntry, ":");
		if (st.countTokens() < 1) {
			return null;
		}

		proxyHost = st.nextToken();
		if (st.hasMoreTokens()) {
			try {
				proxyPort = Integer.parseInt(st.nextToken().trim());
			} catch (NumberFormatException nfe) {
			}
		}

		if (st.hasMoreTokens()) {
			proxyUser = st.nextToken();
		}

		if (st.hasMoreTokens()) {
			proxyPassword = st.nextToken();
		}

		try {
			if (proxyUser == null) {
				proxy = new Socks5Proxy(proxyHost, proxyPort);
			} else if (proxyPassword == null) {
				proxy = new Socks4Proxy(proxyHost, proxyPort, proxyUser);
			} else {
				proxy = new Socks5Proxy(proxyHost, proxyPort);
				UserPasswordAuthentication upa = new UserPasswordAuthentication(proxyUser, proxyPassword);

				((Socks5Proxy) proxy).setAuthenticationMethod(UserPasswordAuthentication.METHOD_ID, upa);
			}
		} catch (UnknownHostException uhe) {
			return null;
		}

		return proxy;
	}

	// Protected Methods
	// =================

	protected void startSession() throws SocksException {
		try {
			_proxySocket = new Socket(_proxyIP, _proxyPort);

			_in = _proxySocket.getInputStream();
			_out = _proxySocket.getOutputStream();
		} catch (SocksException se) {
			throw se;
		} catch (IOException io_ex) {
			throw new SocksException(SOCKS_PROXY_IO_ERROR, "" + io_ex);
		}
	}

	protected abstract Proxy copy();

	protected abstract ProxyMessage formMessage(SocksCmd cmd, InetAddress ip, int port);

	protected abstract ProxyMessage formMessage(SocksCmd cmd, String host, int port) throws UnknownHostException;

	protected abstract ProxyMessage formMessage(InputStream in) throws SocksException, IOException;

	protected ProxyMessage connect(InetAddress ip, int port) throws SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SocksCmd.SOCKS_CMD_CONNECT, ip, port);
			return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
	}

	protected ProxyMessage connect(String host, int port) throws UnknownHostException, SocksException {
		try {
			startSession();
			ProxyMessage request = formMessage(SocksCmd.SOCKS_CMD_CONNECT, host, port);
			return exchange(request);
		} catch (SocksException se) {
			endSession();
			throw se;
		}
	}

	protected ProxyMessage accept() throws IOException, SocksException {
		ProxyMessage msg;
		try {
			msg = formMessage(_in);
		} catch (InterruptedIOException iioe) {
			throw iioe;
		} catch (IOException io_ex) {
			endSession();
			throw new SocksException(SOCKS_PROXY_IO_ERROR, "While Trying accept:" + io_ex);
		}
		return msg;
	}

	protected void endSession() {
		try {
			if (_proxySocket != null) {
				_proxySocket.close();
			}
			
			_proxySocket = null;
		} catch (IOException io_ex) {
		}
	}

	/**
	 * Sends the request to SOCKS server
	 */
	protected void sendMsg(ProxyMessage msg) throws SocksException, IOException {
		msg.write(_out);
	}

	/**
	 * Reads the reply from the SOCKS server
	 */
	protected ProxyMessage readMsg() throws SocksException, IOException {
		return formMessage(_in);
	}

	/**
	 * Sends the request reads reply and returns it throws exception if something wrong with IO or the reply code is not
	 * zero
	 */
	protected ProxyMessage exchange(ProxyMessage request) throws SocksException {
		ProxyMessage reply;
		try {
			request.write(_out);
			reply = formMessage(_in);
		} catch (SocksException s_ex) {
			throw s_ex;
		} catch (IOException ioe) {
			throw (new SocksException(SOCKS_PROXY_IO_ERROR, "" + ioe));
		}
		return reply;
	}

	// Private methods
	// ===============

	// Constants

	// Server replies/errors
	public static final int SOCKS_SUCCESS = 0;
	public static final int SOCKS_FAILURE = 1;
	public static final int SOCKS_BADCONNECT = 2;
	public static final int SOCKS_BADNETWORK = 3;
	public static final int SOCKS_HOST_UNREACHABLE = 4;
	public static final int SOCKS_CONNECTION_REFUSED = 5;
	public static final int SOCKS_TTL_EXPIRE = 6;
	public static final int SOCKS_CMD_NOT_SUPPORTED = 7;
	public static final int SOCKS_ADDR_NOT_SUPPORTED = 8;

	// Client errors
	public static final int SOCKS_NO_PROXY = 1 << 16;
	public static final int SOCKS_PROXY_NO_CONNECT = 2 << 16;
	public static final int SOCKS_PROXY_IO_ERROR = 3 << 16;
	public static final int SOCKS_AUTH_NOT_SUPPORTED = 4 << 16;
	public static final int SOCKS_AUTH_FAILURE = 5 << 16;
	public static final int SOCKS_JUST_ERROR = 6 << 16;

	public static final int SOCKS_DIRECT_FAILED = 7 << 16;
	public static final int SOCKS_METHOD_NOTSUPPORTED = 8 << 16;

//	static final int SOCKS_CMD_CONNECT = 0x1;
//	static final int SOCKS_CMD_BIND = 0x2;
//	static final int SOCKS_CMD_UDP_ASSOCIATE = 0x3;

}








class SocksCmd {
	int _value;
	
	private SocksCmd(int value) {
		_value = value;
	}

	public static SocksCmd createSocksCmd(int value) {
 
		if(value == SOCKS_CMD_CONNECT.getValue()) {
			return SOCKS_CMD_CONNECT;
		} else {
			return new SocksCmd(value);
		}
	}
	
	public static final SocksCmd SOCKS_CMD_CONNECT = new SocksCmd(0x1); 
//	public static final SocksCmd SOCKS_CMD_BIND = new SocksCmd(0x2); 
//	public static final SocksCmd SOCKS_CMD_UDP_ASSOCIATE = new SocksCmd(0x3);	

	
	@Override
	public boolean equals(Object obj) {
		return this.getValue() == ((SocksCmd)obj).getValue();
	}
	
	public int getValue() {
		return _value;
	}
	
	public static SocksCmd getType(int value) {
		
		if(value == SOCKS_CMD_CONNECT.getValue()) {
			return SOCKS_CMD_CONNECT;
		} 
		
		return null;
	}
	
}
