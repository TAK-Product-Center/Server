package tak.server.federation.hub.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.google.common.io.ByteSource;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import tak.server.federation.hub.FederationHubDependencyInjectionProxy;
import tak.server.federation.hub.db.FederationHubDatabase;

public class FederationHubDatabaseServiceImpl implements FederationHubDatabaseService {
	private static final Logger logger = LoggerFactory.getLogger(FederationHubDatabaseService.class);
	
	private static final String FEDERATE_EVENT_COLLECTION_NAME = "federate_event";
	private static final String FEDERATE_METADATA_COLLECTION_NAME = "federate_metadata";
	private static final String FEDERATE_RESOURCES_COLLECTION_NAME = "mission_resources";
	
	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private FederationHubDatabase federationHubDatabase;
	private CacheManager cacheManager;
	
	public FederationHubDatabaseServiceImpl(FederationHubDatabase federationHubDatabase, CacheManager cacheManager) {
		this.federationHubDatabase = federationHubDatabase;
		this.cacheManager = cacheManager;
		
		scheduler.scheduleWithFixedDelay(() -> {
			try {
				if (!isDBConnected())
					return;
				
				long retentionDays = FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfig().getMissionFederationDBRetentionDays();
				
				// delete expired events based on received_time
				Bson receivedTimeFilter = Filters.lt("received_time", new Date(Instant.now().toEpochMilli() - retentionDays*24*60*60 * 1000));
				DeleteResult eventDeleteResult = federateEventCollection().deleteMany(receivedTimeFilter);
				
				if (logger.isDebugEnabled()) {
					logger.debug("Deleted " + eventDeleteResult.getDeletedCount() + " events from the database");
				}
				
				// delete expired resources based on uploadDate
				Bson uploadDateFilter = Filters.lt("uploadDate", new Date(Instant.now().toEpochMilli() - retentionDays*24*60*60 * 1000));
				GridFSFindIterable filesToDelete = resourceCollection().find(uploadDateFilter);
				
				filesToDelete.forEach(file -> {
					resourceCollection().delete(file.getObjectId());
				});
	        } catch (MongoException me) {
	        	logger.error("retention scheduler error: ", me);
	        }
			
		}, 10, 60, TimeUnit.SECONDS);
	}
	
	@Override
	public List<Document> getOfflineUpdates(String id, Date lastUpdate) {
		List<Document> updates = new ArrayList<>();
		
		if (!isDBConnected())
			return updates;
		
        try {
        	Bson sort = Sorts.ascending("received_time");
        	Bson dateFilter = Filters.gte("received_time", lastUpdate);
        	Bson findFilter = Filters.eq("federate_id", id);
        	
        	updates.addAll(federateEventCollection()
        			.find(findFilter)
        			.filter(dateFilter)
        			.sort(sort)
        			.into(new ArrayList<Document>()));
        } catch (MongoException me) {
        	logger.error("getOfflineUpdates error: ", me);
        }
        
        return updates;
	}
	
	@Override
	public void trackFederateConnect(String id) {
		if (!isDBConnected())
			return;
		
        try {
            InsertOneResult result = federateEventCollection().insertOne(new Document()
                    .append("_id", new ObjectId())
                    .append("federate_id", id)
                    .append("event_kind", "connect")
                    .append("event_time", Instant.now())
                    .append("remote", true));            
        } catch (MongoException me) {
        	logger.error("trackFederateConnect error: ", me);
        }
	}
	
	@Override
	public void storeRol(Document rol) {
		if (!isDBConnected())
			return;
		
		try {
			InsertOneResult result = federateEventCollection().insertOne(rol);
		} catch (MongoException me) {
			logger.error("storeRol error: ", me);
		}
	}
	
	@Override
	public ObjectId addResource(byte[] data, Map<String, Object>  parameters) {
		if (!isDBConnected())
			return null;
		
         try {
        	Document metadata = new Document();
        	metadata.putAll(parameters);
			InputStream streamToUploadFrom = ByteSource.wrap(data).openStream();
			GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(1048576)
                    .metadata(metadata);
            ObjectId fileId = resourceCollection().uploadFromStream((String) parameters.get("filename"), streamToUploadFrom, options);
            return fileId;
		} catch (IOException e) {
			logger.error("Error writing resource", e);
		}
		return null;
	}
	
