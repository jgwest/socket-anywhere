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

package com.vfile.vfsio;

import org.apache.commons.vfs.FileObject;

public class VFSFileConfig {
	public final boolean DEBUG = true;

	private FileObject _root;	
	
	public FileObject getRoot() {
		return _root;
	}
	
	public void setRoot(FileObject root) {
		this._root = root;
	}
}
