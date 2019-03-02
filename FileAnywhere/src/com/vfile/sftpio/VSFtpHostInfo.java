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

package com.vfile.sftpio;

import org.apache.commons.vfs.FileSystemException;

import com.vfile.vfsio.VFSFileConfig;

public class VSFtpHostInfo {

	String _host;
	String _username;
	String _password;
	
	VFSFileConfig _vfsConfig;
	
	
	public VSFtpHostInfo(String host, String username, String password) {
		super();
		this._host = host;
		this._username = username;
		this._password = password;
		_vfsConfig = new VFSFileConfig();
		try {
			_vfsConfig.setRoot(VSFtpFile.connect(_host, _username, _password, "/"));
		} catch (FileSystemException e) {
			throw new RuntimeException("Unable to connect", e);
		}
	}

	public String getHost() {
		return _host;
	}

	public String getUsername() {
		return _username;
	}

	public String getPassword() {
		return _password;
	}

	public VFSFileConfig getVfsConfig() {
		return _vfsConfig;
	}
}
