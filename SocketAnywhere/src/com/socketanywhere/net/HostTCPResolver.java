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

package com.socketanywhere.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/** Simple name resolution algorithm implementation; queries network addresses to get all the 
 * local addresses, and otherwise uses InetAddress for name resolution. */
public class HostTCPResolver implements INameResolver {

	@Override
	public List<TLAddress> getLocalAddresses() {
		ArrayList<TLAddress> result = new ArrayList<TLAddress>();

		NetworkInterface iface = null;
		try {
			for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
				iface = (NetworkInterface) ifaces.nextElement();
				InetAddress ia = null;

				for (Enumeration<InetAddress> ips = iface.getInetAddresses(); ips.hasMoreElements();) {
					ia = (InetAddress) ips.nextElement();
					if (ia.getAddress().length == 4) {
						String chn = ia.getCanonicalHostName();
						String ha = ia.getHostAddress();

						TLAddress chn_tl = new TLAddress(chn);
						if (!result.contains(chn_tl)) {
							result.add(chn_tl);
						}

						TLAddress ha_tl = new TLAddress(ha); 
						if (!result.contains(ha_tl)) {
							result.add(ha_tl);
						}

					}
				}
			}
		} catch (SocketException se) { /* Ignore this error */
		}

		return result;

	}

	public List<TLAddress> resolveAddress(TLAddress address) {
		List<TLAddress> result = new ArrayList<TLAddress>();

		if (address.getHostname() == null
				|| address.getHostname().trim().length() == 0
				|| address.getHostname().equalsIgnoreCase("localhost")
				|| address.getHostname().equalsIgnoreCase("::1")
				|| address.getHostname().equalsIgnoreCase("127.0.0.1")) {
			// TODO: MEDIUM - I need a real 'is local host' method for the above
			List<TLAddress> tmpResult = getLocalAddresses();

			
			for(TLAddress t : tmpResult) {
				result.add(new TLAddress(t.getHostname(), address.getPort()));
//				t.setPort(address.getPort());
			}
		
		} else {
		
			List<String> addysToListen = resolveRemoteAddress(address.getHostname());
			
			for (String s : addysToListen) {
				TLAddress ta = new TLAddress(s, address.getPort());
				
				if(!result.contains(ta)) {
					result.add(ta);
				}
			}
		}

		return result;
	}

	private static List<String> resolveRemoteAddress(String name) {
		ArrayList<String> result = new ArrayList<String>();

		try {
			InetAddress[] iarr = InetAddress.getAllByName(name);

			for (int x = 0; x < iarr.length; x++) {

				String chn = iarr[x].getCanonicalHostName();
				String ha = iarr[x].getHostAddress();

				if (!result.contains(chn)) {
					result.add(chn);
				}

				if (!result.contains(ha)) {
					result.add(ha);
				}
			}

			if (!result.contains(name)) {
				// Ensure at a minimum that our specified name gets in
				result.add(name);
			}

		} catch (UnknownHostException e) {
			result.add(name);
		}

		return result;
	}

	
}
