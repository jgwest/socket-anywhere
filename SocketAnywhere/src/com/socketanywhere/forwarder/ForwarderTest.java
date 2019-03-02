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

package com.socketanywhere.forwarder;

import java.io.IOException;

import com.socketanywhere.irc.IRCSocketFactory;
import com.socketanywhere.net.IServerSocketTL;
import com.socketanywhere.net.TLAddress;
import com.socketanywhere.socketfactory.TCPSocketFactory;
import com.socketanywhere.ssl.SSLSecureSocketFactory;

/** Standalone test, with various tests of the forwarder using various factories. */
public class ForwarderTest {

	public static void main(String[] args) {
		m3();
	}
	
	
	public static void m3() {
		TCPSocketFactory tcps = new TCPSocketFactory();

		SSLSecureSocketFactory ftf = new SSLSecureSocketFactory("C:\\temp\\ks\\samplekeystore"); 
		
		IServerSocketTL listen;
		ForwarderThread ft;
		
		try {
			
			listen = tcps.instantiateServerSocket(new TLAddress(1080));
			ft = new ForwarderThread(listen, ftf, new TLAddress("host", 8000));
			ft.start();
			
//			listen = ftf.instantiateServerSocket(new TLAddress("localhost", 8001));
//			ft = new ForwarderThread(listen, new TCPSocketFactory(), new TLAddress("localhost", 1080));
//			ft.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	

//	public static FileObject connect(String host, String user, String password, String remotePath) throws FileSystemException {
//		
//		// we first set strict key checking off
//		FileSystemOptions fsOptions = new FileSystemOptions();
//		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
//		
//		// now we create a new filesystem manager
//		DefaultFileSystemManager fsManager = (DefaultFileSystemManager) VFS.getManager();
//
//		// the url is of form sftp://user:pass@host/remotepath/
//		String uri = "sftp://" + user + ":" + password + "@" + host + "/" + remotePath;
//		
//		// get file object representing the local file
//		FileObject fo = fsManager.resolveFile(uri, fsOptions);
//
//		return fo;
//		
//	}
//
//	
//	public static void m2() {
//		TCPSocketFactory ss = new TCPSocketFactory();
//
//		FileObject o;
//		try {
//			
//			o = connect("hostname", "user", "password", "/"); 
//			VFSFileConfig c = new VFSFileConfig();
//			c.setRoot(o);
//
//			DefaultFileFactory.setDefaultFactory(new VFSFileFactory(c));
//
//		} catch (FileSystemException e1) {
//			e1.printStackTrace();
//		}
//		
//		VFile f = new VFile("/tmp/fl");
//		
//		FileTLFactory ftf = new FileTLFactory(f);
////		SocketSocketFactory ftf = new SocketSocketFactory();
//		
//		IServerSocketTL listen;
//		ForwarderThread ft;
//		
//		try {
//			
//			listen = ss.instantiateServerSocket(new TLAddress(81));
//			ft = new ForwarderThread(listen, ftf, new TLAddress("m", 8001));
//			ft.start();
//			
//			listen = ftf.instantiateServerSocket(new TLAddress("m", 8001));
//			ft = new ForwarderThread(listen, new TCPSocketFactory(), new TLAddress("hostname", 3389)); 
//			ft.start();
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//	}
	

	public static void m1() {
		TCPSocketFactory ssf = new TCPSocketFactory();
		
		IRCSocketFactory isf1 = new IRCSocketFactory();
		IRCSocketFactory isf2 = new IRCSocketFactory();
		
		IServerSocketTL listen;
		ForwarderThread ft;
		
		try {
			
			int basePort = 7000;
			
			// 8000 -> 8001
			listen = ssf.instantiateServerSocket(new TLAddress(basePort));
			ft = new ForwarderThread(listen, isf1, new TLAddress("hostname1", basePort+1)); 
			ft.start();
			
			// 8001 -> 80
			listen = isf2.instantiateServerSocket(new TLAddress(basePort+1));
			ft = new ForwarderThread(listen, ssf, new TLAddress("hostname2", 22)); 
			ft.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
