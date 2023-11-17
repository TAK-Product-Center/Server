
package com.bbn.marti.sync;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGpoint;
import org.postgresql.util.PGobject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.marti.config.Cluster;
import com.bbn.marti.logging.AuditLogUtil;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.service.RetentionPolicyConfig;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.Metadata.Field;
import com.bbn.marti.util.CommonUtil;
import com.google.common.base.Strings;

import tak.server.Constants;

/**
 * Implementation of Enterprise Sync using JDBC to persist files in a database.
 *
 */
public class JDBCEnterpriseSyncService implements EnterpriseSyncService {

	org.slf4j.Logger logger = LoggerFactory.getLogger(JDBCEnterpriseSyncService.class);

	@Autowired
	private JDBCQueryAuditLogHelper wrapper;

	@Autowired
	private DataSource ds;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private CoreConfig coreConfig;

	@Autowired
	private CommonUtil commonUtil;

	private Cluster clusterConfig;

	private boolean esyncEnableCache = false;

	@Autowired(required = false)
	private RetentionPolicyConfig retentionPolicyConfig;

	@EventListener({ContextRefreshedEvent.class})
	public void init() throws RemoteException {
		clusterConfig = coreConfig.getRemoteConfiguration().getCluster();

		// 0 means false, disable esync cache
		if (coreConfig.getRemoteConfiguration().getNetwork().getEsyncEnableCache() == 0) {
			esyncEnableCache = false;
		} else if (coreConfig.getRemoteConfiguration().getNetwork().getEsyncEnableCache() > 0) {
			// positive value means true, enable cache
			esyncEnableCache = true;
		} else {
			// autodetect based on number of processors
			if (Runtime.getRuntime().availableProcessors() > 4) {
				esyncEnableCache = true;
			}
		}
	}
	/**
	 * Columns in the underlying database schema. The names exactly match the
	 * names of the database columns.
	 *
	 *
	 */
	public enum Column {
		altitude(StoredType.decimal),
		data(StoredType.byteArray),
		filename(StoredType.string),
		id(StoredType.integer),
		keywords(StoredType.stringArray),
		location(StoredType.geometry),
		mimetype(StoredType.string),
		name(StoredType.string),
		permissions(StoredType.stringArray),
		remarks(StoredType.string),
		submissiontime(StoredType.timestamp),
		submitter(StoredType.string),
		uid(StoredType.string),
		hash(StoredType.string),
		octet_length(StoredType.integer),
		creatoruid(StoredType.string),
		tool(StoredType.string),
	    expiration(StoredType.integer);

		public final StoredType type;

		private Column(StoredType type) {
			this.type = type;
		}
	}

	/**
	 * Data types used in the underlying SQL schema.
	 *
	 *
	 */
	public enum StoredType {
		byteArray,
		decimal,
		geometry,
		integer,
		string,
		stringArray,
		timestamp
	}

	protected class TypeValuePair {
		public final StoredType type;
		public final Object value;

		protected TypeValuePair(StoredType type, Object value) {
			this.type = type;
			this.value = value;
		}
	}

	/**
	 * java.util.EnumMap stores elements in sorted order. We will use this
	 * property extensively. It's critical to the implementation of this class.
	 */
	public static final EnumMap<Column, Metadata.Field> columnToFieldMap;

	public static final long MAX_DATA_SIZE_BYTES = 1000000000L;	// 1 gigabyte
	public static final long MAXIMUM_TIMESTAMP = 100000000000000l;
	public static final long MINIMUM_TIMESTAMP = -100000000000000l;
	public static final int REGEX_LENGTH_LIMIT = 256;

	private static final String RESOURCE_TABLE = "resource";
	private static final String LATEST_RESOURCE_VIEW = "latestresource";

	private static final List<Column> metadataColumns = Arrays.asList(new Column[] {
			Column.altitude,
			Column.filename,
			Column.id,
			Column.keywords,
			Column.location,
			Column.mimetype,
			Column.name,
			Column.permissions,
			Column.remarks,
			Column.submissiontime,
			Column.submitter,
			Column.uid,
			Column.hash,
			Column.creatoruid,
			Column.tool,
			Column.expiration});

	private static final Logger log = Logger.getLogger(JDBCEnterpriseSyncService.class
			.getCanonicalName());

	/**
	 * Static initializer -- to initialize static class members
	 */
	static {

		columnToFieldMap = new EnumMap<Column, Metadata.Field>(Column.class);
		columnToFieldMap.put(Column.id, Metadata.Field.PrimaryKey);
		columnToFieldMap.put(Column.altitude, Metadata.Field.Altitude);
		columnToFieldMap.put(Column.filename, Metadata.Field.DownloadPath);
		// Column.data is not metadata, so it is omitted from this map
		columnToFieldMap.put(Column.keywords, Metadata.Field.Keywords);
		columnToFieldMap.put(Column.mimetype, Metadata.Field.MIMEType);
		columnToFieldMap.put(Column.name, Metadata.Field.Name);
		// Column.location does not translate directly to a metadata field, so
		// it is omitted from this map
		columnToFieldMap.put(Column.permissions, Metadata.Field.Permissions);
		columnToFieldMap.put(Column.remarks, Metadata.Field.Remarks);
		columnToFieldMap.put(Column.submissiontime,
				Metadata.Field.SubmissionDateTime);
		columnToFieldMap.put(Column.submitter, Metadata.Field.SubmissionUser);
		columnToFieldMap.put(Column.uid, Metadata.Field.UID);
		columnToFieldMap.put(Column.hash, Metadata.Field.Hash);
		columnToFieldMap.put(Column.octet_length, Field.Size);
		columnToFieldMap.put(Column.creatoruid, Field.CreatorUid);
		columnToFieldMap.put(Column.tool, Field.Tool);
		columnToFieldMap.put(Column.expiration, Field.EXPIRATION);

	}

	@Autowired
	private Validator validator;

	public JDBCEnterpriseSyncService() { }

