package com.socketanywhere.util;

public abstract class ManagedThread {

	private final ThreadWrapper _wrapper;
	
	public ManagedThread(String name, boolean isDaemon) {
		_wrapper = new ThreadWrapper(name, this);
		_wrapper.setDaemon(isDaemon);
	}

	public void start() {
		_wrapper.start();
	}
	
	public void setPriority(int priority) {
		_wrapper.setPriority(priority);
	}
	
	public boolean isStarted() {
		return _wrapper.isWrapperStarted();
	}
	
	public boolean isFinished() {
		return _wrapper.isWrapperFinished();	
	}
	
	abstract public void run();
		
	private class ThreadWrapper extends Thread {
		
		private final ManagedThread _parent;
		private boolean isStarted = false;
		private boolean isFinished = false; 
		
		private final Object lock = new Object();
		
		ThreadWrapper(String name, ManagedThread thread) {
			super(name);
			this._parent = thread;
		}
		
		
		@Override
		public void run() {
			ManagedThreadCatalog.getInstance().addManagedThread(_parent);
			synchronized(lock) {
				isStarted = true;
			}
			try {
				_parent.run();
			} finally {
				synchronized(lock) {
					isFinished = true;
				}
				ManagedThreadCatalog.getInstance().removeManagedThread(_parent);
			}
		}
		
		public boolean isWrapperStarted() {
			synchronized(lock) {
				return isStarted;
			}
		}
		
		public boolean isWrapperFinished() {
			synchronized(lock) {
				return isFinished;
			}
		}
	}
	
}
