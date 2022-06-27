

package com.bbn.marti.cot.search.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.AltitudeConverter;
import com.bbn.marti.cot.search.CotSearchQueryMap;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.cot.search.model.CotSearch;
import com.bbn.marti.network.BaseRestController;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import tak.server.Constants;

/*
 * 
 * API to manage submission and status of CoT search queries
 * 
 * base path is /Marti/api/cot/search
 * 
 */

@RestController
public class CotQueryApi extends BaseRestController {
    
    @Autowired
    private CotSearchQueryMap searchQueryMap;
    
    @Autowired
    protected AltitudeConverter converter;
    
    public CotSearchQueryMap getSearchQueryMap() {
        return searchQueryMap;
    }

    public void setSearchQueryMap(CotSearchQueryMap searchQueryMap) {
        this.searchQueryMap = searchQueryMap;
    }
    
    public static final SimpleDateFormat sqlDateFormat = new SimpleDateFormat(Constants.SQL_DATE_FORMAT);

    PrettyTime prettyTimeFormat = new PrettyTime();
    
    Logger logger = LoggerFactory.getLogger(CotQueryApi.class);

    @RequestMapping(value = "cot/search/date", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public Date getDate() {

        logger.debug("get date request");

        return new Date();
    }
    
    @RequestMapping(value = "cot/search/{id}", method = RequestMethod.GET)
    @DateTimeFormat(iso=ISO.DATE)
    public ResponseEntity<ApiResponse<List<CotSearch>>> getSearch(@PathVariable("id") String id) {
        
        if (Strings.isNullOrEmpty(id)) {
            throw new IllegalArgumentException("empty id");
        }
        
        logger.debug("searchQueryMap size: " + searchQueryMap.size());
        
        CotSearch searchQuery = searchQueryMap.get(id);
        
        if (searchQuery == null) {
            throw new RuntimeException("query for id " + id + " not found");
        }
        
        logger.debug("fetched searchQuery: " + searchQuery + " for id " + id);
        
        // return response
        return new ResponseEntity<ApiResponse<List<CotSearch>>>(new ApiResponse<List<CotSearch>>(Constants.API_VERSION, CotSearch.class.getName(), Lists.newArrayList(searchQuery)), HttpStatus.OK);
    }
    
    @RequestMapping(method = RequestMethod.GET)
    @DateTimeFormat(iso=ISO.DATE)
    public ResponseEntity<ApiResponse<List<CotSearch>>> getAllSearches() {
        
        logger.debug("get all search queries");
        
        List<CotSearch> searchQueries = new ArrayList<>(searchQueryMap.values());
        
        if (searchQueries.isEmpty()) {
            throw new RuntimeException("no queries found");
        }
        
        logger.debug("returning searchQueries " + searchQueries);
        
        // return response
        return new ResponseEntity<ApiResponse<List<CotSearch>>>(new ApiResponse<>(Constants.API_VERSION, CotSearch.class.getName(), searchQueries), HttpStatus.OK);
    }
}


