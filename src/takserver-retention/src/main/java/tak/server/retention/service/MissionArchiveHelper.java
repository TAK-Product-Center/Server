package tak.server.retention.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.remote.service.RetentionQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import tak.server.cot.XmlContainer;
import tak.server.retention.config.MissionArchiveStoreConfig;
import tak.server.retention.config.MissionArchiveStoreConfig.MissonArchiveStoreEntry;

public class MissionArchiveHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(MissionArchiveHelper.class);
	
	private static final String ARCHIVE_DIR = "mission-archive/";
	private static final String MISSION_STORE_FILE = ARCHIVE_DIR + "mission-store.yml";

	@Autowired
	private MissionArchiveStoreConfig missionArchiveStore;
	
	@Autowired
	RetentionQueryService retentionQueryService;
	
	public void removeExpiredArchiveEntries(double ttlDays) {
		List<MissonArchiveStoreEntry> entriesToRemove = new ArrayList<>();
		List<MissonArchiveStoreEntry> entriesToKeep = new ArrayList<>();
		
        missionArchiveStore.getMissonArchiveStoreEntries().forEach(missionArchiveStoreEntry-> {
        	double msInArchive = new Date().getTime() - Double.valueOf(missionArchiveStoreEntry.getArchiveTimeMs());
        	double daysInArchive = msInArchive / 1000 / 60 / 60 / 24;
        	if (daysInArchive > ttlDays) {		
        		entriesToRemove.add(missionArchiveStoreEntry);
        	} else {
        		entriesToKeep.add(missionArchiveStoreEntry);
        	}
        });
        
        if (entriesToRemove.size() == 0) return;
        
        missionArchiveStore.setMissonArchiveStoreEntries(entriesToKeep);
        
        try {
        	ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
	        File file = new File(MISSION_STORE_FILE);
	        
	        MissionArchiveStoreConfig newMissionArchiveStoreConfig = new MissionArchiveStoreConfig();
	        newMissionArchiveStoreConfig.setMissonArchiveStoreEntries(entriesToKeep);
			mapper.writeValue(file, newMissionArchiveStoreConfig);
			
			logger.info("Removed Expired Mission Archive Entries " + Arrays.toString(entriesToRemove.toArray()));
		
	        entriesToRemove.forEach(missionArchiveStoreEntry-> {			
		        String filename = missionArchiveStoreEntry.getCreateTime().toString() + "_" + missionArchiveStoreEntry.getMissionName() + ".zip";
	    		filename = filename.replace(":", "-");
	    		String zipPath = ARCHIVE_DIR + filename;
				File missionArchiveFileToDelete = new File(zipPath);
				missionArchiveFileToDelete.delete();
	        });
		} catch (Exception e) {
			logger.error("Error removing expired entries from mission archive: " + Arrays.toString(entriesToRemove.toArray()), e);
			return;
		}        
	}
	
	public String getMissionStoreJson() {
		try {
			MissionArchiveStoreConfig newMissionArchiveStoreConfig = new MissionArchiveStoreConfig();
            newMissionArchiveStoreConfig.setMissonArchiveStoreEntries(missionArchiveStore.getMissonArchiveStoreEntries());
	
			ObjectMapper jsonWriter = new ObjectMapper();
			return jsonWriter.writeValueAsString(newMissionArchiveStoreConfig);
		} catch (Exception e) {
			logger.error("Exception saving mission archive file ", e);
		}

		return "{}";
	}
	
    public synchronized void archiveMissionAndDelete(Map<String,Object> mission) {
    	try {
    		String missionName = (String) mission.get("name");
    		String groupVector = String.valueOf(mission.get("groups"));
    		String creatoruid = (String) mission.get("creatoruid");
    		Timestamp createtime = (Timestamp) mission.get("create_time");
    		Timestamp archiveTime = new Timestamp(new Date().getTime());
    		
    		logger.info("Trying to archive mission " + missionName);
    		byte[] zip = retentionQueryService.getArchivedMission(missionName, groupVector, "");
    		
    		String filename = createtime.toString() + "_" + missionName + ".zip";
    		filename = filename.replace(":", "-");
    		logger.info("Writing " + filename + " to disk");
    		writeMissionToArchive(filename, missionName, createtime, archiveTime, zip);
    		
    		logger.info("Write successful, removing " + missionName + " and its contents from database");
    		retentionQueryService.deleteMission(missionName, creatoruid, groupVector, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("Error archiving mission",e);
		}
    }
    
    public String restoreMissionFromArchive(int id) {
		try {
			Optional<MissonArchiveStoreEntry> matchingEntryOp = missionArchiveStore.getMissonArchiveStoreEntries().stream().filter(m->m.getId() == id).findFirst();
			
			if (!matchingEntryOp.isPresent()) {
				return "Mission to restore not found in mission index";
			}
					
			MissonArchiveStoreEntry matchingEntry = matchingEntryOp.get();
			String filename = ARCHIVE_DIR + matchingEntry.getCreateTime() + "_" + matchingEntry.getMissionName() + ".zip";
			
			Map<String, byte[]> files = new HashMap<>();
			ZipEntry entry;
			final int BUFFER = 2048;
			FileInputStream fis = new FileInputStream(filename);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));
			while ((entry = zis.getNextEntry()) != null) {

				// skip directories
				if (entry.isDirectory()) {
					continue;
				}

				// load in the file
				int count;
				byte data[] = new byte[BUFFER];
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					bos.write(data, 0, count);
				}
				bos.flush();
				bos.close();
				files.put(entry.getName(), bos.toByteArray());
			}
			zis.close();
			
			MissionPackageManifest manifest = null;
			try {
				SAXReader reader = new SAXReader();
				Document doc = reader.read(new ByteArrayInputStream(files.get("MANIFEST/manifest.xml")));
				
				if (doc == null) {
					logger.info("Could not find mission manifest");
					return "Could not find mission manifest file";
				}
				
				manifest = new MissionPackageManifest(doc);
			} catch (Exception e) {
				logger.info("Could not find mission manifest");
				return "Error reading mission manifest file";
			}
			
			Map<String, String> props = new HashMap<>();
			manifest.getConfigurations().forEach(config-> {
				props.put(config.name, config.value);
			});
						
			String defaultRole = null;
			List<String> defaultPermissions = new ArrayList<>();
			if (!StringUtils.isEmpty(manifest.getRole())) {
				defaultRole = manifest.getRole();
				defaultPermissions = manifest.getPermissions();
			}
			
			List<String> groups =  manifest.getGroups();
						
			boolean success = retentionQueryService.restoreMission(files, props, groups, defaultRole, defaultPermissions);
			
			if (success) {
				List<byte[]> cotEvents = new ArrayList<>();
				for (Element missionContent : manifest.getContents()) {
					String zipEntry =  missionContent.attributeValue("zipEntry");
					
					if (zipEntry.endsWith(".cot")) {
						cotEvents.add(files.get(zipEntry));
					} else {
						retentionQueryService.restoreContent(props.get("mission_name"), files.get(zipEntry), missionContent, groups);
					}
				}
				
				retentionQueryService.restoreCoT(props.get("mission_name"), cotEvents, groups);
				
		        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
		        File file = new File(MISSION_STORE_FILE);
				
				// remove the mission from the mission store index			
				missionArchiveStore.getMissonArchiveStoreEntries().remove(matchingEntry);
				MissionArchiveStoreConfig newMissionArchiveStoreConfig = new MissionArchiveStoreConfig();
		        newMissionArchiveStoreConfig.setMissonArchiveStoreEntries(missionArchiveStore.getMissonArchiveStoreEntries());
		        mapper.writeValue(file, newMissionArchiveStoreConfig);
				
				// lastly, remove the zip archive file
				File missionArchiveFileToDelete = new File(filename);
				missionArchiveFileToDelete.delete();
				return props.get("mission_name") + " Restored";
			} else {
				retentionQueryService.deleteMission(props.get("mission_name"), props.get("creatorUid"), groups, true);
				return "Mission name already exsists. Cannot restore.";
			}
		} catch (Exception e) {
			logger.info("Error reading mission zip from archive",e);
			return "Error reading mission zip from archive";
		}
    }

	private void writeMissionToArchive(String filename, String missionName, Timestamp createTime,
			Timestamp archiveTime, byte[] zip) throws Exception {
		String zipPath = ARCHIVE_DIR + filename;
		
		if (new File(zipPath).exists()) {
			return;
		} else {
			Path path = Paths.get(zipPath);
			Files.write(path, zip);
		}
		
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        try {
            File file = new File(MISSION_STORE_FILE);

            MissonArchiveStoreEntry missonArchiveStoreEntry = new MissonArchiveStoreEntry();
            missonArchiveStoreEntry.setMissionName(missionName);
            missonArchiveStoreEntry.setCreateTimeMs(String.valueOf(createTime.getTime()));
            missonArchiveStoreEntry.setArchiveTimeMs(String.valueOf(archiveTime.getTime()));
            missonArchiveStoreEntry.setCreateTime(createTime.toString().replace(":", "-"));
            missonArchiveStoreEntry.setArchiveTime(archiveTime.toString().replace(":", "-"));
            missonArchiveStoreEntry.setId(filename.hashCode());
            missionArchiveStore.addMissionEntry(missonArchiveStoreEntry);
            
            MissionArchiveStoreConfig newMissionArchiveStoreConfig = new MissionArchiveStoreConfig();
            newMissionArchiveStoreConfig.setMissonArchiveStoreEntries(missionArchiveStore.getMissonArchiveStoreEntries());
            
            mapper.writeValue(file, newMissionArchiveStoreConfig);
        } catch (Exception e) {
            logger.error("Exception saving mission archive file ", e);
        }
	}
	
	private class MissionPackageManifest extends XmlContainer {
		public MissionPackageManifest(Document xml) {
			super(xml);	
		}
		
		public List<MissionParameter> getConfigurations() {
			List<Node> configurationParameters = doc.selectNodes("/MissionPackageManifest/Configuration/Parameter");
			
			return configurationParameters.stream().map(node-> {
				Element configuration = (Element) node;
				return new MissionParameter(configuration.attributeValue("name"), configuration.attributeValue("value"));
			}).collect(Collectors.toList());
		}
		
		public List<String> getGroups() {
			List<Node> groups = doc.selectNodes("/MissionPackageManifest/Groups/Group");
			
			return groups.stream().map(node-> {
				Element group = (Element) node;
				return group.attributeValue("name");
			}).collect(Collectors.toList());
		}
		
		public String getRole() {
			List<Node> roles = doc.selectNodes("/MissionPackageManifest/Role");
			Element roleElement = (Element) roles.get(0);
			return roleElement.attributeValue("name");
		}
		
		public List<String> getPermissions() {
			List<Node> permissions = doc.selectNodes("/MissionPackageManifest/Role/Permissions");
			
			return permissions.stream().map(node-> {
				Element permission = (Element) node;
				return permission.attributeValue("name");
			}).collect(Collectors.toList());
		}
		
		public List<Element> getContents() {
			List<Node> configurationParameters = doc.selectNodes("/MissionPackageManifest/Contents/Content");
			
			return configurationParameters.stream().map(node-> {
				Element content = (Element) node;
				return content;
			}).collect(Collectors.toList());
		}
	}
	
	private class MissionParameter {
		String name;
		String value;
		
		MissionParameter(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return "MissionParameter [name=" + name + ", value=" + value + "]";
		}
	}

}
