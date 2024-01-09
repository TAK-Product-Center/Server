package tak.server.feeds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteAtomicReference;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.configuration.CollectionConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.ignite.IgniteHolder;

/**
 * Helper API class to update and retrieve DataFeedStats and related data structures for the DataFeeds
 * in the system.
 */
public class DataFeedStatsHelper  {

    private static DataFeedStatsHelper instance = null;
    private static final Logger logger = LoggerFactory.getLogger(tak.server.feeds.DataFeedStatsHelper.class);
    private Ignite ignite = null;
    private CollectionConfiguration coCfg = null;

    /**
     * Constructor.  Initializes and customizes configuration for collections used in DataFeedStats.
     */
    private DataFeedStatsHelper()
    {
        ignite = IgniteHolder.getInstance().getIgnite();
        this.coCfg = new CollectionConfiguration();
    }

    public static DataFeedStatsHelper getInstance() {
        if (instance == null) {
            synchronized(DataFeedStatsHelper.class) {
                if (instance == null) {
                    instance = new DataFeedStatsHelper();
                }
            }
        }

        return instance;
    }


    /**
     *  Returns a DataFeedStats instance with the latest statistics of the messages for a given DataFeed.
      * @param dfUUID The unique ID of the DataFeed whose stats are being retrieved.
     * @return a DataFeedStats with the statistics for the given DataFeed or null of there haven't been any messages
     * for the given DataFeed unique ID.
     */
    public DataFeedStats getLatestDataFeedStats(String dfUUID) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Getting the Ignite AtomicLongs and AtomicReferences for DataFeed (." + dfUUID + ")");
            }

            IgniteSet<String> dfCotTypes = ignite.set(getDataFeedCotTypesSetKey(dfUUID), coCfg);
            IgniteAtomicLong dfFirstMsgMillis = ignite.atomicLong(getDataFeedFirstMsgMillisKey(dfUUID), 0, false);
            IgniteAtomicLong dfNumBytes = ignite.atomicLong(getDataFeedNumBytesKey(dfUUID), 0, false);
            IgniteAtomicLong dfNumMessages = ignite.atomicLong(getDataFeedNumMessagesKey(dfUUID), 0, false);
            IgniteAtomicReference<Double> dfMinLat = ignite.atomicReference(getDataFeedMinLatKey(dfUUID), (Double)null, false);
            IgniteAtomicReference<Double> dfMaxLat = ignite.atomicReference(getDataFeedMaxLatKey(dfUUID), (Double)null, false);
            IgniteAtomicReference<Double> dfMinLon = ignite.atomicReference(getDataFeedMinLonKey(dfUUID), (Double)null, false);
            IgniteAtomicReference<Double> dfMaxLon = ignite.atomicReference(getDataFeedMaxLonKey(dfUUID), (Double)null, false);


            if (logger.isTraceEnabled()) {
                logger.trace("Assembling the DataFeedStats instance from the data");
            }

            DataFeedStats dataFeedStats = new DataFeedStats(dfUUID, dfCotTypes, dfMinLat.get().doubleValue(),
                    dfMinLon.get().doubleValue(), dfMaxLat.get().doubleValue(), dfMaxLon.get().doubleValue(),
                    dfFirstMsgMillis.get(), System.currentTimeMillis(), dfNumBytes.get(), dfNumMessages.get());

            return dataFeedStats;
        }
        catch(IgniteException ie) {
            logger.error("Unable to get Ignite Atomic References for latest DataFeedStats for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return null;
        }
    }

    /**
     * Add DataFeedStats for a new message for a DataFeed. Also lazy initializes the IgniteQueue of DataFeedStats instances
     * and the IgniteAtomicReference toi track the DataFeedStats for the last DataFeed message.
     * @param dfUUID the unique ID for the DataFeed whose message was received
     * @param dfCotType the set of unique CoT Types for messages on the DataFeed
     * @param dfLat the Latitude associated withe message.
     * @param dfLon the Longitude associated withe message.
     * @param dfMsgMillis the Creation Time in milliseconds since Epoch associated with the message.
     * @param dfMsgBytes the byte size of the message.
     * @return true if the new DataFeedStats was created and added successfully to the Ignite Queue and Atomic Ref.
     * Otherwise returns false.
     */
    public boolean addStatsForDataFeedMessage(String dfUUID, String dfCotType, double dfLat, double dfLon,
                                              long dfMsgMillis, int dfMsgBytes) {
        // We store the the List of unique DataFeed UUIDs in a Set.
        IgniteSet<String> dataFeedUUIDSet = null;
        IgniteSet<String> dfCotTypes = null;
        IgniteAtomicLong dfFirstMsgMillis = null;
        IgniteAtomicLong dfLastMsgMillis = null;
        IgniteAtomicLong dfNumBytes = null;
        IgniteAtomicLong dfNumMessages = null;
        IgniteAtomicReference<Double> dfMinLat = null;
        IgniteAtomicReference<Double> dfMaxLat = null;
        IgniteAtomicReference<Double> dfMinLon = null;
        IgniteAtomicReference<Double> dfMaxLon = null;

        if (logger.isTraceEnabled()) {
            logger.trace("Getting the Ignite Set for of all DataFeed UUIDs.");
        }
        try {
            dataFeedUUIDSet = ignite.set(getDataFeedSetKey(), coCfg);
            dataFeedUUIDSet.add(dfUUID);
        } catch(IgniteException ie) {
            logger.error("Unable to create or retrieve Ignite Set for DataFeedUUIDs.- {}", ie.getMessage());
            return false;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Updating the Ignite variables for Datafeed UUID: {}.", dfUUID);
        }

        try {
            dfNumMessages = ignite.atomicLong(getDataFeedNumMessagesKey(dfUUID), 0, true);
            dfNumMessages.incrementAndGet();
        } catch(IgniteException ie) {
            logger.error("Unable to update the number of messages for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return false;
        }
        try {
            dfNumBytes = ignite.atomicLong(getDataFeedNumBytesKey(dfUUID), 0, true);
            dfNumBytes.addAndGet(dfMsgBytes);
        } catch(IgniteException ie) {
            logger.error("Unable to update the number of bytes received for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return false;
        }
        try {
            dfLastMsgMillis = ignite.atomicLong(getDataFeedLastMsgMillisKey(dfUUID), dfMsgMillis, true);
            dfLastMsgMillis.getAndSet(dfMsgMillis);
        } catch(IgniteException ie) {
            logger.error("Unable to create the Ignite reference to the latest msg creation time  for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return false;
        }
        try {
            dfFirstMsgMillis = ignite.atomicLong(getDataFeedFirstMsgMillisKey(dfUUID), dfMsgMillis-1L, true);
        } catch(IgniteException ie) {
            logger.error("Unable to create the Ignite reference to the first msg creation time for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return false;
        }

        try {
            dfCotTypes = ignite.set(getDataFeedCotTypesSetKey(dfUUID), coCfg);
            dfCotTypes.add(dfCotType);
        } catch(IgniteException ie) {
            logger.error("Unable to update the current CotTypes for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return false;
        }

        try {
            Double latObj = Double.valueOf(dfLat);
            Double lonObj = Double.valueOf(dfLon);
            dfMinLat = ignite.atomicReference(getDataFeedMinLatKey(dfUUID), latObj, true);
            if (latObj.compareTo(dfMinLat.get()) < 0)
                dfMinLat.set(latObj);
            dfMaxLat = ignite.atomicReference(getDataFeedMaxLatKey(dfUUID), latObj, true);
            if (latObj.compareTo(dfMaxLat.get()) > 0)
                dfMaxLat.set(latObj);
            dfMinLon = ignite.atomicReference(getDataFeedMinLonKey(dfUUID), lonObj, true);
            if (lonObj.compareTo(dfMinLon.get()) < 0)
                dfMinLon.set(lonObj);
            dfMaxLon = ignite.atomicReference(getDataFeedMaxLonKey(dfUUID), lonObj, true);
            if (lonObj.compareTo(dfMaxLon.get()) > 0)
                dfMaxLon.set(lonObj);
        } catch(IgniteException ie) {
            logger.error("Unable to update the min/max for Lat/Lon for Datafeed UUID: {} - {}", dfUUID, ie.getMessage());
            return false;
        }

        return true;
    }

    public List<DataFeedStats> getAllLatestDataFeedStats()  {
        List<DataFeedStats> dataFeedStatsList = new ArrayList<DataFeedStats>();

        try {
            // We store the List of unique DataFeed UUIDs in a Set.
            IgniteSet<String> dataFeedUUIDSet = ignite.set(getDataFeedSetKey(), coCfg);

            for (String dfUUID: dataFeedUUIDSet) {
                DataFeedStats latestStats = getLatestDataFeedStats(dfUUID);
                if (latestStats != null) {
                    dataFeedStatsList.add(latestStats);
                }
            }
        } catch(IgniteException ie) {
            logger.error("Unable to iterate through DataFeed UUID Set to get latest DataFeedStats for each DataFeed. - {}", ie.getMessage());
        }

        return dataFeedStatsList;
    }

    /*
     * Avoid mistyping.  Centralize the key generation.
     */
    public String getDataFeedCotTypesSetKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedCotTypes"; }

    public String getDataFeedFirstMsgMillisKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedFirstMsgMillis"; }

    public String getDataFeedLastMsgMillisKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedLastMsgMillis"; }

    public String getDataFeedNumMessagesKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedNumMessages"; }

    public String getDataFeedNumBytesKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedNumBytes"; }

    public String getDataFeedMinLatKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedMinLat"; }

    public String getDataFeedMaxLatKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedMaxLat"; }

    public String getDataFeedMinLonKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedMinLon"; }

    public String getDataFeedMaxLonKey(String dfUUID) { return "dfsHelper-" + dfUUID + "-DataFeedMaxLon"; }

    public String getDataFeedSetKey() { return "dfsHelper-DataFeed-Set";  }
}
