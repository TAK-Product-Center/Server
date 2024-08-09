package tak.server.federation.hub.broker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.FederateGroups;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.ROL;

import tak.server.federation.GuardedStreamHolder;
import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.broker.events.StreamReadyEvent;
import tak.server.federation.hub.broker.events.UpdatePolicy;

public class HubConnectionStore {
    private static final Logger logger = LoggerFactory.getLogger(HubConnectionStore.class);

	private final Map<String, GuardedStreamHolder<FederatedEvent>> clientStreamMap = new ConcurrentHashMap<>();
    private final Map<String, GuardedStreamHolder<ROL>> clientROLStreamMap = new ConcurrentHashMap<>();
    private final Map<String, GuardedStreamHolder<FederateGroups>> clientGroupStreamMap = new ConcurrentHashMap<>();
    private final Map<String, FederateGroups> clientToGroups = new ConcurrentHashMap<>();
    private final Map<String, HubConnectionInfo> connectionInfos = new ConcurrentHashMap<>();
    
    private final Map<String, List<ROL>> tempRolCache = new ConcurrentHashMap<>();
    
    public void cacheRol( ROL rol, String id) {
    	if (!tempRolCache.containsKey(id)) 
    		tempRolCache.put(id, new ArrayList<>());
    	
    	tempRolCache.get(id).add(rol);
    }
    
    public Collection<HubConnectionInfo> getConnectionInfos() {
    	return connectionInfos.values();
    }

    public void addConnectionInfo(String id, HubConnectionInfo info) {
    	connectionInfos.put(id, info);
    }
    
    public void clearIdFromAllStores(String id) {
    	this.clientStreamMap.remove(id);
    	this.clientROLStreamMap.remove(id);
    	this.clientGroupStreamMap.remove(id);
    	this.clientToGroups.remove(id);
    	this.connectionInfos.remove(id);
    	this.tempRolCache.remove(id);
    }
    
    public void clearStreamMaps() {
    	this.clientStreamMap.clear();
    	this.clientROLStreamMap.clear();
    	this.clientGroupStreamMap.clear();
    	this.clientToGroups.clear();
    	this.connectionInfos.clear();
    	this.tempRolCache.clear();
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
    	List<ROL> cachedRol = tempRolCache.get(id);
    	if (cachedRol != null) {
    		FederationHubDependencyInjectionProxy.getSpringContext()
    			.publishEvent(new StreamReadyEvent<ROL>(this, StreamReadyEvent.StreamReadyType.ROL, id, new ArrayList<ROL>(cachedRol)));
    	}
    	tempRolCache.remove(id);
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
}
