package tak.server.grid;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.cluster.ClusterGroupDefinition;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.groups.GroupManager;

import tak.server.Constants;

public class ContactManagerProxyFactory implements FactoryBean<ContactManager> {

	private static final Logger logger = LoggerFactory.getLogger(ContactManagerProxyFactory.class);
	
	@Autowired
	Ignite ignite;
	
	@Override
	public ContactManager getObject() throws Exception {
		
		if (logger.isDebugEnabled()) {
			logger.debug("get " + getObjectType().getSimpleName() + " from ignite");
		}
				
		return ignite.services(ClusterGroupDefinition.getMessagingClusterDeploymentGroup(ignite)).serviceProxy(Constants.DISTRIBUTED_CONTACT_MANAGER, ContactManager.class, false);
	}

	@Override
	public Class<?> getObjectType() {
		return ContactManager.class;
	}

}
