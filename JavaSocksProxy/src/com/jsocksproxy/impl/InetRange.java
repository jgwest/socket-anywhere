/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class InetRange provides the means of defining the range of inetaddresses. It's used by Proxy class to store and look
 * up addresses of machines, that should be contacted directly rather than through the proxy.
 * <P>
 * InetRange provides several methods to add either standalone addresses, or ranges (e.g. 100.200.300.0:100.200.300.255,
 * which covers all addresses on on someones local network). It also provides methods for checking whether given address
 * is in this range. Any number of ranges and standalone addresses can be added to the range.
 */
public class InetRange implements Cloneable {

	Hashtable<String, AllEntry> _hostNames;
	Vector<AllEntry> _all;
	Vector<String> _endNames;

	boolean _useSeparateThread = true;

	/**
	 * Creates the empty range.
	 */
	public InetRange() {
		_all = new Vector<AllEntry>();
		_hostNames = new Hashtable<String, AllEntry>();
		_endNames = new Vector<String>();
	}

	/**
	 * Adds another host or range to this range. The String can be one of those:
	 * <UL>
	 * <li>Host name. eg.(Athena.myhost.com or 45.54.56.65)
	 * 
	 * <li>Range in the form .myhost.net.au <BR>
	 * In which case anything that ends with .myhost.net.au will be considered in the range.
	 * 
	 * <li>Range in the form ddd.ddd.ddd. <BR>
	 * This will be treated as range ddd.ddd.ddd.0 to ddd.ddd.ddd.255. It is not necessary to specify 3 first bytes you
	 * can use just one or two. For example 130. will cover address between 130.0.0.0 and 13.255.255.255.
	 * 
	 * <li>Range in the form host_from[: \t\n\r\f]host_to. <br>
	 * That is two hostnames or ips separated by either whitespace or colon.
	 * </UL>
	 */
	public synchronized boolean add(String s) {
		if (s == null)
			return false;

		s = s.trim();
		if (s.length() == 0)
			return false;

		AllEntry entry;

		if (s.charAt(s.length() - 1) == '.') {
			// thing like: 111.222.33.
			// it is being treated as range 111.222.33.000 - 111.222.33.255

			int[] addr = ip2intarray(s);
			long from, to;
			from = to = 0;

			if (addr == null)
				return false;
			for (int i = 0; i < 4; ++i) {
				if (addr[i] >= 0)
					from += (((long) addr[i]) << 8 * (3 - i));
				else {
					to = from;
					while (i < 4)
						to += 255l << 8 * (3 - i++);
					break;
				}
			}
			entry = new AllEntry(s, null, new Long(from), new Long(to));
			_all.addElement(entry);

		} else if (s.charAt(0) == '.') {
			// Thing like: .myhost.com

			_endNames.addElement(s);
			_all.addElement(new AllEntry(s, null, null, null ));
			
		} else {
			StringTokenizer tokens = new StringTokenizer(s, " \t\r\n\f:");
			if (tokens.countTokens() > 1) {
				entry = new AllEntry(s, null, null, null );
				resolve(entry, tokens.nextToken(), tokens.nextToken());
				_all.addElement(entry);
			} else {
				entry = new AllEntry( s, null, null, null );
				_all.addElement(entry);
				_hostNames.put(s, entry);
				resolve(entry);
			}

		}

		return true;
	}

	/**
	 * Adds another ip for this range.
	 * 
	 * @param ip
	 *            IP os the host which should be added to this range.
	 */
	public synchronized void add(InetAddress ip) {
		long from, to;
		from = to = ip2long(ip);
		_all.addElement(new AllEntry (ip.getHostName(), ip, new Long(from), new Long(to) ));
	}

	/**
	 * Adds another range of ips for this range.Any host with ip address greater than or equal to the address of from
	 * and smaller than or equal to the address of to will be included in the range.
	 * 
	 * @param from
	 *            IP from where range starts(including).
	 * @param to
	 *            IP where range ends(including).
	 */
	public synchronized void add(InetAddress from, InetAddress to) {
		_all.addElement(new AllEntry( from.getHostAddress() + ":" + to.getHostAddress(), null, new Long(ip2long(from)),
				new Long(ip2long(to)) ));
	}

	/**
	 * Checks whether the given host is in the range. Attempts to resolve host name if required.
	 * 
	 * @param host
	 *            Host name to check.
	 * @return true If host is in the range, false otherwise.
	 * @see InetRange#contains(String,boolean)
	 */
	public synchronized boolean contains(String host) {
		return contains(host, true);
	}

