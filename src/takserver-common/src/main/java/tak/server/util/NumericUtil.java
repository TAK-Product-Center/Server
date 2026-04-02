package tak.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NumericUtil {

	static final Logger logger = LoggerFactory.getLogger(NumericUtil.class);

	public static double parseDoubleOrDefault(String n, double d) {
	    try {
	        return Double.parseDouble(n);
	    } catch (NumberFormatException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception in parseDoubleOrDefault", e);
			}

	        return d;
	    }
	}

	public static int parseIntOrDefault(String n, int d) {
	    try {
	        return Integer.parseInt(n);
	    } catch (NumberFormatException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception in parseIntOrDefault", e);
			}

	        return d;
	    }
	}

}
