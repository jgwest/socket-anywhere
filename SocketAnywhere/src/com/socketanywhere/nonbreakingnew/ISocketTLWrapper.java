package com.socketanywhere.nonbreakingnew;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import com.socketanywhere.net.ISocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.nonbreakingnew.cmd.CmdAbstract;

public class ISocketTLWrapper {
	
	private static final AtomicLong atomicLong = new AtomicLong(0); 
	
	private final long _globalId = atomicLong.getAndIncrement();

	private final InputStream _is;
	
	private final SocketOutWriter _writer;
	
	private final TLAddress _address;

	private final boolean _fromServSock;
	
	public ISocketTLWrapper(ISocketTL socket, boolean fromServSock, ConnectionBrain cb) throws IOException {
		if(socket instanceof NBSocket) {
			throw new IllegalArgumentException("invalid param.");
		}
		
		_fromServSock = fromServSock;
		_address = socket.getAddress();

		_is = socket.getInputStream();
		_writer = new SocketOutWriter(socket, this, cb);
		_writer.start();
	}
	
	@Override
	public int hashCode() {
		return (int)_globalId;
	}
	
	@Override
	public boolean equals(Object obj) {
		ISocketTLWrapper other = (ISocketTLWrapper)obj;
		
		return other._globalId == _globalId;
	}

	public void close(boolean blocking) {
		_writer.closeSocket(blocking);
	}


	public boolean isClosed() {
		return _writer.isClosed();
	}

	public boolean isConnected() {
		return _writer.isConnected();
	}

	public TLAddress getAddress() {
		return _address;
	}
	
	public void flush() {
		_writer.flush();
	}
	
	public InputStream getInputStream() throws IOException {
		return _is;
	}
	
	public void writeCommand(CmdAbstract cmd, Triplet debug) {
		
		_writer.sendCommand(cmd, debug);
		
	}
	
	public boolean isFromServSock() {
		return _fromServSock;
	}
	
	public long getGlobalId() {
		return _globalId;
	}
	
	public SocketOutWriter debug_getWriter() {
		return _writer;
	}
}
