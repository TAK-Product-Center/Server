package tak.server.ignite;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public class ApiIgniteBroker {
	
	public static <S, R> R brokerNonLocalServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return IgniteBroker.brokerApiNonLocalServiceCalls(function, proxyName, clazz);
	}
	
	public static <S> void brokerNonLocalVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		IgniteBroker.brokerApiNonLocalVoidServiceCalls(function, proxyName, clazz);
	}
	
	public static <S, R> R brokerServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return IgniteBroker.brokerApiServiceCalls(function, proxyName, clazz);
	}
	
	public static <S> void brokerVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		IgniteBroker.brokerApiVoidServiceCalls(function, proxyName, clazz);
	}
}
