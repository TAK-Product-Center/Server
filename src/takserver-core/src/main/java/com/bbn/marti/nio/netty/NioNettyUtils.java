package com.bbn.marti.nio.netty;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.bbn.marti.util.Assertion;

public class NioNettyUtils {
	
	private final static Logger log = Logger.getLogger(NioNettyUtils.class);
	
	public static List<NetworkInterface> validateMulticastInterfaces(List<NetworkInterface> interfs) {
		List<NetworkInterface> validInterfs = interfs.stream().filter(interf -> isValidMulticastInterface(interf)).collect(Collectors.toList());
		
		if (validInterfs.size() == 0) {
			log.warn("Defined network interfaces do not support multicast -- attempting to fetch all local interfaces that do");
			validInterfs = allMulticastInterfs();
		}
		
		return validInterfs;
	}
	
	private static boolean isValidMulticastInterface(NetworkInterface interf) {
		Assertion.notNull(interf);

		try {
			if (interf.supportsMulticast()) {
				return true;
			} else {
				log.warn("Given network interface claims to not support multicast -- excluding from interfaces list: " + interf);
			}
		} catch (Exception e) {
			log.error("Error encountered trying to determine multicast properties of network interface -- excluding from multicast list: " + interf, e);
		}
		
		return false;
	}
	
	public static List<NetworkInterface> allMulticastInterfs() {
		List<NetworkInterface> result = new LinkedList<NetworkInterface>();

		Enumeration<NetworkInterface> interfsEnum = null;
		try {
			interfsEnum = NetworkInterface.getNetworkInterfaces();
			while (interfsEnum.hasMoreElements()) {
				NetworkInterface finger = interfsEnum.nextElement();
				try {
					if (finger.supportsMulticast()) {
						result.add(finger);
						log.warn("Adding interface to multicast list: " + finger.getDisplayName());
					}
				} catch (IOException e) {
					log.warn("Error trying to inspect multicast properties of network interface -- excluding from multicast list: " + finger, e);
				}
			}
		} catch (IOException e) {
			log.warn("Error trying to inspect network interfaces", e);
			return result;
		}

		return result;
	}
}
