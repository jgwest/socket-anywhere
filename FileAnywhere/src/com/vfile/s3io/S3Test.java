/*
	Copyright 2012, 2013 Jonathan West

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

package com.vfile.s3io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

import com.vfile.DefaultFileFactory;
import com.vfile.VFile;

/** Various simple standalone s3 related api tests. */
public class S3Test {

	public static void main(String[] args) {
		
		s3Test4();
		
	}

	public static void s3Test4() {
		String awsAccessKey = "aws-access-key";
		String awsSecretKey = "aws-secret-key"; 

		try {
			S3HostInfo hostInfo = new S3HostInfo(new AWSCredentials(awsAccessKey, awsSecretKey), "test-bucket");
			
			DefaultFileFactory.setDefaultFactory(new S3FileFactory(hostInfo));
			
			VFile fdir = new VFile("/.");
			
			VFile[] fl = fdir.listFiles();
			
			for(VFile f : fl ) {
				System.out.println(f.getPath() + " " + f.length());
			}
			
			System.exit(0);
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void s3Test3() {
		String awsAccessKey = "aws-access-key";
		String awsSecretKey = "aws-secret-key";

		try {
			S3HostInfo host = new S3HostInfo(new AWSCredentials(awsAccessKey, awsSecretKey), "test-bucket"); 
			
			S3File s3f = new S3File(host, "/test.txt"); 
			
			try {
				S3FileOutputStream fos = new S3FileOutputStream(s3f, true);
				fos.write("I do it for the fun of it.\r\n".getBytes());
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			try {
				S3FileReader fr = new S3FileReader(s3f);
				BufferedReader br = new BufferedReader(fr);
				
				String str;
				while( (str = br.readLine()) != null) { 
					System.out.println(str);
				}
				
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
//			
//			try {
//				byte[] b = new byte[16384];
//				S3FileInputStream fis = new S3FileInputStream(s3f);
//				int c = fis.read(b);
//				
//				fis.close();
//				System.out.println(new String(b, 0, c));
//				
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			

////			S3File nf = new S3File(host, "/test-dir");
////			nf.mkdir();
//			
//			S3File s3f = new S3File(host, "/test-dir");
//			IFile[] fl = s3f.listFiles();
//			
//			for(IFile f : fl) {
//				System.out.println(f.getPath());
//			}
			
		} catch (S3ServiceException e) {
		}
		
	}
	
	public static void s3Test2() {
		// s3Test();
		
			
		String awsAccessKey = "aws-access-key";
		String awsSecretKey = "aws-secret-key";
		
		try {
			S3HostInfo host = new S3HostInfo(new AWSCredentials(awsAccessKey, awsSecretKey), "test-bucket"); 
			
			S3Object so = host.getS3Service().getObject(host.getS3Bucket(), "Version-Strings.txt");
			Object result = so.getMetadata(S3Object.METADATA_HEADER_LAST_MODIFIED_DATE);
			
			Date d = (java.util.Date)result;
			System.out.println(d);
			
						
//			host
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
		
//		s3Test();
	}
	
	public static void s3Test() {
		String awsAccessKey = "aws-access-key";
		String awsSecretKey = "aws-secret-key";
		
		AWSCredentials awsCredentials =  new AWSCredentials(awsAccessKey, awsSecretKey);
		
		try {
			S3Service s3Service = new RestS3Service(awsCredentials);
			
			
			S3Bucket[] myBuckets = s3Service.listAllBuckets();
			
			S3Bucket testBucket = null;
			
			for(S3Bucket bucket : myBuckets) {
				if(bucket.getName().equalsIgnoreCase("test-bucket")) { 
					testBucket = bucket;
				}
			}
			
	        
	        // List the objects in this bucket.
	        S3Object[] objects = s3Service.listObjects(testBucket);

	        // Print out each object's key and size.
	        for (int o = 0; o < objects.length; o++) {
	        	System.out.println(" " + objects[o].getKey() + " (" + objects[o].getContentLength() + " bytes)");
	        }

	        S3Object objectComplete = s3Service.getObject(testBucket, "Version-Strings.txt");

	        System.out.println("Greeting:");
	        BufferedReader reader = new BufferedReader(new InputStreamReader(objectComplete.getDataInputStream()));
	        String data = null;
	        while ((data = reader.readLine()) != null) {
	                System.out.println(data);
	        }

			
//		      VFTPClient ftp = new VFTPClient();
//		      
//		      String server = "localhost";
//		      
//
//		      ftp.connect( server );
//		      boolean result = ftp.login( username, password );
//		      
//		      InputStream is = new VFTPFileInputStream(ftp, "/tmp/Version Strings.txt");
//		      
//
//			
//			S3Object object = new S3Object("Version-Strings.txt");
//			object.setContentType(Mimetypes.MIMETYPE_OCTET_STREAM);
//			object.setDataInputStream(is);
//
//			// Upload the object to our test bucket in S3.
//			object = s3Service.putObject(testBucket, object);
//			
//			System.out.println("Done.");
			
			

		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
