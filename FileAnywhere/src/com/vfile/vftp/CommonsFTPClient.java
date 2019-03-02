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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.net.ftp.parser.FTPFileEntryParserFactory;

/** Wrapper over the Apache Commons FTP client. */
public class CommonsFTPClient {
	
	FTPClient _inner = null;
	
	public CommonsFTPClient() {
		_inner = new FTPClient();
	}

	public boolean abort() throws IOException {
		return _inner.abort();
	}

	public boolean allocate(int bytes, int recordSize) throws IOException {
		return _inner.allocate(bytes, recordSize);
	}


	public boolean allocate(int bytes) throws IOException {
		return _inner.allocate(bytes);
	}


	public boolean appendFile(String remote, InputStream local) throws IOException {
		return _inner.appendFile(remote, local);
	}


	public OutputStream appendFileStream(String remote) throws IOException {
		return _inner.appendFileStream(remote);
	}


	public boolean changeToParentDirectory() throws IOException {
		return _inner.changeToParentDirectory();
	}


	public boolean changeWorkingDirectory(String pathname) throws IOException {
		return _inner.changeWorkingDirectory(pathname);
	}


	public boolean completePendingCommand() throws IOException {
		return _inner.completePendingCommand();
	}


	public void configure(FTPClientConfig config) {
		_inner.configure(config);
	}


	public boolean deleteFile(String pathname) throws IOException {
		return _inner.deleteFile(pathname);
	}


	public void disconnect() throws IOException {
		_inner.disconnect();
	}


	public void enterLocalActiveMode() {
		_inner.enterLocalActiveMode();
	}


	public void enterLocalPassiveMode() {
		_inner.enterLocalPassiveMode();
	}


	public boolean enterRemoteActiveMode(InetAddress host, int port) throws IOException {
		return _inner.enterRemoteActiveMode(host, port);
	}


	public boolean enterRemotePassiveMode() throws IOException {
		return _inner.enterRemotePassiveMode();
	}


	public int getBufferSize() {
		return _inner.getBufferSize();
	}


	public int getDataConnectionMode() {
		return _inner.getDataConnectionMode();
	}


	public boolean getListHiddenFiles() {
		return _inner.getListHiddenFiles();
	}


	public String getModificationTime(String pathname) throws IOException {
		return _inner.getModificationTime(pathname);
	}


	public String getPassiveHost() {
		return _inner.getPassiveHost();
	}


	public int getPassivePort() {
		return _inner.getPassivePort();
	}


	public long getRestartOffset() {
		return _inner.getRestartOffset();
	}


	public String getStatus() throws IOException {
		return _inner.getStatus();
	}


	public String getStatus(String pathname) throws IOException {
		return _inner.getStatus(pathname);
	}


	public String getSystemName() throws IOException {

		return _inner.getSystemName();
	}


	public FTPListParseEngine initiateListParsing() throws IOException {

		return _inner.initiateListParsing();
	}


	public FTPListParseEngine initiateListParsing(String parserKey,
			String pathname) throws IOException {

		return _inner.initiateListParsing(parserKey, pathname);
	}


	public FTPListParseEngine initiateListParsing(String pathname)
			throws IOException {

		return _inner.initiateListParsing(pathname);
	}


	public boolean isRemoteVerificationEnabled() {

		return _inner.isRemoteVerificationEnabled();
	}


	public FTPFile[] listFiles() throws IOException {

		return _inner.listFiles();
	}


	public FTPFile[] listFiles(String pathname) throws IOException {

		return _inner.listFiles(pathname);
	}


	public String listHelp() throws IOException {

		return _inner.listHelp();
	}


	public String listHelp(String command) throws IOException {

		return _inner.listHelp(command);
	}


	public String[] listNames() throws IOException {

		return _inner.listNames();
	}


	public String[] listNames(String arg0) throws IOException {

		return _inner.listNames(arg0);
	}


	public boolean login(String username, String password, String account)
			throws IOException {

		return _inner.login(username, password, account);
	}


	public boolean login(String username, String password) throws IOException {

		return _inner.login(username, password);
	}


	public boolean logout() throws IOException {

		return _inner.logout();
	}


	public boolean makeDirectory(String pathname) throws IOException {

		return _inner.makeDirectory(pathname);
	}


	public String printWorkingDirectory() throws IOException {

		return _inner.printWorkingDirectory();
	}


	public boolean remoteAppend(String filename) throws IOException {

		return _inner.remoteAppend(filename);
	}


	public boolean remoteRetrieve(String filename) throws IOException {

		return _inner.remoteRetrieve(filename);
	}


	public boolean remoteStore(String filename) throws IOException {

		return _inner.remoteStore(filename);
	}


	public boolean remoteStoreUnique() throws IOException {

		return _inner.remoteStoreUnique();
	}


	public boolean remoteStoreUnique(String filename) throws IOException {

		return _inner.remoteStoreUnique(filename);
	}


	public boolean removeDirectory(String pathname) throws IOException {

		return _inner.removeDirectory(pathname);
	}


	public boolean rename(String from, String to) throws IOException {

		return _inner.rename(from, to);
	}


