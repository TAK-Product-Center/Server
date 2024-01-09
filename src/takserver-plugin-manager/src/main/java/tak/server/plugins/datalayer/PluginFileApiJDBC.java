/*
 * Copyright (c) 2013-2015 Raytheon BBN Technologies. Licensed to US Government with unlimited rights.
 */

package tak.server.plugins.datalayer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache.ValueWrapper;

import com.bbn.marti.config.Cluster;

import tak.server.Constants;

public class PluginFileApiJDBC{

	private static final Logger logger = LoggerFactory.getLogger(PluginFileApiJDBC.class);

	private static final String RESOURCE_TABLE = "resource";

	@Autowired
	private DataSource ds;

	@Autowired
	private CacheManager cacheManager;
	
	private boolean esyncEnableCache = false;


	public PluginFileApiJDBC() {

		// 0 means false, disable esync cache
		if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEsyncEnableCache() == 0) {
			esyncEnableCache = false;
		} else if (CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEsyncEnableCache() > 0) {
			// positive value means true, enable cache
			esyncEnableCache = true;
		} else {
			// autodetect based on number of processors
			if (Runtime.getRuntime().availableProcessors() > 4) {
				esyncEnableCache = true;
			}
		}
	}
	
	public InputStream readFileContent(String hash) throws Exception {
		
		if (ds == null) {
			logger.error("DataSource is null");
			throw new Exception("DataSource is null");
		}
		
		String cacheKey = "getContentByHash_" + hash;
		
		Cache cache = null;

		if (isCacheEsync()) {
		
			cache = cacheManager.getCache(Constants.ENTERPRISE_SYNC_CACHE_NAME);
			
			if (cache == null) {
				throw new IllegalStateException("unable to get " + Constants.ENTERPRISE_SYNC_CACHE_NAME);
			}
			
			ValueWrapper resultWrapper = cache.get(cacheKey);
			
			if (resultWrapper != null && resultWrapper.get() != null) {
				
				if (resultWrapper.get() instanceof byte[]) {
				
					// cache hit
					InputStream inputStream = new ByteArrayInputStream((byte[]) resultWrapper.get());
					return inputStream;
				
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("getContentByHash invalid cache result type " + (resultWrapper.get() == null ? "null" : resultWrapper.get().getClass().getName()) + " (should be byte[])");
					} else if (logger.isWarnEnabled()) {		
						logger.warn("getContentByHash invalid cache result type");
					}
				}
			}
		}
		
		try (Connection connection = ds.getConnection(); PreparedStatement query = connection.prepareStatement("SELECT " + "data" + " FROM "
				+ RESOURCE_TABLE + " r WHERE " + "hash"
				+ " = ? "
				+ "ORDER BY " + "submissiontime" + ";")) {
			query.setString(1, hash.toLowerCase());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					
					if (isCacheEsync() == false) {
						
						InputStream result = queryResults.getBinaryStream(1);
						return result;
					
					} else {
						
						byte[] bytes = queryResults.getBytes(1);
						
						// store in cache
						cache.put(cacheKey, bytes);
						
						InputStream inputStream = new ByteArrayInputStream(bytes);
						return inputStream;
					}	
					
				} else {
					logger.debug("getContentByHash no results");
					return null;
				}
			}

		} catch (Exception e) {
			logger.error("exception executing getContentByHash query " + e.getMessage(), e);
			throw e;
		}
		
	}
	
	private boolean isCacheEsync() {
		Cluster clusterConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getCluster();
		return esyncEnableCache || (clusterConfig.isEnabled() && clusterConfig.isKubernetes());
	}

}
