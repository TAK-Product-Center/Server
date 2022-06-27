package tak.server.federation.hub.broker;

import mil.af.rl.rol.value.ResourceDetails;

/*
 *
 * Interface for service that saves and retrieves files and metadata
 *
 */
public interface SyncService {

    /*
     * create or update resource
     *
     */
    void save(byte[] data, ResourceDetails details);

    /*
     * retrieve a file
     *
     */
    SyncResultStream retrieve(String hash);

    /*
     * retrieve a file
     *
     */
    SyncResultBytes retrieveBytes(String hash);

    /*
     * delete by file hash
     *
     */
    void delete(String hash);
}
