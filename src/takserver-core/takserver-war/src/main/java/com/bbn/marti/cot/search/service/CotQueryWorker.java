

package com.bbn.marti.cot.search.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.CotImageBean;
import com.bbn.marti.JDBCQueryAuditLogHelper;
import com.bbn.marti.cot.search.model.CotSearch;
import com.bbn.marti.cot.search.model.CotSearchStatus;
import com.bbn.marti.cot.search.model.query.Column;
import com.bbn.marti.cot.search.model.query.DeliveryProtocol;
import com.bbn.marti.cot.search.model.query.ImageOption;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.google.common.base.Strings;

/**
 *
 * In the context of a worker thread, execute a database query asynchronously and sends the results to the specified IP address.
 *
 *
 */
public class CotQueryWorker implements Runnable {
    private static final int DELIVERY_EXCEPTION_LIMIT = 3;

    private static final String COT_TABLE_NAME = "cot_router";
    private static final String LATEST_VIEW_NAME = "latestcot";

    public static final String SRID = "4326";
    private static final int MAXIMUM_DATAGRAM_BYTES = 65507;

    private static final Logger logger = LoggerFactory.getLogger(CotQueryWorker.class);

    /**
     * The full monty:
     * @param destination
     * @param port
     * @param protocol
     * @param sqlPredicate
     * @param sqlParameters
     * @param resultLimit
     * @param latestOnly
     * @param images
     * @param replayMode
     */
    public CotQueryWorker(String destination,
            int port,
            DeliveryProtocol protocol,
            String sqlPredicate,
            List<Object> sqlParameters,
            Integer resultLimit,
            Boolean latestOnly,
            ImageOption images,
            boolean replayMode,
            Double replaySpeed,
            CotSearch cotSearch) {
        this.host = destination;
        this.port = port;
        this.replaySpeed = (replaySpeed == null) ? 1.0d : replaySpeed;
        this.replayMode = replayMode;
        this.sqlPredicate = (sqlPredicate == null) ? "" : sqlPredicate;
        this.sqlParameters = (sqlParameters == null) ? new LinkedList<Object>() : sqlParameters;
        this.resultLimit = (resultLimit == null) ? null : resultLimit;
        this.imageOption = (images == null) ? ImageOption.NONE : images;
        this.protocol = (protocol == null) ? DeliveryProtocol.TCP : protocol;

        if (cotSearch == null) {
            throw new IllegalArgumentException("null CotSearch");
        }

        this.cotSearch = cotSearch;
    }

    private String host;
    private int port;
    private String sqlPredicate;
    private boolean replayMode;
    private double replaySpeed;
    private List<Object> sqlParameters = new LinkedList<Object>();
    private boolean latestOnly;
    private Integer resultLimit;
    private ImageOption imageOption;
    private DeliveryProtocol protocol;
    private CotSearch cotSearch;

    private long timeMarker = 0;

