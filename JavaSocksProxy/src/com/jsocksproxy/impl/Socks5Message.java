/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012, 2013. 
 */

package com.jsocksproxy.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * SOCKS5 request/response message.
 */

class Socks5Message extends ProxyMessage {
	
	/** Address type of given message */
	AddressType _addrType;

	byte[] _data;

	/**
	 * Server error response.
	 * 
	 * @param cmd
	 *            Error code.
	 */
	public Socks5Message(SocksCmd cmd) {
		super(cmd, null, 0);
		
		_data = new byte[3];
		_data[0] = SOCKS_VERSION; // Version.
		_data[1] = (byte) cmd.getValue(); // Reply code for some kind of failure.
		_data[2] = 0; // Reserved byte.
	}

	/**
	 * Construct client request or server response.
	 * 
	 * @param cmd
	 *            - Request/Response code.
	 * @param ip
	 *            - IP field.
	 * @paarm port - port field.
	 */
	public Socks5Message(SocksCmd cmd, InetAddress ip, int port) {
		super(cmd, ip, port);
		this._host = ip == null ? "0.0.0.0" : ip.getHostName();
		this._version = SOCKS_VERSION;

		byte[] addr;

		if (ip == null) {
			addr = new byte[4];
			addr[0] = addr[1] = addr[2] = addr[3] = 0;
		} else
			addr = ip.getAddress();

		_addrType = addr.length == 4 ? AddressType.SOCKS_ATYP_IPV4 : AddressType.SOCKS_ATYP_IPV6;

		_data = new byte[6 + addr.length];
		_data[0] = (byte) SOCKS_VERSION; // Version
		
		// Command
		if(_command != null) {
			_data[1] = (byte) _command.getValue(); 	
		} else {
			// No 'command' in UDP forward case, so just use 0.
			_data[1] = (byte) 0; 
		}
		
		_data[2] = (byte) 0; // Reserved byte
		_data[3] = (byte) _addrType.getValue(); // Address type

		// Put Address
		System.arraycopy(addr, 0, _data, 4, addr.length);
		// Put port
		_data[_data.length - 2] = (byte) (port >> 8);
		_data[_data.length - 1] = (byte) (port);
	}

	/**
	 * Construct client request or server response.
	 * 
	 * @param cmd
	 *            - Request/Response code.
	 * @param hostName
	 *            - IP field as hostName, uses ADDR_TYPE of HOSTNAME.
	 * @paarm port - port field.
	 */
	public Socks5Message(SocksCmd cmd, String hostName, int port) {
		super(cmd, null, port);
		this._host = hostName;
		this._version = SOCKS_VERSION;

		// System.out.println("Doing ATYP_DOMAINNAME");

		_addrType = AddressType.SOCKS_ATYP_DOMAINNAME;
		byte addr[] = hostName.getBytes();

		_data = new byte[7 + addr.length];
		_data[0] = (byte) SOCKS_VERSION; // Version
		_data[1] = (byte) _command.getValue(); // Command
		_data[2] = (byte) 0; // Reserved byte
		_data[3] = (byte) (AddressType.SOCKS_ATYP_DOMAINNAME).getValue(); // Address type
		_data[4] = (byte) addr.length; // Length of the address

		// Put Address
		System.arraycopy(addr, 0, _data, 5, addr.length);
		// Put port
		_data[_data.length - 2] = (byte) (port >> 8);
		_data[_data.length - 1] = (byte) (port);
	}

	/**
	 * Initialises Message from the stream. Reads server response from given stream.
	 * 
	 * @param in
	 *            Input stream to read response from.
	 * @throws SocksException
	 *             If server response code is not SOCKS_SUCCESS(0), or if any error with protocol occurs.
	 * @throws IOException
	 *             If any error happens with I/O.
	 */
	public Socks5Message(InputStream in) throws SocksException, IOException {
		this(in, true);
	}

	/**
	 * Initialises Message from the stream. Reads server response or client request from given stream.
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
	public Socks5Message(InputStream in, boolean clientMode) throws SocksException, IOException {
		read(in, clientMode);
	}

	/**
	 * Initialises Message from the stream. Reads server response from given stream.
	 * 
	 * @param in
	 *            Input stream to read response from.
	 * @throws SocksException
	 *             If server response code is not SOCKS_SUCCESS(0), or if any error with protocol occurs.
	 * @throws IOException
	 *             If any error happens with I/O.
	 */
	public void read(InputStream in) throws SocksException, IOException {
		read(in, true);
	}

