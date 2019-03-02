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
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;

import com.jsocksproxy.authentication.IServerAuthenticator;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/**
 * SOCKS4 and SOCKS5 proxy, handles both protocols simultaneously
 * 
 * <p>
 * In order to use it you will need to implement ServerAuthenticator interface. There is an implementation of this
 * interface which does no authentication ServerAuthenticatorNone, but it is very dangerous to use, as it will give
 * access to your local network to anybody in the world. One should never use this authentication scheme unless one have
 * pretty good reason to do so. There is a couple of other authentication schemes in socks.server package.
 * 
 * @see com.jsocksproxy.authentication.IServerAuthenticator
 */
public class ProxyServer implements Runnable {

	private IServerAuthenticator _auth;
	private ProxyMessage _msg = null;

	private ISocketTL _sock = null;
	private ISocketTL _remoteSock = null;

	
	private ISocketFactory _serverSocketFactory = null;	
	private ISocketFactory _clientSocketFactory = null;
	
	
	/** This server socket listens on port 1080 (by default) */
	private IServerSocketTL _proxyPortServerSocket = null;
	
	private InputStream _in;
	private OutputStream _out;

	private PipeThread pt1;
	private PipeThread pt2;


	private int _idleTimeout = 180000; // 3 minutes

	private PrintStream _log = null;

	// Public Constructors
	// ///////////////////

	/**
	 * Creates a proxy server with given Authentication scheme.
	 * 
	 * @param auth
	 *            Authentication scheme to be used.
	 */
	public ProxyServer(IServerAuthenticator auth) {
		this._auth = auth;
	}

	// Other constructors
	// //////////////////

	private ProxyServer(IServerAuthenticator auth, ISocketTL s) {
		this._auth = auth;
		this._sock = s;
	}

	// Public methods
	// ///////////////

	public void setClientSocketFactory(ISocketFactory clientSocketFactory) {
		this._clientSocketFactory = clientSocketFactory;
	}
	
	
	public void setServerSocketFactory(ISocketFactory _serverSocketFactory) {
		this._serverSocketFactory = _serverSocketFactory;
	}
	
	/**
	 * Set the logging stream. Specifying null disables logging.
	 */
	public void setLog(OutputStream out) {
		if (out == null) {
			_log = null;
		} else {
			_log = new PrintStream(out, true);
		}
	}


	/**
	 * Sets the timeout for connections, how long should server wait for data to arrive before dropping the connection.<br>
	 * Zero timeout implies infinity.<br>
	 * Default timeout is 3 minutes.
	 */
	public void setIdleTimeout(int timeout) {
		_idleTimeout = timeout;
	}

	/**
	 * Start the Proxy server at given port.<br>
	 * This methods blocks.
	 */
//	public void start(int port) {
//		start(port, null);
//	}

