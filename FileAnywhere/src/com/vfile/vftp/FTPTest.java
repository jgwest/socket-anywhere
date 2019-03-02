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

import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

import com.vfile.interfaces.IFile;

/** A few simple tests of the VFTP API. */
public class FTPTest {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		
	      // Connect and logon to FTP Server
	      VFTPClient ftp = null;

	      ftp = new VFTPClient();
	      
//	      
//	      String username = "anonymous";
//	      String password = "anonymous";
	      
	      String server = "localhost";
	      
	      String username = "username";
	      String password = "password"; 

	      
	      try {
		      ftp.connect( server );
		      boolean result = ftp.login( username, password );
		      
		      VFTPFile f = new VFTPFile(ftp, "/tmp");
		      FileCache.setCacheExpireTime(ftp, 5000);
		      
		      System.out.println("Logged in.");
		      
		      IFile[] files = f.listFiles();
		      for(IFile file : files) {
		    	  
//		    	  System.out.println(file.getName() + " - " + file.length());
		    	  
		      }
		      
//		      VFTPFile f2 = new VFTPFile(ftp, "/tmp/Version Strings.txt");
//
//		      VFTPFileReader fr = new VFTPFileReader(ftp,"/tmp/Version Strings.txt");
//		      
//		      String str = null;
//		      BufferedReader br = new BufferedReader(fr);
//		      do {
//		    	  str = br.readLine();
//		    	  if(str != null) {
//		    		  System.out.println("["+str+"]");
//		    	  }
//		      } while(str != null);
//		      
//		      br.close();
		      
//		      VFTPFile f3 = new VFTPFile(ftp, "/tmp/m2");
//		      
//		      VFTPFileOutputStream fos = new VFTPFileOutputStream(ftp, f3, true);
//		      fos.write("This is a test\n".getBytes());
//		      fos.flush();
//		      fos.close();
		      
		      VFTPFileWriter fw = new VFTPFileWriter(ftp, "/tmp/m22");
		      fw.write("test!!!\n");
		      fw.flush();
		      fw.close();

	      } catch(Exception e) {
	    	  e.printStackTrace();
	      }

	}

	public static void ftpLibInputStreamTream(VFTPClient ftp) throws Exception {
	      FTPFile[] files = ftp.listFiles("/tmp");
	      for(FTPFile file : files) {

	    	  if(file.getName().equalsIgnoreCase("file")) {
	    		  
	    	  }
	      }
	      
		  ftp.setFileType(FTP.BINARY_FILE_TYPE);
	      InputStream fis = ftp.retrieveFileStream("/tmp/Version Strings.txt");

	      byte[] thing = new byte[16384];
	      
	      int bytesRead = fis.read(thing);
	      System.out.println(bytesRead);

	}
	
	public static void ftpLibraryTest(VFTPClient ftp) throws Exception {
	      FTPFile[] fileList = ftp.listFiles("/tmp");
	      
	      for(FTPFile file : fileList) {
	    	  if(!file.getName().equalsIgnoreCase("test-file")) {
	    		  continue;
	    	  }
	    	  System.out.println("Found it:");
	    	  while(true) {
	    		  System.out.println(file.getName() + " " + file.getSize());
	    		  Thread.sleep(1000);
	    	  }
	      }
	}
	
}