	public boolean retrieveFile(String arg0, OutputStream arg1)
			throws IOException {

		return _inner.retrieveFile(arg0, arg1);
	}


	public InputStream retrieveFileStream(String remote) throws IOException {

		return _inner.retrieveFileStream(remote);
	}


	public boolean sendNoOp() throws IOException {

		return _inner.sendNoOp();
	}


	public boolean sendSiteCommand(String arguments) throws IOException {

		return _inner.sendSiteCommand(arguments);
	}


	public void setBufferSize(int bufSize) {

		_inner.setBufferSize(bufSize);
	}


	public void setDataTimeout(int timeout) {

		_inner.setDataTimeout(timeout);
	}


	public boolean setFileStructure(int structure) throws IOException {

		return _inner.setFileStructure(structure);
	}


	public boolean setFileTransferMode(int mode) throws IOException {

		return _inner.setFileTransferMode(mode);
	}


	public boolean setFileType(int fileType, int formatOrByteSize)
			throws IOException {

		return _inner.setFileType(fileType, formatOrByteSize);
	}


	public boolean setFileType(int fileType) throws IOException {

		return _inner.setFileType(fileType);
	}


	public void setListHiddenFiles(boolean listHiddenFiles) {

		_inner.setListHiddenFiles(listHiddenFiles);
	}


	public void setParserFactory(FTPFileEntryParserFactory parserFactory) {

		_inner.setParserFactory(parserFactory);
	}


	public void setRemoteVerificationEnabled(boolean enable) {

		_inner.setRemoteVerificationEnabled(enable);
	}


	public void setRestartOffset(long offset) {

		_inner.setRestartOffset(offset);
	}


	public boolean storeFile(String remote, InputStream local)
			throws IOException {

		return _inner.storeFile(remote, local);
	}


	public OutputStream storeFileStream(String remote) throws IOException {

		return _inner.storeFileStream(remote);
	}


	public boolean storeUniqueFile(InputStream local) throws IOException {

		return _inner.storeUniqueFile(local);
	}


	public boolean storeUniqueFile(String remote, InputStream local)
			throws IOException {

		return _inner.storeUniqueFile(remote, local);
	}


	public OutputStream storeUniqueFileStream() throws IOException {

		return _inner.storeUniqueFileStream();
	}


	public OutputStream storeUniqueFileStream(String remote) throws IOException {

		return _inner.storeUniqueFileStream(remote);
	}


	public boolean structureMount(String pathname) throws IOException {

		return _inner.structureMount(pathname);
	}


	public int abor() throws IOException {

		return _inner.abor();
	}


	public int acct(String account) throws IOException {

		return _inner.acct(account);
	}


	public void addProtocolCommandListener(ProtocolCommandListener listener) {

		_inner.addProtocolCommandListener(listener);
	}


	public int allo(int bytes, int recordSize) throws IOException {

		return _inner.allo(bytes, recordSize);
	}


	public int allo(int bytes) throws IOException {

		return _inner.allo(bytes);
	}


	public int appe(String pathname) throws IOException {

		return _inner.appe(pathname);
	}


	public int cdup() throws IOException {

		return _inner.cdup();
	}


	public int cwd(String directory) throws IOException {

		return _inner.cwd(directory);
	}


	public int dele(String pathname) throws IOException {

		return _inner.dele(pathname);
	}


	public String getControlEncoding() {

		return _inner.getControlEncoding();
	}


	public int getReply() throws IOException {

		return _inner.getReply();
	}


	public int getReplyCode() {

		return _inner.getReplyCode();
	}


	public String getReplyString() {

		return _inner.getReplyString();
	}


	public String[] getReplyStrings() {

		return _inner.getReplyStrings();
	}


	public int help() throws IOException {

		return _inner.help();
	}


	public int help(String command) throws IOException {

		return _inner.help(command);
	}


	public boolean isStrictMultilineParsing() {

		return _inner.isStrictMultilineParsing();
	}


	public int list() throws IOException {

		return _inner.list();
	}


	public int list(String pathname) throws IOException {

		return _inner.list(pathname);
	}


	public int mdtm(String file) throws IOException {

		return _inner.mdtm(file);
	}


	public int mkd(String pathname) throws IOException {

		return _inner.mkd(pathname);
	}


	public int mode(int mode) throws IOException {

		return _inner.mode(mode);
	}


	public int nlst() throws IOException {

		return _inner.nlst();
	}


	public int nlst(String pathname) throws IOException {

		return _inner.nlst(pathname);
	}


	public int noop() throws IOException {

		return _inner.noop();
	}


	public int pass(String password) throws IOException {

		return _inner.pass(password);
	}


	public int pasv() throws IOException {

		return _inner.pasv();
	}


	public int port(InetAddress host, int port) throws IOException {

		return _inner.port(host, port);
	}


	public int pwd() throws IOException {

		return _inner.pwd();
	}


	public int quit() throws IOException {

		return _inner.quit();
	}


	public int rein() throws IOException {

		return _inner.rein();
	}


	public void removeProtocolCommandListener(ProtocolCommandListener listener) {

		_inner.removeProtocolCommandListener(listener);
	}