	/**
	 * Create a server with the specified port, listen backlog, and local IP address to bind to. The localIP argument
	 * can be used on a multi-homed host for a ServerSocket that will only accept connect requests to one of its
	 * addresses. If localIP is null, it will default accepting connections on any/all local addresses. The port must be
	 * between 0 and 65535, inclusive. <br>
	 * This methods blocks.
	 */
	public void start(TLAddress addr) {
		try {

			_proxyPortServerSocket = _serverSocketFactory.instantiateServerSocket(addr);				
			
//			if(addr.getHostname() != null) {
//			} else {
//				_proxyPortServerSocket = _serverSocketFactory.instantiateServerSocket(new TLAddress(port)); //new ServerSocket(port, backlog, localIP);	
//			}
			
			log("Starting SOCKS Proxy on: " + _proxyPortServerSocket.getInetAddress() .getHostname()+ ":" + _proxyPortServerSocket.getInetAddress().getPort());
			while (true) {
				ISocketTL s = _proxyPortServerSocket.accept();
				log("Accepted from: " + s.getAddress());
				
				ProxyServer ps = new ProxyServer(_auth, s);
				ps.setIdleTimeout(_idleTimeout);
				ps.setLog(_log);
				ps.setClientSocketFactory(_clientSocketFactory);
				ps.setServerSocketFactory(_serverSocketFactory);
				
				(new Thread(ps)).start();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
		}
	}

	/**
	 * Stop server operation.It would be wise to interrupt thread running the server afterwards.
	 */
	public void stop() {
		try {
//			}
			if(_proxyPortServerSocket != null) {
				_proxyPortServerSocket.close();
			}
		} catch (IOException ioe) {
		}
	}

	// Runnable interface
	// //////////////////
	public void run() {
		try {
			startSession();
		} catch (IOException ioe) {
			handleException(ioe);
		} finally {
			log("Main thread(client->remote)stopped.");
		}
	}

	// Private methods
	// ///////////////
	private void startSession() throws IOException {
//		_sock.setSoTimeout(_idleTimeout);

		try {
			_auth = _auth.startSession(_sock);
		} catch (IOException ioe) {
			log("Auth throwed exception:" + ioe);
			_auth = null;
			return;
		}

		if (_auth == null) { // Authentication failed
			log("Authentication failed");
			return;
		}

		_in = _auth.getInputStream();
		_out = _auth.getOutputStream();

		_msg = readMsg(_in);
		handleRequest(_msg);
	}

	private void handleRequest(ProxyMessage msg) throws IOException {
		if (!_auth.checkRequest(msg)) {
			throw new SocksException(Proxy.SOCKS_FAILURE);
		}

		if (msg.getIP() == null) {
			if (msg instanceof Socks5Message) {
				msg.setIP(InetAddress.getByName(msg.getHost()));
			} else
				throw new SocksException(Proxy.SOCKS_FAILURE);
		}
		log(msg);

		
		if(msg.getCommand().equals(SocksCmd.SOCKS_CMD_CONNECT)) {
			onConnect(msg);
			
		} else {
			throw new SocksException(Proxy.SOCKS_CMD_NOT_SUPPORTED);
		}
		
	}

	private void handleException(IOException ioe) {
		// If we couldn't read the request, return;
		if (_msg == null) {
			return;
		}
		
		int error_code = Proxy.SOCKS_FAILURE;

		if (ioe instanceof SocksException) {
			error_code = ((SocksException) ioe)._errCode;
		
		} else if (ioe instanceof NoRouteToHostException) {
			error_code = Proxy.SOCKS_HOST_UNREACHABLE;
		
		} else if (ioe instanceof ConnectException) {
			error_code = Proxy.SOCKS_CONNECTION_REFUSED;
		
		} else if (ioe instanceof InterruptedIOException) {
			error_code = Proxy.SOCKS_TTL_EXPIRE;
		}

		if (error_code > Proxy.SOCKS_ADDR_NOT_SUPPORTED || error_code < 0) {
			error_code = Proxy.SOCKS_FAILURE;
		}

		sendErrorMessage(error_code);
	}

	private void onConnect(ProxyMessage msg) throws IOException {
		ISocketTL s;
		ProxyMessage response = null;

		s =  _clientSocketFactory.instantiateSocket(new TLAddress(msg.getIP().getHostAddress(), msg.getPort()));  /// new Socket(msg.getIP(), msg.getPort());
		
		log("Connected to " + s.getAddress().getHostname()+" "+s.getAddress().getPort());

		if (msg instanceof Socks5Message) {
			response = new Socks5Message(SocksCmd.createSocksCmd(Proxy.SOCKS_SUCCESS), (InetAddress)null, 0);
//			response = new Socks5Message(SocksCmd.createSocksCmd(Proxy.SOCKS_SUCCESS), s.getLocalAddress(), s.getLocalPort());
		} else {
			response = new Socks4Message(SocksCmd.createSocksCmd(Socks4Message.REPLY_OK), (InetAddress)null, 0);
//			response = new Socks4Message(SocksCmd.createSocksCmd(Socks4Message.REPLY_OK), s.getLocalAddress(), s.getLocalPort());

		}
		response.write(_out);
		startPipe(s);
	}

	// Private methods
	// ////////////////

	private ProxyMessage readMsg(InputStream in) throws IOException {
		PushbackInputStream push_in;
		if (in instanceof PushbackInputStream) {
			push_in = (PushbackInputStream) in;
		} else {
			push_in = new PushbackInputStream(in);
		}

		int version = push_in.read();
		push_in.unread(version);

		ProxyMessage msg;

		if (version == 5) {
			msg = new Socks5Message(push_in, false);
		} else if (version == 4) {
			msg = new Socks4Message(push_in, false);
		} else {
			throw new SocksException(Proxy.SOCKS_FAILURE);
		}
		return msg;
	}

	private void startPipe(ISocketTL s) throws IOException {
		_remoteSock = s;
		
		pt1 = new PipeThread(_in, s.getOutputStream(), _log, this);
		pt2 = new PipeThread(s.getInputStream(), _out, _log, this);

		pt1.start();
		pt2.start();

		// end the thread after this point
			
	}

	private void sendErrorMessage(int error_code) {
		ProxyMessage err_msg;
		
		if (_msg instanceof Socks4Message) {
			err_msg = new Socks4Message(SocksCmd.createSocksCmd(Socks4Message.REPLY_REJECTED));
		} else {
			err_msg = new Socks5Message(SocksCmd.createSocksCmd(error_code));
		}
		
		try {
			err_msg.write(_out);
		} catch (IOException ioe) { }
	}

	protected synchronized void abort() {
		
		if (_remoteSock != null) {
			try {
				_remoteSock.close();
			} catch (IOException e) {
//				e.printStackTrace();
			}
			_remoteSock = null;
		}
		
		if (_sock != null) {
			try {
				_sock.close();
			} catch (IOException e) {
//				e.printStackTrace();
			}
			_sock = null;
			log("Aborting operation");
		}
		
		if (_proxyPortServerSocket != null) {
			try {
				_proxyPortServerSocket.close();
			} catch (IOException e) {
//				e.printStackTrace();
			}
			_proxyPortServerSocket = null;
		}

		if(pt1 != null) {
			pt1.interrupt();
		}
		
		if(pt2 != null) {
			pt2.interrupt();
		}
						
	}

	final void log(String s) {
		if (_log != null) {
			_log.println(s);
			_log.flush();
		}
	}

	final void log(ProxyMessage msg) {
		log("Request version:" + msg.getVersion() + "\tCommand: " + command2String(msg.getCommand()));
		log("IP:" + msg.getIP() + "\tPort:" + msg.getPort() + (msg.getVersion() == 4 ? "\tUser:" + msg.getUserForSocks4() : ""));
	}


	private static final String command_names[] = { "CONNECT", "BIND", "UDP_ASSOCIATE" };

	private static final String command2String(SocksCmd cmd) {
		
		if (cmd.getValue() > 0 && cmd.getValue() < 4) {
			return command_names[cmd.getValue() - 1];
		} else {
			return "Unknown Command " + cmd;
		}
	}
}


class PipeThread extends Thread {
	private static final int BUF_SIZE = 32 * 1024;

