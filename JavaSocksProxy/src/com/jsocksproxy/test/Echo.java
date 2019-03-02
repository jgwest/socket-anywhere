/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/** Plain SOCKS unaware echo client. */

public class Echo implements Runnable {

//	private int port;
	private InetAddress _peerIp;

	private Socket _ss;
	private InputStream _in;
	private OutputStream _out;

//	private static final int BUF_SIZE = 1024;

	public Echo(String host, int port, String peerHost, int peerPort) throws IOException, UnknownHostException {
		this._peerIp = InetAddress.getByName(peerHost);
//		this.port = port;

		_ss = new Socket(host, port, _peerIp, peerPort);
		_out = _ss.getOutputStream();
		_in = _ss.getInputStream();
		System.out.println("Connected...");
		System.out.println("TO: " + host + ":" + port);
		System.out.println("LocalAddress: " + _ss.getLocalAddress().getHostAddress() + ":" + _ss.getLocalPort());

	}

	public Echo(String host, int port) throws IOException, UnknownHostException {

		System.out.println("Connecting...");
		_ss = new Socket(host, port);
		_out = _ss.getOutputStream();
		_in = _ss.getInputStream();
		System.out.println("TO: " + host + ":" + port);
		System.out.println("LocalAddress: " + _ss.getLocalAddress().getHostAddress() + ":" + _ss.getLocalPort());

	}

	public void send(String s) throws IOException {
		// System.out.println("Sending:"+s);
		_out.write(s.getBytes());
	}

	public void run() {
		byte[] buf = new byte[1024];
		int bytes_read;
		try {
			while ((bytes_read = _in.read(buf)) > 0) {
				System.out.write(buf, 0, bytes_read);
				System.out.flush();
			}
		} catch (IOException io_ex) {
			io_ex.printStackTrace();
		}
	}

	public static void usage() {
		System.err.print("Usage: java Echo host port [peerHost peerPort]\n");
	}

	public static void main(String args[]) {
		int port;
		String host, peerHost;
		int peerPort;
		Echo echo = null;

		if (args.length > 1) {
			try {

				host = args[0];
				port = Integer.parseInt(args[1]);

				if (args.length == 4) {
					peerHost = args[2];
					peerPort = Integer.parseInt(args[3]);
					echo = new Echo(host, port, peerHost, peerPort);
				} else {
					echo = new Echo(host, port);
				}

				Thread thread = new Thread(echo);
				thread.start();

				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				String s;

				s = in.readLine();
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
				while (s != null) {
					echo.send(s + "\r\n");
					s = in.readLine();
				}
			} catch (IOException io_ex) {
				io_ex.printStackTrace();
				System.exit(1);
			} catch (NumberFormatException num_ex) {
				usage();
				num_ex.printStackTrace();
				System.exit(1);
			} finally {
				if (echo != null)
					try {
						echo._ss.close();
					} catch (Exception e) {
					}
			}

		} else {
			usage();
		}
	}

}// End of class
