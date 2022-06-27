package tak.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.Set;

import org.apache.ignite.Ignite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.bbn.marti.injector.ClusterUidCotTagInjector;
import com.bbn.marti.injector.UidCotTagInjector;
import com.bbn.marti.remote.injector.InjectorConfig;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class InjectorTests {

	@Autowired
	Ignite ignite;
	
	private static final String INJECT1 = "inject1";
	private static final String INJECT2 = "inject2";

	private static final String UID1 = "uid1";
	private static final String UID2 = "uid2";
	
	@Test
	public void testDuplicateComparatorForSameUID() throws RemoteException {
		testDuplicateComparatorForSameUID(new UidCotTagInjector());
	}
	
	@Test
	public void testUIDSingleEntryDeletion() throws RemoteException {
		testUIDSingleEntryDeletion(new UidCotTagInjector());
	}
	
	@Test
	public void testAllInjectors() throws RemoteException{
		testAllInjectors(new UidCotTagInjector());
	}
	
	@Test
	public void testClusterInjectors() throws RemoteException {
		ClusterUidCotTagInjector clusterUidCotTagInjector = new ClusterUidCotTagInjector();
		
		testDuplicateComparatorForSameUID(clusterUidCotTagInjector);
		
		testUIDSingleEntryDeletion(clusterUidCotTagInjector);
		
		testAllInjectors(clusterUidCotTagInjector);
	}
	
	private void testDuplicateComparatorForSameUID(UidCotTagInjector injector) {
		assertTrue(injector.setInjector(UID1, INJECT1));
		assertFalse(injector.setInjector(UID1, new String(INJECT1)));
	}
		
	private void testUIDSingleEntryDeletion(UidCotTagInjector injector) throws RemoteException {
		InjectorConfig injectorConfig = new InjectorConfig(UID1, INJECT1);
		injector.setInjector(UID1, INJECT1);
		
		injector.deleteInjector(injectorConfig);
		
		assertFalse(injector.getAllInjectors().parallelStream().anyMatch(ic -> ic.getUid().equals(UID1) && ic.getToInject().equals(INJECT1)));
	}
	
	private void testAllInjectors(UidCotTagInjector injector) {
		injector.setInjector(UID1, new String(INJECT1));
		injector.setInjector(UID1, new String(INJECT2));
		injector.setInjector(UID2, new String(INJECT1));
		
		boolean foundInjector1 = false;
		boolean foundInjector2 = false;
		boolean foundInjector3 = false;
		
		Set<InjectorConfig> injectorConfigs = injector.getAllInjectors();
		
		for (InjectorConfig ic : injectorConfigs) {
			if (ic.getUid().equals(UID1) && ic.getToInject().equals(INJECT1)) 
				foundInjector1 = true;
			if (ic.getUid().equals(UID1) && ic.getToInject().equals(INJECT2)) 
				foundInjector2 = true;
			if (ic.getUid().equals(UID2) && ic.getToInject().equals(INJECT1)) 
				foundInjector3 = true;
		}
	
		assertTrue(injectorConfigs.size() == 3);
		assertTrue(foundInjector1 && foundInjector2 && foundInjector3);
	}
}








