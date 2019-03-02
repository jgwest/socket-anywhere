/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.runner;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import com.jsocksproxy.authentication.IdentAuthenticator;
import com.jsocksproxy.impl.InetRange;
import com.jsocksproxy.impl.ProxyServer;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Command line socks launcher */
public class SocksRunner {

	public static void printUsage() {
		System.out.println("Usage: java SocksRunner [inifile1 inifile2 ...]\n"
				+ "If none inifile is given, uses socks.properties.\n");
	}

	
	
	public static void run(TLAddress addr, String range, String users, int idleTimeout, OutputStream log, ISocketFactory serverSocketFactory, ISocketFactory clientSocketFactory) {
		
		IdentAuthenticator auth = new IdentAuthenticator();
//		OutputStream log = null;
		
		ProxyServer server = new ProxyServer(auth);
		
		if (!addAuthNew(auth, range, users)) {
			System.err.println("Error in range or users");
			printUsage();
			return;
		}
			
		inform("Using Ident Authentication scheme:\n" + auth + "\n");
		
		// For custom SocketAnywhere factory, replace this line
		server.setServerSocketFactory(serverSocketFactory);
		server.setClientSocketFactory(clientSocketFactory);
		
		server.setLog(log);
		server.start(addr);
		
	}
	
	public static void main(String[] args) {
		
		String host = "localhost";
		int port = 1080; 
		String range = "localhost"; 
		String users = null;
		int idleTimeout = 180 * 1000;
		String logFilel = "-";
		
		ISocketFactory clientSocketFactory = new TCPSocketFactory();
		ISocketFactory serverSocketFactory = new TCPSocketFactory();
		
		run(new TLAddress(host, port), range, users, idleTimeout, System.out, serverSocketFactory, clientSocketFactory);

	}
	
	
	@SuppressWarnings("unused")
	private static void standaloneRun(String[] args) {
		String[] file_names;
		int port = 1080;
		String logFile = null;
		String host = null;

//		UserPasswordAuthenticator upaAuth = new UserPasswordAuthenticator(new IUserValidation() {
//			
//			@Override
//			public boolean isUserValid(String username, String password, ISocketTL connection) {
//				return true;
//			}
//			
//			@Override
//			public boolean isUserValid(String username, String password, Socket connection) {
//				return true;
//			}
//		});
		

		
		
		IdentAuthenticator auth = new IdentAuthenticator();
		OutputStream log = null;
		
		ProxyServer server = new ProxyServer(auth);
		
		InetAddress localIP = null;

		if (args.length == 0) {
			file_names = new String[1];
			file_names[0] = "socks.properties";
		} else {
			file_names = args;
		}

		inform("Loading properties");
		for (int i = 0; i < file_names.length; ++i) {

			inform("Reading file " + file_names[i]);

			Properties pr = loadProperties(file_names[i]);
			if (pr == null) {
				System.err.println("Loading of properties from " + file_names[i] + " failed.");
				printUsage();
				return;
			}
			if (!addAuth(auth, pr)) {
				System.err.println("Error in file " + file_names[i] + ".");
				printUsage();
				return;
			}
			// First file should contain all global settings,
			// like port and host and log.
			if (i == 0) {
				String port_s = (String) pr.get("port");
				if (port_s != null)
					try {
						port = Integer.parseInt(port_s);
					} catch (NumberFormatException nfe) {
						System.err.println("Can't parse port: " + port_s);
						return;
					}

				serverInit(pr, server);
				logFile = (String) pr.get("log");
				host = (String) pr.get("host");
			}

			// inform("Props:"+pr);
		}

		if (logFile != null) {
			if (logFile.equals("-")) {
				log = System.out;
			} else {
				try {
					log = new FileOutputStream(logFile);
				} catch (IOException ioe) {
					System.err.println("Can't open log file " + logFile);
					return;
				}
			}
		}
		
		if (host != null) {
			try {
				localIP = InetAddress.getByName(host);
			} catch (UnknownHostException uhe) {
				System.err.println("Can't resolve local ip: " + host);
				return;
			}
		}

		inform("Using Ident Authentication scheme:\n" + auth + "\n");
		
		
		
		// For custom SocketAnywhere factory, replace this line
		server.setServerSocketFactory(new TCPSocketFactory());
		server.setClientSocketFactory(new TCPSocketFactory());
		
		server.setLog(log);
		server.start(new TLAddress(localIP.getHostAddress(), port));
	}

	private static Properties loadProperties(String file_name) {

		Properties pr = new Properties();

		try {
			InputStream fin = new FileInputStream(file_name);
			pr.load(fin);
			fin.close();
		} catch (IOException ioe) {
			return null;
		}
		return pr;
	}

	
	/** Read values from property file, and insert into the IdentAuthenticator*/
	private static boolean addAuthNew(IdentAuthenticator ident, String range, String users) {

		InetRange irange;

		if (range == null) {
			return false;
		}
		irange = parseInetRange(range);

		if (users == null) {
			ident.add(irange, null);
			return true;
		}

		Hashtable<String, String> uhash = new Hashtable<String, String>();

		StringTokenizer st = new StringTokenizer(users, ";");
		while (st.hasMoreTokens()) {
			uhash.put(st.nextToken(), "");
		}

		ident.add(irange, uhash);
		return true;
	}

	
	/** Read values from property file, and insert into the IdentAuthenticator*/
	private static boolean addAuth(IdentAuthenticator ident, Properties pr) {

		InetRange irange;

		String range = (String) pr.get("range");
		if (range == null) {
			return false;
		}
		irange = parseInetRange(range);

		String users = (String) pr.get("users");
		if (users == null) {
			ident.add(irange, null);
			return true;
		}

		Hashtable<String, String> uhash = new Hashtable<String, String>();

		StringTokenizer st = new StringTokenizer(users, ";");
		while (st.hasMoreTokens())
			uhash.put(st.nextToken(), "");

		ident.add(irange, uhash);
		return true;
	}

	/**
	 * Does server initialization.
	 */
	private static void serverInit(Properties props, ProxyServer server) {
		int val;
		val = readInt(props, "idleTimeout");
		if (val >= 0) {
			server.setIdleTimeout(val);
			inform("Setting idle timeout to " + val + " ms.");
		}

	}


	/**
	 * Inits range from the string of semicolon separated ranges.
	 */
	private static InetRange parseInetRange(String source) {
		InetRange irange = new InetRange();

		StringTokenizer st = new StringTokenizer(source, ";");
		while (st.hasMoreTokens())
			irange.add(st.nextToken());

		return irange;
	}

	/**
	 * Integer representation of the property named name, or -1 if one is not found.
	 */
	private static int readInt(Properties props, String name) {
		int result = -1;
		String val = (String) props.get(name);
		if (val == null)
			return -1;
		StringTokenizer st = new StringTokenizer(val);
		if (!st.hasMoreElements())
			return -1;
		try {
			result = Integer.parseInt(st.nextToken());
		} catch (NumberFormatException nfe) {
			inform("Bad value for " + name + ":" + val);
		}
		return result;
	}

	// Display functions
	// /////////////////

	private static void inform(String s) {
		System.out.println(s);
	}

	private static void exit(String msg) {
		System.err.println("Error:" + msg);
		System.err.println("Aborting operation");
		System.exit(0);
	}
}
