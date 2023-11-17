package tak.server.filemanager;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Map.Entry;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.http.MimeTypes;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.bbn.marti.sync.model.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.dao.kml.JDBCCachingKMLDao;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.Metadata.Field;
import com.bbn.marti.sync.repository.ResourceRepository;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.google.common.base.Strings;

import tak.server.Constants;

@RestController
public class FileManagerApi extends BaseRestController {
	
	private static final Logger logger = LoggerFactory.getLogger(FileManagerApi.class);
	
	static final int KBYTES = 1024;
	static final int MBYTES = 1048576;
	static final int GBYTES = 1073741824;
	
	@Autowired
	private CommonUtil commonUtil;
	
	@Autowired
	private EnterpriseSyncService persistenceStore;
	
	@Autowired
	private ResourceRepository resourceRepository;
	
	@Autowired
	private FileManagerService fileManagerService;
	
	@Autowired
    private CommonUtil martiUtil;
	
	@Autowired
	private JDBCCachingKMLDao dao;
	
	@Autowired
    private RequestHolderBean requestHolderBean;
	
	@Autowired
    private ServerInfo serverInfo;

	 @RequestMapping(value = "/files/metadata", method = RequestMethod.GET)
	 public ApiResponse<Collection<Map<String, String>>> getFileMetadata(
			 @RequestParam(value = "page", defaultValue = "-1") int page,
	         @RequestParam(value = "limit", defaultValue = "-1") int limit,
	         @RequestParam(value = "mission", defaultValue = "") String mission,
	         @RequestParam(value = "missionPackage", defaultValue = "false") Boolean missionPackage,
	         @RequestParam(value = "sort", defaultValue = "") String sort,
	         @RequestParam(value = "ascending", defaultValue = "true") Boolean ascending) throws RemoteException {
		 
		 String groupVector = null;
		 final HttpServletRequest request = requestHolderBean.getRequest();
		 NavigableSet<Group> groups = martiUtil.getGroupsFromRequest(request);

		 try {
			// Get group vector for the user associated with this session
			groupVector = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class).getGroupBitVector(request);
		 } catch (Exception e) {
			logger.debug("exception getting group membership for current web user ", e);
		 }
		 
		 
		 List<Map<String, String>> fileJson = new ArrayList<Map<String, String>>();
		 Map<String,String> entry = null;
		 
		 
		 try {
			 String resourceSort = getResourceColumnName(sort);
			 List<Resource> files = null;
			 if(mission.isBlank() && !missionPackage) {
				 if(page == -1 || limit == -1) {
					 if(sort.isBlank()) {
						 files = resourceRepository.findAll();
					 } else {
						 if(ascending) {
							 files = resourceRepository.findAll(Sort.by(resourceSort).ascending());
						 } else {
							 files = resourceRepository.findAll(Sort.by(resourceSort).descending());
						 }
					 }
				 } else {
					 Pageable pageRequest = null;
					 if(sort.isBlank()) {
						 pageRequest = PageRequest.of(page, limit);
					 } else {
						 if(ascending) {
							 pageRequest = PageRequest.of(page, limit, Sort.by(resourceSort).ascending());
						 } else {
							 pageRequest = PageRequest.of(page, limit, Sort.by(resourceSort).descending());
						 }
					 }
					 files = resourceRepository.findAll(pageRequest).getContent();
				 }
			 } else if (mission.isBlank() && missionPackage) {
				 if(page == -1 || limit == -1) {
					 if(sort.isBlank()) {
						 files = fileManagerService.getMissionPackageResources(0, 0, "", true);
					 } else {
						 files = fileManagerService.getMissionPackageResources(0, 0, resourceSort, ascending);
					 }
				 }else {
					 if(sort.isBlank()) {
						 files = fileManagerService.getMissionPackageResources(limit, (page * limit), "", true);
					 } else {
						 files = fileManagerService.getMissionPackageResources(limit, (page * limit), resourceSort, ascending);
					 }
				 }
			 }	else{
				 if(page == -1 || limit == -1) {
					 if(sort.isBlank()) {
						 files = fileManagerService.getResourcesByMission(mission, 0, 0,"", true);
					 } else {
						 files = fileManagerService.getResourcesByMission(mission, 0, 0, resourceSort, ascending);
					 }
				 }else {
					 if(sort.isBlank()) {
						 files = fileManagerService.getResourcesByMission(mission, limit, (page * limit), "", true);
					 } else {
						 files = fileManagerService.getResourcesByMission(mission, limit, (page * limit), resourceSort	, ascending);
					 }
				 }
			 }
			 for (Resource file: files) {
				 
				 entry = buildResourceEntry(file, groups);
				
				fileJson.add(entry);
			 }
			 
			 
		 } catch (Exception e) {
			 logger.error("Unable to get file metadata: ", e);
		 }
		 return new ApiResponse<>(serverInfo.getServerId(), Constants.API_VERSION, "Files", fileJson);
	 }
	 
	 @RequestMapping(value = "/files/metadata/count", method = RequestMethod.GET)
	 public ApiResponse<Integer> getFileCount(@RequestParam(value = "mission", defaultValue = "") String mission,
	         								  @RequestParam(value = "missionPackage", defaultValue = "false") Boolean missionPackage) 
	 throws RemoteException{
		 Integer count = 0;
		 try {
			 if(mission.isBlank() && !missionPackage) {
				 count = (int) resourceRepository.count();
			 } else if (mission.isBlank() && missionPackage) {
				 count = fileManagerService.getPackageResourceCount();
			 } else {
				 count = fileManagerService.getResourceCountByMission(mission);
			 }
		 } catch (Exception e) {
			 logger.error("Unable to get file metadata count: ", e);
		 }
		 return new ApiResponse<>(serverInfo.getServerId(), Constants.API_VERSION, "Count", count);
	 }
	 
	 
	 @RequestMapping(value = "/files/{hash}", method = RequestMethod.GET)
	 public ResponseEntity<ByteArrayResource> getFile(@PathVariable("hash") String hash) throws RemoteException {
		 
		 String groupVector = null;
		 final HttpServletRequest request = requestHolderBean.getRequest();
	
		 try {
			// Get group vector for the user associated with this session
			groupVector = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class).getGroupBitVector(request);
			logger.trace("groups bit vector: " + groupVector);
		 } catch (Exception e) {
			logger.debug("exception getting group membership for current web user ", e);
		 }
		 
		 byte[] array = null;
		 try {
			 
			 array = persistenceStore.getContentByHash(hash, groupVector);
			 List<Metadata> metadataList = persistenceStore.getMetadataByHash(hash, groupVector);
			 ByteArrayResource resource = new ByteArrayResource(array);
			 MediaType mediaType = MediaType.parseMediaType(metadataList.get(0).getFirstSafely(Field.MIMEType));
			 String fileName = metadataList.get(0).getFirstSafely(Field.Name) + "." + mediaType.getSubtype();
			 
		     return ResponseEntity.ok()
		            .contentType(mediaType)
		            .contentLength(resource.contentLength())
		            .header(HttpHeaders.CONTENT_DISPOSITION,
		                    ContentDisposition.attachment()
		                        .filename(fileName)
		                        .build().toString())
		            .body(resource);
			 
		 } catch (Exception e) {
			 logger.error("Unable to get file: ",e);
			 ByteArrayResource resource = new ByteArrayResource(array);
			 return ResponseEntity.internalServerError().body(resource);
		 }
		 
		 
	 
	 }
	 
	 @RequestMapping(value = "/files/{hash}", method = RequestMethod.DELETE)
	 public void deleteFile(@PathVariable("hash") String hash) throws RemoteException {
		 
		 String groupVector = null;
		 final HttpServletRequest request = requestHolderBean.getRequest();
	
		 try {
			// Get group vector for the user associated with this session
			groupVector = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class).getGroupBitVector(request);
			logger.trace("groups bit vector: " + groupVector);
		 } catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e);
		 }
		 
		 try {
			 if(!hash.isEmpty()) {
				 persistenceStore.delete(hash, groupVector);
			 }
			 
		 } catch (Exception e) {
			 logger.error("Unable to delete file ",e);
		 }
	 }
	 
	 
	 @RequestMapping(value = "/files/{hash}", method = RequestMethod.HEAD)
	 public ApiResponse<Map<String, String>> getFileHead(@PathVariable("hash") String hash) throws RemoteException {
		 String groupVector = null;
		 final HttpServletRequest request = requestHolderBean.getRequest();
	
		 try {
			// Get group vector for the user associated with this session
			groupVector = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class).getGroupBitVector(request);
			logger.trace("groups bit vector: " + groupVector);
		 } catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e.getMessage());
		 }
		 Map<String,String> entry = null;
		 
		 try {
			 List<Metadata> list = persistenceStore.getMetadataByHash(hash, groupVector);
			 for (Metadata metadata : list) {
				 
				 try {
					commonUtil.validateMetadata(metadata);
				 } catch (ValidationException | IntrusionException ex) {
					StringBuilder builder = new StringBuilder();
					builder.append("Unsafe item from Enterprise Sync datbase: ");
					builder.append("Primary key " + metadata.getPrimaryKey() + " ");
					logger.warn(builder.toString());
					continue;
				 }
			 
				 entry = buildMetadataEntry(metadata);
			 }
			 
		 }catch (Exception e) {
			 logger.error("Unable to lookup file: ",e);
		 }
		 return new ApiResponse<>(serverInfo.getServerId(), Constants.API_VERSION, "data", entry);
		 
	 }
	 
	 
	 @RequestMapping(value = "/files/{hash}/metadata", method = RequestMethod.PUT)
	 public void putMetadata(@PathVariable("hash") String hash,
	    @RequestParam(value = "user", defaultValue = "") String userParam,
		@RequestParam(value = "expiration", defaultValue = "") String expirationParam,
	    @RequestParam(value = "keywords", defaultValue = "") List<String> keywordsParam) throws RemoteException {
		 
		 String groupVector = null;
		 final HttpServletRequest request = requestHolderBean.getRequest();
	
		 try {
			// Get group vector for the user associated with this session
			groupVector = SpringContextBeanForApi.getSpringContext().getBean(CommonUtil.class).getGroupBitVector(request);
			logger.trace("groups bit vector: " + groupVector);
		 } catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e.getMessage());
		 }
		 
		 try {
		 
			 if (!Strings.isNullOrEmpty(userParam)) {
		    		persistenceStore.updateMetadata(hash, Field.SubmissionUser.toString(), userParam, groupVector);
	    	}
			 if (!Strings.isNullOrEmpty(expirationParam)) {
				 persistenceStore.updateMetadata(hash, Field.EXPIRATION.toString(), expirationParam, groupVector);
	    	}
			 if (keywordsParam != null) {
				 persistenceStore.updateMetadataKeywords(hash, keywordsParam);
	    	}
		 } catch (Exception e) {
			 logger.error("Unable to store metadata", e);
		 }
	 }
	 
	 private Map<String,String> buildMetadataEntry(Metadata metadata){
		 Map<String,String> entry = new HashMap<String, String>();
		 try {
			 entry.put("Name", metadata.getFirstSafely(Field.Name));
			 entry.put("User", metadata.getFirstSafely(Metadata.Field.SubmissionUser));
			 
			 String uid = metadata.getFirstSafely(Field.CreatorUid);
			 entry.put("Creator", ESAPI.encoder().encodeForHTML(dao.latestCallsign(uid)));
			 
			 Integer size = metadata.getSize();
			 if (size == null ) {
				entry.put("Size","Unknown");
			 } else {
				if(size < 1024) {
					entry.put("Size", size + "B");
			 } else if(size < MBYTES) {
				entry.put("Size", (size/KBYTES) + "kB");
			 } else if(size < GBYTES) {
				entry.put("Size", size/MBYTES + "MB");
			 } else {
				entry.put("Size", size/GBYTES + "GB");
			    }
			 }
			 
			 entry.put("Time", metadata.getFirstSafely(Metadata.Field.SubmissionDateTime));
			 
			 entry.put("MimeType", metadata.getFirstSafely(Metadata.Field.MIMEType));
			 
			 String[] keywordsArray = metadata.getKeywords();
			 if(keywordsArray != null) {
				 String keywords = String.join(",", metadata.getKeywords());
				 entry.put("Keywords",keywords);
			 } else {
				 entry.put("Keywords","");
			 }
			 
			 //entry.put("Group", metadata.getFirstSafely(Metadata.Field.));
			 
			// Expiration
			Long expiration = metadata.getExpiration();
			String expiration_string = "";
			if (expiration != null && expiration >= 0) {
			    expiration_string = Instant.ofEpochSecond(expiration).toString();
			    expiration_string = expiration_string.substring(0, expiration_string.length() - 1);
			} else {
			    expiration_string = "none";
			}
			entry.put("Expiration", expiration_string);
			
			entry.put("Hash", metadata.getHash());
		 } catch (Exception e) {
			 logger.error("Unable to build metadata entry",e);
		 }
		return entry;
	 }
	 
	 private Map<String,String> buildResourceEntry(Resource resource, NavigableSet<Group> groups){
		 Map<String,String> entry = new HashMap<String, String>();
		 try {
			 entry.put("Name", resource.getName());
			 entry.put("User", resource.getSubmitter());
			 
			 String uid = resource.getUid();
			 entry.put("Creator", ESAPI.encoder().encodeForHTML(dao.latestCallsign(uid)));
			 
			 Long size = resource.getSize();
			 if (size == null ) {
				entry.put("Size","Unknown");
			 } else {
				if(size < 1024) {
					entry.put("Size", size + "B");
			 } else if(size < MBYTES) {
				entry.put("Size", (size/KBYTES) + "kB");
			 } else if(size < GBYTES) {
				entry.put("Size", size/MBYTES + "MB");
			 } else {
				entry.put("Size", size/GBYTES + "GB");
			    }
			 }
			 
			 entry.put("Time", resource.getSubmissionTime().toString());
			 
			 entry.put("MimeType", resource.getMimeType());
			 
			 resource.setKeywords(fileManagerService.getKeywordsForResource(resource.getHash()));
			 List<String> keywordsArray = resource.getKeywords();
			 if(keywordsArray != null) {
				 String keywords = String.join(",", resource.getKeywords());
				 entry.put("Keywords",keywords);
			 } else {
				 entry.put("Keywords","");
			 }
			 
			 
			Long expiration = resource.getExpiration();
			String expiration_string = "";
			if (expiration != null && expiration >= 0) {
			    expiration_string = Instant.ofEpochSecond(expiration).toString();
			    expiration_string = expiration_string.substring(0, expiration_string.length() - 1);
			} else {
			    expiration_string = "none";
			}
			entry.put("Expiration", expiration_string);
			
			entry.put("Hash", resource.getHash());
			
			resource.setGroups(RemoteUtil.getInstance().getGroupNamesForBitVectorString(
					resource.getGroupVector(), groups));
			
			NavigableSet<String> groupsArray = resource.getGroups();
			if(keywordsArray != null) {
				 String groupString = String.join(",", groupsArray);
				 entry.put("Groups",groupString);
			 } else {
				 entry.put("Groups","");
			 }
			
		 } catch (Exception e) {
			 logger.error("Error getting metadata entry: ",e);
		 }
		return entry;
	 }
	 
	 private String getResourceColumnName(String sort) {
			switch(sort) {
			  case "name":
			    return "name";
			  case "updateTime":
			     return "submissionTime";
			  case "size":
				  return "size";
			  default:
			    return "";
			}
		}
}
