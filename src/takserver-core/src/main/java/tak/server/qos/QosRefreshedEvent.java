package tak.server.qos;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public class QosRefreshedEvent extends ApplicationEvent {

	public QosRefreshedEvent(ApplicationContext source) {
		super(source);
	}
	
}
