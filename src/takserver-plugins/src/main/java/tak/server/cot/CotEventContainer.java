

package tak.server.cot;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.util.DateUtil;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.util.NumericUtil;

public class CotEventContainer extends XmlContainer implements Serializable {
	
	private static final Logger logger = LoggerFactory.getLogger(CotEventContainer.class);

	private static final long serialVersionUID = 2259021466152578797L;

	public interface ContextInitializer {
		public Object initialize();
	}

	public static enum ContextKey {
		PRIMARY_KEY ("primary_key"),
		IMAGE_KEY   ("image"),
		THUMB       ("thumb");

		private final String key;

		ContextKey(String key) {
			this.key = key;
		}

		public String getKey() {
			return key;
		}
	}

	public static final String PRIMARY_KEY = "primary_key";
	public static final String IMAGE_KEY = "image";
	public static final String THUMB_KEY = "thumb";

	public static final String CACHE_HIT_TIME_KEY = "cache_hit_time";
	public static final String CACHE_IMAGE_KEY = "cache_image";
	public static final String CACHE_IMAGE_HIT_TIME_KEY = "cache_image_hit_time";

	protected String uid = null;
	protected byte[] encoding = null;
	protected boolean hasServerTime = false;
	
	private long start = -1L;
	private long stale = -1L;
	private long time = -1L;
	
	private double lat = -1.0;
	private double lon = -1.0;
	private double hae = -1.0;
	private double ce = -1.0;
	private double le = -1.0;
	private double speed = -1.0;
	private double course = -1.0;
	
	// Timestamp for application-level message submission (See SubmissionService.java)
	private Date submissionTime = null;
	
	private long creationTime = -1;

	private int battery = -1;
	
	private String type = null;
	
	private String endpoint = null;
	
	// Flag to track whether this is a new message, or stored message (as is the case with Latest SA)
	private boolean stored = false;
	
	private ByteBuffer protoBufBytes = null;
	
	public CotEventContainer() {
		super();
		if (Strings.isNullOrEmpty(this.uid)) {
			setUid(getRootAttribute("uid"));
		}
		
		if (creationTime == -1) {
			creationTime = new Date().getTime();
		}
	}

	public CotEventContainer(CotEventContainer src) {
		super(src);
		copyInstanceVariables(src);
		if (Strings.isNullOrEmpty(this.uid)) {
			setUid(getRootAttribute("uid"));
		}
		if (creationTime == -1) {
			creationTime = new Date().getTime();
		}
	}

	// Copy the object, optionally with the XML document object only, and no context key-value map
	public CotEventContainer(CotEventContainer src, boolean copyContext) {
		
		if (logger.isTraceEnabled()) {
			logger.trace("message keys on full copy: " + src.getContext().keySet(), new Exception("full copy"));
		}

		if (copyContext) {
			context = new ConcurrentHashMap<>(src.context);
		} else {
			context = new ConcurrentHashMap<>();
		}
		doc = (Document) src.doc.clone();
		copyInstanceVariables(src);
		
		if (Strings.isNullOrEmpty(this.uid)) {
			setUid(getRootAttribute("uid"));
		}
		if (creationTime == -1) {
			creationTime = new Date().getTime();
		}
	}

	// Copy the object, optionally with the XML document object only, and either empty context key-value map or allow certain keys to be removed
	public CotEventContainer(CotEventContainer src, boolean copyContext, Set<String> ignoreKeys) {
		this(src, copyContext);
		
		// clear the specified context keys
		if (copyContext && ignoreKeys != null) {
			ignoreKeys.forEach((key) -> context.remove(key));
		}
		
		if (logger.isTraceEnabled()) {
			logger.trace("message keys on ignorekeys " + ignoreKeys + " copy: " + getContext().keySet(), new Exception("full copy"));
		}
		if (Strings.isNullOrEmpty(this.uid)) {
			setUid(getRootAttribute("uid"));
		}
		if (creationTime == -1) {
			creationTime = new Date().getTime();
		}
	}

