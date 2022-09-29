package tak.server.federation.hub.broker.events;

import org.springframework.context.ApplicationEvent;

public abstract class BrokerServerEvent extends ApplicationEvent {
    private static final long serialVersionUID = -2842031725284479522L;
    public BrokerServerEvent(Object source) {
        super(source);
    }
}
