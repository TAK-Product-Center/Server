

package com.bbn.marti.sync;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.naming.NamingException;

import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.postgresql.util.PGobject;

/**
 * Interface for Enterprise Sync.
 * Supports creation, retrieval, update, and deletion of Enterprise Sync resources (files).
 *
 */
public interface EnterpriseSyncService {

	/**
     * Deletes stored resources from the Enterprise Sync database by hash
     * @throws SQLException
     * @throws NamingException
     */
	void delete(String hash, String groupVector) throws SQLException, NamingException;

	/**
     * Deletes stored resources from the Enterprise Sync database
     * @param primaryKeys List of primary keys to delete
     * @throws SQLException
     * @throws NamingException
     */
    void delete(List<Integer> primaryKeys, String groupVector) throws SQLException, NamingException;

	/**
	* Inserts the file source from a byte array into the database, with the given metadata mapped to columns.
	*
	*/
	Metadata insertResource(Metadata metadata, byte[] content, String groupVector)
			throws SQLException, NamingException, IllegalArgumentException,
			ValidationException, IntrusionException, IllegalStateException, IOException;

	/**
	* Writes the file sourced from an InputStream into the database, with the given metadata mapped to columns.
	*
	*/
	Metadata insertResourceStream(Metadata metadata, InputStream contentStream, String groupVector)
			throws SQLException, NamingException, IllegalArgumentException,
			ValidationException, IntrusionException, IllegalStateException, IOException;

	Metadata insertResourceStreamUID(Metadata metadata, InputStream contentStream, String groupVector, boolean generateUID)
			throws SQLException, NamingException, IllegalArgumentException, ValidationException, IntrusionException,
			IllegalStateException, IOException;

	/**
	 * Gets all Metadata in the database
	 * @return
	 * @throws ValidationException
	 * @throws IntrusionException
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ParseException
	 */
	 Map<String, List<Metadata>> getAllMetadata(String groupVector) throws ValidationException, IntrusionException, SQLException, NamingException, ParseException;

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param uid UID of the object to retrieve
	 * @return the content of the latest stored object matching that UID, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	 byte[] getContentByUid(String uid, String groupVector) throws SQLException, NamingException;

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param uid UID of the object to retrieve
	 * @param maxTimeMillis max time search for files submittted prior to the max time
	 * @return the content of the latest stored object matching that UID, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	 byte[] getContentByUidAndMaxTime(String uid, long maxTimeMillis, String groupVector) throws SQLException, NamingException;

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param uid UID of the object to retrieve
	 * @return the content of the latest stored object matching that UID, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	 byte[] getContentByUidAndTool(String uid, String tool, String groupVector) throws SQLException, NamingException;

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param hash Hash of the object to retrieve
	 * @return the content of the latest stored object matching that hash, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	 byte[] getContentByHash(String hash, String groupVector) throws SQLException, NamingException;

	/**
	 * Gets the content of an Enterprise Sync object. This searches the full resource table vs the latest resource
	 * view. Latest resource is used to back the getContentByHash function and will only return a match for a hash
	 * if it's the most recent entry for a given uid. This function allows searching the history for a given uid.
	 * @param hash Hash of the object to retrieve
	 * @return the content of the latest stored object matching that hash, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	 byte[] getContentByOldHash(String hash, String groupVector) throws SQLException, NamingException;

	/**
	 * Gets all Metadata instances containing the given <code>uid</code>
	 * @param uid
	 * @return a List of all Metadata containing that uid; the list will be empty if the uid is not in the database
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	 List<Metadata> getMetadataByUid(String uid, String groupVector) throws SQLException, NamingException, ValidationException;

	/**
	 * Gets all Metadata instances containing the given <code>uid</code>
	 * @param uid
	 * @return a List of all Metadata containing that uid; the list will be empty if the uid is not in the database
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	 List<Metadata> getMetadataByUidAndTool(String uid, String tool, String groupVector) throws SQLException, NamingException, ValidationException;

	/**
     * Gets the metadata for a specific database record identified by its primary key.
     * @param primaryKey
     * @return the Metadata for the requested file object, or <code>null</code> if the primary key was invalid.
     * @throws SQLException
     * @throws NamingException
     * @throws ValidationException
     */
     Metadata getMetadata(Integer primaryKey, String groupVector) throws SQLException, NamingException, ValidationException;

	/**
	 * Gets all Metadata instances containing the given <code>hash</code>
	 * @param hash
	 * @return a List of all Metadata containing that hash; the list will be empty if the hash is not in the database
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	 List<Metadata> getMetadataByHash(String hash, String groupVector) throws SQLException, NamingException, ValidationException;

	/**
	 * Searches the database for all metadata matching the specified constraints
	 * and returns a sorted <code>Map</code>. All search constraints accept
	 * <code>null</code>; <code>null</code> means "used the default constraint."
	 *
	 * {@literal <}strong{@literal >}Security advisory:{@literal <}strong{@literal >} This method does input validation but
	 * no output validation. The caller is responsible for testing the output to
	 * determine whether it is safe for the caller's purposes. This class
	 * generally makes an honest effort to validate all database input, but it
	 * cannot guarantee the database hasn't been tampered with outside its
	 * scope. So rather than give the illusion of security, this method
	 * delegates output validation to the application layer that called it.
	 *
	 * The returned value is not thread-safe.
	 *
	 * @param minimumAltitude
	 *            minimum altitude, default <code>MIN_DOUBLE</code>. Can be <code>null</code>.
	 * @param maximumAltitude
	 *            maximum altitude, default <code>MAX_DOUBLE</code>. Can be <code>null</code>.
	 * @param stringParameters
	 *            Map of parameter-regex pairs, where the parameter is the
	 *            metadata field to match and the regex is the expression it
	 *            must satisfy. Can be <code>null</code>.
	 * @param spatialConstraints
	 *            a PGobject a circular or rectangular regions. If
	 *            <code>null</code> any metadata with or without a geolocation will be
	 *            matched.
	 * @param minimumTime
	 *            minimum upload time, default is no minimum time. Can be <code>null</code>.
	 * @param maximumTime
	 *            maximum upload time, default is no maxiumum time. Can be <code>null</code>.
	 * @param latestOnly
	 *            if <code>true</code>, only the latest metadata instance for
	 *            each uid will be returned. Can be <code>null</code>; default is <code>false</code>
	 * @return A sorted Map whose keys are the UID and whose values are lists of
	 *         Metadata instances sorted in ascending time order.
	 * @throws SQLException
	 *             if a database read error occurs
	 * @throws NamingException
	 *             if a database connection error occurs
	 * @throws IntrusionException
	 * @throws ValidationException
	 * @throws ParseException
	 * @throws IllegalArgumentException
	 *             if spatial constraint is not a supported geospatial type, or
	 *             input is otherwise contradictory or unparseable
	 *
	 */
	 SortedMap<String, List<Metadata>> search(Double minimumAltitude,
			Double maximumAltitude,
			Map<Metadata.Field, String> stringParameters,
			PGobject spatialConstraints, Timestamp minimumTime,
			Timestamp maximumTime, Boolean latestOnly, String missionName, String tool, String groupVector) throws SQLException,
			NamingException, ValidationException, IntrusionException,
			ParseException;

	 boolean updateMetadata(String hash, String metadataField, String metadataValue, String groupVector)
			throws SQLException, NamingException, ValidationException;

	 boolean updateExpiration(String hash, Long expiration) throws SQLException, NamingException;

	 boolean updateMetadataKeywords(String hash, List<String> keywords) throws
			SQLException, NamingException, ValidationException;


}
