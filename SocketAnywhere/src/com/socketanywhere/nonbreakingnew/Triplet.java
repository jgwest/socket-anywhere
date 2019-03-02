package com.socketanywhere.nonbreakingnew;

public class Triplet {
	final private String _connectorUuid;
	final private int _connectorId;
	
	final private boolean _areWeConnector;

	public Triplet(String connectorUuid, int connectorId, boolean areWeConnector) {
		this._connectorUuid = connectorUuid;
		this._connectorId = connectorId;
		this._areWeConnector = areWeConnector;
	}
	
	public int getConnectorId() {
		return _connectorId;
	}
	
	public String getConnectorUuid() {
		return _connectorUuid;
	}
	
	public boolean areWeConnector() {
		return _areWeConnector;
	}

	@Override
	public int hashCode() {
		return _connectorUuid.hashCode() + _connectorId + (areWeConnector() ? 1 : 0);
		
//		return ("{"+_connectorUuid+", "+_connectorId+", "+areWeConnector()+")").hashCode();
	}
	
	@Override
	public boolean equals(Object paramObject) {
		if(!(paramObject instanceof Triplet)) {
			return false;
		}
		Triplet other = (Triplet)paramObject;
		
		if(other.getConnectorId() != getConnectorId()) {
			return false;
		}
		
		if(!other.getConnectorUuid().equals(getConnectorUuid())) {
			return false;
		}
		
		return other.areWeConnector() == areWeConnector();
		
		
	}
	
	public String toString() {
		String shortUuid = _connectorUuid.substring(_connectorUuid.length()-5,  _connectorUuid.length());
		return "{"+(_areWeConnector ? "C" : "S") +shortUuid+", "+_connectorId+"}";
	}
}
