

package com.bbn.marti.kml.icon.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.xml.bind.JAXBContext;

import com.google.common.base.Strings;

import tak.server.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.Marshaller;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;

import com.bbn.marti.dao.kml.IconsetRepository;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.model.kml.Icon;
import com.bbn.marti.model.kml.Iconset;
import com.bbn.marti.remote.util.SecureXmlParser;

/*
 * Asynchronously process uploaded iconset zipfiles.
 * 
 * 
 */
@Service("iconsetProcessor")
public class IconsetUploadProcessorImpl implements IconsetUploadProcessor {

    // expect filename for iconset xml file, in 
    private static final String ICONSET_FILENAME = "iconset.xml";

    // read buffer size
    private static final int READ_BUFFER_SIZE = 16384;

    private static final String COT_TYPE_UNKNOWN = "a-u";

    // use a concurrent map to lock a particular iconset uid (and associated icons) for update
    private static ConcurrentMap<String, Object> iconsetLockMap = new ConcurrentHashMap<>();

    @Autowired
    private AbstractPlatformTransactionManager transactionManager;

    // Spring Data JPA repository
    @Autowired
    IconsetRepository iconsetRepository;

    // JPA persistence context
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    Marshaller xmlDeserializer;

    private static final Logger logger = LoggerFactory.getLogger(IconsetUploadProcessorImpl.class);
    
    // for audit log
    private String username;
    private String roles;
    private String requestPath;
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
    
    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    // asynchronously process uploaded file
    @Override
    @Async
    public void process(String filename) {

        // This try block just captures any exceptions that would otherwise disappear, since this method runs in a thread from the thread pool and so exception would not propagate back to the caller.
        try {
            
            // initialize the audit log with the current username and roles (optional), so that they will be reflected in the data access audit log
            AuditLogUtil.init(getUsername(), getRoles(), getRequestPath());

            long zipDuration = System.currentTimeMillis();
            long reconcileDuration;
            
            logger.info("processing uploaded iconset zipfile: " + filename);

            ZipInputStream zis = null;
            ZipEntry zipEntry = null;

            try {
                zis = new ZipInputStream(new FileInputStream(filename));
            } catch (IOException e) {
                logger.error("IOException opening zipfile " + filename, e);
            }

            while((zipEntry = zis.getNextEntry()) != null) {
                logger.debug("simple zip entry name: " + zipEntry.getName());
            }

            zis = null;
            zipEntry = null;

            try {
                zis = new ZipInputStream(new FileInputStream(filename));
            } catch (IOException e) {
                logger.error("IOException opening zipfile " + filename, e);
            }

            // maintain a map of Icon objects, including binary data. Populate this list while iterating through the zip file entries. Once this is complete, reconcile this list of icons with the list of icons contained in the iconset.xml file, and persist the resulting final list into the database, along with the iconset metadata.
            List<Icon> zipEntryIconList = new ArrayList<>();

            Iconset iconset = null;

            // traverse the zipfile. Look for iconset.xml and icon files.
            try {
                boolean iconsetFound = false;

                while((zipEntry = zis.getNextEntry()) != null) {
                    try {
                        String zipEntryFilename = zipEntry.getName();
                        logger.debug("zip entry: " + zipEntryFilename + " size: " + zipEntry.getSize());

                        // process only the first iconset file that is encountered in the zipfile 
                        if (!iconsetFound && zipEntryFilename.toLowerCase(Locale.ENGLISH).endsWith(ICONSET_FILENAME)) {
                            iconsetFound = true;
                            logger.debug("iconset xml zip file entry found");

                            byte[] buffer = new byte[READ_BUFFER_SIZE];
                            ByteArrayOutputStream os = new ByteArrayOutputStream();

                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }

                            // no need to close ByteArrayOutputStream
                            long start = System.currentTimeMillis();
                            try {
                                // Marshaller is also an Unmarshaller
                                Document doc = SecureXmlParser.makeDocument(new ByteArrayInputStream(os.toByteArray()));
                                JAXBContext jaxbContext = JAXBContext.newInstance(Iconset.class);
                                jakarta.xml.bind.Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                                iconset = (Iconset) unmarshaller.unmarshal(doc);
                            } catch (Exception e) {
                                logger.error("exception deserializing iconset xml", e);
                            }

                            if (iconset ==  null) {
                                throw new IllegalStateException("null deserialized iconset");
                            }

                            // validate iconset version
                            if (iconset.getVersion() != Constants.ICONSET_CURRENT_VERSION) {
                                throw new IllegalArgumentException("unsupported iconset version in " + ICONSET_FILENAME + ": " + iconset.getVersion());
                            }

                            if (iconset.getIcons() == null) {
                                iconset.setIcons(new ArrayList<Icon>());
                            }

                            if (iconset.getDefaultFriendly() == null) {
                                iconset.setDefaultFriendly("");
                            }

                            if (iconset.getDefaultHostile() == null) {
                                iconset.setDefaultHostile("");
                            }

                            if (iconset.getDefaultUnknown() == null) {
                                iconset.setDefaultUnknown("");
                            }

                            long duration = System.currentTimeMillis() - start;
                            logger.debug("iconset deserialization running time: " + duration + " ms");

                            logger.debug("iconset: " + iconset);

                            // don't consider directories or the iconset.xml file as images
                        } else if (!zipEntryFilename.toLowerCase(Locale.ENGLISH).endsWith(ICONSET_FILENAME) && !zipEntryFilename.endsWith(File.pathSeparator)) {
                            logger.debug("processing image file: " + zipEntryFilename);

                            byte[] buffer = new byte[READ_BUFFER_SIZE];
                            ByteArrayOutputStream os = new ByteArrayOutputStream();

                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }

                            try {
                                // mimetype will be automatically derived from name
                                Icon icon = new Icon(os.toByteArray(), COT_TYPE_UNKNOWN, zipEntryFilename);
                                icon.parseNameAndGroup();

                                zipEntryIconList.add(icon);
                                
                            } catch (IllegalArgumentException e) {
                                logger.debug("invalid icon data in iconset zip for file " + zipEntryFilename, e);
                            }
                        }
                    } catch (Throwable t) {
                        logger.warn("exception processing iconset zip file", t);
                    }
                }

                if (!iconsetFound) {
                    throw new IllegalStateException("no iconset.xml in zip file");
                }

                if (iconset == null) {
                    throw new IllegalStateException("invalid iconset");
                }

                zipDuration = System.currentTimeMillis() - zipDuration;

                reconcileDuration = System.currentTimeMillis();
                zis.closeEntry();
                zis.close();

                logger.debug("number of images retrieved from zip file: " + zipEntryIconList.size());

                Map<String, Icon> iconMap = new HashMap<>();

                if (iconset.getIcons() != null) {
                    for (Icon icon : iconset.getIcons()) {
                        iconMap.put(icon.getName(), icon); 
                    }
                }

                logger.trace("iconMap " + iconMap);

                for (Icon zipEntryIcon : zipEntryIconList) {
                    logger.trace("checking zip image: [ group: " + zipEntryIcon.getGroup() + " name: " + zipEntryIcon.getName() + "]");

                    // since the iconset.xml is now done processing, set the uid in each icon.
                    zipEntryIcon.setIconsetUid(iconset.getUid());
                    zipEntryIcon.setIconset(iconset);

                    // if this icon has a corresponding entry in the icon list parsed from the iconset.xml, use the values for iconMap, use that. Otherwise, use the zipEntryIcon object.
                    Icon iconsetIcon = iconMap.get(zipEntryIcon.getName());

                    if (iconsetIcon != null) {

                        // if the icon entry from the iconset.xml file has a CoT type, use that instead of the default.
                        if (!Strings.isNullOrEmpty(iconsetIcon.getType2525b())) {
                            zipEntryIcon.setType2525b(iconsetIcon.getType2525b());
                        }
                    } else {
                        logger.trace("iconset icon entry not found for [ group: " + zipEntryIcon.getGroup() + " name: " + zipEntryIcon.getName() + "]");
                        iconset.getIcons().add(zipEntryIcon);
                    }
                }

                reconcileDuration = System.currentTimeMillis() - reconcileDuration;


                logger.debug("zip file procesing complete.");

                // swap in the list of icons derived from the zip file entries for the parsed list from the iconset.xml file
                iconset.setIcons(zipEntryIconList);

                logger.trace("iconset: " + iconset);

            } catch (IOException e) {
                // wrap checked exceptions in RuntimeExceptions, and allow other RuntimeExceptions to propagate, so that an invalid iconset will not be saved to the database. 

                logger.error("IOException reading zipfile", e);
                throw new RuntimeException(e);
            }