	/**
	 * Deletes stored resources from the Enterprise Sync database by hash
	 * @throws SQLException
	 * @throws NamingException
	 */
	public void delete(String hash, String groupVector) throws SQLException, NamingException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}


		String sql = "DELETE FROM " + RESOURCE_TABLE + " r WHERE " + Column.hash.toString() + "=?"
				+ RemoteUtil.getInstance().getGroupAndClause();  // only allow delete where there is common group membership

		try (Connection connection = ds.getConnection(); PreparedStatement statement = wrapper.prepareStatement(sql, connection)) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Deleting resource hash =" + StringUtils.normalizeSpace(hash));
			}
			statement.setString(1, hash);
			statement.setString(2, groupVector);
			statement.execute();
		}

		if (isCacheEsync()) {
			try {
				Cache cache = cacheManager.getCache(Constants.ENTERPRISE_SYNC_CACHE_NAME);
				if (cache == null) {
					throw new IllegalStateException("unable to get " + Constants.ENTERPRISE_SYNC_CACHE_NAME);
				}
				cache.clear();
			} catch (Exception e) {
				logger.error("exception evicting from esync cache", e);
			}
		}
	}

	/**
	 * Deletes stored resources from the Enterprise Sync database
	 * @param primaryKeys List of primary keys to delete
	 * @throws SQLException
	 * @throws NamingException
	 */
	public void delete(List<Integer> primaryKeys, String groupVector) throws SQLException, NamingException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		String sql = "DELETE FROM " + RESOURCE_TABLE + " r WHERE " + Column.id.toString() + "=?"
				+ RemoteUtil.getInstance().getGroupAndClause(); // only allow delete where there is common group membership

		try (Connection connection = ds.getConnection(); PreparedStatement statement = wrapper.prepareStatement(sql, connection)) {
			for (Integer key : primaryKeys) {
				log.fine("Deleting resource id=" + key);
				statement.setInt(1, key);
				statement.setString(2, groupVector);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}


	public Metadata insertResource(Metadata metadata, byte[] content, String groupVector)
			throws SQLException, NamingException, IllegalArgumentException,
			ValidationException, IntrusionException, IllegalStateException, IOException {

		return insertResource(metadata, content, groupVector, false);
	}


	/**
	 * Inserts the given byte array into the database, with the given metadata mapped to columns.
	 *
	 */
	public Metadata insertResource(Metadata metadata, byte[] content, String groupVector, boolean validate)
			throws SQLException, NamingException, IllegalArgumentException,
			ValidationException, IntrusionException, IllegalStateException, IOException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		if (validate && validator != null) {
			content = validator.getValidFileContent("Storing resource content to DB", content, content.length, false);
		}

		if (coreConfig.getRemoteConfiguration().getNetwork().isEsyncEnableCotFilter()) {
			try {
				if (Arrays.asList(metadata.getKeywords()).contains("missionpackage")) {
					String cotFilter = coreConfig.getRemoteConfiguration().getNetwork().getEsyncCotFilter();
					if (!Strings.isNullOrEmpty(cotFilter)) {
						content = DataPackageFileBlocker.blockCoT(metadata, content, cotFilter);
						if (content == null) {
							throw new IllegalArgumentException("Found blocked CoT detail : " + cotFilter);
						}
					}
				}
			} catch (Exception e) {
				logger.debug("exception processing esyncCotFilter");
			}
		}

		if(metadata.getHash().isEmpty()) {
			log.fine("No Hash provided, generating one.");
			// Compute the SHA-256 hash
			MessageDigest msgDigest = null;
			try {
				msgDigest = MessageDigest.getInstance("SHA-256");
				msgDigest.update(content);
				byte[] mdbytes = msgDigest.digest();
				StringBuffer hash = new StringBuffer();
				for (int i = 0; i < mdbytes.length; i++) {
					hash.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
				}
				metadata.set(Field.Hash, hash.toString());
			} catch (NoSuchAlgorithmException e) {
			}
		}

		return insertResource(metadata, new ByteArrayInputStream(content), content.length, groupVector);
	}
	@Override
	public Metadata insertResourceStream(Metadata metadata, InputStream contentStream, String groupVector)
			throws SQLException, NamingException, IllegalArgumentException,
			ValidationException, IntrusionException, IllegalStateException, IOException {
		return insertResourceStreamUID(metadata, contentStream, groupVector, true);
	}

	@Override
	public Metadata insertResourceStreamUID(Metadata metadata, InputStream contentStream, String groupVector, boolean generateUID)
			throws SQLException, NamingException, IllegalArgumentException,
			ValidationException, IntrusionException, IllegalStateException, IOException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		Metadata metadataResult = null;

		if (metadata.getNumberOfKeys() == 0) {
			throw new IllegalArgumentException("Uplaod request contains no metadata.");
		}
		commonUtil.validateMetadata(metadata);

		try {
			StringBuilder queryBuilder = new StringBuilder();
			List<TypeValuePair> metadataColumns = new LinkedList<TypeValuePair>();

			queryBuilder.append("INSERT INTO ");
			queryBuilder.append(RESOURCE_TABLE);
			queryBuilder.append(" (");
			queryBuilder.append(Column.data.toString());

			if (generateUID && metadata.getUid() == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No UID provided. Generating one.");
				}
				queryBuilder.append(", ");
				queryBuilder.append(Column.uid.toString());
				metadataColumns.add(new TypeValuePair(StoredType.string, new String[] {UUID.randomUUID().toString()}));
			}

			for (Column columnToEnter : columnToFieldMap.keySet()) {

				if (logger.isDebugEnabled()) {
					logger.debug("metadata key: " + columnToEnter);
				}

				if (columnToEnter.name().equalsIgnoreCase("octet_length")) {
					continue;
				}

				String[] metadataValues = metadata.getAll(columnToFieldMap.get(columnToEnter));
				if (metadataValues != null) {
					metadataColumns.add(new TypeValuePair(columnToEnter.type, metadataValues));
					queryBuilder.append(", ");
					queryBuilder.append(columnToEnter.toString());
				}
			}
			queryBuilder.append(", ");
			queryBuilder.append(Column.location.toString());

			queryBuilder.append(", groups");

			queryBuilder.append(") VALUES (?"); // first wildcard is for the data
			for (int index = 0; index < metadataColumns.size(); index++) {
				queryBuilder.append(", ?");
			}
			boolean haveLocation = (!metadata.getLatitude().isNaN() && !metadata.getLongitude().isNaN());
			if (haveLocation) {
				queryBuilder.append(", ST_GeometryFromText(?,4326)");
			} else {
				queryBuilder.append(", ?");
			}

			queryBuilder.append(", ?" + RemoteUtil.getInstance().getGroupType()); // groups

			queryBuilder.append(");");

			try (Connection connection = ds.getConnection(); PreparedStatement statement = connection.prepareStatement(queryBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {

				// bind input stream to database
				if (logger.isDebugEnabled()) {
					logger.debug("query: " + queryBuilder.toString());
					logger.debug("binding binary stream to the database query");
				}
				statement.setBinaryStream(1, contentStream);

				int columnIndex = 2;
				for (TypeValuePair toStore : metadataColumns) {
					switch (toStore.type) {
					case decimal:
						if (toStore.value == null) {
							statement.setNull(columnIndex, java.sql.Types.DECIMAL);
						} else {
							double number = Double.parseDouble(((String[])toStore.value)[0]);
							statement.setDouble(columnIndex, number);
						}
						break;
					case geometry:
						// Should not be present in columnToFieldMap, so forget it
						break;
					case integer:
						if (toStore.value == null) {
							statement.setNull(columnIndex, java.sql.Types.INTEGER);
						} else {
							statement.setInt(columnIndex, Integer.parseInt(((String[])toStore.value)[0]));
						}
						break;
					case string:
						if (toStore.value == null) {
							statement.setNull(columnIndex, java.sql.Types.VARCHAR);
						} else {
							statement.setString(columnIndex, ((String[])toStore.value)[0]);
						}
						break;
					case stringArray:
						if (toStore.value == null) {
							statement.setNull(columnIndex, java.sql.Types.ARRAY);
						} else {
							statement.setArray(columnIndex,
									wrapper.createArrayOf("varchar", (String[])toStore.value, connection));
						}
						break;
					case timestamp:
						if (toStore.value == null) {
							statement.setNull(columnIndex, java.sql.Types.TIMESTAMP);
						} else {

							SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);

							sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));

							try {

								if (logger.isDebugEnabled()) {
									logger.debug("timestamp type " + toStore.value.getClass().getName() + " value: " + toStore.value);
								}

								String submissionTime = ((String[]) toStore.value)[0];

								if (!Strings.isNullOrEmpty(submissionTime)) {

									Timestamp ts = new Timestamp(sdf.parse(submissionTime).getTime());

									statement.setTimestamp(columnIndex, ts);
								}
							} catch (Exception e) {
								log.fine("exception storing timestamp " + e.getMessage());
							}
						}
						break;
					default:
						throw new IllegalArgumentException("Cannot store type " + toStore.type.toString());
					}
					columnIndex++;
				}

				if (haveLocation) {
					statement.setString(columnIndex, metadata.getPointString());
				} else {
					statement.setNull(columnIndex, java.sql.Types.OTHER);
				}

				columnIndex++;
				statement.setString(columnIndex, groupVector);

				wrapper.auditLog(queryBuilder.toString());

				int insertResult = statement.executeUpdate();

				if (logger.isDebugEnabled()) {
					logger.debug("insertResult: " + insertResult);
				}

				Integer primaryKey = null;

				try (ResultSet generatedKeys = wrapper.getGeneratedKeys(statement)) {
					generatedKeys.next();

					primaryKey = new Integer(generatedKeys.getInt(1));
					Timestamp submissionTime = generatedKeys.getTimestamp(11,
							new GregorianCalendar());

					metadataResult = Metadata.copy(metadata);
					metadataResult.set(Field.PrimaryKey, primaryKey.toString());

					metadataResult.set(Field.SubmissionDateTime,
							new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT).format(submissionTime));
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("exception getting generated keys", e);
					}
				}

				if (primaryKey == null) {
					throw new IllegalArgumentException("null primary key returned from file insert - unable to set hash");
				}

				String updateHashUidSql = "update resource set hash = encode(digest(data, 'sha256'), 'hex') where id = ? returning hash;";

				if (metadata.getUid() != null) {
					// support request-specified or autogenerated random UID
					updateHashUidSql = "update resource set hash = encode(digest(data, 'sha256'), 'hex') where id = ? returning hash;";
				} else {
					// set UID to hash - for data packages
					updateHashUidSql = "update resource set hash = encode(digest(data, 'sha256'), 'hex'), uid = encode(digest(data, 'sha256'), 'hex') where id = ? returning hash;";
				}

				if (logger.isDebugEnabled()) {
					logger.debug("updateHashUidSql: " + updateHashUidSql);
				}

				// Always calculate the SHA-256 hash in the database
				// This has to be done after the insert, postgres won't allow the hash to be calculated during the insert statement
				try (PreparedStatement updateHashStatement = connection.prepareStatement(updateHashUidSql, Statement.RETURN_GENERATED_KEYS)) {

					if (logger.isDebugEnabled()) {
						logger.debug("primary key for hash update: " + primaryKey);
					}

					updateHashStatement.setInt(1, primaryKey);
					updateHashStatement.execute();

					try (ResultSet updateHashResult = wrapper.getGeneratedKeys(updateHashStatement)) {
						updateHashResult.next();

						String hash = updateHashResult.getString(1);

						if (logger.isDebugEnabled()) {
							logger.debug("hash: " + hash);
						}


						if (Strings.isNullOrEmpty(hash)) {
							throw new IllegalArgumentException("null hash");
						}

						// set returned hash in metadata object
						metadataResult.set(Field.Hash, hash);
						if (metadataResult.getUid() == null) {
							metadataResult.set(Field.UID, hash);
						}
					}
				}
			} catch (Exception e) {
				logger.error("exception inserting resource ", e);
				throw e;
			}
		} finally { }

		return metadataResult;
	}

	private Metadata insertResource(Metadata metadata, InputStream contentStream, long contentLen, String groupVector)
			throws SQLException, NamingException, IllegalArgumentException, IllegalStateException, IOException,
			ValidationException, IntrusionException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		if (contentLen == -1) {
			return insertResourceStream(metadata, contentStream, groupVector);
		}

		Metadata returnedMetadata = null;

		if (contentLen <= 0) {
			throw new IllegalArgumentException(
					"Upload request does not contain a positive size argument.");
		} else if (contentLen > MAX_DATA_SIZE_BYTES) {
			throw new IllegalArgumentException("payload larger than maxiumum allowed");
		}

		if (metadata.getNumberOfKeys() == 0) {
			throw new IllegalArgumentException("Uplaod request contains no metadata.");
		}

		commonUtil.validateMetadata(metadata);
		
		try {
			StringBuilder queryBuilder = new StringBuilder();
			List<TypeValuePair> metadataColumns = new LinkedList<TypeValuePair>();

			queryBuilder.append("INSERT INTO ");
			queryBuilder.append(RESOURCE_TABLE);
			queryBuilder.append(" (");
			queryBuilder.append(Column.data.toString());


			if (metadata.getUid() == null) {
				log.fine("No UID provided. Generating one.");
				queryBuilder.append(", ");
				queryBuilder.append(Column.uid.toString());
				metadataColumns.add(new TypeValuePair(StoredType.string, new String[] {UUID.randomUUID().toString()}));
			}

			for (Column columnToEnter : columnToFieldMap.keySet()) {

				logger.debug("metadata key: " + columnToEnter);

				if (columnToEnter.name().equalsIgnoreCase("octet_length")) {
					continue;
				}

				String[] metadataValues = metadata.getAll(columnToFieldMap.get(columnToEnter));
				if (metadataValues != null) {
					metadataColumns.add(new TypeValuePair(columnToEnter.type, metadataValues));
					queryBuilder.append(", ");
					queryBuilder.append(columnToEnter.toString());
				}
			}
			queryBuilder.append(", ");
			queryBuilder.append(Column.location.toString());

			queryBuilder.append(", groups");

			queryBuilder.append(") VALUES (?"); // first wildcard is for the data
			for (int index = 0; index < metadataColumns.size(); index++) {
				queryBuilder.append(", ?");
			}
			boolean haveLocation = (!metadata.getLatitude().isNaN() && !metadata.getLongitude().isNaN());
			if (haveLocation) {
				queryBuilder.append(", ST_GeometryFromText(?,4326)");
			} else {
				queryBuilder.append(", ?");
			}

			queryBuilder.append(", ?" + RemoteUtil.getInstance().getGroupType()); // groups

			queryBuilder.append(");");

			try (Connection connection = ds.getConnection(); PreparedStatement statement = wrapper.prepareInsert(queryBuilder.toString(), connection)) {

				// bind input stream to database
				log.fine("binding binary stream with content length " + contentLen + " to the database query");
				statement.setBinaryStream(1, contentStream, contentLen);

				int columnIndex = 2;
				for (TypeValuePair toStore : metadataColumns) {
					switch (toStore.type) {
						case decimal:
							if (toStore.value == null) {
								statement.setNull(columnIndex, java.sql.Types.DECIMAL);
							} else {
								double number = Double.parseDouble(((String[])toStore.value)[0]);
								statement.setDouble(columnIndex, number);
							}
							break;
						case geometry:
							// Should not be present in columnToFieldMap, so forget it
							break;
						case integer:
							if (toStore.value == null) {
								statement.setNull(columnIndex, java.sql.Types.INTEGER);
							} else {
								statement.setInt(columnIndex, Integer.parseInt(((String[])toStore.value)[0]));
							}
							break;
						case string:
							if (toStore.value == null) {
								statement.setNull(columnIndex, java.sql.Types.VARCHAR);
							} else {
								statement.setString(columnIndex, ((String[])toStore.value)[0]);
							}
							break;
						case stringArray:
							if (toStore.value == null) {
								statement.setNull(columnIndex, java.sql.Types.ARRAY);
							} else {
								statement.setArray(columnIndex,
										wrapper.createArrayOf("varchar", (String[])toStore.value, connection));
							}
							break;
						case timestamp:
							if (toStore.value == null) {
								statement.setNull(columnIndex, java.sql.Types.TIMESTAMP);
							} else {

								SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);

								sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));

								try {

									logger.debug("timestamp type " + toStore.value.getClass().getName() + " value: " + toStore.value);

									String submissionTime = ((String[]) toStore.value)[0];

									if (!Strings.isNullOrEmpty(submissionTime)) {

										Timestamp ts = new Timestamp(sdf.parse(submissionTime).getTime());

										statement.setTimestamp(columnIndex, ts);
									}
								} catch (Exception e) {
									log.fine("exception storing timestamp " + e.getMessage());
								}
							}
							break;
						default:
							throw new IllegalArgumentException("Cannot store type " + toStore.type.toString());
					}
					columnIndex++;
				}

				if (haveLocation) {
					statement.setString(columnIndex, metadata.getPointString());
				} else {
					statement.setNull(columnIndex, java.sql.Types.OTHER);
				}

				columnIndex++;
				statement.setString(columnIndex, groupVector);

				wrapper.doUpdate(statement);

				try (ResultSet generatedKeys = wrapper.getGeneratedKeys(statement)) {
					generatedKeys.next();
					Integer primaryKey = new Integer(generatedKeys.getInt(1));
					Timestamp submissionTime = generatedKeys.getTimestamp(11,
							new GregorianCalendar());

					returnedMetadata = Metadata.copy(metadata);
					returnedMetadata.set(Field.PrimaryKey, primaryKey.toString());

					returnedMetadata.set(Field.SubmissionDateTime,
							new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT).format(submissionTime));
				}

			} catch (Exception e) {
				logger.debug("exception inserting resource " + e.getMessage(), e);
			}
		} finally { }
		return returnedMetadata;

	}

	/**
	 * Creates a metadata instance from one row of a ResultSet, without
	 * advancing the current row.
	 * Precondition: the ResultSet must be from a SELECT statement that
	 * returned all resource columns. Use the <code>getResourceColumns</code>
	 * method to construct the query.
	 *
	 * @param results
	 *            a ResultSet containing all metadata columns in
	 *            METADATA_COLMNS, in that order
	 * @return a Metadata instance constructed from the current row in the
	 *         ResultSet
	 * @throws ValidationException
	 *             if the data fails input validation
	 * @throws SQLException
	 *             if a read error occurs
	 */
	private Metadata parseResourceRow(ResultSet results, List<Column> columns)
			throws ValidationException, SQLException {
		Metadata metadata = new Metadata();

		for (int resultIndex = 1; resultIndex < (results.getMetaData().getColumnCount()+1); ++resultIndex) {
			String[] value = null;
			log.finer("Parsing index " + resultIndex + " column " + results.getMetaData().getColumnName(resultIndex));
			try {
				Column metadataColumn = Column.valueOf(results.getMetaData().getColumnName(resultIndex));
				switch (metadataColumn.type) {
					case string:
						String singleString = results.getString(resultIndex);
						if (!results.wasNull()) {
							value = new String[]{singleString};
						}
						break;
					case stringArray:
						Array array = results.getArray(resultIndex);
						if (!results.wasNull()) {
							String[] dbArray = (String[]) array.getArray();
							value = new String[dbArray.length];
							for (int index = 0; index < dbArray.length; index++) {
								value[index] = new String(dbArray[index]);
							}
						}
						break;
					case integer:
						int integer = results.getInt(resultIndex);
						if (!results.wasNull()) {
							value = new String[]{(new Integer(integer)).toString()};
						}
						break;
					case byteArray:
						log.warning("Data column ("
								+ metadataColumn.toString()
								+ "requested while parsing metadata. Skipping.");
						break;
					case timestamp:
						Timestamp stamp = results.getTimestamp(resultIndex);
						if (!results.wasNull()) {
							value = new String[]{new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT).format(stamp)};
						}
						break;
					case decimal:
						double number = results.getDouble(resultIndex);
						if (!results.wasNull()) {

							value = new String[]{new DecimalFormat().format(number)};
						}
						break;
					case geometry:
						String location = results.getString(resultIndex);
						if (!results.wasNull()) {
							String[] coordinates = parsePoint(location.trim());
							metadata.set(Metadata.Field.Longitude, coordinates[0]);
							metadata.set(Metadata.Field.Latitude, coordinates[1]);
						}
						break;

				}
				if (columnToFieldMap.containsKey(metadataColumn)) {
					metadata.set(columnToFieldMap.get(metadataColumn), value);
				}
			} catch (Exception e) {
				logger.debug("Exception parsing results: " + e.getMessage(), e);
			}
		}

		return metadata;
	}

	private String[] parsePoint(String pointString) {
		if (pointString == null || pointString.isEmpty()) {
			return null;
		}
		String coordinateString = pointString.substring(6, pointString.length() - 1);
		int spaceIndex = coordinateString.indexOf(' ');
		String lon = coordinateString.substring(0, spaceIndex);
		String lat = coordinateString.substring(spaceIndex + 1);
		return new String[] { lon, lat };
	}

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
	public SortedMap<String, List<Metadata>> search(Double minimumAltitude,
													Double maximumAltitude,
													Map<Metadata.Field, String> stringParameters,
													PGobject spatialConstraints, Timestamp minimumTime,
													Timestamp maximumTime, Boolean latestOnly, String missionName, String tool, String groupVector) throws SQLException,
			NamingException, ValidationException, IntrusionException,
			ParseException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		String viewName = (latestOnly != null && latestOnly) ? LATEST_RESOURCE_VIEW : RESOURCE_TABLE + " r ";
		String context = "Parsing search constraints";

		SortedMap<String, List<Metadata>> results = new TreeMap<String, List<Metadata>>();

		if (validator != null && stringParameters != null) {
			for (Entry<Metadata.Field, String> entry : stringParameters
					.entrySet()) {
				validator.getValidInput(context, entry.getValue(),
						"RestrictedRegex", REGEX_LENGTH_LIMIT, true);
			}
		}

		boolean atleastOne = false;

		try {
			StringBuilder sqlQuery = new StringBuilder();
			sqlQuery.append("SELECT ");
			sqlQuery.append(getResourceColumns());
			sqlQuery.append(" FROM ");
			sqlQuery.append(viewName);

			// inner join on the mission_resource and mission tables iff a missionName is provided as a parameter
			if (!Strings.isNullOrEmpty(missionName)) {
				sqlQuery.append(" inner join mission_resource mr on mr.resource_id = r.id "
						+ " inner join mission m on m.id = mr.mission_id ");
			}

			List<Object> queryParameters = new LinkedList<Object>();

			if ( (stringParameters != null && !stringParameters.isEmpty()) || minimumAltitude != null
					|| maximumAltitude != null || (spatialConstraints != null)
					|| minimumTime != null || maximumTime != null || !Strings.isNullOrEmpty(missionName)
					|| tool != null) {
				sqlQuery.append(" WHERE ");


				if (!Strings.isNullOrEmpty(missionName)) {
					sqlQuery.append(" m.name = ? ");
					atleastOne = true;
				}

				if (stringParameters != null) {
					for (Metadata.Field f : Metadata.Field.values()) {
						String constraint = stringParameters.get(f);
						if (constraint != null) {
							if (atleastOne) {
								sqlQuery.append(" AND ");
							}

							if (f.isArray) {
								StringBuilder arrayQuery = new StringBuilder("(");

								for (String v : constraint.split(",")) {
									if (arrayQuery.length() > 1) {
										arrayQuery.append(" OR ");
									}

									arrayQuery.append(" exists ( select * from unnest("
											+ "r." + f.toString() + ") as f where f ~ ? ) ");
									queryParameters.add(v);
								}

								arrayQuery.append(")");
								sqlQuery.append(arrayQuery.toString());

							} else if (f == Metadata.Field.SubmissionDateTime) {
								if (minimumTime == null && maximumTime == null) {
									java.util.Date timeConstraint =
											new SimpleDateFormat(tak.server.Constants.COT_DATE_FORMAT).parse(constraint);
									minimumTime = new Timestamp(
											timeConstraint.getTime());
									maximumTime = new Timestamp(
											timeConstraint.getTime());
								} else {
									throw new IllegalArgumentException(
											"Conflicting time constraints; cannot specify both "
													+ f.toString()
													+ " and a minimum or maximum time.");
								}
							} else {
								sqlQuery.append("r." + f.toString() + " ~ ? ");
								queryParameters.add(constraint);
							}
							atleastOne = true;
						}
					}
				}
				if (minimumAltitude != null) {
					if (atleastOne) {
						sqlQuery.append("AND ");
					}
					sqlQuery.append(Column.altitude.toString() + " >= ?");
					queryParameters.add(minimumAltitude);
					atleastOne = true;
				}
				if (maximumAltitude != null) {
					if (atleastOne) {
						sqlQuery.append("AND ");
					}
					sqlQuery.append(Column.altitude.toString() + " <= ?");
					queryParameters.add(maximumAltitude);
					atleastOne = true;
				}

				if (spatialConstraints != null) {
					if (atleastOne) {
						sqlQuery.append("AND ");
                    }
					if (spatialConstraints instanceof PGbox) {
						PGpoint[] points = ((PGbox) spatialConstraints).point;
						sqlQuery.append("ST_SetSRID(ST_MakeBox2D(ST_Point(");
						sqlQuery.append(points[0].x);
						sqlQuery.append(", ");
						sqlQuery.append(points[0].y);
						sqlQuery.append("), ST_POINT(");
						sqlQuery.append(points[1].x);
						sqlQuery.append(", ");
						sqlQuery.append(points[1].y);
						sqlQuery.append(")), 4326) ~ location ");
						atleastOne = true;
					} else if (spatialConstraints instanceof PGcircle) {
						PGcircle circle = (PGcircle) spatialConstraints;
						sqlQuery.append("ST_DWithin(location, ST_GeographyFromText('POINT(");
						sqlQuery.append(circle.center.x);
						sqlQuery.append(" ");
						sqlQuery.append(circle.center.y);
						sqlQuery.append(")'), ");
						sqlQuery.append(circle.radius);
						sqlQuery.append(")");
						atleastOne = true;
					} else {
						throw new IllegalArgumentException(
								"Illegal type "
										+ spatialConstraints.getClass()
										.getCanonicalName()
										+ " in spatial constraint. Only PGbox, PGcircle are supported.");
					}
				}
				if (minimumTime != null) {
					if (atleastOne) {
						sqlQuery.append("AND ");
					}
					sqlQuery.append(Column.submissiontime.toString()
							+ " >= ?");
					queryParameters.add(minimumTime);
					atleastOne = true;
				}
				if (maximumTime != null) {
					if (atleastOne) {
						sqlQuery.append(" AND ");
					}
					sqlQuery.append(Column.submissiontime.toString()
							+ " <= ?");
					queryParameters.add(maximumTime);
					atleastOne = true;
				}
				if (tool != null) {
					if (atleastOne) {
						sqlQuery.append(" AND ");
					}
					sqlQuery.append(Column.tool.toString()
							+ " = ?");
					queryParameters.add(tool);
					atleastOne = true;
				}

			}



			// filter by group membership
			if (atleastOne) {
				sqlQuery.append(RemoteUtil.getInstance().getGroupAndClause("r"));
			} else {
				sqlQuery.append(" where" + RemoteUtil.getInstance().getGroupClause("r"));
			}

			// Terminate the query
			sqlQuery.append(" ORDER BY r." + Column.submissiontime.toString() + ";");

			log.fine("resource search query: " + sqlQuery);

			try (Connection connection = ds.getConnection(); PreparedStatement statement = wrapper.prepareStatement(sqlQuery.toString(), connection)) {

				int argCount = 1;

				if (!Strings.isNullOrEmpty(missionName)) {
					statement.setString(argCount++, missionName);
				}

				for (Object parameter : queryParameters) {
					if (parameter instanceof Double) {
						statement.setDouble(argCount, (Double) parameter);
					} else if (parameter instanceof Integer) {
						statement.setInt(argCount, (Integer) parameter);
					} else if (parameter instanceof Timestamp){
						statement.setTimestamp(argCount, (Timestamp)parameter);
					} else {
						statement.setString(argCount, parameter.toString());
					}
					argCount++;
				}

				statement.setString(argCount, groupVector);

				try (ResultSet queryResults = wrapper.doQuery(statement)) {

					int resultCount = 0;
					while (queryResults.next()) {
						try {
							Metadata metadata = parseResourceRow(queryResults, metadataColumns);
							if (results.containsKey(metadata.getUid())) {
								results.get(metadata.getUid()).add(metadata);
							} else {
								List<Metadata> list = new LinkedList<Metadata>();
								list.add(metadata);
								results.put(metadata.getUid(), list);
							}
						} catch (ValidationException ex) {
							log.warning(ex.getMessage() + ": "
									+ ex.getCause().getMessage());
							continue;
						}
						resultCount++;
					}
					log.fine("Found " + resultCount + " results");
				}
			}
		} finally { }
		return results;
	}

	/**
	 * Gets all Metadata in the database
	 * @return
	 * @throws ValidationException
	 * @throws IntrusionException
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ParseException
	 */
	public Map<String, List<Metadata>> getAllMetadata(String groupVector)
			throws ValidationException, IntrusionException, SQLException, NamingException, ParseException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		return this.search(null, null, null, null, null, null, null, null, null, groupVector);
	}

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param uid UID of the object to retrieve
	 * @return the content of the latest stored object matching that UID, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	public byte[] getContentByUid(String uid, String groupVector) throws SQLException, NamingException {
		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		byte[] result = null;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + Column.data.toString() + " FROM "
				+ RESOURCE_TABLE + " r WHERE " + Column.uid.toString()
				+ " = ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+" ORDER BY id desc limit 1;", connection)) {
			query.setString(1, uid);
			query.setString(2, groupVector);
			log.fine("Executing SQL: " + query.toString());

			log.fine("PersistenceStore principal: " + AuditLogUtil.getUsername());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = queryResults.getBytes(1);
				}
			}
		}

		return result;
	}

	@Override
	public byte[] getContentByUidAndMaxTime(String uid, long maxTimeMillis, String groupVector) throws SQLException, NamingException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		byte[] result = null;
		//		DbQueryWrapper wrapper = new DbQueryWrapper();
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + Column.data.toString() + " FROM "
				+ RESOURCE_TABLE + " r WHERE " + Column.uid.toString()
				+ " = ? "
				+ " and " + Column.submissiontime.toString() + " <= ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+ " ORDER BY " + Column.submissiontime.toString() + " desc limit 1;", connection)) {
			query.setString(1, uid);
			query.setTimestamp(2, new Timestamp(maxTimeMillis));
			query.setString(3, groupVector);
			log.fine("Executing SQL: " + query.toString());

			log.fine("PersistenceStore principal: " + AuditLogUtil.getUsername());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = queryResults.getBytes(1);
				}
			}
		}
		return result;
	}

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param uid UID of the object to retrieve
	 * @return the content of the latest stored object matching that UID, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	public byte[] getContentByUidAndTool(String uid, String tool, String groupVector) throws SQLException, NamingException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		byte[] result = null;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + Column.data.toString() + " FROM "
				+ LATEST_RESOURCE_VIEW + " r WHERE " + Column.uid.toString()
				+ " ~ ? "
				+ " and tool = ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+" ORDER BY " + Column.submissiontime.toString() + ";", connection)) {
			query.setString(1, uid);
			query.setString(2, tool);
			query.setString(3, groupVector);
			log.fine("Executing SQL: " + query.toString());

			log.fine("PersistenceStore principal: " + AuditLogUtil.getUsername());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = queryResults.getBytes(1);
				}
			}
		}

		return result;
	}

	/**
	 * Gets the content of an Enterprise Sync object.
	 * @param hash Hash of the object to retrieve
	 * @return the content of the latest stored object matching that hash, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	public byte[] getContentByHash(String hash, String groupVector) throws SQLException, NamingException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		String cacheKey = "getContentByHash_" + hash + "_" + groupVector;

		Cache cache = null;

		if (isCacheEsync()) {

			cache = cacheManager.getCache(Constants.ENTERPRISE_SYNC_CACHE_NAME);

			if (cache == null) {
				throw new IllegalStateException("unable to get " + Constants.ENTERPRISE_SYNC_CACHE_NAME);
			}

			ValueWrapper resultWrapper = cache.get(cacheKey);

			if (resultWrapper != null && resultWrapper.get() != null) {

				if (resultWrapper.get() instanceof byte[]) {

					// cache hit
					return (byte[]) resultWrapper.get();

				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("getContentByHash invalid cache result type " + (resultWrapper.get() == null ? "null" : resultWrapper.get().getClass().getName()) + " (should be byte[])");
					} else if (logger.isWarnEnabled()) {
						logger.warn("getContentByHash invalid cache result type");
					}
				}
			}
		}

		byte[] result = null;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + Column.data.toString() + " FROM "
				+ RESOURCE_TABLE + " r WHERE " + Column.hash.toString()
				+ " = ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+ "ORDER BY " + Column.submissiontime.toString() + ";", connection)) {
			query.setString(1, hash.toLowerCase());
			query.setString(2, groupVector);
			log.fine("getContentByHash Executing SQL: " + query.toString());

			log.fine("PersistenceStore principal: " + AuditLogUtil.getUsername());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = queryResults.getBytes(1);
				} else {
					logger.debug("getContentByHash no results");
				}
			}

		} catch (Exception e) {
			logger.error("exception executing getContentByHash query " + e.getMessage(), e);
		}
		if (isCacheEsync() && result != null) {
			// store in cache
			cache.put(cacheKey, result);
		}
		return result;
	}

	/**
	 * Gets the content of an Enterprise Sync object. This searches the full resource table vs the latest resource
	 * view. Latest resource is used to back the getContentByHash function and will only return a match for a hash
	 * if it's the most recent entry for a given uid. This function allows searching the history for a given uid.
	 * @param hash Hash of the object to retrieve
	 * @return the content of the latest stored object matching that hash, or <code>null</code> if no match
	 * @throws SQLException
	 * @throws NamingException
	 */
	public byte[] getContentByOldHash(String hash, String groupVector) throws SQLException, NamingException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		byte[] result = null;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + Column.data.toString() + " FROM "
				+ RESOURCE_TABLE + " r WHERE " + Column.hash.toString()
				+ " = ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+ "ORDER BY " + Column.submissiontime.toString() + ";", connection)) {
			query.setString(1, hash.toLowerCase());
			query.setString(2, groupVector);
			log.fine("getContentByHash Executing SQL: " + query.toString());

			log.fine("PersistenceStore principal: " + AuditLogUtil.getUsername());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = queryResults.getBytes(1);
				} else {
					logger.debug("getContentByHash no results");
				}
			}

		} catch (Exception e) {
			logger.error("exception executing getContentByHash query " + e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Gets all Metadata instances containing the given <code>uid</code>
	 * @param uid
	 * @return a List of all Metadata containing that uid; the list will be empty if the uid is not in the database
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	public List<Metadata> getMetadataByUid(String uid, String groupVector) throws SQLException, NamingException, ValidationException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		List<Metadata> results = new LinkedList<Metadata>();
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + getResourceColumns()
				+ " FROM " + RESOURCE_TABLE + " r WHERE "
				+ Column.uid.toString() + " ~ ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+ "ORDER BY " + Column.submissiontime.toString() + " DESC;", connection)) {
			query.setString(1, uid);
			query.setString(2, groupVector);
			log.fine("Executing SQL: " + query.toString());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					results.add(parseResourceRow(queryResults, metadataColumns));
				}
			}
		}
		return results;
	}

	/**
	 * Gets all Metadata instances containing the given <code>uid</code>
	 * @param uid
	 * @return a List of all Metadata containing that uid; the list will be empty if the uid is not in the database
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	public List<Metadata> getMetadataByUidAndTool(String uid, String tool, String groupVector) throws SQLException, NamingException, ValidationException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		List<Metadata> results = new LinkedList<Metadata>();
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + getResourceColumns()
				+ " FROM " + RESOURCE_TABLE + " r WHERE "
				+ Column.uid.toString() + " ~ ? "
				+ " and tool = ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+ "ORDER BY " + Column.submissiontime.toString() + " DESC;", connection)) {
			query.setString(1, uid);
			query.setString(2, tool);
			query.setString(3, groupVector);
			log.fine("Executing SQL: " + query.toString());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					results.add(parseResourceRow(queryResults, metadataColumns));
				}
			}
		}
		return results;
	}

	/**
	 * Gets the metadata for a specific database record identified by its primary key.
	 * @param primaryKey
	 * @return the Metadata for the requested file object, or <code>null</code> if the primary key was invalid.
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	public Metadata getMetadata(Integer primaryKey, String groupVector) throws SQLException, NamingException, ValidationException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		Metadata result = null;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + getResourceColumns()
				+ " FROM " + LATEST_RESOURCE_VIEW + " WHERE "
				+ Column.id.toString() + " = ?"
				+ RemoteUtil.getInstance().getGroupAndClause(), connection)) {
			query.setInt(1, primaryKey);
			query.setString(2, groupVector);
			log.fine("Executing SQL: " + query.toString());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					result = parseResourceRow(queryResults, metadataColumns);
				}
			}
		}
		return result;
	}

	/**
	 * Gets all Metadata instances containing the given <code>hash</code>
	 * @param hash
	 * @return a List of all Metadata containing that hash; the list will be empty if the hash is not in the database
	 * @throws SQLException
	 * @throws NamingException
	 * @throws ValidationException
	 */
	@SuppressWarnings("unchecked")
	public List<Metadata> getMetadataByHash(String hash, String groupVector) throws SQLException, NamingException, ValidationException {

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalArgumentException("empty group vector");
		}

		String cacheKey = "getMetadataByHash_" + hash + "_" + groupVector;

		Cache cache = null;

		if (isCacheEsync()) {

			cache = cacheManager.getCache(Constants.ENTERPRISE_SYNC_CACHE_NAME);

			if (cache == null) {
				throw new IllegalStateException("unable to get " + Constants.ENTERPRISE_SYNC_CACHE_NAME);
			}

			ValueWrapper resultWrapper = cache.get(cacheKey);

			if (resultWrapper != null && resultWrapper.get() != null) {

				if (resultWrapper.get() instanceof List<?> && !((List<?>) resultWrapper.get()).isEmpty()) {

					// cache hit
					if (logger.isDebugEnabled()) {
						logger.debug("EnterpriseSync getMetadataByHash cache hit " + cacheKey);
					}
					return (List<Metadata>) resultWrapper.get();
				} else {
					if (logger.isWarnEnabled()) {
						logger.warn("getMetadataByHash invalid cache result type (should be List<Metadata>)");
					}
				}
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(("EnterpriseSync getMetadataByHash cache miss " + cacheKey));
		}

		List<Metadata> results = new LinkedList<Metadata>();
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper.prepareStatement("SELECT " + getResourceColumns()
				+ " FROM " + RESOURCE_TABLE + " r WHERE "
				+ Column.hash.toString() + " = ? "
				+ RemoteUtil.getInstance().getGroupAndClause()
				+ "ORDER BY " + Column.submissiontime.toString() + " DESC;", connection)) {
			query.setString(1, hash.toLowerCase());
			query.setString(2, groupVector);
			log.fine("Executing SQL: " + query.toString());

			try (ResultSet queryResults = query.executeQuery()) {
				if (queryResults.next()) {
					results.add(parseResourceRow(queryResults, metadataColumns));
				}
			}
		}
		if (isCacheEsync() && results != null && !((List<Metadata>) results).isEmpty()) {
			// store in cache
			cache.put(cacheKey, results);
		}
		return results;
	}

	/**
	 * Gets a comma-delimited list of the column names from the resource table.
	 * @return
	 */
	private String getResourceColumns() {
		StringBuilder builder = new StringBuilder();
		Iterator<Column> itr = metadataColumns.iterator();
		while (itr.hasNext()) {
			Column column = itr.next();
			if (column == Column.location) {
				builder.append("ST_AsText(");
				builder.append("r." + column.toString());
				builder.append(") as location");
			} else {
				builder.append("r." + column.toString());
			}
			builder.append(",");

		}
		builder.append(" octet_length(r.data)");
		return builder.toString();
	}

	@Override
	public boolean updateMetadata(String hash, String metadataField, String metadataValue, String groupVector) throws
			SQLException, NamingException, ValidationException {
		boolean updated = false;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper
				.prepareStatement("update resource set " + metadataField + " = ? where hash = ?", connection)) {
			query.setString(1, metadataValue);
			query.setString(2, hash);
			log.fine("Executing SQL: " + query.toString());
			updated = query.executeUpdate() > 0;
		}
		// evict from cache
		if (updated) {
			String cacheKey = "getMetadataByHash_" + hash + "_" + groupVector;
			Cache cache = null;
			if (isCacheEsync()) {
				cache = cacheManager.getCache(Constants.ENTERPRISE_SYNC_CACHE_NAME);
				if (cache == null) {
					throw new IllegalStateException("unable to get " + Constants.ENTERPRISE_SYNC_CACHE_NAME);
				}
				cache.evictIfPresent(cacheKey);
			}
		}
		return updated;
	}

	@Override
	public boolean updateMetadataKeywords(String hash, List<String> keywords) throws
			SQLException, NamingException, ValidationException {
		boolean updated = false;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper
				.prepareStatement("update resource set keywords = ? where hash = ?", connection)) {
			query.setArray(1,
					wrapper.createArrayOf("varchar", keywords.toArray(), connection));
			query.setString(2, hash);
			log.fine("Executing SQL: " + query.toString());
			updated = query.executeUpdate() > 0;
		}
		return updated;
	}

	@Override
	public boolean updateExpiration(String hash, Long expiration) throws
			SQLException, NamingException {
		boolean updated = false;
		try (Connection connection = ds.getConnection(); PreparedStatement query = wrapper
				.prepareStatement("update resource set expiration = ? where hash = ?", connection)) {
			query.setLong(1, expiration);
			query.setString(2, hash);
			// TODO bump back down to fine
			updated = query.executeUpdate() > 0;
			log.fine("Executing SQL: " + query.toString() + " updated:" + updated);
			try {
				log.info("trying to update expiration on resource " + hash);
				retentionPolicyConfig.setResourceExpiryTask(hash, expiration);
			} catch (Exception e) {
				logger.error(" Exception getting Retention service, task not scheduled immediately " + hash);
			}
			return updated;
		}
	}

	private boolean isCacheEsync() {
		return esyncEnableCache || (clusterConfig.isEnabled() && clusterConfig.isKubernetes());
	}
}
