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

package com.socketanywhere.obfuscator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.socketanywhere.net.ISocketFactory;
import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;

/** Encrypts/decrypts sent/received data by running the data transformers against it; wraps
 * an inner socket for which the resulting data is sent to, or from which data is read.  */	
public class OBSocketImpl implements ISocketTL {

	ISocketFactory _innerFactory = new TCPSocketFactory();
	TLAddress _addr;
	
	ISocketTL _socket;
	InputStream _inputStream;
	OutputStream _outputStream;
	
	boolean _socketCameFromServerSocket = false;
	
	List<IDataTransformer> _dataTransformerList;
	
	private String _debugStr;
	
	private void intiatiateTransformers(List<IDataTransformer> transformers) {
		_dataTransformerList = new ArrayList<IDataTransformer>();
		
		for(IDataTransformer dt : transformers) {
			_dataTransformerList.add(dt.instantiateDataTransformer());
		}
	}
	
	public OBSocketImpl(List<IDataTransformer> transformers, ISocketFactory factory) throws IOException {
		_innerFactory = factory;
		
		intiatiateTransformers(transformers);
		
	}

	/** Uses the default factory (TCP) */
	public OBSocketImpl(TLAddress address, List<IDataTransformer> transformers) throws IOException {
		_addr = address;
		intiatiateTransformers(transformers);
		connect(address);
	}
	
	public OBSocketImpl(TLAddress address, List<IDataTransformer> transformers, ISocketFactory factory) throws IOException {
		_addr = address;
		_innerFactory = factory;
		intiatiateTransformers(transformers);
		connect(address);
	}
	
	// Called by server-socket only
	protected OBSocketImpl(List<IDataTransformer> transformers, ISocketTL socket) throws IOException {
		intiatiateTransformers(transformers);
		_socket = socket;
		_addr = _socket.getAddress();
		
		_inputStream = new OBInputStream(_socket.getInputStream(), _dataTransformerList);
		_outputStream = new OBOutputStream(_socket.getOutputStream(), _dataTransformerList);		
		
		_socketCameFromServerSocket = true;
	}


	
	@Override
	public void close() throws IOException {
		_socket.close();
	}

	@Override
	public void connect(TLAddress endpoint, int timeout) throws IOException {
		// TODO: LOWER - Implement timeout
		connect(endpoint);
	}

	@Override
	public void connect(TLAddress endpoint) throws IOException {
	
		_socketCameFromServerSocket = false;
		_addr = endpoint;
		
		_socket = _innerFactory.instantiateSocket(endpoint);
		
		_inputStream = new OBInputStream(_socket.getInputStream(), _dataTransformerList);
		_outputStream = new OBOutputStream(_socket.getOutputStream(), _dataTransformerList);		
		
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return _inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return _outputStream;
	}

	@Override
	public boolean isClosed() {
		return _socket.isClosed();
	}

	@Override
	public boolean isConnected() {
		return _socket.isConnected();
	}

	@Override
	public TLAddress getAddress() {
		return _addr;
	}
	
	@Override
	public String toString() {
		return "[OBSocketImpl: fromServerSocket:"+_socketCameFromServerSocket+" addr:"+_addr+", inner:{"+super.toString()+"} ]";
	}

	@Override
	public void setDebugStr(String s) {
		_debugStr = s;
	}

	@Override
	public String getDebugStr() {
		return _debugStr;
	}

}
