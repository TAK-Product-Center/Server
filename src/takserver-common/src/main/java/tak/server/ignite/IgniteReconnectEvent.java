package tak.server.ignite;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public class IgniteReconnectEvent extends ApplicationEvent {

	public IgniteReconnectEvent(ApplicationContext source) {
		super(source);
	}
}
