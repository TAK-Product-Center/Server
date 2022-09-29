package tak.server.api;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.ignite.services.ServiceContext;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.Metadata.Field;

import tak.server.PluginManager;
import tak.server.plugins.PluginFileApi;
import tak.server.system.ApiDependencyProxy;;

public class DistributedPluginFileApi implements PluginFileApi, org.apache.ignite.services.Service {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(DistributedPluginFileApi.class);

	private EnterpriseSyncService enterpriseSyncService() {
	    return ApiDependencyProxy.getInstance().enterpriseSyncService();
	}

    private PluginManager pluginManager() throws Exception {
    	return ApiDependencyProxy.getInstance().pluginManager();
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
	public Metadata newFile(Metadata metadata, InputStream inputStream) throws Exception {
		
		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();
		
		Metadata returnedMetadata = enterpriseSyncService().insertResourceStream(metadata, inputStream, groupVectorForAdminUser);
				
		// if plugin classname is set, notify the plugin with the file upload event. 
		if (metadata.getPluginClassName() != null) {
			
			PluginManager pluginManager = pluginManager();
			if (pluginManager != null) {
				logger.info("Notifying the plugin {} with file upload event", metadata.getPluginClassName());
				pluginManager.onFileUpload(metadata.getPluginClassName(), returnedMetadata);
			}else {
				logger.error("Plugin Manager is null");
			}

		} 
		return returnedMetadata;
	}
	
//	@Override
//	public InputStream readFileContent(String hash) throws Exception {
//		
//		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();
//
//		byte[] content = enterpriseSyncService().getContentByHash(hash, groupVectorForAdminUser);
//		
//		return new ByteArrayInputStream(content);
//	}
	
	@Override
	public List<Metadata> getMetadataByHash(String hash) throws Exception {
		
		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();

		List<Metadata> metadataList = enterpriseSyncService().getMetadataByHash(hash, groupVectorForAdminUser);
		
		return metadataList;
	}
	
	
	@Override
	public boolean updateMetadata(String hash, String metadataField, String metadataValue) throws Exception {
		
		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();

		return enterpriseSyncService().updateMetadata(hash, metadataField, metadataValue, groupVectorForAdminUser);
		
	}

	@Override
	public void deleteFile(String hash) throws Exception {
		
		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();

		enterpriseSyncService().delete(hash, groupVectorForAdminUser);
	}

	@Override
	public SortedMap<String, List<Metadata>> search(Double minimumAltitude, Double maximumAltitude,
			Map<Field, String> metadataConstraints, Double[] bbox, Double[] circle, Timestamp minimumTime,
			Timestamp maximumTime, Boolean latestOnly, String missionName, String tool) throws Exception {
		
		final String groupVectorForAdminUser = RemoteUtil.getInstance().getBitStringAllGroups();
		
		// Create an PGobject
		PGobject spatialConstraints = null;
		if (bbox != null && circle != null) {
			throw new Exception("Cannot specify both bbox and circle parameters as spatial contraints");
		} else if (bbox != null) {
			if (bbox.length != 4) {
				throw new Exception("bbox param must have the length of 4");
			}
			spatialConstraints = new PGbox(bbox[0], bbox[1], bbox[2], bbox[3]);
		} else if (circle != null) {
			if (circle.length != 3) {
				throw new Exception("circle param must have the length of 3");
			}
			spatialConstraints = new PGcircle(circle[0], circle[1], circle[2]);
		}

		return enterpriseSyncService().search(minimumAltitude, maximumAltitude, metadataConstraints, spatialConstraints, minimumTime, maximumTime, latestOnly, missionName, tool, groupVectorForAdminUser);
	}

	@Override
	public InputStream readFileContent(String hash) throws Exception {
		throw new Exception("This method should not be called from here. Use the version in the takserver-plugin-manager instead");
	}
	
}