	public CotEventContainer(Document xml) {
		super(xml);
		if (Strings.isNullOrEmpty(this.uid)) {
			setUid(getRootAttribute("uid"));
		}
		if (creationTime == -1) {
			creationTime = new Date().getTime();
		}
	}

	public String getAccess() {
		return getRootAttribute("access");
	}

	public String getCallsign() {
		Attribute callsignAttr = (Attribute) doc.selectSingleNode("/event/detail/contact/@callsign");
		if (callsignAttr != null) {
			return callsignAttr.getValue();
		} else {
			return null;
		}
	}

	public Double getCe() {
		return NumericUtil.parseDoubleOrDefault(getPointAttribute("ce"), 999999);
	}

	public String getDetailXml() {
		Element detailElem = (Element) doc.selectSingleNode("/event/detail");
		if (detailElem != null) {
			return detailElem.asXML();
		} else {
			return null;
		}
	}

	public String getEndpoint() {

		if (endpoint == null) {
			synchronized(this) {
				if (endpoint == null) {
					Attribute endpointAttr = (Attribute) doc.selectSingleNode("/event/detail/contact/@endpoint");
					if (endpointAttr != null) {
						endpoint = endpointAttr.getValue();
					} 
				}
			}			
		}

		return endpoint;
	}

	public Double getHae() { return NumericUtil.parseDoubleOrDefault(getPointAttribute("hae"), 0); }

	public String getHow() {
		return getRootAttribute("how");
	}

	public String getLat() {
		return getPointAttribute("lat");
	}

	public Double getLe() {
		return NumericUtil.parseDoubleOrDefault(getPointAttribute("le"), 0);
	}

	public String getLon() {
		return getPointAttribute("lon");
	}

	public String getOpex() {
		return getRootAttribute("opex");
	}

	private String getPointAttribute(String attribute) {
		return doc.getRootElement().element("point").attributeValue(attribute);
	}

	public String getQos() {
		return getRootAttribute("qos");
	}

	private String getRootAttribute(String attribute) {
		return doc.getRootElement().attributeValue(attribute);
	}

	public String getStale() {
		return getRootAttribute("stale");
	}

	public String getStart() {
		return getRootAttribute("start");
	}

	public String getTime() {
		return getRootAttribute("time");
	}

	public String getType() {
		
		if (type == null) {
			synchronized (this) {
				if (type == null) {
					type = getRootAttribute("type");
				}
			}
		}
		
		return type; 
	}

