

package com.bbn.marti.kml.icon.api;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.dao.kml.IconRepository;
import com.bbn.marti.dao.kml.IconsetRepository;
import com.bbn.marti.kml.icon.service.IconsetUploadProcessor;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.model.kml.Icon;
import com.bbn.marti.model.kml.Icon.IconParts;
import com.bbn.marti.model.kml.Iconset;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.service.kml.IconStrategy;
import com.bbn.marti.util.KmlUtils;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

import tak.server.Constants;
import tak.server.cot.CotElement;

/*
 * 
 * API to manage icon sets
 * 
 * base path is /Marti/api/icon and /Marti/api/iconset
 * 
 */

@RestController
public class IconsetIconApi extends BaseRestController {

    Logger logger = LoggerFactory.getLogger(IconsetIconApi.class);

    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    IconsetUploadProcessor uploadProcessor;
    
    @Autowired
    IconRepository iconRepository;
    
    @Autowired
    IconsetRepository iconsetRepository;
    
    @Autowired
    IconStrategy<CotElement> iconStrategy;
    
    @Autowired
    ServletContext servletContext;
    
    // export the icon path so that other components, like the MissionKMLServlet, can identify custom icon urls
    public static final String ICON_API_PATH = "api/icon";

    /*
     * handle POST with a file payload. Save the uploaded file in temporary storage, in the location specified by the servlet container.
     * 
     */
    @RequestMapping(value = "iconset", method = RequestMethod.POST)
    @DateTimeFormat(iso = ISO.DATE)
    public ResponseEntity<ApiResponse<String>> postIconsetZip(@RequestParam("file") MultipartFile file) {

        logger.trace("POST Iconset request");

        if (!file.isEmpty()) {
            try {
                // The multipart file object will be stored in memory or on disk by Apache Commons IO. Store it in a temporary location on disk, provided by the servlet container.
                logger.debug("filename: " + file.getName() + " file content-type: " + file.getContentType());

                // get tempDir location according to J2EE spec
                File tempDir = (File) request.getServletContext().getAttribute(ServletContext.TEMPDIR);

                if (tempDir == null) {
                    throw new IllegalStateException("Unable to detect temporary file location from servlet container");
                }
                
                // initialize the audit log with the current http request
                AuditLogUtil.init(request);
                
                // get the username and role values for this thread and set them in the icon processor
                uploadProcessor.setUsername(AuditLogUtil.getUsername());
                uploadProcessor.setRoles(AuditLogUtil.getRoles());

                logger.debug("tempDir: " + tempDir.getAbsolutePath());

                String uploadedFilename = tempDir.getAbsolutePath() + File.pathSeparator + UUID.randomUUID() + "_" + file.getName();

                logger.debug("tempFilePath: " + uploadedFilename);

                try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(uploadedFilename)))){
                    byte[] bytes = file.getBytes();
                    stream.write(bytes);
                    stream.flush();
                } catch (Exception e) {
                    logger.error("Exception saving posted file to disk.", e);
                    return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), "Error saving file"), HttpStatus.INTERNAL_SERVER_ERROR);
                }
                
                // asynchronously process the uploaded file
                uploadProcessor.process(uploadedFilename);
             
                // success response means that the file was successfully uploaded - not that processing was successful.
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), "You successfully uploaded"), HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), "You failed to upload => " + e.getMessage()), HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<ApiResponse<String>>(new ApiResponse<String>(Constants.API_VERSION, String.class.getName(), "You failed to upload because the file was empty."), HttpStatus.OK);
        }
    }

    @RequestMapping(value = "icon/{uid}/{group}/{name:.+}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getIcon(@PathVariable("uid") String uid, @PathVariable("group") String group, @PathVariable("name") String name) {

        logger.trace("GET Icon request - uid: " + uid + " group: " + group + " name: " + name);
        
        Icon icon = null;
        
        if (Strings.isNullOrEmpty(uid) || Strings.isNullOrEmpty(group) || Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Invalid request: uid: " + uid + " group: " + group + " name: " + name);
        }
        
        try {
            logger.debug("fetching icon by uid: " + uid + " group: " + group + " name: " + name);
            icon = iconRepository.findIconByIconsetUidAndGroupAndName(uid, group, name);
        } catch (Throwable t) {
            logger.error("exception fetching icon for uid: " + uid + " name " + name);
        }
        
        if (icon != null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            
            if (!Strings.isNullOrEmpty(icon.getMimeType())) {
                responseHeaders.add("Content-Type", icon.getMimeType());
            } else {
                responseHeaders.add("Content-Type", "image/png");
            }
            
            
            return new ResponseEntity<byte[]>(icon.getBytes(), responseHeaders, HttpStatus.OK);
        }
        
        return new ResponseEntity<byte[]>("".getBytes(), HttpStatus.NOT_FOUND);
    }
    
    // get all icon urls for an iconset. Mostly for testing.
    @RequestMapping(value = "iconseturl/{uid}", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<List<String>>> getAllIconUrlsForIconset(@PathVariable("uid") String uid) {

        logger.trace("GET all icon urls for iconset with uid: " + uid);

        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("Invalid request: uid: ");
        }

        Iconset iconset = null;

        try {
            logger.debug("fetching iconset by uid: " + uid);

            iconset = iconsetRepository.findByUid(uid);

        } catch (Throwable t) {
            logger.error("exception fetching icon for uid: " + uid);
        }

        if (iconset == null) { 
            throw new NotFoundException("iconset " + uid + " not found");
        }


        // get list of icon urls from iconset
        String baseUrl = request.getRequestURL().toString();

        baseUrl = baseUrl.replaceFirst("iconseturl", "icon");

        List<String> iconUrls = new ArrayList<>();

        if (iconset.getIcons() != null) {
            for (Icon icon : iconset.getIcons()) {
                String url;
                try {
                    url = KmlUtils.getBaseUrl(request) + "/" + ICON_API_PATH + "/" + icon.getIconsetUid() + "/" + icon.getGroup() + "/" + icon.getName();
                    iconUrls.add(url);
                } catch (ServletException | IOException e) {
                    logger.debug("exception getting servlet base url");
                }
            }
        }

        return new ResponseEntity<ApiResponse<List<String>>>(new ApiResponse<List<String>>(Constants.API_VERSION, String.class.getName(), iconUrls), HttpStatus.OK);
    }
    
    //  get uids of all installed iconsets
    @RequestMapping(value = "iconset/all/uid", method = RequestMethod.GET)
    public ApiResponse<Set<String>> getAllIconsetUids() {

        logger.trace("GET all installed iconset uids");
      
        return new ApiResponse<>(Constants.API_VERSION, String.class.getName(), iconsetRepository.getAllUids());
    }
    
     public static enum IconsetResult {
        SUCCESS, ERROR
    }

     /*
      * Determine a URL for icon based on information that is expected to be found in the CoT event.
      */
     @RequestMapping(value = "iconurl", method = RequestMethod.GET)
     public ResponseEntity<String> getIconUrl(
             @RequestParam(value = "iconsetpath", required = false) String iconsetPath,
             @RequestParam(value = "cotType", required = false) String cotType,
             @RequestParam(value = "medevac", required = false) boolean medevac,
             @RequestParam(value = "groupName", required = false) String groupName,
             @RequestParam(value = "role", required = false) String role,
             @RequestParam(value = "color", required = false) Long color, // Color is a parameter here, because it can be set by ATAK and is part of the custom icons standard, but is currently ignored by Marti.
             @RequestParam(value = "relative", required = false) boolean relative,
             HttpServletRequest request) {

         

         String url = "";

         if (!Strings.isNullOrEmpty(iconsetPath) && (!iconsetPath.startsWith("COT_MAPPING_2525B"))) {
             // if the iconset path is there, use that as the icon url.
             IconParts parts = Icon.parseIconPath(iconsetPath);

             url = ICON_API_PATH + "/" + parts.iconsetUid + "/" + (Strings.isNullOrEmpty(parts.group) ? "" : parts.group + "/") + parts.name;
         } else { // otherwise, use medevac, group, or cotType
             url = KmlUtils.get2525BIconUrl(cotType, medevac, groupName);
         }

         logger.debug("icon url: " + url);

         // exception, and respond with 400 Bad Request if we have not come up with a non-empty url at this point

         if (Strings.isNullOrEmpty(url)) {
             throw new IllegalArgumentException("not enough information to construct an image url");
         }

         if (!relative) {
             try {
                 url = KmlUtils.getBaseUrl(request) + "/" + url;
             } catch (ServletException | IOException e) {
                 logger.debug("Exception getting servlet request base url", e);
             }
         }

         // success response
         HttpHeaders responseHeaders = new HttpHeaders();
         responseHeaders.add("Content-Type", "text/plain");
         return new ResponseEntity<String>(url, responseHeaders, HttpStatus.OK);
     }
     
     // Get icon image
     @RequestMapping(value = "iconimage", method = RequestMethod.GET)
     public ResponseEntity<byte[]> getIconImage(
             @RequestParam(value = "iconsetpath", required = false) String iconsetPath,
             @RequestParam(value = "cotType", required = false) String cotType,
             @RequestParam(value = "medevac", required = false) boolean medevac,
             @RequestParam(value = "groupName", required = false) String groupName,
             @RequestParam(value = "role", required = false) String role,
             @RequestParam(value = "color", required = false) Long color,
             @RequestParam(value = "relative", required = false) boolean relative) {

         // Note: color is a parameter here, because it can be set by ATAK and is part of the custom icons standard, but is currently ignored by Marti.

         logger.debug("GET iconimage iconsetpath: " + iconsetPath + " cotType: " + cotType + " medevac: " + medevac + " groupName: " + groupName + " role: " + role + " color: " + color + " relative: " + relative);

         String url = "";
         
         HttpHeaders responseHeaders = new HttpHeaders();
         responseHeaders.add("Content-Type", "image/png");

         // custom icon case
         if (!Strings.isNullOrEmpty(iconsetPath)) {
             // if the iconset path is there, use that as the icon url.
             IconParts parts = Icon.parseIconPath(iconsetPath);

             Icon icon = null;

             try {
                 logger.debug("fetching icon by uid: " + parts.iconsetUid + " group: " + parts.group + " name: " + parts.name);
                 icon = iconRepository.findIconByIconsetUidAndGroupAndName(parts.iconsetUid, parts.group, parts.name);
             } catch (Throwable t) {
                 logger.error("exception fetching icon for uid: " + parts.iconsetUid + " group: " + parts.group + " name: " + parts.name);
             }

             if (icon != null) {
                 if (!Strings.isNullOrEmpty(icon.getMimeType())) {
                     responseHeaders.remove("Content-Type");
                     responseHeaders.add("Content-Type", icon.getMimeType());
                 } else {
                     
                     // default to PNG if the mime type is not stored
                     
                     responseHeaders.remove("Content-Type");
                     responseHeaders.add("Content-Type", "image/png");
                 }
                 
                 return new ResponseEntity<byte[]>(icon.getBytes(), responseHeaders, HttpStatus.OK);
             } 

         // local icon file case
         } else {
             url = KmlUtils.get2525BIconUrl(cotType, medevac, groupName);
             
             if (Strings.isNullOrEmpty(url)) {
                 throw new NotFoundException("no valid icon type found");
             }
             
             // hack image content type
             if (!url.toLowerCase().contains(".png")) {
                 responseHeaders.remove("Content-Type");
                 responseHeaders.add("Content-Type", "image/jpeg");
             }

             try (InputStream is = servletContext.getResourceAsStream(url)){

                 byte[] image = ByteStreams.toByteArray(is);
                 
                 return new ResponseEntity<byte[]>(image, responseHeaders, HttpStatus.OK);
             } catch (Exception e) {
                 throw new NotFoundException("exception getting local servlet file icon resource as stream", e);
             }
         }
  
         throw new NotFoundException();
     }
}


