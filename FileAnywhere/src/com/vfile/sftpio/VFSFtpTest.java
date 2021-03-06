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

import com.vfile.interfaces.IFile;

public class VFSFtpTest {

	public static void main(String[] args) {
		
		try {
			
			final String HOST_NAME = "hostname";
			final String USER_NAME = "user";
			final String PASSWORD = "password";
			
			VSFtpHostInfo hostInfo = new VSFtpHostInfo(HOST_NAME, USER_NAME, PASSWORD); 

			VSFtpFileFactory factory = new VSFtpFileFactory(hostInfo);

			IFile file = factory.createFile("/");
			
			IFile[] fl = file.listFiles();
			
			for(IFile f : fl) {
				System.out.println(f.getName());
				
				if(f.isDirectory()) {
					if(f.listFiles() != null) {
						for(IFile g : f.listFiles()) {
							System.out.println(g.getPath());
						}
					}
				}
			}
			
			
			VSFtpFileWriter fw = new VSFtpFileWriter(new VSFtpFile(hostInfo, "/tmp/sftp-test"));
			fw.write("test 2!\n");
			fw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { Thread.sleep(5000); } catch (InterruptedException e) { }
			System.exit(1);
		}

	}
	

}
