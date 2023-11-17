package tak.server.retention;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tak.server.Constants;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.MissionArchiveConfig;
import com.bbn.marti.remote.service.MissionArchiveManager;
import com.bbn.marti.remote.service.RetentionPolicyConfig;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.service.MissionService;
@RestController
public class RetentionApi extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(RetentionApi.class);

    @Autowired
    private RetentionPolicyConfig retentionPolicyConfig;
    
    @Autowired
    private MissionArchiveManager missionArchiveManager;

    @Autowired
    MissionService missionService;

    @Autowired
    private EnterpriseSyncService syncStore;

    @RequestMapping(value = "/retention/policy", method = RequestMethod.GET)
    ResponseEntity<ApiResponse<Map<String, Integer>>> getRetentionPolicy(HttpServletResponse response) {
        ResponseEntity<ApiResponse<Map<String, Integer>>> result = null;
        try {
            result =  new ResponseEntity<ApiResponse<Map<String, Integer>>>(new ApiResponse<Map<String, Integer>>(Constants.API_VERSION,
                    "RetentionPolicies", retentionPolicyConfig.getRetentionPolicyMap()), HttpStatus.OK);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Exception accessing retentionPolicyConfig. Retention Polices not accessible ");
            }
        }

        if (result == null) {
            result = new ResponseEntity<ApiResponse<Map<String, Integer>>>(new ApiResponse<Map<String, Integer>>(Constants.API_VERSION,
                    "RetentionPolicies", new HashMap<String, Integer>()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Get all data retention policies" + result);
        }
        return result;
    }

    @RequestMapping(value = "/retention/policy", method = RequestMethod.PUT)
    ResponseEntity<ApiResponse<Map<String, Integer>>> updateRetentionPolicy(@RequestBody Map<String, Integer> policyMap) {
        ResponseEntity<ApiResponse<Map<String, Integer>>> result = null;
        logger.info(policyMap.toString());
        try {
            retentionPolicyConfig.updateRetentionPolicyMap(policyMap);
            result =  new ResponseEntity<ApiResponse<Map<String, Integer>>>(new ApiResponse<Map<String, Integer>>(Constants.API_VERSION,
                    "RetentionPolicies", retentionPolicyConfig.getRetentionPolicyMap()), HttpStatus.OK);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception accessing retentionPolicyConfig. Retention Polices not accessible ");
            }
        }

        if (result == null) {
            result = new ResponseEntity<ApiResponse<Map<String, Integer>>>(new ApiResponse<Map<String, Integer>>(Constants.API_VERSION,
                    "RetentionPolicies", new HashMap<String, Integer>()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Updated retention policy " + result);
        }
        return result;
    }

    @RequestMapping(value = "/retention/service/schedule", method = RequestMethod.GET)
    ResponseEntity<ApiResponse<String>> getRetentionServiceSchedule(HttpServletResponse response) {
        ResponseEntity<ApiResponse<String>> result = null;
        try {
            result =  new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                    "RetentionServiceSchedule", retentionPolicyConfig.getRetentionServiceSchedule()), HttpStatus.OK);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception accessing retentionPolicyConfig. Retention Service schedule is not accessible ");
            }
        }

        if (result == null) {
            result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                    "RetentionServiceSchedule", new String()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Get retention service schedule " + result);
        }
        return result;
    }

    @RequestMapping(value = "/retention/service/schedule", method = RequestMethod.PUT)
    ResponseEntity<ApiResponse<String>> setRetentionServiceSchedule(@RequestBody String cronexp) throws ValidationException {
        ResponseEntity<ApiResponse<String>> result = null;
        if (logger.isDebugEnabled()) {
            logger.debug(" setRetentionSchedule called " + cronexp);
        }
        if (! CronExpression.isValidExpression(cronexp) && !cronexp.equals("-")) {
            if (logger.isErrorEnabled()) {
                logger.error(" Invalid cron expression " + cronexp + " schedule not changed");
            }
        } else {
            try {
                result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                        "RetentionServiceSchedule", retentionPolicyConfig.setRetentionServiceSchedule(cronexp)), HttpStatus.OK);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Exception accessing retentionPolicyConfig. Retention Service schedule is not accessible ");
                }
            }

            if (result == null) {
                result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                        "RetentionServiceSchedule", new String()), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Get retention service schedule " + result);
            }
        }
        return result;
    }

    @RequestMapping(value = "/retention/mission/{name}/expiry/{time}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void scheduleMissionExpiration(@PathVariable(value = "name") String missionName,
                                          @PathVariable(value = "time") Long seconds,
                                          HttpServletRequest request)  {
        if (logger.isDebugEnabled()) {
            logger.debug(" schedule the mission expiration " + missionName + " at time " + seconds);
        }

        if (Strings.isNullOrEmpty(missionName)) {
            throw new IllegalArgumentException("empty mission name ");
        }

        if (seconds != null) {
            try {
                missionService.setExpiration(missionName, seconds);
                retentionPolicyConfig.setMissionExpiryTask(missionName, seconds);
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Exception accessing retentionPolicyConfig. Retention Service schedule is not accessible ");
                }
            }
        }
    }

    @RequestMapping(value = "/retention/resource/{name}/expiry/{time}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void scheduleResourceExpiration(@PathVariable(value = "name") String name,
                                           @PathVariable(value = "time") Long seconds,
                                           HttpServletRequest request)  {
        if (logger.isDebugEnabled()) {
            logger.debug(" schedule the resource expiration " + name + " at time " + seconds);
        }

        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("empty mission name ");
        }

        if (seconds != null) {
            try {
                syncStore.updateExpiration(name, seconds);
                retentionPolicyConfig.setResourceExpiryTask(name, seconds);
            } catch (Exception e) {
            	logger.error("Exception accessing retentionPolicyConfig. Retention Service schedule is not accessible ", e);
            }
        }
    }
    
    @RequestMapping(value = "/retention/missionarchive", method = RequestMethod.GET)
    ResponseEntity<ApiResponse<String>> getMissionArchive(HttpServletResponse response) {
        ResponseEntity<ApiResponse<String>> result = null;
        try {
            result =  new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                    "MissionArchive", missionArchiveManager.getMissionArchive()), HttpStatus.OK);
        } catch (Exception e) {
        	logger.error("Exception accessing mission archive.", e);
        }

        if (result == null) {
            result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                    "MissionArchive", new String()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }
    
    @RequestMapping(value = "/retention/restoremission", method = RequestMethod.POST)
    ResponseEntity<ApiResponse<String>> restoreMissionFromArchive(@RequestBody Integer id, HttpServletResponse response) {
    	ResponseEntity<ApiResponse<String>> result = null;
        try {
            result =  new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                    "MissionArchiveRestore", missionArchiveManager.restoreMissionFromArchive(id)), HttpStatus.OK);
        } catch (Exception e) {
        	logger.error("Exception accessing mission archive.", e);
        }

        if (result == null) {
            result = new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION,
                    "MissionArchiveRestore", new String("Server Error trying to restore mission")), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    	
        return result;
    }
    
    @RequestMapping(value = "/retention/missionarchiveconfig", method = RequestMethod.GET)
    ResponseEntity<ApiResponse<MissionArchiveConfig>> getMissionArchiveConfig(HttpServletResponse response) {
    	ResponseEntity<ApiResponse<MissionArchiveConfig>> result = null;
        try {
            result =  new ResponseEntity<ApiResponse<MissionArchiveConfig>>(new ApiResponse<MissionArchiveConfig>(Constants.API_VERSION,
                    "MissionArchiveConfig", missionArchiveManager.getMissionArchiveConfig()), HttpStatus.OK);
        } catch (Exception e) {
        	logger.error("Exception accessing mission archive config.", e);
        }

        if (result == null) {
            result = new ResponseEntity<ApiResponse<MissionArchiveConfig>>(new ApiResponse<MissionArchiveConfig>(Constants.API_VERSION,
                    "MissionArchiveConfig", new MissionArchiveConfig()), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }
    
    @RequestMapping(value = "/retention/missionarchiveconfig", method = RequestMethod.PUT)
    ResponseEntity<ApiResponse<MissionArchiveConfig>> updateMissionArchiveConfig(@RequestBody MissionArchiveConfig missionArchiveConfig, HttpServletResponse response) {
    	ResponseEntity<ApiResponse<MissionArchiveConfig>> result = null;
    	try {
            missionArchiveManager.updateMissionArchiveConfig(missionArchiveConfig);
            result =  new ResponseEntity<ApiResponse<MissionArchiveConfig>>(new ApiResponse<MissionArchiveConfig>(Constants.API_VERSION,
                    "MissionArchiveConfig", missionArchiveManager.getMissionArchiveConfig()), HttpStatus.OK);
        } catch (Exception e) {
        	logger.error("Exception updating Mission Archive Config.", e);
        }
    	
    	if (result == null) {
    		result = new ResponseEntity<ApiResponse<MissionArchiveConfig>>(new ApiResponse<MissionArchiveConfig>(Constants.API_VERSION,
                    "MissionArchiveConfig", new MissionArchiveConfig()), HttpStatus.INTERNAL_SERVER_ERROR);
    	}
        return result;
    }
}
