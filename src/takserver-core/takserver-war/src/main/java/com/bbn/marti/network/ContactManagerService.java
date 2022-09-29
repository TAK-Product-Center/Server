
package com.bbn.marti.network;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.bbn.marti.remote.ClientEndpoint;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.security.web.MartiValidator;

import tak.server.cache.ContactCacheHelper;

/**
 * 
 *
 */
public class ContactManagerService {

	Logger logger = LoggerFactory.getLogger(ContactManagerService.class);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private CoreConfig config;

	@Autowired
	private ContactCacheHelper contactCache;

	private AtomicLong lastUpdateMillis = new AtomicLong(-1);

	public List<ClientEndpoint> getCachedClientEndpointData(boolean connected, boolean recent, String groupVector, long secAgo) {

		try {


			boolean skipCache = !config.getCachedConfiguration().getBuffer().getQueue().isEnableClientEndpointCache();

			List<ClientEndpoint> result = null;

			String key = contactCache.getKeyGetCachedClientEndpointData(connected, recent, secAgo);

			result = (List<ClientEndpoint>) contactCache.getContactsCache().getIfPresent(key); 

			if (logger.isDebugEnabled()) {
				logger.debug("got cache result " + key + " of size " + (result == null ? "null" : result.size()));
			}

			if (result == null || result.isEmpty() || lastUpdateMillis.get() == -1L || (System.currentTimeMillis() - lastUpdateMillis.get() >= (config.getCachedConfiguration().getBuffer().getQueue().getContactCacheUpdateRateLimitSeconds() * 1000))) {

				synchronized(this) {

					if (logger.isDebugEnabled()) {
						logger.debug("contact cache miss - querying contacts");
					}

					result = (List<ClientEndpoint>) contactCache.getContactsCache().getIfPresent(key); 

					if (logger.isDebugEnabled()) {
						logger.debug("got cache result " + key + " of size " + (result == null ? "null" : result.size()));
					}

					if (result == null || (System.currentTimeMillis() - lastUpdateMillis.get() >= (config.getCachedConfiguration().getBuffer().getQueue().getContactCacheUpdateRateLimitSeconds() * 1000))) {

						result = queryClientEndpointData(connected, recent, groupVector, secAgo);

						if (!skipCache) {
							try {
								lastUpdateMillis.set(System.currentTimeMillis());

								if (logger.isDebugEnabled()) {
									logger.debug("caching result " + key + " of size " + result.size());
								}

								contactCache.getContactsCache().put(key, result);
							} catch (Exception e) {
								if (logger.isDebugEnabled()) {
									logger.debug("error caching contact endpoint data", e);
								}
							}
						}
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("query result size: " + (result == null ? "null" : result.size()));
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("contact cache hit - " + key);
				}
			}

			List<ClientEndpoint> groupFilteredResult = new ArrayList<>(result);

			if (logger.isDebugEnabled()) {
				logger.debug("contacts result size: " + result.size() + " copy result size: " + groupFilteredResult.size());
			}

			groupFilteredResult.removeIf(ce -> ce.getGroups() == null || !RemoteUtil.getInstance().isGroupVectorAllowed(groupVector, ce.getGroups()));

			return groupFilteredResult;		

		} catch (Exception e) {
			logger.error("exception getting non-cached contacts", e);
			throw new TakException(e);
		}
	}

	private List<ClientEndpoint> queryClientEndpointData(boolean connected, boolean recent, String groupVector, long secAgo) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		String query = null;

		//There is the remote possibility that the queries below return multiple values
		//for a callsign/uid pair if there are multiple entries at exactly the same timestamp.
		//However, the changes needed to address this condition would introduce a performance hit
		//that's not worth the benefit.

		if (recent) {
			// only show the most recent uid for a given callsign
			query = "select ce.callsign, ce.uid, ce.username, cee.created_ts, cet.event_name, cee.groups " +
					"from " +
					"	(select ce2.callsign, max(cee2.id) client_endpoint_event_id " +
					"		from client_endpoint ce2 join client_endpoint_event cee2 on ce2.id = cee2.client_endpoint_id" + (secAgo > 0 ? " where cee2.created_ts >= (current_timestamp - (? || ' seconds')::interval) " : "") +
					"		group by ce2.callsign) t1 " +
					" join client_endpoint_event cee on t1.client_endpoint_event_id = cee.id " +
					" join client_endpoint ce on cee.client_endpoint_id = ce.id " +
					" join connection_event_type cet on cee.connection_event_type_id = cet.id ";
		} else {
			query = "select ce.callsign, ce.uid, ce.username, cee.created_ts, cet.event_name, cee.groups " +
					"from " +
					"	(select ce2.callsign, ce2.uid, ce2.username, max(cee2.created_ts) last_event_time " +
					"	from client_endpoint ce2 join client_endpoint_event cee2 on ce2.id = cee2.client_endpoint_id" + (secAgo > 0 ? " where cee2.created_ts >= (current_timestamp - (? || ' seconds')::interval) " : "") +
					"	group by ce2.callsign, ce2.uid, ce2.username ) t1 join client_endpoint ce on t1.callsign = ce.callsign and t1.uid = ce.uid and t1.username = ce.username " +
					"join client_endpoint_event cee on ce.id = cee.client_endpoint_id and cee.created_ts = t1.last_event_time " +
					"join connection_event_type cet on cee.connection_event_type_id = cet.id ";
		}

		if (connected) {
			query += " where cet.event_name = 'Connected' ";
		}

		//Sorting is managed by the database and applies to all filter combinations
		query += " order by callsign, uid, username ";

		if (logger.isDebugEnabled()) {
			logger.debug("executing client endpoints query");
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace("client endpoints query: " + query + " secAgo: " + secAgo);
		}

		Object[] arguments = new Object[] { secAgo };

		if (secAgo > 0) {

			return jdbcTemplate.query(query, arguments, new ClientEndpointResultSetExtractor());
		} 

		return jdbcTemplate.query(query, new ClientEndpointResultSetExtractor());
	}

