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

package com.vfile.interfaces;

import java.io.IOException;

public interface IFileReader {

	public void close() throws IOException;

	public String getEncoding();

	public int read() throws IOException;

	public int read(char[] cbuf, int offset, int length) throws IOException;

	public boolean ready() throws IOException;

	public void mark(int readAheadLimit) throws IOException;

	public boolean markSupported();

	public int read(char[] cbuf) throws IOException;

	public void reset() throws IOException;

	public long skip(long n) throws IOException;
	
}
