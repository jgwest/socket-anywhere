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
import java.net.UnknownHostException;

/**
 * Abstract class which describes SOCKS4/5 response/request.
 */
public abstract class ProxyMessage {
	
	/** Host as an IP address */
	protected InetAddress _ip = null;
	
	/** SOCKS version, or version of the response for SOCKS4 */
	protected int _version;
	
	/** Port field of the request/response */
	protected int _port;
	
	/** Request/response code as an int */
	SocksCmd _command;
	
	/** Host as string. */
	String _host = null;
	
	/** User field for SOCKS4 request messages */
	String _user = null;
	

	ProxyMessage(SocksCmd command, InetAddress ip, int port) {
		this._command = command;
		this._ip = ip;
		this._port = port;
	}

	ProxyMessage() {
	}

	/**
	 * Initializes Message from the stream. Reads server response from given stream.
	 * 
	 * @param in
	 *            Input stream to read response from.
	 * @throws SocksException
	 *             If server response code is not SOCKS_SUCCESS(0), or if any error with protocol occurs.
	 * @throws IOException
	 *             If any error happens with I/O.
	 */
	public abstract void read(InputStream in) throws SocksException, IOException;

	/**
	 * Initializes Message from the stream. Reads server response or client request from given stream.
	 * 
	 * @param in
	 *            Input stream to read response from.
	 * @param clinetMode
	 *            If true read server response, else read client request.
	 * @throws SocksException
	 *             If server response code is not SOCKS_SUCCESS(0) and reading in client mode, or if any error with
	 *             protocol occurs.
	 * @throws IOException
	 *             If any error happens with I/O.
	 */
	public abstract void read(InputStream in, boolean client_mode) throws SocksException, IOException;

	/**
	 * Writes the message to the stream.
	 * 
	 * @param out
	 *            Output stream to which message should be written.
	 */
	public abstract void write(OutputStream out) throws SocksException, IOException;

	/**
	 * Get the Address field of this message as InetAddress object.
	 * 
	 * @return Host address or null, if one can't be determined.
	 */
	public InetAddress getInetAddress() throws UnknownHostException {
		return _ip;
	}
	
	public int getVersion() {
		return _version;
	}

	public String getUser() {
		return _user;
	}
	
	public InetAddress getIP() {
		return _ip;
	}
	
	public int getPort() {
		return _port;
	}
	
	public void setIP(InetAddress ip) {
		this._ip = ip;
	}
	
	public String getHost() {
		return _host;
	}
	
	public String getUserForSocks4() {
		return _user;
	}
	
	
	
	/**
	 * Get string representation of this message.
	 * 
	 * @return string representation of this message.
	 */
	public String toString() {
		return "Proxy Message:\n" + "Version:" + _version + "\n" + "Command: " + _command + "\n" + "IP:     " + _ip + "\n"
				+ "Port:   " + _port + "\n" + "User:   " + _user + "\n";
	}
	
	public SocksCmd getCommand() {
		return _command;
	}

	// Package methods
	// ////////////////

	static final String bytes2IPV4(byte[] addr, int offset) {
		String hostName = "" + (addr[offset] & 0xFF);
		for (int i = offset + 1; i < offset + 4; ++i)
			hostName += "." + (addr[i] & 0xFF);
		return hostName;
	}

	static final String bytes2IPV6(byte[] addr, int offset) {
		// Have no idea how they look like!
		return null;
	}

}
