package tak.server.plugins;

import org.springframework.context.ApplicationEvent;

public class PluginsStartedEvent extends ApplicationEvent {	

	private String message;

	public PluginsStartedEvent(Object source, String message) {
		super(source);
		this.message = message;
	}
	public String getMessage() {
		return message;
	}

}
