/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * SOCKS4 Reply/Request message.
 */

class Socks4Message extends ProxyMessage {

	private byte[] _msgBytes;
	private int _msgLength;

	
	/**
	 * Server failed reply, cmd command for failed request
	 */
	public Socks4Message(SocksCmd cmd) {
		super(cmd, null, 0);
		this._user = null;

		_msgLength = 2;
		_msgBytes = new byte[2];

		_msgBytes[0] = (byte) 0;
		_msgBytes[1] = (byte) _command.getValue();
	}

	/**
	 * Server successful reply
	 */
	public Socks4Message(SocksCmd cmd, InetAddress ip, int port) {
		this(0, cmd, ip, port, null);
	}

	/**
	 * Client request
	 */
	public Socks4Message(SocksCmd cmd, InetAddress ip, int port, String user) {
		this(SOCKS_VERSION, cmd, ip, port, user);
	}

	/**
	 * Most general constructor
	 */
	private Socks4Message(int version, SocksCmd cmd, InetAddress ip, int port, String user) {
		super(cmd, ip, port);
		this._user = user;
		this._version = version;

		_msgLength = user == null ? 8 : 9 + user.length();
		_msgBytes = new byte[_msgLength];

		_msgBytes[0] = (byte) version;
		_msgBytes[1] = (byte) _command.getValue();
		_msgBytes[2] = (byte) (port >> 8);
		_msgBytes[3] = (byte) port;

		byte[] addr;

		if (ip != null)
			addr = ip.getAddress();
		else {
			addr = new byte[4];
			addr[0] = addr[1] = addr[2] = addr[3] = 0;
		}
		System.arraycopy(addr, 0, _msgBytes, 4, 4);

		if (user != null) {
			byte[] buf = user.getBytes();
			System.arraycopy(buf, 0, _msgBytes, 8, buf.length);
			_msgBytes[_msgBytes.length - 1] = 0;
		}
	}

	/**
	 * Initialise from the stream If clientMode is true attempts to read a server response otherwise reads a client
	 * request see read for more detail
	 */
	public Socks4Message(InputStream in, boolean clientMode) throws IOException {
		_msgBytes = null;
		read(in, clientMode);
	}

	public void read(InputStream in) throws IOException {
		read(in, true);
	}

	public void read(InputStream in, boolean clientMode) throws IOException {
		DataInputStream d_in = new DataInputStream(in);
		
		_version = d_in.readUnsignedByte();
		_command = SocksCmd.createSocksCmd(d_in.readUnsignedByte());
		
		if (clientMode && _command.getValue() != REPLY_OK) {
			String errMsg;
			
			if (_command.getValue() > REPLY_OK && _command.getValue() < REPLY_BAD_IDENTD) {
				errMsg = replyMessage[_command.getValue() - REPLY_OK];
			} else {
				errMsg = "Unknown Reply Code";
			}
			throw new SocksException(_command.getValue(), errMsg);
		}
		
		_port = d_in.readUnsignedShort();
		
		byte[] addr = new byte[4];
		d_in.readFully(addr);
		
		_ip = bytes2IP(addr);
		_host = _ip.getHostName();
		
		if (!clientMode) {
			int b = in.read();
			// Hope there are no idiots with user name bigger than this
			byte[] userBytes = new byte[256];
			int i = 0;
			for (i = 0; i < userBytes.length && b > 0; ++i) {
				userBytes[i] = (byte) b;
				b = in.read();
			}
			_user = new String(userBytes, 0, i);
		}
	}

	public void write(OutputStream out) throws IOException {
		if (_msgBytes == null) {
			Socks4Message msg = new Socks4Message(_version, _command, _ip, _port, _user);
			_msgBytes = msg._msgBytes;
			_msgLength = msg._msgLength;
		}
		out.write(_msgBytes);
	}

	public byte[] getMsgBytes() {
		return _msgBytes;
	}
	
	public int getMsgLength() {
		return _msgLength;
	}
	
	// Class methods
	static InetAddress bytes2IP(byte[] addr) {
		String s = bytes2IPV4(addr, 0);
		try {
			return InetAddress.getByName(s);
		} catch (UnknownHostException uh_ex) {
			return null;
		}
	}
	
	

	// Constants

	static final String[] replyMessage = { "Request Granted", "Request Rejected or Failed",
			"Failed request, can't connect to Identd", "Failed request, bad user name" };

	static final int SOCKS_VERSION = 4;

	public final static int REQUEST_CONNECT = 1;
	public final static int REQUEST_BIND = 2;

	public final static int REPLY_OK = 90;
	public final static int REPLY_REJECTED = 91;
	public final static int REPLY_NO_CONNECT = 92;
	public final static int REPLY_BAD_IDENTD = 93;

}
