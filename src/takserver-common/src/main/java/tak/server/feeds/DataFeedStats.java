package tak.server.feeds;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Class: DataFeedStats
 * Description: Encapsulates the statistics (including message and size rates) for a given DataFeed at some point in time.
 *              Initialized at construction time and not intended to have setters.
 */
public class DataFeedStats implements Serializable
{
    private String dataFeedUUID = null;

    private HashSet<String> dataFeedCotTypes = null;

    private double dataFeedMinLat = 0.0;

    private double dataFeedMinLon = 0.0;

    private double dataFeedMaxLat = 0.0;

    private double dataFeedMaxLon = 0.0;

    private long dataFeedMsgMillis = 0L;

    private long dataFeedNumMessages = 0L;

    private long dataFeedFirstMsgMillis = 0L;

    private long dataFeedTotalByteSize = 0L;

    private float dataFeedMsgAvgRatePerSec = 0.0f;

    private float dataFeedMsgAvgBytesPerSec = 0.0f;

    /**
     * Constructor
     * @param dfUUID The unique identifier of the DataFeed
     * @param dfCotTypes The super-set of cot-types for messages associated with this DataFeed
     * @param dfMinLat The minimum latitude of the bounding box containing all messages for this DataFeed
     * @param dfMinLon The minimum longitude of the bounding box containing all messages for this DataFeed
     * @param dfMaxLat The maximum latitude of the bounding box containing all messages for this DataFeed
     * @param dfMaxLon The maximum longitude of the bounding box containing all messages for this DataFeed
     * @param dfCurMsgMillis The current message's creation time in milliseconds
     * @param dfNumMessages The total number of messages (including this one) for the DataFeed
     * @param dfFirstMsgMillis The time of the DataFeed's first message creation (for calculating the average rates)
     * @param dfTotalByteSize The total bytes sent through the DataFeed (for calculating average byte rate)
     */
    public DataFeedStats(String dfUUID, Set<String> dfCotTypes, double dfMinLat, double dfMinLon, double dfMaxLat,
                         double dfMaxLon, long dfFirstMsgMillis, long dfCurMsgMillis, long dfTotalByteSize,
                         long dfNumMessages)
    {
        float millisDiff = (float)(dfCurMsgMillis - dfFirstMsgMillis);

        this.dataFeedUUID = dfUUID;
        this.dataFeedCotTypes = new HashSet<String>(dfCotTypes);
        this.dataFeedMinLat = dfMinLat;
        this.dataFeedMinLon = dfMinLon;
        this.dataFeedMaxLat = dfMaxLat;
        this.dataFeedMaxLon = dfMaxLon;
        this.dataFeedMsgMillis = dfCurMsgMillis;
        this.dataFeedNumMessages = dfNumMessages;
        this.dataFeedFirstMsgMillis = dfFirstMsgMillis;
        this.dataFeedTotalByteSize = dfTotalByteSize;
        this.dataFeedMsgAvgRatePerSec = dfNumMessages * 1000.0f / millisDiff;
        this.dataFeedMsgAvgBytesPerSec = ((float)(dfTotalByteSize) * 1000.0f) / millisDiff;
    }

    public String getDataFeedUUID() {
        return dataFeedUUID;
    }

    public Set<String> getDataFeedCotTypes() {
        return dataFeedCotTypes;
    }

    public double getDataFeedMinLat() { return dataFeedMinLat; }

    public double getDataFeedMinLon() { return dataFeedMinLon; }

    public double getDataFeedMaxLat() { return dataFeedMaxLat; }

    public double getDataFeedMaxLon() { return dataFeedMaxLon; }

    /**
     * Convenience method to return the extents as a string containing the points (comma-separated) defining the bounding box
     * @return a string representing the extents bounding box as 4 comma-separated values
     */
    public String getDataFeedExtents() {
        return String.format("%f,%f,%f,%f", dataFeedMinLat, dataFeedMinLon, dataFeedMaxLat, dataFeedMaxLon);
    }

    public long getDataFeedMsgMillis() { return dataFeedMsgMillis; }

    public long getDataFeedNumMessages() { return dataFeedNumMessages; }

    public long getDataFeedFirstMsgMillis() { return dataFeedFirstMsgMillis; }

    public long getDataFeedTotalByteSize() { return dataFeedTotalByteSize; }

    public float getDataFeedMsgAvgRatePerSec() {
        return dataFeedMsgAvgRatePerSec;
    }

    public float getDataFeedMsgAvgBytesPerSec() {
        return dataFeedMsgAvgBytesPerSec;
    }

    @java.lang.Override
    public java.lang.String toString() {
        final java.lang.StringBuilder sb = new java.lang.StringBuilder("DataFeedStats{");
        sb.append("dataFeedUUID='").append(dataFeedUUID).append('\'');
        sb.append(", dataFeedCotTypes=").append(dataFeedCotTypes);
        sb.append(", dataFeedMinLat=").append(dataFeedMinLat);
        sb.append(", dataFeedMinLon=").append(dataFeedMinLon);
        sb.append(", dataFeedMaxLat=").append(dataFeedMaxLat);
        sb.append(", dataFeedMaxLon=").append(dataFeedMaxLon);
        sb.append(", dataFeedMsgMillis=").append(dataFeedMsgMillis);
        sb.append(", dataFeedNumMessages=").append(dataFeedNumMessages);
        sb.append(", dataFeedFirstMsgMillis=").append(dataFeedFirstMsgMillis);
        sb.append(", dataFeedTotalByteSize=").append(dataFeedTotalByteSize);
        sb.append(", dataFeedMsgAvgRatePerSec=").append(dataFeedMsgAvgRatePerSec);
        sb.append(", dataFeedMsgAvgBytesPerSec=").append(dataFeedMsgAvgBytesPerSec);
        sb.append('}');
        return sb.toString();
    }
}