	/**
	 * Checks whether the given host is in the range.
	 * <P>
	 * Algorithm: <BR>
	 * <ol>
	 * <li>Look up if the hostname is in the range (in the Hashtable).
	 * <li>Check if it ends with one of the specified endings.
	 * <li>Check if it is ip(eg.130.220.35.98). If it is check if it is in the range.
	 * <li>If attemptResolve is true, host is name, rather than ip, and all previous attempts failed, try to resolve the
	 * hostname, and check whether the ip associated with the host is in the range.It also repeats all previous steps with
	 * the hostname obtained from InetAddress, but the name is not always the full name,it is quite likely to be the
	 * same. Well it was on my machine.
	 * </ol>
	 * 
	 * @param host
	 *            Host name to check.
	 * @param attemptResolve
	 *            Whether to lookup ip address which corresponds to the host,if required.
	 * @return true If host is in the range, false otherwise.
	 */
	public synchronized boolean contains(String host, boolean attemptResolve) {
		if (_all.size() == 0)
			return false; // Empty range

		host = host.trim();
		if (host.length() == 0)
			return false;

		if (checkHost(host))
			return true;
		if (checkHostEnding(host))
			return true;

		long l = host2long(host);
		if (l >= 0)
			return contains(l);

		if (!attemptResolve)
			return false;

		try {
			InetAddress ip = InetAddress.getByName(host);
			return contains(ip);
		} catch (UnknownHostException uhe) {

		}

		return false;
	}

	/**
	 * Checks whether the given ip is in the range.
	 * 
	 * @param ip
	 *            Address of the host to check.
	 * @return true If host is in the range, false otherwise.
	 */
	public synchronized boolean contains(InetAddress ip) {
		if (checkHostEnding(ip.getHostName()))
			return true;
		if (checkHost(ip.getHostName()))
			return true;
		return contains(ip2long(ip));
	}

	/**
	 * Get all entries in the range as strings. <BR>
	 * These strings can be used to delete entries from the range with remove function.
	 * 
	 * @return Array of entries as strings.
	 * @see InetRange#remove(String)
	 */
	public synchronized String[] getAll() {
		int size = _all.size();
		
		AllEntry entry;
		String all_names[] = new String[size];

		for (int i = 0; i < size; ++i) {
			entry = _all.elementAt(i);
			all_names[i] = entry._str;
		}
		return all_names;
	}

	/**
	 * Removes an entry from this range.<BR>
	 * 
	 * @param s
	 *            Entry to remove.
	 * @return true if successful.
	 */
	public synchronized boolean remove(String s) {
		Enumeration<AllEntry> en = _all.elements();
		while (en.hasMoreElements()) {
			AllEntry entry = en.nextElement();
			
			if (s.equals(entry._str)) {
				_all.removeElement(entry);
				_endNames.removeElement(s);
				_hostNames.remove(s);
				return true;
			}
		}
		return false;
	}

	/** Get string representation of this Range. */
	public String toString() {
		String all[] = getAll();
		if (all.length == 0)
			return "";

		String s = all[0];
		for (int i = 1; i < all.length; ++i)
			s += "; " + all[i];
		return s;
	}

	/** Creates a clone of this Object */
	@SuppressWarnings("unchecked")
	public Object clone() {
		InetRange new_range = new InetRange();
		new_range._all = (Vector<AllEntry>) _all.clone();
		new_range._endNames = (Vector<String>) _endNames.clone();
		new_range._hostNames = (Hashtable<String, AllEntry>) _hostNames.clone();
		return new_range;
	}

	// Private methods
	// ///////////////
	/**
	 * Same as previous but used internally, to avoid unnecessary conversion of IPs, when checking subranges
	 */
	private synchronized boolean contains(long ip) {
		Enumeration<AllEntry> en = _all.elements();
		while (en.hasMoreElements()) {
			AllEntry obj = en.nextElement();
			Long from = obj._from == null ? null : (Long)obj._from;
			Long to = obj._to == null ? null : (Long) obj._to;
			if (from != null && from.longValue() <= ip && to.longValue() >= ip)
				return true;

		}
		return false;
	}

	private boolean checkHost(String host) {
		return _hostNames.containsKey(host);
	}

	private boolean checkHostEnding(String host) {
		Enumeration<String> en = _endNames.elements();
		while (en.hasMoreElements()) {
			if (host.endsWith(en.nextElement()))
				return true;
		}
		return false;
	}

