package tak.server.ignite;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class IgniteReconnectEventHandler {
	private static final Executor executor = Executors.newSingleThreadExecutor();
	
	private static final Set<Runnable> preActions = ConcurrentHashMap.newKeySet();
	private static final Set<Runnable> postActions = ConcurrentHashMap.newKeySet();
	private static final Set<Runnable> services = ConcurrentHashMap.newKeySet();
	private static final Set<Runnable> listeners = ConcurrentHashMap.newKeySet();
	
	public static void registerPreAction(Runnable callback) {
		preActions.add(callback);
        callback.run();
    }
	
	public static void unregisterPreAction(Runnable callback) {
		preActions.remove(callback);
    }
	
	public static void registerPostAction(Runnable callback) {
		postActions.add(callback);
        callback.run();
    }
	
	public static void unregisterPostAction(Runnable callback) {
		postActions.remove(callback);
    }
	
	public static void registerService(Runnable callback) {
		services.add(callback);
        callback.run();
    }
	
	public static void unregisterService(Runnable callback) {
		services.remove(callback);
    }

    public static void registerListener(Runnable callback) {
        listeners.add(callback);
        callback.run();
    }
    
    public static void unregisterListener(Runnable callback) {
    	listeners.remove(callback);
    }

    public static void reconnect() {
		executor.execute(() -> {
			// events that need to happen before services run
			preActions.forEach(Runnable::run);
			services.forEach(Runnable::run);
			listeners.forEach(Runnable::run);
			// events that need to happen after services run
			postActions.forEach(Runnable::run);
		});
	}
}
