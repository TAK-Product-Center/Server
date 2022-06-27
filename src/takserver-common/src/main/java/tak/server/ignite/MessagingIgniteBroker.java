package tak.server.ignite;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public class MessagingIgniteBroker {
	
	public static <S, R> R brokerNonLocalServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return IgniteBroker.brokerMessagingNonLocalServiceCalls(function, proxyName, clazz);
	}
	
	public static <S> void brokerNonLocalVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		IgniteBroker.brokerMessagingNonLocalVoidServiceCalls(function, proxyName, clazz);
	}
	
	public static <S, R> R brokerServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return IgniteBroker.brokerMessagingServiceCalls(function, proxyName, clazz);
	}
	
	public static <S> void brokerVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		IgniteBroker.brokerMessagingVoidServiceCalls(function, proxyName, clazz);
	}
}