	public String getUid() {
		return !Strings.isNullOrEmpty(uid) ? uid : getRootAttribute("uid");
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setStale(String stale) {
		doc.getRootElement().addAttribute("stale", stale);
	}

	public void setTime(String time) {
		doc.getRootElement().addAttribute("time", time);
		
		try {
			this.time = Long.parseLong(time);
		} catch (Exception e) {	}
	}

	public void setServerTime(String time) {
		setTime(time);
		hasServerTime = true;
	}

	public boolean hasServerTime() {
		return this.hasServerTime;
	}
	
	public void setType(String type) {
		doc.getRootElement().addAttribute("type", type);
	}

	public CotEventContainer copy() {
		return new CotEventContainer(this);
	}

	public String partial() {
		return "CoT msg: type: " + this.getType() + " uid: " + this.getUid() + " sender: " + this.getContext(Constants.SOURCE_TRANSPORT_KEY);
	}
	public byte[] getEncoding() {
		return this.encoding;
	}

	public byte[] getOrInstantiateEncoding() {
		if (this.encoding == null) {
			this.encoding = this.asXml().getBytes();
		}

		return this.encoding;
	}

	ByteBuffer bufferEncoding = null;

	public ByteBuffer getOrInstantiateBufferEncoding(Charset encoder) {
		if (this.bufferEncoding == null) {
			this.bufferEncoding = encoder.encode(this.asXml());
		}

		return bufferEncoding.duplicate();
	}

	public ByteBuffer getOrInstantiateBufferEncoding() {
		return getOrInstantiateBufferEncoding(Charsets.UTF_8);
	}

	public CotElement toCotElement() {

		CotElement cotElement = new CotElement();

		cotElement.uid = getUid();
		cotElement.lon = Double.parseDouble(getLon());
		cotElement.lat = Double.parseDouble(getLat());
		cotElement.cottype = getType();
		cotElement.le = getLe();
		cotElement.detailtext = getDetailXml();
		cotElement.how = getHow();
		cotElement.ce = getCe();

		cotElement.setHae(getHae());
		cotElement.setServertime(DateUtil.millisFromCotTimeStr(getTime()));
		cotElement.setStaletime(DateUtil.millisFromCotTimeStr(getStale()));

		return cotElement;
	}
	
	public long getStaleLong() {
		return this.stale;
	}

	public long getStartLong() {
		return this.start;
	}
	
	public long getTimeLong() {
		return this.time;
	}
	
	public void setTimeLong(long time) {
		this.time = time;
	}
	
	public void setStaleLong(long stale) {
		this.stale = stale;
	}
	
	public void setStartLong(long start) {
		this.start = start;
	}
	
	public double getLatDouble() {
		return this.lat;
	}

	public double getLonDouble() {
		return this.lon;
	}
	
	public double getHaeDouble() {
		return this.hae;
	}
	
	public void setLatDouble(double lat) {
		this.lat = lat;
	}
	
	public void setLonDouble(double lon) {
		this.lon = lon;
	}
	
	public void setHaeDouble(double hae) {
		this.hae = hae;
	}
	
	public double getCeDouble() {
		return this.ce;
	}

	public double getLeDouble() {
		return this.le;
	}
	
	public double getSpeedDouble() {
		return this.speed;
	}
	
	public void setCeDouble(double ce) {
		this.ce = ce;
	}
	
	public void setLeDouble(double le) {
		this.le = le;
	}
	
	public void setSpeedDouble(double speed) {
		this.speed = speed;
	}
	
	public double getCourseDouble() {
		return this.course;
	}
	
	public void setCourseDouble(double course) {
		this.course = course;
	}
	
	public int getBatteryInt() {
		return this.battery;
	}
	
	public void setBatteryInt(int battery) {
		this.battery = battery;
	}
	
	public boolean isStored() {
		return stored;
	}

	public void setStored(boolean stored) {
		this.stored = stored;
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}
	
	public Date getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(Date submissionTime) {
		this.submissionTime = submissionTime;
	}
	
	public ByteBuffer getProtoBufBytes() {
		return protoBufBytes;
	}

	public void setProtoBufBytes(ByteBuffer protoBufBytes) {
		this.protoBufBytes = protoBufBytes;
	}

	private void copyInstanceVariables(CotEventContainer toCopy) {
		this.start = toCopy.getStartLong();
		this.stale = toCopy.getStaleLong();
		this.time = toCopy.getTimeLong();
		
		this.lat = toCopy.getLatDouble();
		this.lon = toCopy.getLonDouble();
		this.hae = toCopy.getHaeDouble();
		this.ce = toCopy.getCeDouble();
		this.le = toCopy.getLeDouble();
		this.speed = toCopy.getSpeedDouble();
		this.course = toCopy.getCourseDouble();

		this.battery = toCopy.getBatteryInt();
		this.stored = toCopy.isStored();
		
		this.submissionTime = toCopy.getSubmissionTime();
		this.creationTime = toCopy.creationTime;
		this.protoBufBytes = toCopy.getProtoBufBytes();
	}
	
	public CotElement asCotElement() {
		CotElement element = new CotElement();

		element.uid = getUid();
		element.lon = lon;
		element.lat = lat;
		element.hae = ((Double) hae).toString();
		element.cottype = getType();
		element.servertime = new Timestamp(this.getTimeLong());
		element.le = le;
		element.detailtext = this.getDetailXml();
		element.cotId = null;
		element.how = getHow();
		element.staletime = new Timestamp(getStaleLong());
		element.ce = this.getCeDouble();

		return element;
	}
	
	
}
