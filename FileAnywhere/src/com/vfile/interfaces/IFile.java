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
import java.net.URI;

public interface IFile {

	public final static String separator = "/"; 
	
	public boolean canExecute();

	public boolean canRead();

	public boolean canWrite();

	public int compareTo(IFile pathname);

	public boolean createNewFile() throws IOException;

	public boolean delete();

	public void deleteOnExit();

	public boolean equals(Object obj);

	public boolean exists();

	public IFile getAbsoluteFile();

	public String getAbsolutePath();

	public IFile getCanonicalFile() throws IOException;

	public String getCanonicalPath() throws IOException;

	public long getFreeSpace();

	public String getName();

	public String getParent();

	public IFile getParentFile();

	public String getPath();

	public long getTotalSpace();

	public long getUsableSpace();

	public int hashCode();

	public boolean isAbsolute();

	public boolean isDirectory();

	public boolean isFile();

	public boolean isHidden();

	public long lastModified();

	public long length();

	public String[] list();

	public String[] list(IFilenameFilter filter);

	public IFile[] listFiles();

	public IFile[] listFiles(IFileFilter filter);

	public IFile[] listFiles(IFilenameFilter filter);

	public boolean mkdir();

	public boolean mkdirs();

	public boolean renameTo(IFile dest);

	public boolean setExecutable(boolean executable, boolean ownerOnly);

	public boolean setExecutable(boolean executable);

	public boolean setLastModified(long time);

	public boolean setReadable(boolean readable, boolean ownerOnly);

	public boolean setReadable(boolean readable);

	public boolean setReadOnly();


	public boolean setWritable(boolean writable, boolean ownerOnly);

	public boolean setWritable(boolean writable);

	public String toString();

	public URI toURI();

	
}
