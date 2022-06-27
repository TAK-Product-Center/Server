package tak.server.messaging;

@FunctionalInterface
public interface Messenger<T> {
	
	void send(T message);

}
