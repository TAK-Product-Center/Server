package tak.server.filemanager;

import java.util.List;

import com.bbn.marti.sync.model.Resource;

public interface FileManagerService {

	List<String> getKeywordsForResource(String hash);
	
	List<Resource> getResourcesByMission(String mission, int limit, int offset, String sort, Boolean ascending);
	
	List<Resource> getMissionPackageResources(int limit, int offset, String sort, Boolean ascending);
	
	int getPackageResourceCount();
	
	int getResourceCountByMission(String mission);
	
	
	
}
