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
import java.net.InetAddress;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.jsocksproxy.impl.InetRange;
import com.jsocksproxy.impl.ProxyMessage;


/**
 * An implementation of socks.ServerAuthentication which provides simple authentication based on the host from which the
 * connection is made and the name of the user on the remote machine, as reported by identd daemon on the remote
 * machine.
 * <p>
 * It can also be used to provide authentication based only on the contacting host address.
 */

public class IdentAuthenticator extends ServerAuthenticatorNone {
	/** Vector of InetRanges */
	Vector<InetRange> _hosts;

	/** Vector of user hashes */
	Vector<Hashtable<String, String>> _users;

	String _user;

	/**
	 * Constructs empty IdentAuthenticator.
	 */
	public IdentAuthenticator() {
		_hosts = new Vector<InetRange>();
		_users = new Vector<Hashtable<String, String>>();
	}

	/**
	 * Used to create instances returned from startSession.
	 * 
	 * @param in
	 *            Input stream.
	 * @param out
	 *            OutputStream.
	 * @param user
	 *            Username associated with this connection,could be null if name was not required.
	 */
	IdentAuthenticator(InputStream in, OutputStream out, String user) {
		super(in, out);
		this._user = user;
	}

	/**
	 * Adds range of addresses from which connection is allowed. Hashtable users should contain user names as keys and
	 * anything as values (value is not used and will be ignored).
	 * 
	 * @param hostRange
	 *            Range of ip addresses from which connection is allowed.
	 * @param users
	 *            Hashtable of users for whom connection is allowed, or null to indicate that anybody is allowed to
	 *            connect from the hosts within given range.
	 */
	public synchronized void add(InetRange hostRange, Hashtable<String, String> users) {
		this._hosts.addElement(hostRange);
		this._users.addElement(users);
	}

	/**
	 * Grants permission only to those users, who connect from one of the hosts registered with add(InetRange,Hashtable)
	 * and whose names, as reported by identd daemon, are listed for the host the connection came from.
	 */
	public IServerAuthenticator startSession(Socket s) throws IOException {

		int ind = getRangeIndex(s.getInetAddress());
		String user = null;

		// System.out.println("getRangeReturned:"+ind);

		if (ind < 0)
			return null; // Host is not on the list.

		ServerAuthenticatorNone auth = (ServerAuthenticatorNone) super.startSession(s);

		// System.out.println("super.startSession() returned:"+auth);
		if (auth == null)
			return null;

		// do the authentication

		Hashtable<String, String> user_names = _users.elementAt(ind);

		if (user_names != null) { // If need to do authentication
			Ident ident;
			ident = new Ident(s);
			// If can't obtain user name, fail
			if (!ident._successful)
				return null;
			// If user name is not listed for this address, fail
			if (!user_names.containsKey(ident._userName))
				return null;
			user = ident._userName;
		}
		return new IdentAuthenticator(auth.getInputStream(), auth.getOutputStream(), user);

	}

	/**
	 * For SOCKS5 requests always returns true. For SOCKS4 requests checks whether the user name supplied in the request
	 * corresponds to the name obtained from the ident daemon.
	 */
	public boolean checkRequest(ProxyMessage msg, Socket s) {
		// If it's version 5 request, or if anybody is permitted, return true;
		if (msg.getVersion() == 5 || _user == null)
			return true;

		if (msg.getVersion() != 4)
			return false; // Who knows?

		return _user.equals(msg.getUser());
	}

	/** Get String representation of the IdentAuthenticator. */
	public String toString() {
		String s = "";

		for (int i = 0; i < _hosts.size(); ++i)
			s += "Range:" + _hosts.elementAt(i) + "\nUsers:" + userNames(i) + "\n";
		return s;
	}

	// Private Methods
	// ////////////////
	private int getRangeIndex(InetAddress ip) {
		int index = 0;
		Enumeration<InetRange> en = _hosts.elements();
		while (en.hasMoreElements()) {
			InetRange ir = en.nextElement();
			if (ir.contains(ip))
				return index;
			index++;
		}
		return -1; // Not found
	}

	private String userNames(int i) {
		if (_users.elementAt(i) == null)
			return "Everybody is permitted.";

		Enumeration<String> en = _users.elementAt(i).keys();
		if (!en.hasMoreElements())
			return "";
		String s = en.nextElement().toString();
		while (en.hasMoreElements())
			s += "; " + en.nextElement();

		return s;
	}

}
