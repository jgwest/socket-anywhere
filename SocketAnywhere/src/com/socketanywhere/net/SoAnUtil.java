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

package com.socketanywhere.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.apache.commons.codec.binary.Base64;

import com.socketanywhere.filefactory.FileTLRuntimeException;

/** Utility methods for various library-related functions. */
public class SoAnUtil {

	public static final INameResolver DEFAULT_NAME_RESOLVER = new HostTCPResolver();
	
	// TODO: ARCHITECTURE - Continue to implement custom name resolver
	public static INameResolver getNameResolver() {
		return DEFAULT_NAME_RESOLVER;
	}
	
//	public static void main(String[] args) {
//		String src = "This is a test!!!!!!!!!!1111111111111";
//		String result1 = encodeBase64(src.getBytes());
//		
//		System.out.println(result1);
//		
//		byte[] result2 = decodeBase64(result1);
//		String result2Str = new String(result2);
//		System.out.println("["+result2Str+"]");
//	}
	
	private static Base64 _base64Instance = new Base64();
	
	public static byte[] decodeBase64(String msg) {
		synchronized(_base64Instance) {
			return _base64Instance.decode(msg);	
		}
		
	}
	
	public static String encodeBase64(byte[] bytes) {
		synchronized(_base64Instance) {
			return _base64Instance.encodeToString(bytes).replaceAll("[\\r\\n]", "");  
		}
	}
	
	public static boolean isAddressAnIP(String address) {
		int numPeriods = 0;

		// IPV6
		if(address.contains(":")) return true;
		
		for(int x = 0; x < address.length(); x ++) {
			char c = address.charAt(x);
			if(c == '.') numPeriods++;
			if(!Character.isDigit(c)) {
				return false;
			}
		}
		
		if(numPeriods == 3) return true;
		else return false;
		
	}
	
	public static UUID generateUUID() {
		return UUID.randomUUID();
	}
	
	
	public static String convertTLAddress(TLAddress addr) {
		String hostname = addr.getHostname();
		if(hostname == null || hostname.trim().length() == 0) {
			throw new FileTLRuntimeException("Invalid hostname entry in address list.");
		}
		
		int port = addr.getPort();
		String result;
		
		if(port != TLAddress.UNDEFINED_PORT) {
			result = hostname +"#"+port;
		} else {
			result = hostname;
		}
		return result;
		
	}
	
	/**
	 * The parameter to this method must first have come through resolveTLAddress
	 * to remove any empty hosts, otherwise an exception will be thrown.
	 * @param addrList
	 * @return
	 */
	public static List<String> convertTLAddressList(List<TLAddress> addrList) {
		List<String> ls = new ArrayList<String>();
		
		for(TLAddress addr : addrList) {
			
			ls.add(convertTLAddress(addr));
		}
		
		return ls;
	}
	
	public static String convertStackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		
		e.printStackTrace(pw);
		pw.flush();
		sw.flush(); 

		return sw.toString();
	}
	
	public static void appendDebugStr(ISocketTL socket, String str) {
		if(socket.getDebugStr() != null) {
			socket.setDebugStr(socket.getDebugStr()+str);
		} else {
			socket.setDebugStr(str);
		}
	}

	/** If you're in the office, wear headphones when using this :P */
	public static void makeNoise() {
		for(int x = 0; x < 6; x++) {
			playSound(new File("C:\\Windows\\Media\\notify.wav"));
			try {
				Thread.sleep(2 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static void playSound(File f) {
		
		try {
			final FileInputStream fis = new FileInputStream(f);
			BufferedInputStream bis = new BufferedInputStream(fis);
			try {

				Clip clip = AudioSystem.getClip();
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(bis);
				clip.open(inputStream);
				clip.start();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
//				fis.close();
			}
		} catch (Throwable t) {
			// Ignore
		}
	}

}
