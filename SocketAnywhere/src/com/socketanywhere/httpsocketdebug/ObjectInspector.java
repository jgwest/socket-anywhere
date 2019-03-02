package com.socketanywhere.httpsocketdebug;

import com.socketanywhere.multiplexingnew.MNConnectionBrain;
import com.socketanywhere.multiplexingnew.MQMessage;
import com.socketanywhere.multiplexingnew.MessageQueue;

public class ObjectInspector {

	public static void inspect(MonitoredObject mo, StringBuilder sb) {
		Object o = mo.object.get();
		if(o != null && o instanceof MNConnectionBrain) {
			
			MNConnectionBrain brain = (MNConnectionBrain)o;
			
			MessageQueue mq = new MessageQueue(ObjectInspector.class);
			
			MQMessage message = new MQMessage("Debug", null, null, mq);
			
			brain.getMessageQueue().addMessage(message);
			
			MQMessage response = mq.getNextMessageBlocking();
			sb.append((String)response.getParam());
			
			
			
		}
	}
}
