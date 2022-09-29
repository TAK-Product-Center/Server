package tak.server.plugins;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.Metadata.Field;

import tak.server.plugins.datalayer.PluginFileApiJDBC;

public class PluginFileApiImpl implements PluginFileApi{
		
	private PluginFileApi pluginFileApiFromApiProcess;
	
	private PluginFileApiJDBC pluginFileApiJDBC;
	
	public PluginFileApiImpl(PluginFileApi pluginFileApiFromApiProcess, PluginFileApiJDBC pluginFileApiJDBC) {
		
		this.pluginFileApiFromApiProcess = pluginFileApiFromApiProcess;
		this.pluginFileApiJDBC = pluginFileApiJDBC;
	
	}

	@Override
	public Metadata newFile(Metadata metadata, InputStream inputStream) throws Exception {
		return pluginFileApiFromApiProcess.newFile(metadata, inputStream);
	}

	@Override
	public List<Metadata> getMetadataByHash(String hash) throws Exception {
		return pluginFileApiFromApiProcess.getMetadataByHash(hash);
	}

	@Override
	public boolean updateMetadata(String hash, String metadataField, String metadataValue) throws Exception {
		return pluginFileApiFromApiProcess.updateMetadata(hash, metadataField, metadataValue);
	}

	@Override
	public void deleteFile(String hash) throws Exception {
		pluginFileApiFromApiProcess.deleteFile(hash);	
	}

	@Override
	public SortedMap<String, List<Metadata>> search(Double minimumAltitude, Double maximumAltitude,
			Map<Field, String> metadataConstraints, Double[] bbox, Double[] circle, Timestamp minimumTime,
			Timestamp maximumTime, Boolean latestOnly, String missionName, String tool) throws Exception {
		return pluginFileApiFromApiProcess.search(minimumAltitude, maximumAltitude, metadataConstraints, bbox, circle, minimumTime, maximumTime, latestOnly, missionName, tool);
	}

	@Override
	public InputStream readFileContent(String hash) throws Exception {
		return pluginFileApiJDBC.readFileContent(hash);
	}
	
}
