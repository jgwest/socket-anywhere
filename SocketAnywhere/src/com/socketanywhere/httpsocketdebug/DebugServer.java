package com.socketanywhere.httpsocketdebug;

import java.util.List;

public class DebugServer extends NanoHTTPD {
	public DebugServer(int port) {
		super(port);
		System.out.println("Listening on port:"+port);
	}


	@Override
	public Response serve(IHTTPSession session) {
		 StringBuilder sb = new StringBuilder();
	        sb.append("<html>");
	        
	        List<MonitoredObject> mol = DebugInstance.getInstance().getMonitoredObjects();

	        for(MonitoredObject mo : mol) {
	        	ObjectInspector.inspect(mo, sb);
	        	
	        }
	        
//	        for(MonitoredObje)
	        
	        
	        sb.append("</html>");
	        
	    return new Response(sb.toString());
	}
}