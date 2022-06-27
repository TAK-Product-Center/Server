

package com.bbn.marti.groups;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.bbn.marti.exceptions.DuplicateException;
import com.bbn.marti.remote.exception.*;
import com.google.common.base.Strings;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomExceptionHandler {

    Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);
 
    // Respond to RMI connection exception with 404 not found 
    @ExceptionHandler({RemoteLookupFailureException.class})
    public ResponseEntity<ErrorResponse> handleRMIExceptionRequest(Exception e) {
        HttpHeaders headers = new HttpHeaders();

        HttpStatus status = HttpStatus.NOT_FOUND;

        // return a 404 status with a JSON response containing the error message
        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 0L, "RMI Service Not Found"), headers, status);
    }

    // General "Not Found" handler 
    @ExceptionHandler({NotFoundException.class, EmptyResultDataAccessException.class})
    public ResponseEntity<ErrorResponse> notFound(Exception e) {
        HttpHeaders headers = new HttpHeaders();

        HttpStatus status = HttpStatus.NOT_FOUND;

        // return a 404 status with a JSON response containing the error message
        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 1L, "Not Found" + (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }

    // Respond to an invalid request with a 400 error 
    @ExceptionHandler({DuplicateFederateException.class, IllegalArgumentException.class, NumberFormatException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Bad request handler handling exception ", e);

        HttpStatus status = HttpStatus.BAD_REQUEST;

        // return a 400 status with a JSON response containing the error message
        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 2L, "Invalid Request" + (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }
    
    //  Respond to a JSON serialization error with a 501 Not Implemented 
    @ExceptionHandler({HttpMessageNotWritableException.class})
    public ResponseEntity<ErrorResponse> handleJsonException(Exception e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("JSON Serialization Exception", e);

        HttpStatus status = HttpStatus.NOT_IMPLEMENTED;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 3L, "JSON serialization error"), headers, status);
    }
    
    //  This exception occurs if the JSON body cannot be deserialized 
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleMissingJsonBodyException(Exception e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Invalid Request Body Exception " + e.getMessage(), e);

        HttpStatus status = HttpStatus.BAD_REQUEST;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 2L, "Invalid Request Body"), headers, status);
    }
    
    //  Respond to a application or data-level duplicate exception with 409 Conflict
    @ExceptionHandler({DuplicateException.class})
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("DuplicateException", e);

        HttpStatus status = HttpStatus.CONFLICT;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 4L, "Duplicate Exception " + (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }
    
    //  Respond to a validation exception with a 400 Bad Request
    @ExceptionHandler({ValidationException.class})
    public ResponseEntity<ErrorResponse> handleDuplicateException(ValidationException e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Validation Exception", e);

        HttpStatus status = HttpStatus.BAD_REQUEST;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 5L, (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }
    
    //  For a generic TakException, just return a 500 Internal Server Error response, and don't include any detail in the JSON response
    @ExceptionHandler({TakException.class, NullPointerException.class})
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Exception", e);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 6L, ""), headers, status);
    }
    
    //  Respond to a validation exception with a 304 Not Modified. The client should wait some time and retry.
    @ExceptionHandler({RetryableException.class})
    public ResponseEntity<ErrorResponse> handleDuplicateException(RetryableException e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Retryable Exception", e);

        HttpStatus status = HttpStatus.NOT_MODIFIED;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 7L, (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }

    //  Respond to a validation exception with a 304 Not Modified. The client should wait some time and retry.
    @ExceptionHandler({MissionDeletedException.class})
    public ResponseEntity<ErrorResponse> handleMissionDeletedException(MissionDeletedException e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Mission Deleted Exception", e);

        HttpStatus status = HttpStatus.GONE;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 8L, (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }


    //  Respond to a unauthorized exception with a 401 Unauthorized.
    @ExceptionHandler({UnauthorizedException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        HttpHeaders headers = new HttpHeaders();

        logger.debug("Unauthorized Exception", e);

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 9L, (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }

    //  Respond to a unauthorized exception with a 401 Unauthorized.
    @ExceptionHandler({ForbiddenException.class})
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
        HttpHeaders headers = new HttpHeaders();

        logger.error("Forbidden Exception : " + e.getMessage(), e);

        HttpStatus status = HttpStatus.FORBIDDEN;

        return new ResponseEntity<ErrorResponse>(new ErrorResponse(status, 10L, (!Strings.isNullOrEmpty(e.getMessage()) ? ": " + e.getMessage() : "")), headers, status);
    }
}