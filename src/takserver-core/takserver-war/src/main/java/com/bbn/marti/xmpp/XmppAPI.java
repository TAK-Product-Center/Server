package com.bbn.marti.xmpp;

import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.jivesoftware.whack.ExternalComponentManager;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Xmpp;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;

import nl.goodbytes.xmpp.xep0363.Component;
import nl.goodbytes.xmpp.xep0363.Slot;
import nl.goodbytes.xmpp.xep0363.SlotManager;

@RestController
public class XmppAPI extends BaseRestController {

    private final Validator validator = new MartiValidator();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.marti.xmpp.XmppAPI.class);
    private final String XMPP_TOOL = "xmpp";
    private static final AtomicBoolean lock = new AtomicBoolean();

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private EnterpriseSyncService syncStore;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {

        synchronized (lock) {
            if (lock.get()) {
                return;
            }
            lock.set(true);

            Thread initThread = new Thread(new Runnable() {
                public void run() {
                    Xmpp xmppConfig;
                    try {
                        xmppConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getXmpp();
                    } catch (Exception re) {
                        logger.error("unable to obtain xmpp config: " + re.getMessage());
                        return;
                    }

                    if (xmppConfig == null) {
                        return;
                    }

                    int retry = 0;
                    boolean connected = false;
                    while (!connected) {
                        try {
                            logger.info("Registering external component");

                            ExternalComponentManager manager = new ExternalComponentManager(
                                    xmppConfig.getXmppHost(), xmppConfig.getXmppPort());

                            String domain = "upload";

                            manager.setSecretKey(domain, xmppConfig.getXmppSharedSecret());

                            final URL endpoint = new URL(
                                    "https", xmppConfig.getTakServerHost(), xmppConfig.getTakServerPort(),
                                    "/Marti/api/xmpp/transfer");

                            final Component component = new Component(domain, endpoint);

                            manager.addComponent(domain, component);

                            connected = true;
                            logger.error("Component registration succeeded");

                        } catch (Exception ce) {
                            logger.error("Component registration failed: " + ce.getMessage());

                            if (retry < xmppConfig.getXmppComponentRetryCount()) {
                                retry++;
                                int delay = xmppConfig.getXmppComponentRetryDelay();
                                try {
                                    logger.error("Retrying in " + delay + " seconds");
                                    Thread.sleep(delay * 1000);
                                } catch (InterruptedException ie) {
                                    logger.error("Retry delay interrupted: " + ie.getMessage());
                                }
                                continue;
                            } else {
                                break;
                            }
                        }
                    }
                }
            });
            initThread.start();
        }
    }

    @RequestMapping(value = "/xmpp/transfer/{uid}/{filename}", method = RequestMethod.GET)
    ResponseEntity getFile(
            @PathVariable("uid") String uid,
            @PathVariable("filename") String filename,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String groupVector = martiUtil.getGroupBitVector(request);

            // validate inputs
            validator.getValidInput(XMPP_TOOL, uid,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
            validator.getValidInput(XMPP_TOOL, filename,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);

            List<Metadata> metadataList = syncStore.getMetadataByUidAndTool(uid, XMPP_TOOL, groupVector);
            if (metadataList == null || metadataList.size() == 0) {
                logger.error("Unable to find metadata for xmpp transfer slot uid in enterprise sync!: " + uid);
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
            Metadata metadata = metadataList.get(0);

            byte[] contents = syncStore.getContentByUidAndTool(uid, XMPP_TOOL, groupVector);
            if (contents == null) {
                logger.error("Unable to find content for xmpp transfer slot uid in enterprise sync!: " + uid);
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }

            final String eTagRequest = request.getHeader( "If-None-Match" );
            if (eTagRequest != null && eTagRequest.equals(metadata.getHash())) {
                return new ResponseEntity(HttpStatus.NOT_MODIFIED);
            }

            String mimeType = metadata.getFirst(Metadata.Field.MIMEType);
            mimeType = validator.getValidInput("MIME Type", mimeType, "MartiSafeString", MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
            response.setContentType(mimeType);

            String sizeStrVal = String.valueOf(metadata.getSize());
            sizeStrVal = validator.getValidInput("Content length", sizeStrVal, "NonNegativeInteger", MartiValidatorConstants.LONG_STRING_CHARS, true);
            response.setContentLength(Integer.parseInt(sizeStrVal));

            response.setHeader( "Cache-Control", "max-age=31536000" );

            String hash = metadata.getHash();
            hash = validator.getValidInput("Header Etag", hash, "Hexidecimal", MartiValidatorConstants.LONG_STRING_CHARS, false);
            response.setHeader( "ETag", hash);

            response.getOutputStream().write(contents);
            return new ResponseEntity(HttpStatus.OK);

        } catch (ValidationException e) {
            logger.error("ValidationException in putFile : " + e.getMessage());
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception in getFile : " + e.getMessage());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String trimSemiColon(String contentType) {
        if (contentType.charAt(contentType.length()-1) == ';') {
            contentType = contentType.substring(0, contentType.length()-1);
        }
        return contentType;
    }

    @RequestMapping(value = "/xmpp/transfer/{uid}/{filename}", method = RequestMethod.PUT)
    ResponseEntity putFile(
            @PathVariable("uid") String uid,
            @PathVariable("filename") String filename,
            @RequestBody byte[] contents,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // validate inputs
            validator.getValidInput(XMPP_TOOL, uid,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
            validator.getValidInput(XMPP_TOOL, filename,
                    MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);

            final Slot slot = SlotManager.getInstance().consumeSlotForPut(UUID.fromString(uid));
            if (slot == null) {
                String message = "The requested slot " + uid + " " +
                        "is not available. Either it does not exist, or has already been used.";
                logger.info(message);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
            }

            if (request.getContentLength() != slot.getSize()) {
                String message = "Content length in request " + request.getContentLength() +
                        " does not correspond with slot size " + slot.getSize();
                logger.info(message);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
            }

            String contentType = request.getHeader("content-type");
            if (contentType != null) {
                contentType = trimSemiColon(contentType.toLowerCase());
                if (slot.getContentType() != null) {
                    String slotContentType = trimSemiColon(slot.getContentType());
                    if (!slotContentType.equalsIgnoreCase(contentType)) {
                        String message = "Content type in request " + contentType +
                                " does not correspond with slot content type " + slotContentType;
                        message = validator.getValidInput(XMPP_TOOL, message,
                                MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
                        logger.info(message); // log forging prevention
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
                    }
                }
            }

            //
            // build up the metadata for adding to enterprise sync
            //
            Metadata toStore = new Metadata();
            toStore.set(Metadata.Field.Keywords, XMPP_TOOL);
            toStore.set(Metadata.Field.Tool, XMPP_TOOL);
            toStore.set(Metadata.Field.DownloadPath, filename);
            toStore.set(Metadata.Field.Name, filename);
            toStore.set(Metadata.Field.MIMEType, contentType);
            toStore.set(Metadata.Field.UID, new String[]{uid});

            if (slot.getCreator() != null) {
                toStore.set(Metadata.Field.SubmissionUser, slot.getCreator().toString());
            }

            //
            // add mission package to enterprise sync
            //
            Metadata metadata = syncStore.insertResource(toStore, contents, martiUtil.getGroupBitVector(request));

            String url = request.getRequestURL().toString();
            url = validator.getValidInput("Location", url, "MartiSafeString", MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
            response.setHeader("Location", url);
            return new ResponseEntity(HttpStatus.CREATED);

        } catch (ValidationException e) {
            logger.error("ValidationException in putFile : " + e.getMessage());
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception in putFile : " + e.getMessage());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
