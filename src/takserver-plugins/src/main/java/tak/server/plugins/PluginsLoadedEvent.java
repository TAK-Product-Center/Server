package tak.server.plugins;

import org.springframework.context.ApplicationEvent;

public class PluginsLoadedEvent extends ApplicationEvent {	
	
	private static final long serialVersionUID = 1987349857934L;
	private String message;

	public PluginsLoadedEvent(Object source, String message) {
		super(source);
		this.message = message;
	}
	public String getMessage() {
		return message;
	}

}
