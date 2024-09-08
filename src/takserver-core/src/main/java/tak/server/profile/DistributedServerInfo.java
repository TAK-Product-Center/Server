package tak.server.profile;

import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Cluster;
import com.bbn.marti.config.Configuration;
import com.bbn.marti.remote.ServerInfo;
import com.google.common.base.Strings;

import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.Constants;
import tak.server.ignite.IgniteHolder;

/*
 *
 * Provide internal information about the server, including the server id.
 *
 */
public final class DistributedServerInfo implements ServerInfo, Service {

	private static final long serialVersionUID = 5013754145266368606L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedServerInfo.class);

	private volatile String serverId = null;

	private boolean isCluster = false;

	public DistributedServerInfo(Ignite ignite) {

		if (logger.isDebugEnabled()) {
			logger.debug("DistributedServerInfo contructor " + System.identityHashCode(this));
		}

	}

	@Override
	public void cancel(ServiceContext ctx) {
		if (logger.isDebugEnabled()) {
			logger.debug(getClass().getSimpleName() + " service cancelled");
		}
	}

	@Override
	public void init(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("init method " + getClass().getSimpleName());
		}
	}

	@Override
	public void execute(ServiceContext ctx) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("execute method " + getClass().getSimpleName());
		}
	}

	@Override
	public String getServerId() {
		if (serverId == null) {
			synchronized (this) {
				if (serverId == null) {
					try {

						Configuration conf = CoreConfigFacade.getInstance().getRemoteConfiguration();

						String id = conf.getNetwork().getServerId();

						if (Strings.isNullOrEmpty(id)) {
							id = generateServerId();
						}

						isCluster = conf.getCluster().isEnabled();

						if (isCluster) {

							id += IgniteHolder.getInstance().getIgniteId();
						}

						serverId = id;

					} catch (Exception e) {
						logger.error("error setting server id", e);
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("serverId: " + serverId);
		}

		return serverId;
	}


	// If the server id is blank, generate a server id and save it to CoreConfig.xml - only if it is blank.
	private String generateServerId() {

		String id = UUID.randomUUID().toString().replace("-", "");

		try {
			CoreConfigFacade.getInstance().setAndSaveServerId(id);
			CoreConfigFacade.getInstance().saveChangesAndUpdateCache();

		} catch (Exception e) {
			logger.warn("execption saving server ID to configuration", e);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("generated new serverId: " + id);
		}

		return id;
	}

	@Override
	public String getSubmissionTopic() {
		return Constants.SUBMISSION_TOPIC_BASE + getServerId();
	}

	@Override
	public String getTakMessageTopic() {
		return Constants.TAK_MESSAGE_TOPIC_BASE + getServerId();
	}

	@Override
	public boolean isCluster() {
		Cluster cluster = CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster();
		return cluster == null ? false : cluster.isEnabled(); 
	}

	@Override
	public String getNatsURL() {
		Cluster cluster = CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster();
		return cluster == null ? "" : cluster.getNatsURL(); 
	}

	@Override
	public String getNatsClusterId() {
		Cluster cluster = CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster();
		return cluster == null ? "" : cluster.getNatsClusterID();
	}
}