	private class ClientEndpointResultSetExtractor implements ResultSetExtractor<List<ClientEndpoint>> {

		@Override
		public List<ClientEndpoint> extractData(ResultSet resultSet) throws SQLException {

			List<ClientEndpoint> list = new ArrayList<>();

			if (resultSet == null) {
				throw new IllegalArgumentException("Null ResultSet");
			}
			org.owasp.esapi.Validator validator = MartiValidator.getInstance();
			while(resultSet.next()) {
				try {

					String uid = resultSet.getString("uid");
					validator.isValidInput("Client Endpoint uid", uid, MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, false);
					String callsign = resultSet.getString("callsign");
					validator.isValidInput("Client Endpoint callsign", callsign, MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, false);
					String username = resultSet.getString("username");
					validator.isValidInput("Client Endpoint username", username, MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, true);
					java.sql.Timestamp ts = resultSet.getTimestamp("created_ts");
					String lastEventName = resultSet.getString("event_name");
					validator.isValidInput("Client Endpoint event name", lastEventName, MartiValidator.Regex.MartiSafeString.name(), MartiValidator.DEFAULT_STRING_CHARS, false);
					String groups = resultSet.getString("groups");
					list.add(new ClientEndpoint(callsign, uid, username, new Date(ts.getTime()), lastEventName, groups));
				} catch (Throwable t) {
					logger.debug("Exception processing row in ClientEndpointResultSetExtractor: ", t);
				}
			}

			return list;
		}
	}

	public String getCallsignForUid(String clientUid, String groupVector) {
		List<ClientEndpoint> clientEndpoints = getCachedClientEndpointData(true, true, groupVector, 0);
		for (ClientEndpoint clientEndpoint : clientEndpoints) {
			if (clientEndpoint.getUid().compareToIgnoreCase(clientUid) == 0) {
				return clientEndpoint.getCallsign();
			}
		}
		return null;
	}
}
