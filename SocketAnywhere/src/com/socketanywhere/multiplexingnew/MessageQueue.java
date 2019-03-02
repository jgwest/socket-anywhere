/*
	Copyright 2014 Jonathan West

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

package com.socketanywhere.multiplexingnew;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.socketanywhere.net.ImmutableSafe;
import com.socketanywhere.net.SoAnUtil;
import com.socketanywhere.nonbreakingnew.NBLog;

@ImmutableSafe
public class MessageQueue {

	private final Object _lock = new Object();
	
	private Queue<MQMessage> _messages = new LinkedList<MQMessage>();
	
	private String _debug;
	
	public MessageQueue(Object debugObj) {
		_debug = debugObj.getClass().getName();
		StackTraceElement[] arr = Thread.currentThread().getStackTrace();
		
		StackTraceElement ste = arr[3];
		_debug += " "+ste.getClassName()+"."+ste.getMethodName()+":"+ste.getLineNumber();
	}

	public MessageQueue(String debug) {
		_debug = debug;
		StackTraceElement[] arr = Thread.currentThread().getStackTrace();
		
		StackTraceElement ste = arr[Math.min(3, arr.length-1)];
		_debug += " "+ste.getClassName()+"."+ste.getMethodName()+":"+ste.getLineNumber();
		
	}

	public void addMessage(MQMessage msg) {

		synchronized(_lock) {
			
			_messages.offer(msg);
			_lock.notifyAll();
		}
	}

	/** Returns empty queue if expired */
	public Queue<MQMessage> getNextMessagesBlockingTimeout(long expireTimeInNanos) {
		long absExpireTimeInNanos = System.nanoTime() + expireTimeInNanos;
		
		try {
			synchronized (_lock) {
				while(_messages.size() == 0 && System.nanoTime() < absExpireTimeInNanos) {
					_lock.wait(100);
				}
				
				Queue<MQMessage> result = _messages;
				_messages = new LinkedList<MQMessage>();
				return result;
			}
			
		} catch(InterruptedException ie) {
			ie.printStackTrace();
			return null;
		}
	}

	/** Returns empty queue if expired */
	public MQMessage getNextMessageBlockingTimeout(long expireTimeInNanos) {
		long absExpireTimeInNanos = System.nanoTime() + expireTimeInNanos;
		
		try {
			synchronized (_lock) {
				while(_messages.size() == 0 && System.nanoTime() < absExpireTimeInNanos) {
					_lock.wait(100);
				}

				return _messages.poll();
			}
			
		} catch(InterruptedException ie) {
			ie.printStackTrace();
			return null;
		}
	}

	
	
	/** param is whether or not to complain if no messages are received in 5 seconds */
	public Queue<MQMessage> getNextMessagesBlocking(boolean reportNoResponse) {
		boolean itGotOut = false;
		try {
			long fiveSecondsFromNowInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS);
			
			synchronized (_lock) {
				while(_messages.size() == 0) {
					_lock.wait(5000);

					if(reportNoResponse && System.nanoTime() > fiveSecondsFromNowInNanos && _messages.size() == 0) {
						NBLog.err("Waiting for messages in: "+_debug);
						NBLog.err(SoAnUtil.convertStackTrace(new Throwable()));
						NBLog.err("------------------------------");
						itGotOut = true;
						
					}
					
				}				
				Queue<MQMessage> result = _messages;
				_messages = new LinkedList<MQMessage>();
				return result;
			}
			
			
		} catch(InterruptedException ie) {
			ie.printStackTrace();
			return null;
		} finally {
			if(itGotOut) {
				NBLog.err("it got out!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}			
		}
	}
	
	public MQMessage getNextMessageBlocking() {
		return getNextMessageBlocking(true);
	}
	
	public MQMessage getNextMessageBlocking(boolean reportNoResponse) {
		try {
//			StackTraceElement ste = Thread.currentThread().getStackTrace()[3];
			
			long fiveSecondsFromNowInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(5, TimeUnit.SECONDS);
			
			boolean itGotOut = false;
			
			synchronized (_lock) {
				while(_messages.size() == 0) {
//					System.out.println("GET - "+_debug+" "+_messages.size());
					_lock.wait(5000);
					
					if(System.nanoTime() > fiveSecondsFromNowInNanos && _messages.size() == 0 && reportNoResponse) {
						
						
						NBLog.err("Waiting for messages in: "+_debug);
						NBLog.err(SoAnUtil.convertStackTrace(new Throwable()));
						NBLog.err("------------------------------");

						itGotOut = true;
						
					}
					
				}
				if(itGotOut && reportNoResponse) {
					NBLog.err("it got out!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" +_debug);
				}
				
				return _messages.poll();
			}
		} catch(InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getDebug() {
		return _debug;
	}
	
	
	public List<MQMessage> debugCloneCurrContents() {
		List<MQMessage> result = new ArrayList<MQMessage>();

		synchronized (_lock) {
			
			for(MQMessage curr : _messages) {
				
				result.add(new MQMessage(curr.getName(), curr.getSource(), curr.getParam(), curr.getResponseQueue()));
				
			}
		}
		
		return result;
	}
}


