package tak.server.util;

public class NumericUtil {

	public static double parseDoubleOrDefault(String n, double d) {
	    try {
	        return Double.parseDouble(n);
	    } catch (NumberFormatException e) {
	        return d;
	    }
	}

	public static int parseIntOrDefault(String n, int d) {
	    try {
	        return Integer.parseInt(n);
	    } catch (NumberFormatException e) {
	        return d;
	    }
	}

}
