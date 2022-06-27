package com.bbn.marti.sync;

import java.rmi.RemoteException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.MartiValidator;

@RestController
public class MetadataApi extends BaseRestController {

	@Autowired
    private Validator validator;

    private static final Logger logger = LoggerFactory.getLogger(MetadataApi.class);
    
    @Autowired
    private EnterpriseSyncService syncStore;

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private CommonUtil martiUtil;

    @RequestMapping(value = "/sync/metadata/{hash}/{metadata}", method = RequestMethod.PUT)
    public ResponseEntity setMetadata(@PathVariable("hash") @NotNull String hash,
                                  @PathVariable("metadata") @NotNull String metadataField,
                                  @RequestBody @NotNull String metadataValue, HttpServletRequest request)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            try {
                validator.getValidInput("MetadataApi", hash,
                        MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
                validator.getValidInput("MetadataApi", metadataField,
                        MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
                validator.getValidInput("MetadataApi", metadataValue,
                        MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            } catch (com.bbn.marti.remote.exception.ValidationException e) {
                logger.error("ValidationException in setMetadata!", e);
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

            boolean status = false;
            if ((metadataField.compareToIgnoreCase(Metadata.Field.Tool.name()) == 0) ||
                (metadataField.compareToIgnoreCase(Metadata.Field.MIMEType.name()) == 0)) {
                status = syncStore.updateMetadata(hash, metadataField.toLowerCase(),
                        metadataValue, martiUtil.getGroupBitVector(request));

                List<String> names = missionRepository.getMissionNamesContainingHash(hash);
                for (String name : names) {
                    missionService.invalidateMissionCache(name);
                }

            } else {
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity(status ? HttpStatus.OK : HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            logger.error("Exception in setMetadata : " + e.getMessage());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/sync/metadata/{hash}/keywords", method = RequestMethod.PUT)
    public ResponseEntity setMetadataKeywords(@PathVariable("hash") @NotNull String hash,
                                        @RequestBody List<String> keywords)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            try {
                validator.getValidInput("MetadataApi", hash,
                        MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
                for (String keyword : keywords) {
                    validator.getValidInput("MetadataApi", keyword,
                            MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
                }
            } catch (com.bbn.marti.remote.exception.ValidationException e) {
                logger.error("ValidationException in setMetadataKeywords!", e);
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

            boolean status = syncStore.updateMetadataKeywords(hash, keywords);

            List<String> names = missionRepository.getMissionNamesContainingHash(hash);
            for (String name : names) {
                missionService.invalidateMissionCache(name);
            }

            return new ResponseEntity(status ? HttpStatus.OK : HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            logger.error("Exception in setMetadata : " + e.getMessage());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/sync/metadata/{hash}/expiration", method = RequestMethod.PUT)
    public ResponseEntity setExpiration(@PathVariable("hash") @NotNull String hash,
                                 @RequestParam(value = "expiration") Long expiration)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            try {
                validator.getValidInput("MetadataApi", hash,
                        MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
            } catch (com.bbn.marti.remote.exception.ValidationException e) {
                logger.error("ValidationException in setExpiration!", e);
                return new ResponseEntity(HttpStatus.BAD_REQUEST);
            }

            boolean status = false;
            if (logger.isDebugEnabled()) {
                logger.debug(" MetadataApi setting resource expiration " + expiration + " for hash " + hash);
            }
            status = syncStore.updateExpiration(hash, expiration);
            if (logger.isDebugEnabled()) {
                logger.info(" MetadataApi set resource status : " + status);
            }
            return new ResponseEntity(status ? HttpStatus.OK : HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Exception in setExpiration : " + e.getMessage());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