	public int rest(String marker) throws IOException {

		return _inner.rest(marker);
	}


	public int retr(String pathname) throws IOException {

		return _inner.retr(pathname);
	}


	public int rmd(String pathname) throws IOException {

		return _inner.rmd(pathname);
	}


	public int rnfr(String pathname) throws IOException {

		return _inner.rnfr(pathname);
	}


	public int rnto(String pathname) throws IOException {

		return _inner.rnto(pathname);
	}


	public int sendCommand(int command, String args) throws IOException {

		return _inner.sendCommand(command, args);
	}


	public int sendCommand(int command) throws IOException {

		return _inner.sendCommand(command);
	}


	public int sendCommand(String arg0, String arg1) throws IOException {

		return _inner.sendCommand(arg0, arg1);
	}


	public int sendCommand(String command) throws IOException {

		return _inner.sendCommand(command);
	}


	public void setControlEncoding(String encoding) {

		_inner.setControlEncoding(encoding);
	}


	public void setStrictMultilineParsing(boolean strictMultilineParsing) {

		_inner.setStrictMultilineParsing(strictMultilineParsing);
	}


	public int site(String parameters) throws IOException {

		return _inner.site(parameters);
	}


	public int smnt(String dir) throws IOException {

		return _inner.smnt(dir);
	}


	public int stat() throws IOException {

		return _inner.stat();
	}


	public int stat(String pathname) throws IOException {

		return _inner.stat(pathname);
	}


	public int stor(String pathname) throws IOException {

		return _inner.stor(pathname);
	}


	public int stou() throws IOException {

		return _inner.stou();
	}


	public int stou(String pathname) throws IOException {

		return _inner.stou(pathname);
	}


	public int stru(int structure) throws IOException {

		return _inner.stru(structure);
	}


	public int syst() throws IOException {

		return _inner.syst();
	}


	public int type(int fileType, int formatOrByteSize) throws IOException {

		return _inner.type(fileType, formatOrByteSize);
	}


	public int type(int fileType) throws IOException {

		return _inner.type(fileType);
	}


	public int user(String username) throws IOException {

		return _inner.user(username);
	}


	public void connect(InetAddress host, int port, InetAddress localAddr,
			int localPort) throws SocketException, IOException {

		_inner.connect(host, port, localAddr, localPort);
	}


	public void connect(InetAddress host, int port) throws SocketException,
			IOException {

		_inner.connect(host, port);
	}


	public void connect(InetAddress host) throws SocketException, IOException {

		_inner.connect(host);
	}


	public void connect(String hostname, int port, InetAddress localAddr,
			int localPort) throws SocketException, IOException {

		_inner.connect(hostname, port, localAddr, localPort);
	}


	public void connect(String hostname, int port) throws SocketException,
			IOException {

		_inner.connect(hostname, port);
	}


	public void connect(String hostname) throws SocketException, IOException {

		_inner.connect(hostname);
	}


	public int getConnectTimeout() {

		return _inner.getConnectTimeout();
	}


	public int getDefaultPort() {

		return _inner.getDefaultPort();
	}


	public int getDefaultTimeout() {

		return _inner.getDefaultTimeout();
	}


	public InetAddress getLocalAddress() {

		return _inner.getLocalAddress();
	}


	public int getLocalPort() {

		return _inner.getLocalPort();
	}


	public InetAddress getRemoteAddress() {

		return _inner.getRemoteAddress();
	}


	public int getRemotePort() {

		return _inner.getRemotePort();
	}


	public int getSoLinger() throws SocketException {

		return _inner.getSoLinger();
	}


	public int getSoTimeout() throws SocketException {

		return _inner.getSoTimeout();
	}


	public boolean getTcpNoDelay() throws SocketException {

		return _inner.getTcpNoDelay();
	}


	public boolean isConnected() {

		return _inner.isConnected();
	}


	public void setConnectTimeout(int connectTimeout) {

		_inner.setConnectTimeout(connectTimeout);
	}


	public void setDefaultPort(int port) {

		_inner.setDefaultPort(port);
	}


	public void setDefaultTimeout(int timeout) {

		_inner.setDefaultTimeout(timeout);
	}


	public void setReceiveBufferSize(int size) throws SocketException {

		_inner.setReceiveBufferSize(size);
	}


	public void setSendBufferSize(int size) throws SocketException {

		_inner.setSendBufferSize(size);
	}


	public void setServerSocketFactory(ServerSocketFactory factory) {

		_inner.setServerSocketFactory(factory);
	}


	public void setSocketFactory(SocketFactory factory) {

		_inner.setSocketFactory(factory);
	}


	public void setSoLinger(boolean on, int val) throws SocketException {

		_inner.setSoLinger(on, val);
	}


	public void setSoTimeout(int timeout) throws SocketException {

		_inner.setSoTimeout(timeout);
	}


	public void setTcpNoDelay(boolean on) throws SocketException {

		_inner.setTcpNoDelay(on);
	}


	public boolean verifyRemote(Socket socket) {

		return _inner.verifyRemote(socket);
	}

}