            logger.debug("saving iconset");

            long persistDuration = System.currentTimeMillis();

            // save the iconset, replacing any existing iconset with the same Uid
            insertOrReplace(iconset);

            persistDuration = System.currentTimeMillis() - persistDuration;
            
            logger.info("zip file " + filename + " iconset name " + iconset.getName() + " uid " + iconset.getUid() + " processing complete");

            logger.debug("zip processing duration: " + zipDuration + " ms list reconciliation duration: " + reconcileDuration + " ms persist duration: " + persistDuration + " ms");
        } catch (Throwable t) {
            logger.warn("exception processing iconset zipfile", t);
        }
    }

    private void insertOrReplace(final Iconset iconset) {

        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();

        // set database transaction properties
        definition.setReadOnly(false);

        // always use new transaction
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definition.setTimeout(30);

        // create transaction template
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager, definition);

        // create a lock if there is not already one present. For this operation itself, lock on the class itself
        iconsetLockMap.putIfAbsent(iconset.getUid(), new Object());
     
        // allow concurrent execution except when the same iconset uid is being updated
        synchronized(iconsetLockMap.get(iconset.getUid())) {
            transactionTemplate.execute(new TransactionCallback<Iconset>() {
                // delete if necessary, then insert
                public Iconset doInTransaction(TransactionStatus status) {
                    if (Strings.isNullOrEmpty(iconset.getUid())) {
                        throw new IllegalArgumentException("empty uid");
                    }

                    try {

                        Iconset savedIconset = iconsetRepository.findByUid(iconset.getUid());

                        if (savedIconset != null) {
                            iconsetRepository.deleteByUid(iconset.getUid());
                            entityManager.flush();
                        }

                        entityManager.persist(iconset);

                    } catch (Throwable t) {
                        logger.error("exception inserting or deleting iconset", t);
                    }

                    return iconset;
                }
            });
        }
    }
}