    @Override
    public void run() {
        // set query status to processing

        cotSearch.updateStatus(CotSearchStatus.SUBMITTED, "query " + cotSearch.getId() + " processing", new Date(), true);

        int numberOfResults = 0;
        try {
            timeMarker = System.currentTimeMillis();
            numberOfResults = countCotEvents(sqlPredicate, sqlParameters, latestOnly);
            logger.debug("Counted " + numberOfResults + " results in "
                    + (System.currentTimeMillis() - timeMarker) + " ms." );
            if (resultLimit != null) {
                numberOfResults = Math.min(resultLimit, numberOfResults);
            }

            cotSearch.setCount(numberOfResults);

        } catch (SQLException e) {
            String msg = "exception getting query result count";
            logger.error(msg, e);

            cotSearch.updateStatus(CotSearchStatus.ERROR, msg, new Date(), false);
            return;
        } catch (NamingException e) {
            logger.error("", e);

            cotSearch.updateStatus(CotSearchStatus.ERROR, "", new Date(), false);
            return;
        }

        // Set up delivery transport and send HTTP error message if you can't
        Socket socket = null;
        InetAddress destination = null;
        DatagramSocket datagramSocket = null;

        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("Query ");
        messageBuilder.append(cotSearch.getId());
        messageBuilder.append(" submitted at ");
        messageBuilder.append((new Date()).toString());
        if (numberOfResults == 0) {
            messageBuilder.append("No matching results found.");
        } else {
            if (replayMode) {
                messageBuilder.append(" Replaying ");
            } else {
                messageBuilder.append(" Sending ");
            }
            messageBuilder.append(numberOfResults);
            messageBuilder.append(" results ");
            if (replayMode && Math.abs(replaySpeed - 1.0d) > .0001 ) {
                messageBuilder.append(" at " + (new DecimalFormat("#0.0#")).format(replaySpeed) + "x speed ");
            }
            messageBuilder.append(" to ");
            messageBuilder.append(host);
            messageBuilder.append(":");
            messageBuilder.append(port);

            if (numberOfResults >= 1000) {
                messageBuilder.append(" Due to the large number of results, this query may take a few");
                messageBuilder.append(" seconds to process.");
            }
        }

        long lastServerTime = System.currentTimeMillis();  // Only used in Replay mode

        List<CotEventWrapper> results = new LinkedList<CotEventWrapper>();

        int count = 1;
        int errorCount = 0;

        timeMarker = System.currentTimeMillis();
        try {
            cotSearch.updateStatus(CotSearchStatus.PROCESSING, messageBuilder.toString(), new Date(), true);

            results = getCotEvents(sqlPredicate, sqlParameters, latestOnly, resultLimit, imageOption);
        } catch (SQLException e) {
            logger.error("SQLException exception retrieving CoT data", e);
            cotSearch.setActive(false);
            cotSearch.updateStatus(CotSearchStatus.ERROR, "query " + cotSearch.getId() + e.getClass().getName() + " " + e.getMessage(), new Date(), true);
            return;
        } catch (Exception e) {
            logger.error("exception retrieving CoT data", e);
            cotSearch.updateStatus(CotSearchStatus.ERROR, "query " + cotSearch.getId() + e.getClass().getName() + " " + e.getMessage(), new Date(), true);
            cotSearch.setActive(false);
            return;
        }

        logger.debug("Found " + results.size() + " results in " + (System.currentTimeMillis() - timeMarker) + " ms.");

        if (replayMode) {
            logger.debug("Sending result in REPLAY mode");
            // Reverse the order of the list so events replay in original order
            Collections.reverse(results);

            cotSearch.updateStatus(CotSearchStatus.REPLAYING, "", new Date(), true);
        } else {
            logger.debug("Sending result in SEND mode");


            cotSearch.updateStatus(CotSearchStatus.SENDING, "", new Date(), true);
        }

        if (logger.isDebugEnabled()) {
        	logger.debug("CoT query results to send to " + protocol + " endpoint: " + results);
        }

        for (CotEventWrapper event : results) {
            try {
                if (replayMode) {
                    long currentServerTime = event.receivedTime;
                    long delayMilliseconds = (long)((currentServerTime - lastServerTime) / replaySpeed);
                    logger.debug("delaying " + delayMilliseconds + " milliseconds.");
                    if (delayMilliseconds > 0) {
                        try {
                            Thread.sleep(delayMilliseconds);
                        } catch (InterruptedException ex) {
                            logger.warn("Interrupted while sending results in replay mode: message timing may be incorrect");
                        }
                    }
                    lastServerTime = currentServerTime;
                }

                switch(protocol) {
                case TCP:
                case STCP:
                    socket = new Socket(host, port);
                    socket.getOutputStream().write(event.content.getBytes());
                    socket.getOutputStream().flush();
                    socket.close();
                    break;
                case UDP:
                    if (datagramSocket == null) {
                        destination = InetAddress.getByName(host);
                        datagramSocket = new DatagramSocket();
                    }
                    byte[] payload = event.content.getBytes();
                    if (payload.length > MAXIMUM_DATAGRAM_BYTES) {
                        logger.warn("Query result with primary key " + event.primaryKey + " is oversized; cannot send via UDP.");
                        continue;
                    }
                    DatagramPacket packet = new DatagramPacket(payload, payload.length, destination, port);
                    datagramSocket.send(packet);
                    break;
                }
                logger.debug("Sent result #" + count);
                logger.trace(event.content);
                count++;
            }  catch (IOException ex) {
                String msg = "Failed to send query result to " + host.toString() + ":" + port + ": " + ex.getMessage() + " errorCount: " + errorCount;
                logger.debug(msg, ex);

                cotSearch.updateStatus(CotSearchStatus.ERROR, msg, new Date(), false);

                errorCount++;
                if (errorCount > DELIVERY_EXCEPTION_LIMIT) {
                    break;
                }
            } catch (NullPointerException npe) {
                String msg = "Received a null query result.";

                cotSearch.updateStatus(CotSearchStatus.ERROR, msg, new Date(), true);

                logger.debug(msg, npe);
                errorCount++;
                if (errorCount > DELIVERY_EXCEPTION_LIMIT) {
                    cotSearch.setActive(false);
                    break;
                }
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        logger.warn("Failed to close output socket: " + e.getMessage(), e);
                    }
                }
            }
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.warn("Error closing socket: " + ex.getMessage(), ex);
            }
        }

        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }

        if (!cotSearch.getStatus().equals(CotSearchStatus.ERROR)) {
            cotSearch.updateStatus(CotSearchStatus.DONE, "processing complete", new Date(), false);
        }
    }

    private int countCotEvents(String predicate, List<Object> constraints, boolean latestByUid) throws SQLException, NamingException {
    	StringBuilder queryBuilder = new StringBuilder();
    	queryBuilder.append("SELECT COUNT(");
    	queryBuilder.append(Column.id.toString());
    	queryBuilder.append(") FROM ");

    	if (latestByUid) {
    		queryBuilder.append(LATEST_VIEW_NAME);
    	} else {
    		queryBuilder.append(COT_TABLE_NAME);
    	}

    	if (predicate != null && !predicate.isEmpty()) {
    		queryBuilder.append(" WHERE ");
    		queryBuilder.append(predicate);
    	}
    	queryBuilder.append(";");

    	try (Connection connection = SpringContextBeanForApi.getSpringContext().getBean(DataSource.class).getConnection(); PreparedStatement statement = SpringContextBeanForApi.getSpringContext().getBean(JDBCQueryAuditLogHelper.class).prepareStatement(queryBuilder.toString(), connection)) {

    		int parameterIndex = 1;
    		for (Object constraint : constraints) {
    			if (constraint instanceof Double) {
    				statement.setDouble(parameterIndex, (Double) constraint);
    			} else if (constraint instanceof Integer) {
    				statement.setInt(parameterIndex, (Integer) constraint);
    			} else if (constraint instanceof Timestamp){
    				statement.setTimestamp(parameterIndex, (Timestamp)constraint);
    			} else {
    				statement.setString(parameterIndex, constraint.toString());
    			}
    			parameterIndex++;
    		}

    		logger.debug(statement.toString());
    		try (ResultSet result = statement.executeQuery()) {
    			result.next();
    			int resultCount = result.getInt(1);
    			return resultCount;
    		}
    	}
    }

    /**
     * Retrieves the CoT events from the persistence layer and returns them as Strings.
     *
     * @param predicate Parameterized query predicate specifying which events to retrieve
     * @param constraints Values for the query parameters
     * @param latestByUid if <code>true</code>, return only the latest event for each UID
     * @param limit maximum number of results to return
     * @param imageOption specifies whether to include the full image, thumbnail, or no image in the query results
     * @return a <code>List</code> of CoT events with their associated time stamps. Will be empty if query
     * matched no stored events.
     * @throws SQLException
     * @throws NamingException
     */
    private List<CotEventWrapper> getCotEvents(String predicate, List<Object> constraints, boolean latestByUid, Integer limit, ImageOption imageOption) throws SQLException, NamingException {
        List<CotEventWrapper> events = new LinkedList<CotEventWrapper>();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT *, ST_AsText(");
        queryBuilder.append(Column.event_pt.toString());
        queryBuilder.append(") FROM ");
        if (latestByUid) {
            queryBuilder.append(LATEST_VIEW_NAME);
        } else {
            queryBuilder.append(COT_TABLE_NAME);
        }

        if (predicate != null && !predicate.isEmpty()) {
            queryBuilder.append(" WHERE ");
            queryBuilder.append(predicate);
        }

        // Caution: ResultSender class assumes query results are in descending time order.
        // So if you change the sort order of the query you had better update ResultSender's replay mode.
        queryBuilder.append(" ORDER BY ");
        queryBuilder.append(Column.servertime.toString());
        queryBuilder.append(" DESC ");

        if (limit != null ) {
            queryBuilder.append(" LIMIT ");
            queryBuilder.append(limit);
        }
        queryBuilder.append(";");

        try (Connection connection = SpringContextBeanForApi.getSpringContext().getBean(DataSource.class).getConnection(); PreparedStatement statement = SpringContextBeanForApi.getSpringContext().getBean(JDBCQueryAuditLogHelper.class).prepareStatement(queryBuilder.toString(), connection)) {
        	int parameterIndex = 1;
        	for (Object constraint : constraints) {
        		logger.debug("Setting constraint " + parameterIndex + " = " + constraint.toString());
        		if (constraint instanceof Double) {
        			statement.setDouble(parameterIndex, (Double) constraint);
        		} else if (constraint instanceof Integer) {
        			statement.setInt(parameterIndex, (Integer) constraint);
        		} else if (constraint instanceof Timestamp){
        			statement.setTimestamp(parameterIndex, (Timestamp)constraint);
        		} else {
        			statement.setString(parameterIndex, constraint.toString());
        		}
        		parameterIndex++;
        	}

        	logger.debug(statement.toString());
        	try (ResultSet results = statement.executeQuery()) {

        		String cotString = null;
        		while (results.next()) {
        			try {
        				Document event = CotImageBean.buildCot(results);
        				cotString = event.asXML();
        				Timestamp receivedTime = results.getTimestamp(Column.servertime.toString());
        				int primaryKey = results.getInt(Column.id.toString());

        				if (!Strings.isNullOrEmpty(cotString)) {
        					events.add(new CotEventWrapper(cotString, receivedTime.getTime(), primaryKey));
        				}
        			} catch (Exception ex) {
        				logger.error("Error parsing query result: " + ex.getMessage(), ex);
        			}
        		}
        	}
        }
        logger.debug("Found " + events.size() + " results.");
        return events;
    }
}
