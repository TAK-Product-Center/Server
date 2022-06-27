package com.bbn.marti.sync.api;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

import tak.server.Constants;

/*
 * 
 * REST API for properties and user preference data associated with client UIDs.
 * 
 */
@RestController
public class PropertiesApi extends BaseRestController {
    
    private Map<String, Multimap<String, String>> uidPropertiesMap = new ConcurrentHashMap<>();

    /*
     * get all uids containing properties
     */
    @RequestMapping(value = "/properties/uids", method = RequestMethod.GET)
    ApiResponse<Collection<String>> getAllPropertyKeys() throws RemoteException {

        return new ApiResponse<Collection<String>>(Constants.API_VERSION, "UIDs", uidPropertiesMap.keySet());
    }
    
    /*
     * get properties for a given uid
     */
    @RequestMapping(value = "/properties/{uid}/all", method = RequestMethod.GET)
    ApiResponse<Map<String, Collection<String>>> getAllPropertyForUid(@PathVariable("uid") String uid) throws RemoteException {

        Multimap<String, String> props = uidPropertiesMap.get(uid);
        
        if (props == null) {
            throw new NotFoundException("no properties stored for uid " + uid);
        }

        return new ApiResponse<>(Constants.API_VERSION, "Properties", props.asMap());
    }
    
    /*
     * get all values for a property, per uid
     */
    @RequestMapping(value = "/properties/{uid}/{key}", method = RequestMethod.GET)
    ApiResponse<Collection<String>> getPropertyForUid(@PathVariable("uid") String uid, @PathVariable("key") String key) throws RemoteException {

        Multimap<String, String> props = uidPropertiesMap.get(uid);
        
        if (props == null) {
            throw new NotFoundException("no properties stored for uid " + uid);
        }
        
        Collection<String> vals = props.get(key);
        
        if (vals == null) {
            throw new NotFoundException("no properties stored for uid " + uid + " for key " + key);
        }

        return new ApiResponse<>(Constants.API_VERSION, "Properties", vals);
    }
    
   /*
    * store a property for a uid
    */
   @RequestMapping(value = "/properties/{uid}", method = RequestMethod.PUT)
   ApiResponse<Map.Entry<String, String>> storeProperty(@PathVariable("uid") String uid, @RequestBody Map.Entry<String, String> keyValuePair) throws RemoteException {
       
       if (Strings.isNullOrEmpty(uid)) {
           throw new IllegalArgumentException("UID must be specified");
       }
       
       if (keyValuePair == null || Strings.isNullOrEmpty(keyValuePair.getKey()) || Strings.isNullOrEmpty(keyValuePair.getValue())) {
           throw new IllegalArgumentException("Key-Value pair must be specified in request body");
       }
           
       uidPropertiesMap.putIfAbsent(uid, new ConcurrentMultiHashMap<String, String>());
       
       Multimap<String, String> uidPropMap = uidPropertiesMap.get(uid);
       
       if (uidPropMap == null) {
           throw new ConcurrentModificationException("uid " + uid + " deleted by a concurrent API call during retrieval");
       }
       
       uidPropMap.put(keyValuePair.getKey(), keyValuePair.getValue());
       
       return new ApiResponse<>(Constants.API_VERSION, "KeyValuePair", keyValuePair);
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
       
       if (!uidPropertiesMap.containsKey(uid)) {
           throw new NotFoundException("uid not found");
       }
           
       Multimap<String, String> uidPropMap = uidPropertiesMap.get(uid);
       
       if (uidPropMap == null) {
           throw new ConcurrentModificationException("uid " + uid + " deleted by a concurrent API call during retrieval");
       }
       
       uidPropMap.removeAll(key);
   }
   
   /*
    * clear all properties for a uid
    */
   @RequestMapping(value = "/properties/{uid}/all", method = RequestMethod.DELETE)
   public void clearAllProperty(@PathVariable("uid") String uid) throws RemoteException {
       
       if (Strings.isNullOrEmpty(uid)) {
           throw new IllegalArgumentException("UID must be specified");
       }
       
       uidPropertiesMap.remove(uid);
   }
}
