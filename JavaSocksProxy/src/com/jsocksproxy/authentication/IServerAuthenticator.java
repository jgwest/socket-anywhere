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
import java.net.DatagramPacket;
import java.net.Socket;

import com.jsocksproxy.impl.ProxyMessage;
import com.socketanywhere.net.ISocketTL;

/**
 * Classes implementing this interface should provide socks server with authentication and authorization of users.
 **/
public interface IServerAuthenticator {

	/**
	 * This method is called when a new connection accepted by the server.
	 * <p>
	 * At this point no data have been extracted from the connection. It is responsibility of this method to ensure that
	 * the next byte in the stream after this method have been called is the first byte of the socks request message.
	 * For SOCKSv4 there is no authentication data and the first byte in the stream is part of the request. With SOCKSv5
	 * however there is an authentication data first. It is expected that implementations will process this
	 * authentication data.
	 * <p>
	 * If authentication was successful an instance of ServerAuthentication should be returned, it later will be used by
	 * the server to perform authorization and some other things. If authentication fails null should be returned, or an
	 * exception may be thrown.
	 * 
	 * @param s
	 *            Accepted Socket.
	 * @return An instance of ServerAuthenticator to be used for this connection or null
	 */
	IServerAuthenticator startSession(Socket s) throws IOException;
	
	IServerAuthenticator startSession(ISocketTL s) throws IOException;
	

	/**
	 * This method should return input stream which should be used on the accepted socket.
	 * <p>
	 * SOCKSv5 allows to have multiple authentication methods, and these methods might require some kind of
	 * transformations being made on the data.
	 * <p>
	 * This method is called on the object returned from the startSession function.
	 */
	InputStream getInputStream();

	/**
	 * This method should return output stream to use to write to the accepted socket.
	 * <p>
	 * SOCKSv5 allows to have multiple authentication methods, and these methods might require some kind of
	 * transformations being made on the data.
	 * <p>
	 * This method is called on the object returned from the startSession function.
	 */
	OutputStream getOutputStream();


	/**
	 * This method is called when a request have been read.
	 * <p>
	 * Implementation should decide wether to grant request or not. Returning true implies granting the request, false
	 * means request should be rejected.
	 * <p>
	 * This method is called on the object returned from the startSession function.
	 * 
	 * @param msg
	 *            Request message.
	 * @return true to grant request, false to reject it.
	 */
	boolean checkRequest(ProxyMessage msg);

	/**
	 * This method is called when datagram is received by the server.
	 * <p>
	 * Implementations should decide whether it should be forwarded or dropped. It is expected that implementation will
	 * use datagram address and port information to make a decision, as well as anything else. Address and port of the
	 * datagram are always correspond to remote machine. It is either destination or source address. If out is true
	 * address is destination address, else it is a source address, address of the machine from which datagram have been
	 * received for the client.
	 * <p>
	 * Implementations should return true if the datagram is to be forwarded, and false if the datagram should be
	 * dropped.
	 * <p>
	 * This method is called on the object returned from the startSession function.
	 * 
	 * @param out
	 *            If true the datagram is being send out(from the client), otherwise it is an incoming datagram.
	 * @return True to forward datagram false drop it silently.
	 */
	boolean checkRequest(DatagramPacket dp, boolean out);

	/**
	 * This method is called when session is completed. Either due to normal termination or due to any error condition.
	 * <p>
	 * This method is called on the object returned from the startSession function.
	 */
	void endSession();
}