	@Override
	public byte[] getResource(ObjectId resourceObjectId) {
		if (!isDBConnected())
			return null;
		
         try {
             try (GridFSDownloadStream downloadStream = resourceCollection().openDownloadStream(resourceObjectId)) {
                 int fileLength = (int) downloadStream.getGridFSFile().getLength();
                 
     			long maxSizeBytes = FederationHubDependencyInjectionProxy.getInstance().fedHubServerConfig().getMissionFederationDisruptionMaxFileSizeBytes();
    			if (fileLength > maxSizeBytes) {
    				logger.info("File size of " + fileLength + " exceeds config limit of " + maxSizeBytes + "MB. Skipping " + downloadStream.getGridFSFile().getMetadata());
    				return null;
    			}
                 
                 byte[] bytesToWriteTo = new byte[fileLength];
                 downloadStream.read(bytesToWriteTo);
                 return bytesToWriteTo;
             }
		} catch (Exception e) {
			logger.error("Error reading resource", e);
		}
		return null;
	}

	@Override
	@Cacheable(value = "federate_metadata", key = "{#root.args[0]}", sync = true)
	public Document getFederateMetadata(String federateServerId) {
		if (!isDBConnected())
			return null;
		
		try {
			Bson filter = Filters.eq("federate_id", federateServerId);
			Document result = federateMetadataCollection().find(filter).first();
			return result;
		} catch (MongoException me) {
			logger.error("getFederateMetadata error", me);
			return null;
		}
	}
	
	@Override
	public List<Document> getFederateMetadatas() {
		List<Document> results = new ArrayList<>();

		if (!isDBConnected())
			return results;
		
		try {
			results.addAll(federateMetadataCollection().find().into(new ArrayList<Document>()));
		} catch (MongoException me) {
			logger.error("getFederateMetadatas error", me);
		}
		return results;
	}

	@Override
	@CachePut(value = "federate_metadata", key = "{#root.args[0]}")
	public Document addFederateMetadata(String id, Certificate[] certificates) {
		if (!isDBConnected())
			return null;
		
        try {
        	List<byte[]> binaryCerts = new ArrayList<>();
        	for (int i = 1; i < certificates.length; i++) {
        		if (certificates[i] == null) {
                    break;
                }
        		binaryCerts.add(certificates[i].getEncoded());
        	}
        	
        	Bson filter = Filters.eq("federate_id", id);
        	Bson update = Updates.combine(
        			Updates.set("federate_id", id), 
        			Updates.set("cert_array", binaryCerts), 
        			Updates.set("last_update", Instant.now()));
        	
        	UpdateOptions options = new UpdateOptions().upsert(true);
        	
        	UpdateResult result = federateMetadataCollection().updateOne(filter, update, options);
            
            return federateMetadataCollection().find(filter).first();
        } catch (Exception e) {
        	logger.error("addFederateMetadata error", e);
        	return null;
        }
	}
	
	private MongoCollection<Document> federateEventCollection;
	private MongoCollection<Document> federateEventCollection() {
		if (federateEventCollection == null)
			federateEventCollection = federationHubDatabase.getDB().getCollection(FEDERATE_EVENT_COLLECTION_NAME);
		
		return federateEventCollection;
	}
	
	private MongoCollection<Document> federateMetadataCollection;
	private MongoCollection<Document> federateMetadataCollection() {
		if (federateMetadataCollection == null)
			federateMetadataCollection = federationHubDatabase.getDB().getCollection(FEDERATE_METADATA_COLLECTION_NAME);
		
		return federateMetadataCollection;
	}
	
	private GridFSBucket gridFSBucket;
	private GridFSBucket resourceCollection() {
		if (gridFSBucket == null)
			gridFSBucket = GridFSBuckets.create(federationHubDatabase.getDB(), FEDERATE_RESOURCES_COLLECTION_NAME);
		
		return gridFSBucket;
	}
	
	private CacheStats getCoffeeCacheStats(String cacheName) {
		try {
			 org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
		        Cache nativeCoffeeCache = (Cache) cache.getNativeCache();
		        return nativeCoffeeCache.stats();
		} catch(Exception e) {
			logger.error("getCoffeeCacheStats: " + cacheName, e);
		}
		return null;
    }
	
	private boolean isDBConnected() {
		return federationHubDatabase.isDBConnected();
	}
}
