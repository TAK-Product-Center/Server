package tak.server.plugins;

public class ReservedConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 9879879871L;

	public ReservedConfigurationException(String keyword) {
        super("'" + keyword + "' is a reserved configuration option. Do not use this in your plugin configuration.");
    }

}
