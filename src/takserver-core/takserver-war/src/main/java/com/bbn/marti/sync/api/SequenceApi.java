package com.bbn.marti.sync.api;


import java.io.IOException;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.TakException;

/*
 *
 * REST API for synchronized sequence number generation
 *
 */
@RestController
public class SequenceApi extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SequenceApi.class);
    private static HashMap<String, Integer> sequenceMap = new HashMap<String, Integer>();
    private static Object lock = new Object();

    @RequestMapping(value = "/sync/sequence/{key}", method = RequestMethod.GET)
    ResponseEntity<String> getNextInSequence(@PathVariable("key") @NotNull String key)
            throws IOException, TakException {
        try {
            Integer next;
            synchronized (lock) {
                next = sequenceMap.get(key);
                if (next == null) {
                    next = 0;
                }

                next++;
                sequenceMap.put(key, next);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<String>(next.toString(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Exception in getNextInSequence!", e);
            throw new TakException();
        }
    }
}
