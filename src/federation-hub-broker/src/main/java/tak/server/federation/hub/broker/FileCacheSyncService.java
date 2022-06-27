package tak.server.federation.hub.broker;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import mil.af.rl.rol.value.ResourceDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

/*
 *
 * Sync service that stores file resources on disk. They are tracked
 * by hash, including a metadata object (ResourceDetails), stored in an in-memory map
 *
 */
public class FileCacheSyncService implements SyncService {

    private static final Logger logger = LoggerFactory.getLogger(FileCacheSyncService.class);

    private Map<String, ResourceDetails> resourceMap = new ConcurrentHashMap<>();

    public FileCacheSyncService() { }

    @Override
    public void save(byte[] data, ResourceDetails details) {

        Objects.requireNonNull(details, "resource details");

        if (Strings.isNullOrEmpty(details.getSha256())) {
            throw new IllegalArgumentException("empty resource hash");
        }

        String filepath = saveAsTempFile(data, details.getSha256());

        details.setLocalPath(filepath);
        details.setTsStored(new Date());
        details.setSize(data.length);

        // TODO: can calculate hash here

        resourceMap.put(details.getSha256(), details);

        logger.debug("file cache resource saved: " + details + " - size: " + data.length + " total files cached: " + resourceMap.size());
    }

    @Override
    public SyncResultStream retrieve(String hash) {

        SyncResultStream result = new SyncResultStream();

        ResourceDetails details = resourceMap.get(hash);

        if (details == null) {

            ResourceDetails retrieved = resourceMap.remove(hash);

            if (retrieved != null) {
                logger.error("resource with hash " + hash + " does not exist on disk. Probably deleted by temp directory cleanup."); 
            }

            throw new RuntimeException("resource for hash " + hash + " not found");
        }

        if (Strings.isNullOrEmpty(details.getLocalPath())) {
            throw new IllegalArgumentException("empty local path in resource details object " + details);
        }

        Path path = FileSystems.getDefault().getPath(details.getLocalPath());

        try {
            byte[] bytes = ByteStreams.toByteArray(new BufferedInputStream(Files.newInputStream(path)));

            result.setInputStream(new ByteArrayInputStream(bytes));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public SyncResultBytes retrieveBytes(String hash) {
        SyncResultBytes result = new SyncResultBytes();
        ResourceDetails details = resourceMap.get(hash);
        if (details == null) {
            ResourceDetails retrieved = resourceMap.remove(hash);
            if (retrieved != null) {
                logger.error("resource with hash " + hash + " does not exist on disk. Probably deleted by temp directory cleanup.");
            }
            throw new RuntimeException("resource for hash " + hash + " not found");
        }

        if (Strings.isNullOrEmpty(details.getLocalPath())) {
            throw new IllegalArgumentException("empty local path in resource details object " + details);
        }

        Path path = FileSystems.getDefault().getPath(details.getLocalPath());

        try {
            byte[] bytes = ByteStreams.toByteArray(new BufferedInputStream(Files.newInputStream(path)));
            result.setBytes(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Override
    public void delete(String hash) {
        throw new UnsupportedOperationException("not implemented");
    }

    private String saveAsTempFile(byte[] data, String hash) {

        try {

            Objects.requireNonNull(data, "resource bytes");

            if (Strings.isNullOrEmpty(hash)) {
                throw new IllegalArgumentException("empty resource hash");
            }

            if (data.length == 0) {
                throw new IllegalArgumentException("empty resource data array");
            }

            Path filePath = Files.createTempFile(hash, "");
            File file = filePath.toFile();
            Files.write(filePath, data);
            file.deleteOnExit();
            // mark for deletion on JVM exit. OS temp file functions should clean it up also at some future time,
            // since it will be created in a temp directory by the JVM/OS

            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("error saving file", e);
        }
    }
}
