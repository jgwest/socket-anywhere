package com.socketanywhere.net;

@ImmutableSafe
public class ConstructorTransfer<T> {
	private final Object lock = new Object();
	private T inner;
	
	
	public ConstructorTransfer(T inner) {
		this.inner = inner;
	}
	
	public T get() {
		
		synchronized(lock) {
			T result = inner;
			inner = null;
			
			return result;
		}
	}

	@Override
	public String toString() {
		throw new UnsupportedOperationException();
	}
}