	private long _lastReadTime;

	private int _idleTimeout = 180000; // 3 minutes

	private final InputStream _in;
	private final OutputStream _out;
	
	
	private final PrintStream _log;
	
	private final ProxyServer _parent;
	
	public PipeThread(InputStream in, OutputStream out, PrintStream log, ProxyServer parent) {
		_in = in;
		_out = out;
		_log = log;
		_parent = parent;
	}	
	
	@Override
	public void run() {
		
		try {
			
			pipe(_in, _out);			
		} catch (IOException e) {
//			e.printStackTrace();
		} finally {
			abort();
		}
		
	}

	
	
	/** Read from InputStream, write to OutputStream, until a read on InputStream returns -1 */
	private void pipe(InputStream in, OutputStream out) throws IOException {
		
		_lastReadTime = System.currentTimeMillis();
		byte[] buf = new byte[BUF_SIZE];
		int len = 0;
		
		while (len >= 0) {
			try {
				if (len != 0) {
					out.write(buf, 0, len);
					out.flush();
				}
				len = in.read(buf);
				_lastReadTime = System.currentTimeMillis();
			
			} catch (InterruptedIOException iioe) {
			
				if (_idleTimeout == 0) {
					return;// Other thread interrupted us.
				}
				
				long timeSinceRead = System.currentTimeMillis() - _lastReadTime;
				if (timeSinceRead >= _idleTimeout - 1000) { // -1s for adjustment.
					return;
				}
				
				len = 0;

			}
		}
	}

	private synchronized void abort() {
//		log("Aborting operation");
		_parent.abort();
	}

	final void log(String s) {
		if (_log != null) {
			_log.println(s);
			_log.flush();
		}
	}

}
