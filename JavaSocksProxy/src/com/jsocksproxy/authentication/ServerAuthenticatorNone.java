/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.authentication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import com.jsocksproxy.impl.ProxyMessage;
import com.socketanywhere.net.ISocketTL;


/**
 * An implementation of ServerAuthenticator, which does <b>not</b> do any authentication.
 * <P>
 * <FONT size="+3" color ="FF0000"> Warning!!</font><br>
 * Should not be used on machines which are not behind the firewall.
 * <p>
 * It is only provided to make implementing other authentication schemes easier.<br>
 * For Example: <tt><pre>
   class MyAuth extends socks.server.ServerAuthenticator{
    ...
    public ServerAuthenticator startSession(Socket s){
      if(!checkHost(s.getInetAddress()) return null;
      return super.startSession(s);
    }

    boolean checkHost(java.net.Inetaddress addr){
      boolean allow;
      //Do it somehow
      return allow;
    }
   }
</pre></tt>
 */
public class ServerAuthenticatorNone implements IServerAuthenticator {

	static final byte[] socks5response = { 5, 0 };

	private InputStream _in;
	private OutputStream _out;

	/**
	 * Creates new instance of the ServerAuthenticatorNone.
	 */
	public ServerAuthenticatorNone() {
		this._in = null;
		this._out = null;
	}

	/**
	 * Constructs new ServerAuthenticatorNone object suitable for returning from the startSession function.
	 * 
	 * @param in
	 *            Input stream to return from getInputStream method.
	 * @param out
	 *            Output stream to return from getOutputStream method.
	 */
	public ServerAuthenticatorNone(InputStream in, OutputStream out) {
		this._in = in;
		this._out = out;
	}

	/**
	 * Grants access to everyone.Removes authentication related bytes from the stream, when a SOCKS5 connection is being
	 * made, selects an authentication NONE.
	 */
	public IServerAuthenticator startSession(Socket s) throws IOException {

		PushbackInputStream in = new PushbackInputStream(s.getInputStream());
		OutputStream out = s.getOutputStream();

		int version = in.read();
		if (version == 5) {
			if (!selectSocks5Authentication(in, out, 0)) {
				return null;
			}
		} else if (version == 4) {
			// Else it is the request message already, version 4
			in.unread(version);
		} else {
			return null;
		}

		return new ServerAuthenticatorNone(in, out);
	}

	/**
	 * Get input stream.
	 * 
	 * @return Input stream specified in the constructor.
	 */
	public InputStream getInputStream() {
		return _in;
	}

	/**
	 * Get output stream.
	 * 
	 * @return Output stream specified in the constructor.
	 */
	public OutputStream getOutputStream() {
		return _out;
	}

	/**
	 * Always returns true.
	 */
	public boolean checkRequest(ProxyMessage msg) {
		return true;
	}

	/**
	 * Always returns true.
	 */
	public boolean checkRequest(java.net.DatagramPacket dp, boolean out) {
		return true;
	}

	/**
	 * Does nothing.
	 */
	public void endSession() {
	}

	/**
	 * Convenience routine for selecting SOCKSv5 authentication.
	 * <p>
	 * This method reads in authentication methods that client supports, checks whether it supports given method. If it
	 * does, the notification method is written back to client, that this method have been chosen for authentication. If
	 * given method was not found, authentication failure message is send to client ([5,FF]).
	 * 
	 * @param in
	 *            Input stream, version byte should be removed from the stream before calling this method.
	 * @param out
	 *            Output stream.
	 * @param methodId
	 *            Method which should be selected.
	 * @return true if methodId was found, false otherwise.
	 */
	static public boolean selectSocks5Authentication(InputStream in, OutputStream out, int methodId) throws IOException {

		int num_methods = in.read();
		if (num_methods <= 0) {
			return false;
		}
		
		byte method_ids[] = new byte[num_methods];
		byte response[] = new byte[2];
		boolean found = false;

		response[0] = (byte) 5; // SOCKS version
		response[1] = (byte) 0xFF; // Not found, we are pessimistic

		int bread = 0; // bytes read so far
		while (bread < num_methods) {
			bread += in.read(method_ids, bread, num_methods - bread);
		}

		for (int i = 0; i < num_methods; ++i) {
			if (method_ids[i] == methodId) {
				found = true;
				response[1] = (byte) methodId;
				break;
			}
		}

		out.write(response);
		return found;
	}

	@Override
	public IServerAuthenticator startSession(ISocketTL s) throws IOException {
		PushbackInputStream in = new PushbackInputStream(s.getInputStream());
		OutputStream out = s.getOutputStream();

		int version = in.read();
		if (version == 5) {
			if (!selectSocks5Authentication(in, out, 0)) {
				return null;
			}
		} else if (version == 4) {
			// Else it is the request message already, version 4
			in.unread(version);
		} else {
			return null;
		}

		return new ServerAuthenticatorNone(in, out);
	}
	
}
