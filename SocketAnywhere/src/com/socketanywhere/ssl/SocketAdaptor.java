/*
	Copyright 2012 Jonathan West

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
*/

package com.socketanywhere.ssl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;

/** Converts an ISocketTL into a Socket; this is used in the internal mechanism of SSL, as a necessity
 * in order to allow other socket implementations to be wrapped by the SSL implementation. */
public class SocketAdaptor extends Socket {
	
	ISocketTL _inner;
	
	public SocketAdaptor(ISocketTL inner) {
		_inner = inner;
	}
	
	@Override
	public void bind(SocketAddress arg0) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void close() throws IOException {
		_inner.close();
	}

	@Override
	public void connect(SocketAddress host, int port) throws IOException {
		InetSocketAddress isa = ((InetSocketAddress)host);
		_inner.connect(new TLAddress(isa.getAddress().getHostAddress(), port));
	}

	@Override
	public void connect(SocketAddress host) throws IOException {
		InetSocketAddress isa = ((InetSocketAddress)host);
		_inner.connect(new TLAddress(isa.getAddress().getHostAddress(), isa.getPort()));
	}

	@Override
	public SocketChannel getChannel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetAddress getInetAddress() {

		try {
			InetAddress result = InetAddress.getByName(_inner.getAddress().getHostname()); 
			return result;
		} catch (UnknownHostException e) {
			return null;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {

		return _inner.getInputStream();
	}

	@Override
	public boolean getKeepAlive() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InetAddress getLocalAddress() {
		throw new UnsupportedOperationException();	}

	@Override
	public int getLocalPort() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getOOBInline() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {

		return _inner.getOutputStream();
	}

	@Override
	public int getPort() {

		return _inner.getAddress().getPort();
	}

	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		throw new UnsupportedOperationException();
//		return _inner.getRemoteSocketAddress();
	}

	@Override
	public boolean getReuseAddress() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSoLinger() throws SocketException {
		return -1;
	}

	@Override
	public synchronized int getSoTimeout() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getTcpNoDelay() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getTrafficClass() throws SocketException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBound() {
		
		return _inner.isConnected();
	}

	@Override
	public boolean isClosed() {
		return _inner.isClosed();
	}

	@Override
	public boolean isConnected() {
		return _inner.isConnected();
	}

	@Override
	public boolean isInputShutdown() {
		return isClosed();
	}

	@Override
	public boolean isOutputShutdown() {
		return isClosed();
	}

	@Override
	public void sendUrgentData(int arg0) throws IOException {
		return;
	}

	@Override
	public void setKeepAlive(boolean arg0) throws SocketException {
		return;
	}

	@Override
	public void setOOBInline(boolean arg0) throws SocketException {
		return;
	}

	@Override
	public void setPerformancePreferences(int arg0, int arg1, int arg2) {
		return;
	}

	@Override
	public synchronized void setReceiveBufferSize(int arg0) throws SocketException {
		return;
	}

	@Override
	public void setReuseAddress(boolean arg0) throws SocketException {
		return;
	}

	@Override
	public synchronized void setSendBufferSize(int arg0) throws SocketException {
		return;
	}

	@Override
	public void setSoLinger(boolean arg0, int arg1) throws SocketException {
		return;
	}

	@Override
	public synchronized void setSoTimeout(int arg0) throws SocketException {
		return;
	}

	@Override
	public void setTcpNoDelay(boolean arg0) throws SocketException {
		return;
	}

	@Override
	public void setTrafficClass(int arg0) throws SocketException {
		return;
	}

	@Override
	public void shutdownInput() throws IOException {
		return;
	}

	@Override
	public void shutdownOutput() throws IOException {
		return;
	}

	@Override
	public String toString() {
		return _inner.toString();
	}

	@Override
	public boolean equals(Object o) {
		return _inner.equals(o);
	}

	@Override
	public int hashCode() {
		return _inner.hashCode();
	}

}
