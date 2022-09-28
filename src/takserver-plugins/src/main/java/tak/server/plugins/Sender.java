package tak.server.plugins;

public interface Sender<T> extends PluginLifecycle {
	
	/*
	 * Send a plugin message, through TAK Server to a specified set of groups.
	 * 
	 * @param  message  TAK Proto message to send. Groups, destination callsigns and destination client UIDS may be specified in the message object.
	 *  The message will be delivered to any client connected to the TAK server that is a member of one or more of these groups. Delivery can be specified to client UIDs (preferred) or callsigns.
	 *  Callsign addressing is not recommended because callsigns are self-declared.
	 * 
	 * @see atakmap.commoncommo.protobuf.v1.Message
	 */
	void send(T message);
	
	void send(T message, String feedUuid);
}
