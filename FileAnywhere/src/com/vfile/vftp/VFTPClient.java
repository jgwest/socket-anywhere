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

package com.vfile.vftp;

import java.io.IOException;

public class VFTPClient extends CommonsFTPClient {
	
	private String _activeLogin = null;
	private String _activePassword = null;
	private String _activeAccount = null;
	
	public VFTPClient() {
		super();
	}
	
	@Override
	public boolean login(String username, String password) throws IOException {
		boolean result = super.login(username, password);
		
		if(result) {
			_activeLogin = username;
			_activePassword = password;
		}
		
		return result;		
	}
	
	@Override
	public boolean login(String username, String password, String account) throws IOException {
		boolean result = super.login(username, password, account);
		
		if(result) {
			_activeLogin = username;
			_activePassword = password;
			_activeAccount = account;
		}
		
		return result;
	}
	
	
	/** This should only be used by the InputStream/OutputStream implementations */
	protected VFTPClient cloneConnectionForStream() throws IOException {
		VFTPClient result = new VFTPClient();
		
		result.connect( getRemoteAddress());
		
		boolean b = false;
		if(_activeAccount == null) {
			b = result.login( _activeLogin, _activePassword); 
		} else {
			b = result.login( _activeLogin, _activePassword, _activeAccount);
		}
		
		if(b) {
			return result;
		} else {
			return null;
		}
		
	}

}
