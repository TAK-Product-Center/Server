

package com.bbn.marti.cot.search.service;

import java.util.List;

import com.bbn.marti.cot.search.model.query.DeliveryProtocol;
import com.bbn.marti.cot.search.model.query.ImageOption;

/*
 * 
 * 
 */
public interface CotSearchService {
    
    // asynchronously execute a CoT search query, and return a unique identifier for the request 
    public String executeCotQueryAsync(String destination, int port, DeliveryProtocol protocol, String sqlPredicate, List<Object> sqlParameters, Integer resultLimit, Boolean latestOnly, ImageOption images, boolean replayMode, Double replaySpeed);
}
