package com.bbn.marti.sync.api;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.bbn.marti.sync.repository.ExternalMissionDataRepository;
import com.bbn.marti.sync.service.PropertiesService;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

import com.bbn.marti.remote.ServerInfo;
import tak.server.Constants;

/*
 * 
 * REST API for properties and user preference data associated with client UIDs.
 * 
 */
@RestController
public class PropertiesApi extends BaseRestController {
    
    @Autowired
    private PropertiesService propertiesService;
    
    @Autowired
    private ServerInfo serverInfo;

    /*
     * get all uids containing properties
     */
    @RequestMapping(value = "/properties/uids", method = RequestMethod.GET)
    public ApiResponse<Collection<String>> getAllPropertyKeys() throws RemoteException {

        return new ApiResponse<Collection<String>>(serverInfo.getServerId(), Constants.API_VERSION, "UIDs", propertiesService.findAllUids());
    }
    
    /*
     * get properties for a given uid
     */
    @RequestMapping(value = "/properties/{uid}/all", method = RequestMethod.GET)
    public ApiResponse<Map<String, Collection<String>>> getAllPropertyForUid(@PathVariable("uid") String uid) throws RemoteException {

    	Map<String, Collection<String>> props = propertiesService.getKeyValuesByUid(uid);
    	
        if (props == null) {
            throw new NotFoundException("no properties stored for uid " + uid);
        }

        return new ApiResponse<>(serverInfo.getServerId(), Constants.API_VERSION, "Properties", props);
    }
    
    /*
     * get all values for a property, per uid
     */
    @RequestMapping(value = "/properties/{uid}/{key}", method = RequestMethod.GET)
    public ApiResponse<Collection<String>> getPropertyForUid(@PathVariable("uid") String uid, @PathVariable("key") String key) throws RemoteException {
        
        Collection<String> vals = propertiesService.getValuesByKeyAndUid(uid, key);
        
        if (vals == null) {
            throw new NotFoundException("no properties stored for uid " + uid + " for key " + key);
        }

        return new ApiResponse<>(serverInfo.getServerId(), Constants.API_VERSION, "Properties", vals);
    }
    
   /*
    * store a property for a uid
    */
   @RequestMapping(value = "/properties/{uid}", method = RequestMethod.PUT)
   public ApiResponse<Map.Entry<String, String>> storeProperty(@PathVariable("uid") String uid, @RequestBody Map.Entry<String, String> keyValuePair) throws RemoteException {
       
       if (Strings.isNullOrEmpty(uid)) {
           throw new IllegalArgumentException("UID must be specified");
       }
       
       if (keyValuePair == null || Strings.isNullOrEmpty(keyValuePair.getKey()) || Strings.isNullOrEmpty(keyValuePair.getValue())) {
           throw new IllegalArgumentException("Key-Value pair must be specified in request body");
       }
       
       propertiesService.putKeyValue(uid, keyValuePair.getKey(), keyValuePair.getValue());
       
       return new ApiResponse<>(serverInfo.getServerId(), Constants.API_VERSION, "KeyValuePair", keyValuePair);
   }
   
   /*
    * clear a property for a uid
    */
   @RequestMapping(value = "/properties/{uid}/{key}", method = RequestMethod.DELETE)
   public void clearProperty(@PathVariable("uid") String uid, @PathVariable("key") String key) throws RemoteException {
       
       if (Strings.isNullOrEmpty(uid)) {
           throw new IllegalArgumentException("UID must be specified");
       }
       
       if (Strings.isNullOrEmpty(key)) {
           throw new IllegalArgumentException("key must be specified");
       }
       
       propertiesService.deleteKey(uid, key);
   }
   
   /*
    * clear all properties for a uid
    */
   @RequestMapping(value = "/properties/{uid}/all", method = RequestMethod.DELETE)
   public void clearAllProperty(@PathVariable("uid") String uid) throws RemoteException {
       
       if (Strings.isNullOrEmpty(uid)) {
           throw new IllegalArgumentException("UID must be specified");
       }
       
       propertiesService.deleteAllKeysByUid(uid);
   }
}
