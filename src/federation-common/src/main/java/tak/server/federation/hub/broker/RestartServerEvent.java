package tak.server.federation.hub.broker;

import org.springframework.context.ApplicationEvent;

public class RestartServerEvent extends ApplicationEvent {
    private static final long serialVersionUID = -2842031725284479522L;
    public RestartServerEvent(Object source) {
        super(source);
    }
}
