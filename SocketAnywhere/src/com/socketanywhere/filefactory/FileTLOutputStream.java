/*
	Copyright 2012, 2019 Jonathan West

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

package com.socketanywhere.filefactory;
import java.io.IOException;
import java.io.OutputStream;

import com.socketanywhere.net.ThreadAssert;
import com.vfile.VFile;
import com.vfile.VFileOutputStream;


// TODO: ARCHITECTURE - Cleanup - Ability to test a conn id for activity, and then to delete if not

/**
 * 
 * File Messages:
 * 
 * Writes:
 * - filetl-packet-source[_sourceName]-to-dest[_remoteName]-packetid[_nextFileNum]-connuuid[uuid]
 * 
 */
public class FileTLOutputStream extends OutputStream {

	String _sourceName = null;
	String _remoteName = null;
	String _connUUID = null;

	int _nextFileNum = 1;
	boolean _isOpen = false;
	VFile _directory = null;
	FileSocketImpl _socketImpl = null;
	
	Object osLock = new Object();
	
	public FileTLOutputStream(FileSocketImpl socketImpl, VFile directory, String sourceName, String remoteName, String connUUID) throws IOException  {
		_sourceName = sourceName;
		_remoteName = remoteName;
		_connUUID = connUUID;
		_isOpen = true;
		_directory = directory;
		
		_socketImpl = socketImpl;
		
		if(!directory.exists() || !_directory.isDirectory() || !_directory.canRead()) {
			throw(new FileTLIOException("Invalid directory"));
		}
		
		FileTLLogger.outputStreamCreated(this);
		
	}
	
	@Override
	public void close() throws IOException {
		_isOpen = false;
		_socketImpl.close();
	}
	
	/** A close operation that was initiated by a close call on our side*/
	protected void informLocalClose() {
		_isOpen = false;
		FileTLLogger.outputStreamClosed(this);
	}
	
	protected void informRemoteClose() {
		_isOpen = false;
		FileTLLogger.outputStreamClosed(this);
		// TODO: CURR - What needs to be deleted?
	}

	private void assertOpen() throws IOException {
		if(!_isOpen) {
			throw(new IOException("FileTL Output Stream has closed."));
		}
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		assertOpen();
		// ThreadAssert.assertSingleThreadCaller(this);
	
		synchronized(osLock) {
		
			String filefn = "filetl-packet-source["+_sourceName+"]-to-dest["+_remoteName+"]-packetid["+_nextFileNum+"]-connuuid["+_connUUID+"]";
			
			VFile f = new VFile(_directory.getPath()+VFile.separator+filefn);
			
			_nextFileNum++;
			
			String fileFilenameComplete = f.getName()+"-ready";
			VFile renamedFile = new VFile(f.getParent() + VFile.separator + fileFilenameComplete);
			
			if(f.exists()) {
				throw(new IOException("File with the name of the next output packet exists."));
			}
	
			VFileOutputStream fos = new VFileOutputStream(f);
			
			fos.write(b, off, len);
			fos.flush();
			fos.close();
	
			FileTLUtil.renameMessageFile(f, renamedFile);			
					
			FileTLLogger.writeOutputData(len, _sourceName, _remoteName);
			
		}
		
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(int b) throws IOException {
		assertOpen();
		byte[] bytearr = new byte[1];
		bytearr[0] = (byte)(b & 0x000F);

		write(bytearr);
	}

}
