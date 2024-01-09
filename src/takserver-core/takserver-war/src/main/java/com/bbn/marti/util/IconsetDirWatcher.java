

package com.bbn.marti.util;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import com.bbn.marti.kml.icon.service.IconsetUploadProcessor;

/*
 * 
 * Watch a directory for create or update file events, and send those files to the iconset processor to be extracted and loaded. The directory-watching event loop runs in its own thread, and will hold onto
 * this thread for the lifetime of the app.
 * 
 */
public class IconsetDirWatcher implements DirWatcher, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(IconsetDirWatcher.class);

    @Value("${takserver.iconsets.dir}")
    private String dir;

    private Path dirPath;

    private WatchService watcher;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor executor;

    private AtomicBoolean terminateFlag;

    @Autowired
    private IconsetUploadProcessor iconsetProcessor;

    
    public IconsetDirWatcher() { }

    /*
     * initialize directory watching
     */
    @EventListener({ContextRefreshedEvent.class})
    public void init() {

        try {

        	if (logger.isTraceEnabled()) {
        		logger.trace("in init - dir: " + dir);
        	}

            watcher = FileSystems.getDefault().newWatchService();

            // assert that path is set
            Assert.hasLength(dir);

            // make sure there's an executor   
            Assert.notNull(executor);

            // and an iconsetProcessor
            Assert.notNull(iconsetProcessor);

            terminateFlag = new AtomicBoolean();

            dirPath = FileSystems.getDefault().getPath(dir);

            // listen for create and modify events
            dirPath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);

            // do file system event processing loop in a separate thread
            executor.execute(this);

        } catch (Throwable e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("exception starting directory watching for Iconsets", e);
        	}
        }
    }

    @PreDestroy
    public void destroy() {
        logger.trace("setting termination flag for directory watching event loop");
        terminateFlag.set(true);
    }

    @Override
    public void run() {
        // start watching the directory
        watch();
    }

    // encapsulate directory watching loop - execute. This will spend most of its time blocking waiting for activity in the directory.
    @SuppressWarnings("finally")
    private void watch() {
        while (!terminateFlag.get()) {
            try {
                // wait for key to be signaled
                WatchKey key;
                try {
                    key = watcher.poll(1000, TimeUnit.MILLISECONDS);
                    if (key == null) {
                        continue;
                    }

                } catch (InterruptedException e) {
                    continue;
                }

                logger.trace("received directory watch event");


                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    logger.trace("watch event kind: " + kind);

                    // This key is registered only for ENTRY_CREATE and ENTRY_MODIFY events, but an OVERFLOW event can
                    // occur regardless if events are lost or discarded.
                    if (kind == OVERFLOW) {
                        logger.warn("directory watching event overflow - Iconset upload might not have been detected");
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path eventContext = null;

                    try {
                        eventContext = ev.context();
                        if (eventContext == null) {
                            continue;
                        }

                    } catch (Exception e) {
                        logger.debug("exception getting file system event context", e);
                        continue;
                    }

                    try {

                        Path child = dirPath.resolve(eventContext);

                        if (child != null) {

                            logger.trace("child path: " + child);

                            String filename = child.toAbsolutePath().toString();

                            logger.trace("created or updated filename: " + filename);

                            // set username and roles for audit log
                            iconsetProcessor.setUsername("local tomcat");
                            iconsetProcessor.setRoles("");
                            iconsetProcessor.setRequestPath("local file " + filename);

                            // process the uploaded file. 
                            iconsetProcessor.process(filename);

                        } else {
                            logger.trace("null child path");
                        }
                    } catch (Exception e) {
                        logger.error("exception processing iconset file", e);
                        continue;
                    }
                }

                // Reset the key -- required in order to keep listening to events
                boolean valid = key.reset();
                if (!valid) {
                    logger.error("Iconset zip upload directory " + dir + " is no longer accessible - terminating directory watching");
                    break;
                }

            } catch (Throwable e) {
                logger.warn("Exception processing watched filesystem event", e);
            } finally {
                continue; // this loop needs to busy-wait / poll for an event, so don't let it exit even if something unexpected happens in the current loop
            }
        }
    } 
}