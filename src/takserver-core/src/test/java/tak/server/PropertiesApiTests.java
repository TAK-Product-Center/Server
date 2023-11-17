package tak.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.bbn.marti.sync.api.PropertiesApi;
import com.bbn.marti.sync.service.PropertiesService;
import com.google.common.collect.Multimap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class PropertiesApiTests {
	
	@Autowired
	private PropertiesService propertiesService;
	
	@Autowired
	private PropertiesApi propertiesApi;
	
	@Autowired
	private ServerInfo serverInfo;
	
	// Get UID
	@Test
	public void getUidsShouldReturnAllUids() throws Exception {
		List<String> uids = Arrays.asList("1234","2358"); 
		assertEquals(propertiesApi.getAllPropertyKeys().getData(), uids);
		//assertEquals(true,true);
	}
	
	// Get KVs by Uid
	@Test
	public void getPropertiesForUidShouldReturnAllProperties() throws Exception {
		String uid = "1234"; 
		Multimap<String, String> uidKvMap = new ConcurrentMultiHashMap<String, String>();
		uidKvMap.put("Key1", "value1");
		uidKvMap.put("Key1", "value2");
		uidKvMap.put("Key2", "value0");
		uidKvMap.put("Key2", "value1");
		
		assertEquals(propertiesApi.getAllPropertyForUid(uid).getData(), uidKvMap.asMap());
	}
	
	@Test(expected = NotFoundException.class)
	public void getPropertiesForBadUidShouldThrowException() throws Exception {
		String uid = "5678"; 
		
		propertiesApi.getAllPropertyForUid(uid);
	}
	
	// Get Values by UID and Key
	@Test
	public void getValuesForUidAndKeyShouldReturnAllValues() throws Exception {
		String uid = "1234"; 
		String key = "Key1";
		List<String> values = new ArrayList<String>();
		values.add("value1");
		values.add("value2");
		
		assertEquals(propertiesApi.getPropertyForUid(uid, key).getData(), values);
	}
	
	@Test(expected = NotFoundException.class)
	public void getValuesForBadUidShouldThrowException() throws Exception {
		String uid = "5678"; 
		String key = "Key2";
		
		propertiesApi.getPropertyForUid(uid,key);
	}
	
	// Put KV Pair
	@Test(expected = IllegalArgumentException.class)
	public void putKvWithBadUidShouldThrowError() throws Exception {
		String uid = "";
		propertiesApi.storeProperty(uid, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void putKvWithBadKvPairShouldThrowError() throws Exception {
		String uid = "1234";
		propertiesApi.storeProperty(uid, null);
	}
	@Test
	public void putKvForUidShouldReturnKv() throws Exception {
		String uid = "1234"; 
		Map.Entry<String, String> uidKvMap = Map.entry("Key1", "value");
		
		assertEquals(propertiesApi.storeProperty(uid,uidKvMap).getData(), uidKvMap);
	}
	
	// Delete Key from UID 
	@Test(expected = IllegalArgumentException.class)
	public void deleteKeyWithBadUidShouldThrowError() throws Exception {
		String uid = "";
		propertiesApi.clearProperty(uid, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void deleteKeyWithBadKeyPairShouldThrowError() throws Exception {
		String uid = "1234";
		propertiesApi.clearProperty(uid, null);
	}
	@Test
	public void deleteKvForUidShouldNotThrowException() throws Exception {
		String uid = "1234"; 
		String key = "Key1";
		propertiesApi.clearProperty(uid, key);
	}
	
	
	// Delete all Keys by UID
	@Test(expected = IllegalArgumentException.class)
	public void deleteAllKeysWithBadUidShouldThrowError() throws Exception {
		String uid = "";
		propertiesApi.clearAllProperty(uid);
	}
	@Test
	public void deleteKeysForUidShouldNotThrowException() throws Exception {
		String uid = "1234"; 
		String key = "Key1";
		propertiesApi.clearAllProperty(uid);
	}
	
}
