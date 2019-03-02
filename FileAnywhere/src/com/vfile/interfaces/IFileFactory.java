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

package com.vfile.interfaces;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

public interface IFileFactory {
	
	// File

	IFile createFile(IFile parent, String child);
	
	IFile createFile(String pathname);
	
	IFile createFile(String parent, String child);
	
	IFile createFile(URI uri);
	
	
	// FileInputStream
	
	IFileInputStream createFileInputStream(IFile file) throws FileNotFoundException;
	
	IFileInputStream createFileInputStream(String name) throws FileNotFoundException;


	// FileOutputStream
	
	IFileOutputStream createFileOutputStream(String name) throws FileNotFoundException;
	
	IFileOutputStream createFileOutputStream(String name, boolean append) throws FileNotFoundException;
	
	IFileOutputStream createFileOutputStream(IFile file) throws FileNotFoundException;
	
	IFileOutputStream createFileOutputStream(IFile file, boolean append) throws FileNotFoundException;
	
	// FileReader
	IFileReader createFileReader(String fileName) throws FileNotFoundException;
	
	IFileReader createFileReader(IFile file) throws FileNotFoundException;

	
	// FileWriter
	IFileWriter createFileWriter(String fileName) throws IOException;
	
	IFileWriter createFileWriter(String fileName, boolean append) throws IOException;
	
	IFileWriter createFileWriter(IFile file) throws IOException;
	
	IFileWriter createFileWriter(IFile file, boolean append) throws IOException;

	
}
