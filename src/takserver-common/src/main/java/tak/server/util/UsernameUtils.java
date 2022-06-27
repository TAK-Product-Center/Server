package tak.server.util;

public class UsernameUtils {
	
	public static final String ERROR_MESSAGE_FOR_INVALID_USERNAME = "Username is invalid. Username requirements: minimum of 4 characters and contains only letters, numbers, dots, underscores and hyphens.";
	
	public static boolean isValidUsername(String username) {
		
		if (username == null) {
			return false;
		}
		
		if (username.length() < 4) {
			return false;
		}
		
		if (!username.matches("^[a-zA-Z0-9_.\\-]+$")) {
			return false;
		} 
		
		return true;
	}
	
}
