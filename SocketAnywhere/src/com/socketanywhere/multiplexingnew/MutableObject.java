package com.socketanywhere.multiplexingnew;

public class MutableObject<E> {
	
	E _inner;
	
	public MutableObject(E inner) {
		_inner = inner;
	}
	
	public E get() {
		return _inner;
	}
	
	public void set(E obj) {
		_inner = obj;
	}

}
