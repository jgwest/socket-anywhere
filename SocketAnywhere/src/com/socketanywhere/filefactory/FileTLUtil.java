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

package com.socketanywhere.filefactory;

import java.io.IOException;
import java.util.UUID;

import com.socketanywhere.net.SoAnUtil;
import com.vfile.VFile;
import com.vfile.VFileWriter;

/** Generic static file specific utilities. */
public class FileTLUtil {
	
//	public static final FileTLFileFilter FILE_TL_FILE_FILTER = new FileTLFileFilter();
	
	public static String extractField(String field, String filename) {
		String fullField = "-"+field+"[";
		int start = filename.indexOf(fullField) + fullField.length();
		int end = filename.indexOf("]", start);
		return filename.substring(start, end);
	}
	
	/** A crucial file is defined as one for which the failure to delete it should halt the
	 * data transfer entirely. */
	public static void deleteCrucialFile(VFile file) {
		final long WAIT_TIME_BETWEEN_ATTEMPTS = 250;
		final long MAX_WAIT_TIME = 4000;
		
		if(!file.exists()) { 
			return;
		}
		
		long startTime = System.currentTimeMillis();
		
		boolean result;
		do {
			result = file.delete();
			
			if(!result && !file.exists()) { 
				result = true; // Close enough :)
			}
			
			if(!result) {
				sleep(WAIT_TIME_BETWEEN_ATTEMPTS);
			}
		} while(!result && System.currentTimeMillis() - startTime < MAX_WAIT_TIME);
		
		if(!result && file.exists()) {
			// This cannot be allowed to fail.
			throw new FileTLRuntimeException("Unable to delete ["+file.getPath()+"]");
		}
	}
	
	protected static void deleteFileIfExists(VFile directory, String fileName) {
		String path = directory.getPath();
		path += VFile.separator;
		
		path += fileName;
		
		VFile f = new VFile(path);

		deleteCrucialFile(f);
		
	}
	
	protected static boolean filePatternExistsInDirectory(VFile directory, String startsWithPattern) {
		VFile[] fileList = directory.listFiles();
		boolean matchingEntryFound = false;
		
		for(VFile dirEntry : fileList) {
			
			String dirEntryName = dirEntry.getName();
			
			if(dirEntryName.startsWith(startsWithPattern)) {
				matchingEntryFound = true;
			}
			
		}
		return matchingEntryFound;
	}
	
	public static void sleep(long timeInMsecs) {
		
		try {
			Thread.sleep(timeInMsecs);
		} catch (InterruptedException e) {
			throw new FileTLRuntimeException("Thread interrupted");
		}
		
	}
	
	/** A close followed by a rename seems to fail sometimes. This will try multiple times until the rename succeeds. 
	 * 
	 * Note: If at any point both the source and dest no longer exist, this is considered
	 * a successful rename (as its likely that the rename succeeded, and then was quickly deleted by another process)  
	 * */
	public static void renameMessageFile(VFile fromFile, VFile toFile) throws FileTLIOException {
		final int WAIT_TIME_BETWEEN_ATTEMPTS = 500;
		boolean renameComplete;
		long startTime = System.currentTimeMillis();
		
		int attemptNum = 0;
		do {
			renameComplete = fromFile.renameTo(toFile);
			attemptNum++;
			if(attemptNum > 1) {
				System.err.println("Rename Attempt"+attemptNum);
			}
			
			// Handle the case where the rename is reported to have failed, but
			// it actually renamed the file
			if(!renameComplete && toFile.exists() && !fromFile.exists()) {
				renameComplete = true;
			}
			
			// Both are gone, likely the rename suceeded
			if(!renameComplete && !toFile.exists() && !fromFile.exists()) {
				renameComplete = true;
			}
			
			if(!renameComplete && System.currentTimeMillis() - startTime > 10000) {
				throw new FileTLIOException("Unable to rename pipe within 10 seconds - from ["+fromFile+"] to ["+toFile+"]");
			}
			
			if(!renameComplete) {
				sleep(WAIT_TIME_BETWEEN_ATTEMPTS); 
			}
			
		} while(!renameComplete);
		
	}
	
	protected static void writeAndRenameEmptyMessageFile(VFile msgFile) { 
		if(msgFile.getName().endsWith("-ready")) {
			throw new FileTLRuntimeException("Invalid message file format (ends with -ready)");
		}
		
		try {
			VFileWriter fw = new VFileWriter(msgFile);
			fw.write("\n");
			fw.flush();
			fw.close();
			
			
			VFile renameToFile = new VFile(msgFile.getParent() + VFile.separator + msgFile.getName()+"-ready");

			renameMessageFile(msgFile, renameToFile);
			
			// Success is a rename succeeding, or both files no longer appearing (because the rename actually
			// succeeded, but then was quickly deleted by another process)
			boolean renameSuccess = (!msgFile.exists() && renameToFile.exists()) 
				|| (!msgFile.exists() && !renameToFile.exists());
			
			if(!renameSuccess) {
				throw new FileTLIOException("Unable to rename");
			}
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
			throw new FileTLRuntimeException("Unable to write messageFile");
		}

	}
	
	// TODO: LOWER - Consider removing instances of this, replacing with writeAndRenameEmptyMessageFile 
	public static void writeEmptyMessageFile(VFile msgFile) {
		
		try {
			VFileWriter fw = new VFileWriter(msgFile);
			fw.write("\n");
			fw.flush();
			fw.close();
			
		} catch(IOException ioe) {
			throw new FileTLRuntimeException("Unable to write messageFile");
		}
		
	}

	public static String generateUUID() {
		UUID uuid = SoAnUtil.generateUUID();
		return uuid.toString();		
	}
}

//class FileTLFileFilter implements FileFilter {
//
//	@Override
//	public boolean accept(File pathname) {
//		if(pathname.isFile() && pathname.getName().startsWith("filetl-")) {
//			return true;
//		}
//		return false;
//	}
//}
