package com.bbn.marti.sync.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.sync.model.Caveat;
import com.bbn.marti.sync.model.Classification;
import com.bbn.marti.sync.repository.CaveatRepository;
import com.bbn.marti.sync.repository.ClassificationRepository;
import tak.server.Constants;


@RestController
public class ClassificationApi extends BaseRestController {
	
	private static final Logger logger = LoggerFactory.getLogger(ClassificationApi.class);

    @Autowired
    private ClassificationRepository classificationRepository;
    
    @Autowired
    private CaveatRepository caveatRepository;
	
    @RequestMapping(value = "/classification", method = RequestMethod.GET)
    ApiResponse<List<Classification>> getAllClassifications() {
        return new ApiResponse<List<Classification>>(Constants.API_VERSION,
                Classification.class.getSimpleName(), classificationRepository.findAll());
    }
    
    @RequestMapping(value = "/classification/{level}", method = RequestMethod.GET)
    ApiResponse<Classification> getClassificationForLevel(@PathVariable("level") String level) {
        return new ApiResponse<Classification>(Constants.API_VERSION,
                Classification.class.getSimpleName(), classificationRepository.findByLevel(level));
    }
    
    @RequestMapping(value = "/classification", method = RequestMethod.PUT)
    ApiResponse<Classification> setCaveatsForClassification(@RequestBody Classification classification) throws Exception{
    	logger.debug("Set caveats for Classification level {}", classification.getLevel());
    	try {
    		Classification dbClassification = classificationRepository.findByLevel(classification.getLevel());
    		Long classification_id = dbClassification.getId();
    		logger.debug("Found classification_id {} for Classification level {}", classification_id, classification.getLevel());
    		
    		classificationRepository.unlinkAllCaveats(classification_id);
    		logger.debug("Unlinked all caveats for classification_id {}", classification_id);
    		
    		for (Caveat caveat: classification.getCaveats()) {
    			Caveat dbCaveat = caveatRepository.findByName(caveat.getName());
    			Long caveat_id = dbCaveat.getId();
    			classificationRepository.linkCaveat(classification_id, caveat_id);
    			logger.info("Linked caveat {} to classification_id {}", caveat_id, classification_id);
    		}
    		
        	logger.info("Successfully set caveats for classification level {}", classification.getLevel());

            return new ApiResponse<Classification>(Constants.API_VERSION,
                    Classification.class.getSimpleName(), classification);
            
    	}catch (Exception e) {
    		logger.error( "Error in setCaveatsForClassification",e );
    		throw new Exception("Server error: " + e.getLocalizedMessage());
    	}

    }
    
    @RequestMapping(value = "/classification/{level}", method = RequestMethod.POST)
    ApiResponse<Classification> newClassification(@PathVariable("level") String level) {
    	Classification classification = new Classification(level);
    	classificationRepository.save(classification);
        return new ApiResponse<Classification>(Constants.API_VERSION,
                Classification.class.getSimpleName(), classification);
    }
    
    @RequestMapping(value = "/classification/{level}", method = RequestMethod.DELETE)
    ApiResponse<Long> deleteClassification(@PathVariable("level") String level)  throws Exception{
    	Classification dbClassification = classificationRepository.findByLevel(level);
    	if (dbClassification == null) {
    		throw new Exception("Classification not found");
    	}
    	classificationRepository.unlinkAllCaveats(dbClassification.getId());

    	classificationRepository.deleteClassificationOnly(dbClassification.getId());

        return new ApiResponse<Long>(Constants.API_VERSION,
        		Long.class.getSimpleName(), 0L);
    }
    
    @RequestMapping(value = "/caveat", method = RequestMethod.GET)
    ApiResponse<List<Caveat>> getAllCaveat() {
    	 return new ApiResponse<List<Caveat>>(Constants.API_VERSION,
    			 Caveat.class.getSimpleName(), caveatRepository.findAll());
    }
    
    @RequestMapping(value = "/caveat/{name}", method = RequestMethod.POST)
    ApiResponse<Caveat> newCaveat(@PathVariable("name") String name) {
    	Caveat caveat = new Caveat(name);
    	caveatRepository.save(caveat);
        return new ApiResponse<Caveat>(Constants.API_VERSION,
                Classification.class.getSimpleName(), caveat);
    }
    
    @RequestMapping(value = "/caveat/{name}", method = RequestMethod.DELETE)
    ApiResponse<Long> deleteCaveat(@PathVariable("name") String name) throws Exception{
    	
    	Caveat dbCaveat = caveatRepository.findByName(name);
    	if (dbCaveat == null) {
    		throw new Exception("Caveat not found");
    	}
    	caveatRepository.unlinkAllClassifications(dbCaveat.getId());
    	
    	long re = caveatRepository.deleteByName(name);
        return new ApiResponse<Long>(Constants.API_VERSION,
        		Long.class.getSimpleName(), re);
    }
    
}