package com.socketanywhere.unreliable;

public class URCentralInfo {

	private final INukeConnection _connInfo;
	
	public URCentralInfo(INukeConnection connInfo) {
		_connInfo = connInfo;
	}
	
	protected INukeConnection getConnInfo() {
		return _connInfo;
	}
	
	public static interface INukeConnection {
		
		public void eventAboutToSendData(UROutputStream stream, byte[] data, int off, int len);
		
		public void eventReceivedData(URInputStream stream, byte[] data, int off, int len);
		
	}
}
