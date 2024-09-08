package com.bbn.marti.sync;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.marti.config.Cluster;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.TakException;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;

import tak.server.Constants;

public class EnterpriseSyncCacheHelper {

	private static final Logger logger = LoggerFactory.getLogger(EnterpriseSyncCacheHelper.class);

	private com.github.benmanes.caffeine.cache.Cache<String, FileWrapper> caffeineFileCache = null;

	private org.springframework.cache.Cache springFileCache = null;

	@Autowired
	private JDBCQueryAuditLogHelper queryHelper;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private CacheManager cacheManager;

	private final ConcurrentHashMap<String, Semaphore> resourceHashAvailableMap = new ConcurrentHashMap<>();

	private boolean esyncEnableCache = false;

	private Cluster clusterConfig;

	@EventListener({ContextRefreshedEvent.class})
	public void init() {

		clusterConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster();

		// 0 means false, disable esync cache
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEsyncEnableCache() == 0) {
			esyncEnableCache = false;
			logger.info("file cache explicity disabled.");
		} else if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEsyncEnableCache() > 0) {
			// positive value means true, enable cache
			esyncEnableCache = true;
			logger.info("file cache explicity enabled.");
		} else {
			// autodetect based on number of processors
			if (Runtime.getRuntime().availableProcessors() > 1) {
				esyncEnableCache = true;
				logger.info("multicore CPU detected. File cache enabled.");
			}
		}

		// only instiate the cache that we need (spring or caffeine) based on config
		if (isCacheSpring()) {
			springFileCache = cacheManager.getCache(Constants.ENTERPRISE_SYNC_CACHE_NAME);
		} else {
			caffeineFileCache = Caffeine.newBuilder()
					.expireAfterWrite(CoreConfigFacade.getInstance().getCachedConfiguration().getBuffer().getQueue().getCaffeineFileCacheSeconds(), TimeUnit.SECONDS)
					.build();
		}

		logger.info("Initialzed EnterpriseSyncCacheHelper with file cache TTL {} seconds. Implementation: {}",
				String.valueOf(CoreConfigFacade.getInstance().getCachedConfiguration().getBuffer().getQueue().getCaffeineFileCacheSeconds()), isCacheSpring() ? "ignite" : "caffeine");
	}

	// get file. If not found, query the database to fetch it.
	// @returns FileWrapper result. If no result, return null
	// The FileWrapper object contains the group vector. Use that to check access control.
	public FileWrapper getFileByHash(String hash) {

		if (Strings.isNullOrEmpty(hash)) {
			throw new IllegalArgumentException("empty hash");
		}

		// if file cache disabled, query db to get file bytes
		if (!isCacheEsync()) {
			return getFileFromDB(hash);
		}

		// cache enabled
		FileWrapper file = getFileFromCache(hash);

		if (file != null) {
			// cache hit - no lock
			return file;
		}

		Semaphore lock = null;

		try {
			// only lock on cache miss. block to acquire semaphore.
			lock = getResourceLock(hash);
			lock.acquire();

			// double-checked cache get
			file = getFileFromCache(hash);

			if (file != null) {
				// cache hit - double-checked lock
				return file;
			}

			// query for the file
			file = getFileFromDB(hash);

			if (file != null && file.getContents() != null) {

				if (isCacheSpring()) {
					// cache the file
					springFileCache.put(hash, file);
				} else {
					caffeineFileCache.put(hash, file);
				}
			}
		} catch (InterruptedException e) {
			logger.error("interrupted", e);
		} finally {
			try {
				// release lock and remove it from lock map
				lock.release();
			} finally {
				deleteLock(hash);
			}
		}

		return file;
	}

	// get from cache, if miss return null
	private FileWrapper getFileFromCache(String hash) {

		// spring (ignite) cache 
		if (isCacheSpring()) {
			ValueWrapper valueWrapper = springFileCache.get(hash);

			if (valueWrapper == null) {
				return null;
			}

			Object value = valueWrapper.get();

			if (value instanceof FileWrapper) {
				return (FileWrapper) value;
			}

			throw new IllegalArgumentException("invalid cached object type " + value.getClass().toString());
		}

		// caffeine cache
		return caffeineFileCache.getIfPresent(hash);
	}

	private FileWrapper getFileFromDB(String hash) {

		if (Strings.isNullOrEmpty(hash)) {
			throw new IllegalArgumentException("empty hash");
		}

		FileWrapper fileWrapper = null;

		try (Connection connection = dataSource.getConnection(); PreparedStatement query = queryHelper.prepareStatement(
				"SELECT data, groups FROM resource r WHERE hash = ? ORDER BY submissionTime limit 1;", connection)) {

			query.setString(1, hash.toLowerCase());
			logger.debug("getFileFromDB Executing SQL: {}",  query.toString());

			try (ResultSet queryResults = query.executeQuery()) {

				fileWrapper = new FileWrapper();

				if (queryResults.next()) {
					fileWrapper.setContents(queryResults.getBytes(1));
					fileWrapper.setHash(hash);
					fileWrapper.setGroupVector(queryResults.getString(2));
				} else {
					logger.info("getContentByHash no results {}", hash);
				}
			}

		} catch (Exception e) {
			String msg = "exception executing getContentByHash query " + e.getMessage();
			logger.error(msg, e);
			throw new TakException(msg, e);
		}

		return fileWrapper;

	}
	
	public FileWrapper getInputStreamFileWrapperFromDB(String hash) {
		
		if (Strings.isNullOrEmpty(hash)) {
			throw new IllegalArgumentException("empty hash");
		}
		
		FileWrapper fileWrapper = null;

		try (Connection connection = dataSource.getConnection(); PreparedStatement query = queryHelper.prepareStatement(
				"SELECT data, groups, length(data) FROM resource r WHERE hash = ? ORDER BY submissionTime limit 1", connection)) {

			query.setString(1, hash.toLowerCase());
			logger.debug("getInputStreamFileWrapperFromDB Executing SQL: {}",  query.toString());

			try (ResultSet queryResults = query.executeQuery()) {

				fileWrapper = new FileWrapper();
				
				if (queryResults.next()) {
					
					InputStream contentStream = queryResults.getBinaryStream(1);
					long contentLen = queryResults.getLong(3);
					
					logger.debug("content length {}, stream {}, ", contentLen, contentStream);
					
					fileWrapper.setInputStream(queryResults.getBinaryStream(1));
					fileWrapper.setHash(hash);
					fileWrapper.setGroupVector(queryResults.getString(2));
				} else {
					logger.info("getContentByHash no results {}", hash);
				}
			}

		} catch (Exception e) {
			String msg = "exception executing getInputStreamFileWrapperFromDB query " + e.getMessage();
			logger.error(msg, e);
			throw new TakException(msg, e);
		}

		return fileWrapper;

	}
	
	public FileWrapper getInputStreamFileWrapperFromDBbyUid(String uid) {
		
		if (Strings.isNullOrEmpty(uid)) {
			throw new IllegalArgumentException("empty uid");
		}
		
		FileWrapper fileWrapper = null;

		try (Connection connection = dataSource.getConnection(); PreparedStatement query = queryHelper.prepareStatement(
				"SELECT data, groups FROM resource r WHERE uid = ? ORDER BY submissionTime desc limit 1", connection)) {

			query.setString(1, uid.toLowerCase());
			logger.debug("getInputStreamFileWrapperFromDBbyUid Executing SQL: {}",  query.toString());

			try (ResultSet queryResults = query.executeQuery()) {

				fileWrapper = new FileWrapper();

				if (queryResults.next()) {
					fileWrapper.setInputStream(queryResults.getBinaryStream(1));
					fileWrapper.setUid(uid);
					fileWrapper.setGroupVector(queryResults.getString(2));
				} else {
					logger.info("getInputStreamFileWrapperFromDBbyUid no results {}", uid);
				}
			}

		} catch (Exception e) {
			String msg = "exception executing getInputStreamFileWrapperFromDB query " + e.getMessage();
			logger.error(msg, e);
			throw new TakException(msg, e);
		}

		return fileWrapper;

	}

	private boolean isCacheEsync() {
		return esyncEnableCache || (clusterConfig.isEnabled() && clusterConfig.isKubernetes());
	}

	private boolean isCacheSpring() {
		return clusterConfig.isEnabled() && clusterConfig.isKubernetes();
	}

	private Semaphore getResourceLock(String hash) {
		if (Strings.isNullOrEmpty(hash)) {
			throw new IllegalArgumentException("empty hash");
		}

		Semaphore lock = resourceHashAvailableMap.get(hash);

		if (lock != null) {
			return lock;
		}

		lock = new Semaphore(1, true);

		resourceHashAvailableMap.putIfAbsent(hash, lock);

		return lock;
	}

	private void deleteLock(String hash) {
		resourceHashAvailableMap.remove(hash);
	}
}
