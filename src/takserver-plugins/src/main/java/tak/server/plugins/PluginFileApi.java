package tak.server.plugins;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.bbn.marti.sync.Metadata;

public interface PluginFileApi{

	Metadata newFile(Metadata metadata, InputStream inputStream) throws Exception;
	
	List<Metadata> getMetadataByHash(String hash) throws Exception;
	
	boolean updateMetadata(String hash, String metadataField, String metadataValue) throws Exception;
	
	void deleteFile(String hash) throws Exception;
	
	/**
	 * Double[] bbox must have the size of 4, 
	 * Double[] circle must have the size of 3
	 * Cannot specify both bbox and circle. One of them must be null
	 */
	SortedMap<String, List<Metadata>> search(Double minimumAltitude, Double maximumAltitude,
		Map<Metadata.Field, String> metadataConstraints, Double[] bbox, Double[] circle, Timestamp minimumTime,
		Timestamp maximumTime, Boolean latestOnly, String missionName, String tool) throws Exception;
	
	InputStream readFileContent(String hash) throws Exception;
	
}
