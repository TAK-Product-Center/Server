package com.bbn.marti.sync.api;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.bbn.marti.config.Filter;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.RemoteCachedSubscription;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.RemoteSubscriptionMetrics;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;
import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;

import tak.server.CommonConstants;
import tak.server.Constants;
import tak.server.cache.ActiveGroupCacheHelper;


/*
 * 
 * REST API providing a view into subscription information
 * 
 */
@RestController
public class SubscriptionApi extends BaseRestController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionApi.class);

    private static final String CONTEXT = "SubscriptionApi";

    // keep a reference to the currently active request
    @Autowired
    private HttpServletRequest request;

    @Autowired
    SubscriptionManagerLite subscriptionManager;
    
    @Autowired
    ContactManager contactManager;

    @Autowired
    private CommonUtil martiUtil;
    
    @Autowired
    @Qualifier(CommonConstants.MESSENGER_GROUPMANAGER_NAME)
    private GroupManager groupManager;

    @Autowired
    private Validator validator;

    @Autowired
    private ActiveGroupCacheHelper activeGroupCacheHelper;
    
    // GET all subscriptions
    @RequestMapping(value = "/subscriptions/all", method = RequestMethod.GET)
    ResponseEntity<ApiResponse<Set<SubscriptionInfo>>> getAllSubscriptions(
            @RequestParam(value = "sortBy", defaultValue = "CALLSIGN") SubscriptionSortField sortBy,
            @RequestParam(value = "direction", defaultValue = "ASCENDING") SubscriptionSortOrder direction,
            @RequestParam(value = "page", defaultValue = "-1") int page,
            @RequestParam(value = "limit", defaultValue = "-1") int limit) throws RemoteException {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("groupManager: " + groupManager.getClass().getName());
    	}
    	        
        final NavigableSet<SubscriptionInfo> subs = new ConcurrentSkipListSet<>();

        List<RemoteSubscription> subscriptions = null;
        
        String sort = sortBy == SubscriptionSortField.CALLSIGN ? "callsign" : "clientUid";
        int dir = direction == SubscriptionSortOrder.ASCENDING ? 1 : -1;

        // let admin see all subscriptions, otherwise group filter the list.
        if (martiUtil.isAdmin()) {
        	subscriptions = subscriptionManager.getCachedSubscriptionList(null, sort, dir, page, limit);
        } else {
        	try {
        		// Get group vector for the user associated with this session
        		String groupVector = martiUtil.getGroupBitVector(request);

        		subscriptions = subscriptionManager.getSubscriptionsWithGroupAccess(groupVector, false);
        		
        		boolean reversed = false;
                
                if (direction.equals(SubscriptionSortOrder.DESCENDING)) {
                    reversed = true;
                }
                
                if (sortBy.equals(SubscriptionSortField.CALLSIGN)) {
                    Collections.sort(subscriptions, RemoteSubscription.sortByCallsign(reversed));
                } else {
                    Collections.sort(subscriptions, RemoteSubscription.sortByClientUid(reversed));
                }
        	} catch (Exception e) {
        		logger.debug("exception getting group membership for current web user " + e.getMessage());
        	}
        }
        
        java.util.Objects.requireNonNull(subscriptions, "subscription list");
        
        if (logger.isDebugEnabled()) {
        	logger.debug("subscriptions: " + subscriptions);
        }
        
        for (RemoteSubscription subscription : subscriptions) {
            
            SubscriptionInfo si = new SubscriptionInfo(subscription);
            if (!Strings.isNullOrEmpty(subscription.getConnectionId())) {
            	String outVector = "";
            	String inVector = "";
            	
            	if (subscription instanceof RemoteCachedSubscription) {
            		RemoteCachedSubscription rcs = (RemoteCachedSubscription) subscription;
            		outVector = rcs.outboundVector;
            		inVector = rcs.inboundVector;
            	} else {
            		outVector = groupManager.getCachedOutboundGroupVectorByConnectionId(subscription.getConnectionId()); 
            		inVector = groupManager.getCachedInboundGroupVectorByConnectionId(subscription.getConnectionId()); 
            	}  
            	
            	Collection<Group> allOutGroups = groupManager.getAllGroups();
            	Collection<Group> allInGroups = allOutGroups.stream()
            			.map(group -> group.getCopy())
            			.peek(group -> group.setDirection(Direction.IN))
            			.collect(Collectors.toSet());
            			
                NavigableSet<Group> outGroupsForSub = RemoteUtil.getInstance().getGroupsForBitVectorString(outVector, allOutGroups);
                NavigableSet<Group> inGroupsForSub = RemoteUtil.getInstance().getGroupsForBitVectorString(inVector, allInGroups);
                
                outGroupsForSub.addAll(inGroupsForSub);
    
                if (logger.isDebugEnabled()) {
                    logger.debug("groups for sub: " + outGroupsForSub);
                }
                
                si.setGroups(outGroupsForSub);
            }
            
            subs.add(si);

            RemoteSubscriptionMetrics metrics = subscriptionManager.
                    getSubscriptionMetricsForClientUid(subscription.clientUid);
            if (metrics != null) {
                si.setMetrics(metrics);
            }
        }
        
        return new ResponseEntity<ApiResponse<Set<SubscriptionInfo>>>(new ApiResponse<Set<SubscriptionInfo>>(Constants.API_VERSION, SubscriptionInfo.class.getSimpleName(), subs), new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = "/subscriptions/add", method = RequestMethod.POST)
    public ResponseEntity<ApiResponse<SubscriptionInfo>> addSubscription(@RequestBody tmpStaticSub tmpSub){
        try {
            RemoteSubscription remoteSubscription = new RemoteSubscription();
            //Validate uid
            validator.getValidInput(CONTEXT, tmpSub.uid, "MartiSafeString", MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
            remoteSubscription.uid = tmpSub.uid.trim();
            remoteSubscription.clientUid = tmpSub.uid.trim();

            //Checking port to see if they still entered invalid port after error messages were shown
            int port = Integer.parseInt(tmpSub.subport);
            if (port < 1 || port > 65535){
                return new ResponseEntity<>(new ApiResponse<>(Constants.API_VERSION, null, null), new HttpHeaders(), HttpStatus.BAD_REQUEST);
            }
            remoteSubscription.to = tmpSub.protocol + ":" + tmpSub.subaddr + ":" + tmpSub.subport;

            String xpathString = tmpSub.xpath.replaceAll("\\s", "");
            //If user enters blank (or spaces) for xpath set it to null in sub object
            //null xpath means it is set to * in subscriptionManager.initializeStaticSubscription
            if(xpathString.equals("")){
                remoteSubscription.xpath = null;
            }
            else {
                //Validate xpath
                xpathString = validator.getValidInput(CONTEXT, xpathString, "XpathBlackList",
							2048, true);
                XPath xpath = XPathFactory.newInstance().newXPath();
                xpath.compile(xpathString);
                remoteSubscription.xpath = xpathString;
            }

            List<String> filterGroupsList;
            if(!tmpSub.filterGroups.replaceAll("\\s", "").equals("")){
                //Remove leading and trailing whitespace from all elements in filterGroups list
                filterGroupsList = Arrays.asList(tmpSub.filterGroups.split(","));
                filterGroupsList.replaceAll(String::trim);
                //Validate each group name entered
                for(String groupName : filterGroupsList){
                    validator.getValidInput(CONTEXT + "validate sub filter groups", groupName, "MartiSafeString", MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
                }
            }
            else{
                filterGroupsList = new ArrayList<String>();
            }
            remoteSubscription.filterGroups = filterGroupsList;

            if (!Strings.isNullOrEmpty(tmpSub.iface)) {
                remoteSubscription.iface = tmpSub.iface;
            }

            subscriptionManager.addSubscription(remoteSubscription);
            SubscriptionInfo info = new SubscriptionInfo(remoteSubscription);
            return new ResponseEntity<ApiResponse<SubscriptionInfo>>(new ApiResponse<SubscriptionInfo>(Constants.API_VERSION, SubscriptionInfo.class.getSimpleName(), info), new HttpHeaders(), HttpStatus.CREATED);
        }
        catch (Exception e){
            logger.error(e.getMessage());
            return new ResponseEntity<ApiResponse<SubscriptionInfo>>(new ApiResponse<SubscriptionInfo>(Constants.API_VERSION, null, null), new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value = "/subscriptions/delete/{uid}", method = RequestMethod.DELETE)
    public ResponseEntity<ApiResponse<String>> deleteSubscription(@PathVariable(value = "uid") String uid){
        try{
            logger.info("Attempting to delete " + uid);
            boolean deleted = subscriptionManager.deleteSubscriptionFromUI(uid);
          
            if(deleted){
                return new ResponseEntity<ApiResponse<String>>(
                        new ApiResponse<String>(Constants.API_VERSION, String.class.getSimpleName(), "Successfully deleted subscription with uid: " + uid), new HttpHeaders(), HttpStatus.OK);
            }
            else{
                return new ResponseEntity<ApiResponse<String>>(
                        new ApiResponse<String>(Constants.API_VERSION, String.class.getSimpleName(), "Could not delete subscription with uid: " + uid), new HttpHeaders(), HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e){
            return new ResponseEntity<ApiResponse<String>>(
                    new ApiResponse<String>(Constants.API_VERSION, String.class.getSimpleName(), "Error deleting subscription with uid: " + uid), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/subscriptions/incognito/{uid}", method = RequestMethod.POST)
    public ResponseEntity toggleIncognito(@PathVariable(value = "uid") String uid){
        try{
            boolean incognito = subscriptionManager.toggleIncognito(uid);

            logger.debug("Setting incognito for subscription: " + uid + ", to : " + incognito);

            return new ResponseEntity(HttpStatus.OK);
        }
        catch (Exception e){
            logger.error("Exception in toggleIncognito!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/subscriptions/{clientUid}/filter",
            method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE)
    ResponseEntity setFilter(
            @PathVariable("clientUid") @NotNull String clientUid,
            @RequestBody @NotNull Filter filter) throws RemoteException {

        if (filter == null || filter.getGeospatialFilter() == null) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        boolean success = subscriptionManager.setGeospatialFilterOnSubscription(clientUid,
                filter.getGeospatialFilter());

        return new ResponseEntity(success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/subscriptions/{clientUid}/filter",
            method = RequestMethod.DELETE)
    ResponseEntity deleteFilter(
            @PathVariable("clientUid") @NotNull String clientUid) throws RemoteException {
        boolean success = subscriptionManager.setGeospatialFilterOnSubscription(clientUid, null);

        return new ResponseEntity(success ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static final class SubscriptionInfo implements Comparable<SubscriptionInfo> {
        
        @Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((clientUid == null) ? 0 : clientUid.hashCode());
			result = prime * result + ((dn == null) ? 0 : dn.hashCode());
			result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
			result = prime * result + ((port == null) ? 0 : port.hashCode());
			result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
			result = prime * result + ((role == null) ? 0 : role.hashCode());
			result = prime * result + ((subscriptionUid == null) ? 0 : subscriptionUid.hashCode());
			result = prime * result + ((username == null) ? 0 : username.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SubscriptionInfo other = (SubscriptionInfo) obj;
			if (clientUid == null) {
				if (other.clientUid != null)
					return false;
			} else if (!clientUid.equals(other.clientUid))
				return false;
			if (dn == null) {
				if (other.dn != null)
					return false;
			} else if (!dn.equals(other.dn))
				return false;
			if (ipAddress == null) {
				if (other.ipAddress != null)
					return false;
			} else if (!ipAddress.equals(other.ipAddress))
				return false;
			if (port == null) {
				if (other.port != null)
					return false;
			} else if (!port.equals(other.port))
				return false;
			if (protocol == null) {
				if (other.protocol != null)
					return false;
			} else if (!protocol.equals(other.protocol))
				return false;
			if (role == null) {
				if (other.role != null)
					return false;
			} else if (!role.equals(other.role))
				return false;
			if (subscriptionUid == null) {
				if (other.subscriptionUid != null)
					return false;
			} else if (!subscriptionUid.equals(other.subscriptionUid))
				return false;
			if (username == null) {
				if (other.username != null)
					return false;
			} else if (!username.equals(other.username))
				return false;
			if (handlerType == null) {
				if (other.handlerType != null)
					return false;
			} else if (!handlerType.equals(other.handlerType))
				return false;
			return true;
		}

		private static final Logger logger = LoggerFactory.getLogger(SubscriptionInfo.class);
        
        public SubscriptionInfo() { }
        
        public SubscriptionInfo(RemoteSubscription sub) {
            
            if (!Strings.isNullOrEmpty(sub.callsign)) {
                setCallsign(sub.callsign);
            }
            else {
                setCallsign(sub.uid);
            }
            
            if (!Strings.isNullOrEmpty(sub.clientUid)) {
                setClientUid(sub.clientUid);
            }
            
            setLastReportMilliseconds(sub.lastProcTime.get());
            
            setPendingWrites(sub.writeQueueDepth.get());
            
            setNumProcessed(sub.numHits.get());
            
            if (!Strings.isNullOrEmpty(sub.takv)) {
                setTakClient(sub.takv);
            }
            
            try {
                
                if (Strings.isNullOrEmpty(sub.takv)) {
                    throw new IllegalArgumentException("Malformed takv in subscription: " + sub.to);
                }
                
                String[] tokens = sub.takv.split(":");
                
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Malformed takv fieldto in subscription: " + sub.to);
                }
                
                setTakClient(tokens[0]);
                setTakVersion(tokens[1]);
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid to field in subscription", sub);
                }
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("exception parsing subscription 'to' information", e);
                }
            }
            
            setTeam(sub.team);
            
            setRole(sub.role);
            
            setSubscriptionUid(sub.uid);
            
            logger.debug("subcription 'to':" + sub.to);

            try {
                
                if (Strings.isNullOrEmpty(sub.to)) {
                    throw new IllegalArgumentException("Malformed to in subscription: " + sub.to);
                }
                
                String[] tokens = sub.to.split(":");
                
                if (tokens.length < 3) {
                    throw new IllegalArgumentException("Malformed to in subscription: " + sub.to);
                }

                setProtocol(tokens[0].split(" ")[0]);
                setIpAddress(Strings.isNullOrEmpty(tokens[1]) ? "" : tokens[1].replace("/", ""));
                setPort(tokens[2]);
                setXpath(sub.xpath);
                
            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid to field in subscription", sub);
                }
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("exception parsing subscription 'to' information", e);
                }
            }
            
            try {
                
                if (Strings.isNullOrEmpty(sub.uid)) {
                    throw new IllegalArgumentException("Malformed clientUid in subscription: " + sub.uid);
                }
                
                String[] tokens = sub.uid.split(":");
                
                if (tokens.length < 1) {
                    throw new IllegalArgumentException("Malformed clientUid in subscription: " + sub.uid);
                }

                setProtocol(Strings.isNullOrEmpty(tokens[0]) ? "" : tokens[0].toLowerCase().contains("topic") ? "WebSockets" : tokens[0]);

            } catch (IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("invalid uid field in subscription", sub);
                }
            } catch (Exception e) {
                if (logger.isTraceEnabled()) {
                    logger.trace("exception parsing subscription 'uid' information", e);
                }
            }

            setUsername(sub.getUsername());
            setDn(sub.getDn());

            setIncognito(sub.incognito);
        }

        public void setMetrics(RemoteSubscriptionMetrics metrics) {
            setAppFramerate(metrics.appFramerate);
            setBattery(metrics.battery);
            setBatteryStatus(metrics.batteryStatus);
            setBatteryTemp(metrics.batteryTemp);
            setDeviceDataRx(metrics.deviceDataRx);
            setDeviceDataTx(metrics.deviceDataTx);
            setHeapCurrentSize(metrics.heapCurrentSize);
            setHeapFreeSize(metrics.heapFreeSize);
            setHeapMaxSize(metrics.heapMaxSize);
            setDeviceIPAddress(metrics.ipAddress);
            setStorageAvailable(metrics.storageAvailable);
            setStorageTotal(metrics.storageTotal);
        }

        private String dn = "";
        private String callsign = "";
        private String clientUid = "";
        private long lastReportMilliseconds = 0L;
        private String takClient = "";
        private String takVersion = "";
        private String username = "";
        private NavigableSet<Group> groups = new ConcurrentSkipListSet<Group>();
        private String role = "";
        private String team = "";
        private String ipAddress = "";
        private String port = "";
        private long pendingWrites = 0L;
        private long numProcessed = 0L;
        private String protocol = "";
        private String xpath = "";
        private String subscriptionUid = "";
        private String appFramerate = "";
        private String battery = "";
        private String batteryStatus = "";
        private String batteryTemp = "";
        private String deviceDataRx = "";
        private String deviceDataTx = "";
        private String heapCurrentSize = "";
        private String heapFreeSize = "";
        private String heapMaxSize = "";
        private String deviceIPAddress = "";
        private String storageAvailable = "";
        private String storageTotal = "";
        private boolean incognito = false;
        private String handlerType = "";
        
        

        public String getDn() {
            return dn;
        }

        public void setDn(String dn) {
            this.dn = dn;
        }

        public String getCallsign() {
            return callsign;
        }

        public void setCallsign(String callsign) {
            this.callsign = callsign;
        }

        public String getClientUid() {
            return clientUid;
        }

        public void setClientUid(String clientUid) {
            this.clientUid = clientUid;
        }

        public long getLastReportMilliseconds() {
            return lastReportMilliseconds;
        }
        
        public long getLastReportDiffMilliseconds() {
            return System.currentTimeMillis() - getLastReportMilliseconds();
        }

        public void setLastReportMilliseconds(long lastReportMilliseconds) {
            this.lastReportMilliseconds = lastReportMilliseconds;
        }

        public String getTakClient() {
            return takClient;
        }

        public void setTakClient(String takClient) {
            this.takClient = takClient;
        }

        public String getTakVersion() {
            return takVersion;
        }

        public void setTakVersion(String takVersion) {
            this.takVersion = takVersion;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public NavigableSet<Group> getGroups() {
            return groups;
        }

        public void setGroups(NavigableSet<Group> groups) {
            this.groups = groups;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
        
        public long getPendingWrites() {
            return pendingWrites;
        }

        public void setPendingWrites(long pendingWrites) {
            this.pendingWrites = pendingWrites;
        }
        
        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }
        
        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getXpath() {
            return xpath;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }
        
        public String getSubscriptionUid() {
            return subscriptionUid;
        }

        public void setSubscriptionUid(String subscriptionUid) {
            this.subscriptionUid = subscriptionUid;
        }
        
        public long getNumProcessed() {
            return numProcessed;
        }

        public void setNumProcessed(long numProcessed) {
            this.numProcessed = numProcessed;
        }

        public String getAppFramerate() {
            return appFramerate;
        }

        public void setAppFramerate(String appFramerate) {
            this.appFramerate = appFramerate;
        }

        public String getBattery() {
            return battery;
        }

        public void setBattery(String battery) {
            this.battery = battery;
        }

        public String getBatteryStatus() {
            return batteryStatus;
        }

        public void setBatteryStatus(String batteryStatus) {
            this.batteryStatus = batteryStatus;
        }

        public String getBatteryTemp() {
            return batteryTemp;
        }

        public void setBatteryTemp(String batteryTemp) {
            this.batteryTemp = batteryTemp;
        }

        public String getDeviceDataRx() {
            return deviceDataRx;
        }

        public void setDeviceDataRx(String deviceDataRx) {
            this.deviceDataRx = deviceDataRx;
        }

        public String getDeviceDataTx() {
            return deviceDataTx;
        }

        public void setDeviceDataTx(String deviceDataTx) {
            this.deviceDataTx = deviceDataTx;
        }

        public String getHeapCurrentSize() {
            return heapCurrentSize;
        }

        public void setHeapCurrentSize(String heapCurrentSize) {
            this.heapCurrentSize = heapCurrentSize;
        }

        public String getHeapFreeSize() {
            return heapFreeSize;
        }

        public void setHeapFreeSize(String heapFreeSize) {
            this.heapFreeSize = heapFreeSize;
        }

        public String getHeapMaxSize() {
            return heapMaxSize;
        }

        public void setHeapMaxSize(String heapMaxSize) {
            this.heapMaxSize = heapMaxSize;
        }

        public String getDeviceIPAddress() {
            return deviceIPAddress;
        }

        public void setDeviceIPAddress(String deviceIPAddress) {
            this.deviceIPAddress = deviceIPAddress;
        }

        public String getStorageAvailable() {
            return storageAvailable;
        }

        public void setStorageAvailable(String storageAvailable) {
            this.storageAvailable = storageAvailable;
        }

        public String getStorageTotal() {
            return storageTotal;
        }

        public void setStorageTotal(String storageTotal) {
            this.storageTotal = storageTotal;
        }

        public boolean getIncognito() { return incognito; }

        public void setIncognito(boolean incognito) { this.incognito = incognito; }

        public String getHandlerType() { return handlerType; }

        public void setHandlerType(String handlerType) { this.handlerType = handlerType; }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("SubscriptionInfo [dn=");
            builder.append(dn);
            builder.append(", callsign=");
            builder.append(callsign);
            builder.append(", clientUid=");
            builder.append(clientUid);
            builder.append(", lastReportMilliseconds=");
            builder.append(lastReportMilliseconds);
            builder.append(", takClient=");
            builder.append(takClient);
            builder.append(", takVersion=");
            builder.append(takVersion);
            builder.append(", username=");
            builder.append(username);
            builder.append(", groups=");
            builder.append(groups);
            builder.append(", role=");
            builder.append(role);
            builder.append(", team=");
            builder.append(team);
            builder.append(", ipAddress=");
            builder.append(ipAddress);
            builder.append(", port=");
            builder.append(port);
            builder.append(", pendingWrites=");
            builder.append(pendingWrites);
            builder.append(", protocol=");
            builder.append(protocol);
            builder.append(", xpath=");
            builder.append(xpath);
            builder.append(", subscriptionUid=");
            builder.append(subscriptionUid);
            builder.append(", appFramerate=");
            builder.append(appFramerate);
            builder.append(", battery=");
            builder.append(battery);
            builder.append(", batteryStatus=");
            builder.append(batteryStatus);
            builder.append(", batteryTemp=");
            builder.append(batteryTemp);
            builder.append(", deviceDataRx=");
            builder.append(deviceDataRx);
            builder.append(", deviceDataTx=");
            builder.append(deviceDataTx);
            builder.append(", heapFreeSize=");
            builder.append(heapFreeSize);
            builder.append(", heapMaxSize=");
            builder.append(heapMaxSize);
            builder.append(", deviceIPAddress=");
            builder.append(deviceIPAddress);
            builder.append(", storageAvailable=");
            builder.append(storageAvailable);
            builder.append(", storageTotal=");
            builder.append(storageTotal);
            builder.append(", incognito=");
            builder.append(incognito);
            builder.append(", handlerType=");
            builder.append(handlerType);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public int compareTo(SubscriptionInfo o) {
            return ComparisonChain.start().compare(this.hashCode(), o.hashCode()).result();
        }
    }
    private static class tmpStaticSub{
        private String uid;


        private String protocol;
        private String subaddr;
        private String subport;
        private String to;
        private String xpath;
        private String filterGroups;
        private String iface;

        public String getXpath() {
            return xpath;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getSubaddr() {
            return subaddr;
        }

        public void setSubaddr(String subaddr) {
            this.subaddr = subaddr;
        }

        public String getSubport() {
            return subport;
        }

        public void setSubport(String subport) {
            this.subport = subport;
        }

        public tmpStaticSub(){}

        public String getFilterGroups() {
            return filterGroups;
        }

        public void setFilterGroups(String filterGroups) {
            this.filterGroups = filterGroups;
        }

        public String getIface() {
            return iface;
        }

        public void setIface(String iface) {
            this.iface = iface;
        }
    }

    private void doSetActiveGroups(String username, List<Group> activeGroups, String clientUid) {

        // store the active group selection in the cache
        activeGroupCacheHelper.setActiveGroupsForUser(username, activeGroups);

        // reauthenticate currently connected streaming clients for this username
        groupManager.authenticateCoreUsers("X509", username);

        subscriptionManager.sendLatestReachableSA(username);

        if (clientUid != null) {
            // notify other devices with same username of the group update so they can update
            subscriptionManager.sendGroupsUpdatedMessage(username, clientUid);
        }
    }

    @RequestMapping(value = "/groups/active", method = RequestMethod.PUT)
    public ResponseEntity setActiveGroups(
            @RequestBody Group[] activeGroups, @RequestParam(value = "clientUid", required = false) String clientUid) {
        try {
            // get username and groups from request
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // make sure activeGroups were passed in
            if (activeGroups == null) {
                logger.error("call to setActiveGroups missing groups!");
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

            doSetActiveGroups(username, Arrays.asList(activeGroups), clientUid);

            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            logger.error("exception in setActiveGroups!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/groups/activebits", method = RequestMethod.PUT)
    public ResponseEntity setActiveGroups(
            @RequestBody Integer[] activebits, @RequestParam(value = "clientUid", required = false) String clientUid) {
        try {
            // get username and groups from request
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            List<Group> activeGroups = new LinkedList<>();
            for (Group group : martiUtil.getAllInOutGroups()) {
                Group copy = group.getCopy();
                boolean active = Arrays.asList(activebits).contains(group.getBitpos());
                copy.setActive(active);
                activeGroups.add(copy);
            }

            doSetActiveGroups(username, activeGroups, clientUid);

            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            logger.error("exception in setActiveGroups!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/groups/update/{username:.+}", method = RequestMethod.GET)
    public ResponseEntity groupsUpdated(@PathVariable(value = "username") String username) {
        try {
            // reauthenticate currently connected streaming clients for this username
            groupManager.authenticateCoreUsers("X509", username);

            // notify the clients of the group update so they can update their group selection UI
            subscriptionManager.sendGroupsUpdatedMessage(username, null);

            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in groupsUpdated!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

