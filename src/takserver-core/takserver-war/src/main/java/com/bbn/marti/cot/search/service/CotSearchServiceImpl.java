

package com.bbn.marti.cot.search.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.cot.search.CotSearchQueryMap;
import com.bbn.marti.cot.search.model.CotSearch;
import com.bbn.marti.cot.search.model.query.DeliveryProtocol;
import com.bbn.marti.cot.search.model.query.ImageOption;

/*
 * 
 */
public class CotSearchServiceImpl implements CotSearchService {

    private static ExecutorService executor = Executors.newCachedThreadPool();
    
    private static final Logger logger = LoggerFactory.getLogger(CotSearchServiceImpl.class);
    
    @Autowired
    private CotSearchQueryMap cotSearchQueryMap;

    @Override
    public String executeCotQueryAsync(String destination, int port, DeliveryProtocol protocol, String sqlPredicate, List<Object> sqlParameters, Integer resultLimit, Boolean latestOnly, ImageOption images, boolean replayMode, Double replaySpeed) {
    	
    	if (logger.isDebugEnabled()) {
    		logger.debug("executeCotQueryAsync destination: " + destination + " port: " + port + " protocol: " + protocol + " sqlPredicate: " + sqlPredicate + " sqlParameters: " + sqlParameters);
    	}
        
        // generate a unique identifier for this CoT query, which will be used to track the progress of the query and result transmission.
        String queryId = UUID.randomUUID().toString(); 
        
        CotSearch cotSearch = new CotSearch(queryId, "New query " + queryId);
        
        cotSearchQueryMap.put(queryId, cotSearch);
        
        // start CoT query execution in a worker thread from the cached thread pool.
        executor.execute(new CotQueryWorker(destination, port, protocol, sqlPredicate, sqlParameters, resultLimit, latestOnly, images, replayMode, replaySpeed, cotSearch));
        
        logger.debug("queryId: " + queryId + " cotSearch: " + cotSearch);
        
        return queryId;
    }
}