	/**
	 * Initialises Message from the stream. Reads server response or client request from given stream.
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
	@SuppressWarnings("unused")
	public void read(InputStream in, boolean clientMode) throws SocksException, IOException {
		_data = null;
		_ip = null;

		DataInputStream di = new DataInputStream(in);

		_version = di.readUnsignedByte();
		_command = SocksCmd.createSocksCmd(di.readUnsignedByte());
		
		if (clientMode && _command.getValue() != 0)
			throw new SocksException(_command.getValue());

		int reserved = di.readUnsignedByte();
		_addrType = AddressType.getType(di.readUnsignedByte());

		byte addr[];

		if(_addrType == AddressType.SOCKS_ATYP_IPV4) {
			addr = new byte[4];
			di.readFully(addr);
			_host = bytes2IPV4(addr, 0);
			
		} else if(_addrType == AddressType.SOCKS_ATYP_IPV6) {
			addr = new byte[SOCKS_IPV6_LENGTH];// I believe it is 16 bytes,huge!
			di.readFully(addr);
			_host = bytes2IPV6(addr, 0);
			
		} else if(_addrType == AddressType.SOCKS_ATYP_DOMAINNAME) {
			// System.out.println("Reading ATYP_DOMAINNAME");
			addr = new byte[di.readUnsignedByte()];// Next byte shows the length
			di.readFully(addr);
			_host = new String(addr);
			
		} else {
			throw (new SocksException(Proxy.SOCKS_JUST_ERROR));
		}

		_port = di.readUnsignedShort();

		if (_addrType != AddressType.SOCKS_ATYP_DOMAINNAME && _doResolveIP) {
			try {
				_ip = InetAddress.getByName(_host);
			} catch (UnknownHostException uh_ex) {
			}
		}
	}

	/**
	 * Writes the message to the stream.
	 * 
	 * @param out
	 *            Output stream to which message should be written.
	 */
	public void write(OutputStream out) throws SocksException, IOException {
		if (_data == null) {
			Socks5Message msg;

			if (_addrType == AddressType.SOCKS_ATYP_DOMAINNAME) {
				msg = new Socks5Message(_command, _host, _port);
			} else {
				if (_ip == null) {
					try {
						_ip = InetAddress.getByName(_host);
					} catch (UnknownHostException uh_ex) {
						throw new SocksException(Proxy.SOCKS_JUST_ERROR);
					}
				}
				msg = new Socks5Message(_command, _ip, _port);
			}
			_data = msg._data;
		}
		out.write(_data);
	}

	/**
	 * Returns IP field of the message as IP, if the message was created with ATYP of HOSTNAME, it will attempt to
	 * resolve the hostname, which might fail.
	 * 
	 * @throws UnknownHostException
	 *             if host can't be resolved.
	 */
	public InetAddress getInetAddress() throws UnknownHostException {
		if (_ip != null) {
			return _ip;
		}

		return (_ip = InetAddress.getByName(_host));
	}

	/**
	 * Returns string representation of the message.
	 */
	public String toString() {
		String s = "Socks5Message:" + "\n" + "VN   " + _version + "\n" + "CMD  " + _command + "\n" + "ATYP " + _addrType
				+ "\n" + "ADDR " + _host + "\n" + "PORT " + _port + "\n";
		return s;
	}

	/**
	 * Whether to resolve hostIP returned from SOCKS server that is whether to create InetAddress object from the hostName
	 * string
	 */
	static public boolean resolveIP() {
		return _doResolveIP;
	}

	/**
	 * Whether to resolve hostIP returned from SOCKS server that is whether to create InetAddress object from the hostName
	 * string
	 * 
	 * @param doResolve
	 *            Whether to resolve hostIP from SOCKS server.
	 * @return Previous value.
	 */
//	static public boolean resolveIP(boolean doResolve) {
//		boolean old = _doResolveIP;
//		_doResolveIP = doResolve;
//		return old;
//	}

	/*
	 * private static final void debug(String s){ if(DEBUG) System.out.print(s); } private static final boolean DEBUG =
	 * false;
	 */

	// SOCKS5 constants
	public static final int SOCKS_VERSION = 5;
		
	public static final int SOCKS_IPV6_LENGTH = 16;

	private static boolean _doResolveIP = true;

}

class AddressType {
	int _value;
	
	public AddressType(int value) {
		_value = value;
	}

	public static final AddressType SOCKS_ATYP_IPV4 = new AddressType(0x1); // Where is 2??
	public static final AddressType SOCKS_ATYP_DOMAINNAME = new AddressType(0x3); // !!!!rfc1928
	public static final AddressType SOCKS_ATYP_IPV6 = new AddressType(0x4);	

	public int getValue() {
		return _value;
	}
	
	public static AddressType getType(int value) {
		if(value == SOCKS_ATYP_IPV4.getValue()) {
			return SOCKS_ATYP_IPV4;
		}
		
		if(value == SOCKS_ATYP_DOMAINNAME.getValue()) {
			return SOCKS_ATYP_DOMAINNAME;
		}
		
		if(value == SOCKS_ATYP_IPV6.getValue()) {
			return SOCKS_ATYP_IPV6;
		}
		
		return null;
	}
	
}