	private void resolve(AllEntry entry) {
		// First check if it's in the form ddd.ddd.ddd.ddd.
		long ip = host2long(entry._str);
		if (ip >= 0) {
			entry._from = entry._to = new Long(ip);
		} else {
			InetRangeResolver res = new InetRangeResolver(entry);
			res.resolve(_useSeparateThread);
		}
	}

	private void resolve(AllEntry entry, String from, String to) {
		long f, t;
		if ((f = host2long(from)) >= 0 && (t = host2long(to)) >= 0) {
			entry._from  = new Long(f);
			entry._to = new Long(t);
		} else {
			InetRangeResolver res = new InetRangeResolver(entry, from, to);
			res.resolve(_useSeparateThread);
		}
	}

	// Class methods
	// /////////////

	// Converts ipv4 to long value(unsigned int)
	// /////////////////////////////////////////
	static long ip2long(InetAddress ip) {
		long l = 0;
		byte[] addr = ip.getAddress();

		if (addr.length == 4) { // IPV4
			for (int i = 0; i < 4; ++i)
				l += (((long) addr[i] & 0xFF) << 8 * (3 - i));
		} else { // IPV6
			return 0; // Have no idea how to deal with those
		}
		return l;
	}

	long host2long(String host) {
		long ip = 0;

		// check if it's ddd.ddd.ddd.ddd
		if (!Character.isDigit(host.charAt(0)))
			return -1;

		int[] addr = ip2intarray(host);
		if (addr == null)
			return -1;

		for (int i = 0; i < addr.length; ++i)
			ip += ((long) (addr[i] >= 0 ? addr[i] : 0)) << 8 * (3 - i);

		return ip;
	}

	static int[] ip2intarray(String host) {
		int[] address = { -1, -1, -1, -1 };
		int i = 0;
		StringTokenizer tokens = new StringTokenizer(host, ".");
		if (tokens.countTokens() > 4)
			return null;
		while (tokens.hasMoreTokens()) {
			try {
				address[i++] = Integer.parseInt(tokens.nextToken()) & 0xFF;
			} catch (NumberFormatException nfe) {
				return null;
			}

		}
		return address;
	}

	/*
	 * //* This was the test main function //**********************************
	 * 
	 * public static void main(String args[])throws UnknownHostException{ int i;
	 * 
	 * InetRange ir = new InetRange();
	 * 
	 * 
	 * for(i=0;i<args.length;++i){ System.out.println("Adding:" + args[i]); ir.add(args[i]); }
	 * 
	 * String host; java.io.DataInputStream din = new java.io.DataInputStream(System.in); try{ host = din.readLine();
	 * while(host!=null){ if(ir.contains(host)){ System.out.println("Range contains ip:"+host); }else{
	 * System.out.println(host+" is not in the range"); } host = din.readLine(); } }catch(java.io.IOException io_ex){
	 * io_ex.printStackTrace(); } }******************
	 */

}

class InetRangeResolver implements Runnable {

//	Object[] entry;
	AllEntry entry;

	String from, to;

	InetRangeResolver(AllEntry entry) {
		this.entry = entry;
		from = to = null;
	}

	InetRangeResolver(AllEntry entry, String from, String to) {
		this.entry = entry;
		this.from = from;
		this.to = to;
	}

	public final void resolve() {
		resolve(true);
	}

	public final void resolve(boolean inSeparateThread) {
		if (inSeparateThread) {
			Thread t = new Thread(this);
			t.start();
		} else
			run();

	}

	public void run() {
		try {
			if (from == null) {
				InetAddress ip = InetAddress.getByName(entry._str);
				entry._inetAddr = ip;
				Long l = new Long(InetRange.ip2long(ip));
				entry._from = entry._to = l;
			} else {
				InetAddress f = InetAddress.getByName(from);
				InetAddress t = InetAddress.getByName(to);
				entry._from = new Long(InetRange.ip2long(f));
				entry._to = new Long(InetRange.ip2long(t));

			}
		} catch (UnknownHostException uhe) {
			// System.err.println("Resolve failed for "+from+','+to+','+entry[0]);
		}
	}

}

class AllEntry {
	
	public AllEntry(String str, InetAddress inetAddr, Long from, Long to) {
		_str = str;
		_inetAddr = inetAddr;
		_from = from;
		_to = to;
	}
	
	String _str;
	InetAddress _inetAddr;
	Long _from;
	Long _to;
}