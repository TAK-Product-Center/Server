package tak.server.ignite;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 */
public class PluginIgniteBroker {
	
	
	public static <S, R> R brokerServiceCalls(Function<S, R> function, String proxyName, Class<?> clazz) {
		return IgniteBroker.brokerPluginServiceCalls(function, proxyName, clazz);
	}
	
	public static <S> void brokerVoidServiceCalls(Consumer<S> function, String proxyName, Class<?> clazz) {
		IgniteBroker.brokerPluginVoidServiceCalls(function, proxyName, clazz);
	}
}
