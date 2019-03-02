package com.socketanywhere.multiplexingnew;


public class MQMessage {

	private final String _name;
	private final Class<?> _source;
	private final Object _param;
	private final MessageQueue _responseQueue;
	
	public MQMessage(String name, Class<?> source, Object param, MessageQueue responseQueue) {
		this._name = name;
		this._source = source;
		this._param = param;
		this._responseQueue = responseQueue;
	}

	public String getName() {
		return _name;
	}
	
	public Class<?> getSource() {
		return _source;
	}
	public Object getParam() {
		return _param;
	}
	
	public MessageQueue getResponseQueue() {
		return _responseQueue;
	}
	
	@Override
	public String toString() {
		String name = _source != null ? _source.getSimpleName() : ""; 
		
		return MQMessage.class.getSimpleName()+" - _name: "+_name+" source: "+name+"     _param:{"+_param+"}       _responseQueue:"+_responseQueue;
	}
}
