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

package com.vfile.s3io;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;

public class S3HostInfo {
	S3Bucket _bucket = null;
	S3Service _s3Service = null;
	
	AWSCredentials _awsCredentials = null;
	String _bucketName;
	
	public S3HostInfo(AWSCredentials awsCredentials, String bucketName) throws S3ServiceException {
		_awsCredentials = awsCredentials;
		_bucketName = bucketName;
		
		_s3Service = new RestS3Service(awsCredentials);
		
		_bucket = _s3Service.getBucket(bucketName);
		
	}
	
	public S3Bucket getS3Bucket() {
		return _bucket;
	}
	
	public S3Service getS3Service() {
		return _s3Service;
	}
}
