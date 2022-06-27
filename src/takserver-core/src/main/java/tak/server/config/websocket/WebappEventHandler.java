

package tak.server.config.websocket;

public interface WebappEventHandler<T> {
   public void onWebEvent(T event);
}
