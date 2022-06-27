package tak.server.plugins;

public interface Receiver<T> extends PluginLifecycle {
	void onMessage(T message);
}
