package com.vfile.varchiveio;

import java.io.File;
import java.io.IOException;

import com.vfile.interfaces.IFile;
import com.vfile.interfaces.IFileWriter;

public class VATest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		File f = new File("");
		
//		
//
////		for(IFile f : fl.listFiles()) {
////			System.out.println(f.getPath());
////		}
//		
//		try {
//			IFileReader fr = new VAFileReader(test);
//			BufferedReader br = new BufferedReader(fr);
//			
//			String str;
//			while(null != (str = br.readLine())) {
//				System.out.println(str);
//			}
//			br.close();
//			
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
		
		
		
	}

	
	public static void m1() {
		VAFileFactory factory = new VAFileFactory(new File("c:\\temp\\test.zip"));
		
		IFile f = factory.createFile("test");
		
		try {
			IFileWriter fw = new VAFileWriter(f, false);
			fw.write("hi!\n");
			fw.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
