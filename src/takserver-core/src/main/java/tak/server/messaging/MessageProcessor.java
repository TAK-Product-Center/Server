package tak.server.messaging;

@FunctionalInterface
public interface MessageProcessor<T> {
		
	void process(T message);

}
