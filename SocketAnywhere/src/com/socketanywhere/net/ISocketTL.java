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

package com.socketanywhere.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISocketTL {

	public void close() throws IOException;

	public void connect(TLAddress endpoint, int timeout) throws IOException;

	public void connect(TLAddress endpoint) throws IOException;

	public InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException;

	public boolean isClosed();

	/**
	 * NOTE: From JavaDocs: Closing a socket doesn't clear its connection state, which means this method will
	 * return true for a closed socket (see isClosed()) if it was successfully connected prior to being closed.
	 */
	public boolean isConnected();

	public TLAddress getAddress();

	public void setDebugStr(String s);

	public String getDebugStr();

}
