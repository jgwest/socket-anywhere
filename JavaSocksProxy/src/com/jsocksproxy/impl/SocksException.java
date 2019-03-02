/**
 * Java SOCKS Proxy code originally from http://sourceforge.net/projects/jsocks/
 * Listed Developers: Kirill Kouzoubov, Robert Simac (2010)
 * 
 * Licensed by original developers under the GNU Library or Lesser General Public License (LGPL).
 * 
 * Recent contributions by Jonathan West, 2012. 
 */

package com.jsocksproxy.impl;

/**
 * Exception thrown by various socks classes to indicate errors with protocol or unsuccessful server responses.
 */
public class SocksException extends java.io.IOException {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct a SocksException with given errorcode.
	 * <p>
	 * Tries to look up message which corresponds to this error code.
	 * 
	 * @param errCode
	 *            Error code for this exception.
	 */
	public SocksException(int errCode) {
		this._errCode = errCode;
		if ((errCode >> 16) == 0) {
			// Server reply error message
			_errString = errCode <= serverReplyMessage.length ? serverReplyMessage[errCode] : UNASSIGNED_ERROR_MESSAGE;
		} else {
			// Local error
			errCode = (errCode >> 16) - 1;
			_errString = errCode <= localErrorMessage.length ? localErrorMessage[errCode] : UNASSIGNED_ERROR_MESSAGE;
		}
	}

	/**
	 * Constructs a SocksException with given error code and message.
	 * 
	 * @param errCode
	 *            Error code.
	 * @param errString
	 *            Error Message.
	 */
	public SocksException(int errCode, String errString) {
		this._errCode = errCode;
		this._errString = errString;
	}

	/**
	 * Get the error code associated with this exception.
	 * 
	 * @return Error code associated with this exception.
	 */
	public int getErrorCode() {
		return _errCode;
	}

	/**
	 * Get human readable representation of this exception.
	 * 
	 * @return String representation of this exception.
	 */
	public String toString() {
		return _errString;
	}

	static final String UNASSIGNED_ERROR_MESSAGE = "Unknown error message";
	static final String serverReplyMessage[] = { "Succeeded", "General SOCKS server failure",
			"Connection not allowed by ruleset", "Network unreachable", "Host unreachable", "Connection refused",
			"TTL expired", "Command not supported", "Address type not supported" };

	static final String localErrorMessage[] = { "SOCKS server not specified", "Unable to contact SOCKS server",
			"IO error", "None of Authentication methods are supported", "Authentication failed", "General SOCKS fault" };

	String _errString;
	int _errCode;

}// End of SocksException class

