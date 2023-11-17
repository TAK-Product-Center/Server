package tak.server.federation.message;

@SuppressWarnings({"PMD.VariableNamingConventions"})
public class MetadataConstants {

	public static final String Message_UUID = "messageUUID";
	public static final String NEXT_IFP_NODE = "nextIFPnodeID";
	public static final String Destinations = "destinations"; // list of RogerConnections
	public static final String ExpiredMessage = "isExpired";

	/** Set in Message constructor with System.currentTimeMillis */
	public static final String TimeCreated = "create.timestamp";
	public static final String FlowID = "flowID";
	public static final String MessageDeadline = "deadline";
	public static final String MIME_Type = "MIME_Type";

	public static final String IsDataStoreQuery = "isDataStoreQuery";
	//Protocol("protocol") -- replaced by Source
	public static final String PayloadSize = "payloadSize"; //in bytes
	public static final String Priority = "priority";
	public static final String Source = "source"; // RogerConnection source
	public static final String Subject = "subject";
	public static final String CertPath = "certPath";

	/**
	 * String of comma-separated content keywords, or "tags"
	 */
	public static final String ContentKeywords = "contentKeywords";
	/**
	 * should be a valid UUID as a String
	 */
	public static final String ParentMessageID = "parentMessageID";
	/**
	 * supports multi-layer children by recursively keeping parent's parent (or parent's parent's parent's, etc) metadata in this field.
	 * Currently expected to be a Map{@literal <}String,Object{@literal >}, which will only work for in-process plugins
	 */
	public static final String ParentMetadata = "parentMetadata";
	public static final String NumChildrenCreated = "numChildrenCreated";
	public static final String ChildMessageIndex = "childMessageIndex";

	public static final String StreamUUID = "streamUUID";
	public static final String SequenceNumber = "sequenceNumber";

	public static final String Filename = "fileName";

	public static final String IsAlreadyProcessed = "isAlreadyProcessed";

	public static final String NumSendAttempts = "numSendAttempts";

	public static final String HttpRequestURI = "http request uri";

	/** Expected to be a String[] */
	public static final String HttpHeaders = "http_headers";
	public static final String SingleShotMessage = "single_shot_message";


	public static final String  Hash = "hash";
	public static final String Callsign = "callsign";


}
