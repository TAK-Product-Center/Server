package tak.server.federation.hub.broker;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;

import tak.server.federation.GuardedStreamHolder;

public class HubConnectionStore {
    private static final Logger logger = LoggerFactory.getLogger(HubConnectionStore.class);

	private final Map<String, GuardedStreamHolder<FederatedEvent>> clientStreamMap = new ConcurrentHashMap<>();
    private final Map<String, GuardedStreamHolder<ROL>> clientROLStreamMap = new ConcurrentHashMap<>();
    private final Map<String, GuardedStreamHolder<FederateGroups>> clientGroupStreamMap = new ConcurrentHashMap<>();
    private final Map<String, FederateGroups> clientToGroups = new ConcurrentHashMap<>();
    private final Map<String, SSLSession> sessionMap =  new ConcurrentHashMap<>();
    private final Map<String, ConnectionInfo> connectionInfos = new ConcurrentHashMap<>();
    
    
    public Collection<ConnectionInfo> getConnectionInfos() {
    	return connectionInfos.values();
    }
    
    public void addConnectionInfo(String id, ConnectionInfo info) {
    	connectionInfos.put(id, info);
    }
    
    public void clearIdFromAllStores(String id) {
    	this.clientStreamMap.remove(id);
    	this.clientROLStreamMap.remove(id);
    	this.clientGroupStreamMap.remove(id);
    	this.clientToGroups.remove(id);
    	this.sessionMap.remove(id);
    	this.connectionInfos.remove(id);
    }
    
    public void clearStreamMaps() {
    	this.clientStreamMap.clear();
    	this.clientROLStreamMap.clear();
    	this.clientGroupStreamMap.clear();
    	this.clientToGroups.clear();
    	this.sessionMap.clear();
    	this.connectionInfos.clear();
    }
    
    public Map<String, GuardedStreamHolder<FederatedEvent>> getClientStreamMap() {
    	return Collections.unmodifiableMap(clientStreamMap);
    }
    
    public void addClientStreamHolder(String id, GuardedStreamHolder<FederatedEvent> holder) {
    	this.clientStreamMap.put(id, holder);
    }
    
    public Map<String, GuardedStreamHolder<ROL>> getClientROLStreamMap() {
    	return Collections.unmodifiableMap(clientROLStreamMap);
    }
    
    public void addRolStream(String id, GuardedStreamHolder<ROL> holder) {
    	this.clientROLStreamMap.put(id, holder);
    }
    
    public void removeRolStream(String id) {
    	this.clientROLStreamMap.remove(id);
    }
    
    public Map<String, GuardedStreamHolder<FederateGroups>> getClientGroupStreamMap() {
    	return Collections.unmodifiableMap(clientGroupStreamMap);
    }
    
    public void addGroupStream(String id, GuardedStreamHolder<FederateGroups> holder) {
    	this.clientGroupStreamMap.put(id, holder);
    }
    
    public void removeGroupStream(String id) {
    	this.clientGroupStreamMap.remove(id);
    }
    
    public Map<String, FederateGroups> getClientToGroupsMap() {
    	return Collections.unmodifiableMap(clientToGroups);
    }
    
    public void putFederateGroups(String id, FederateGroups federateGroups) {
    	this.clientToGroups.put(id, federateGroups);
    }
    
    public void removeFederateGroups(String id) {
    	this.clientToGroups.remove(id);
    }

    public Map<String, SSLSession> getSessionMap() {
    	return Collections.unmodifiableMap(sessionMap);
    }
    
    public void addSession(String id, SSLSession session) {
    	this.sessionMap.put(id, session);
    }
    
    public void removeSession(String id) {
    	this.sessionMap.remove(id);
    }
}
