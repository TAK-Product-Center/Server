

package com.bbn.marti;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.naming.NamingException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.dao.kml.IconRepository;
import com.bbn.marti.dao.kml.KMLDao;
import com.bbn.marti.kml.icon.api.IconsetIconApi;
import com.bbn.marti.model.kml.Icon;
import com.bbn.marti.model.kml.Icon.IconParts;
import com.bbn.marti.remote.exception.GroupForbiddenException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.service.kml.KMLService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.Coord;
import com.bbn.marti.util.KmlUtils;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.bbn.security.web.MartiValidatorConstants;
import com.bbn.security.web.MartiValidatorConstants.Regex;
import com.google.common.base.Strings;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Schema;
import de.micromata.opengis.kml.v_2_2_0.gx.SimpleArrayField;
import tak.server.Constants;
import tak.server.cot.CotElement;
import tak.server.util.Association;

public class MissionKMLServlet extends LatestKMLServlet {
	
	
	private class TimeInterval {
		public TimeInterval(Timestamp start, Timestamp stop) {
			begin = start;
			end = stop;
		}
		protected Timestamp begin;
		protected Timestamp end;
	}
	
	protected class KMLServletParameterException extends IllegalArgumentException {
		private static final long serialVersionUID = 7865175209958637387L;
		
		public KMLServletParameterException(String msg) {
			super(msg);
		}
	}
	
    private static final long serialVersionUID = -8497312774594243328L;
    private static final String CONTEXT = "ExportMissionKML";
    public static SimpleDateFormat cotDateFormat = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MissionKMLServlet.class);

    @Autowired
    private IconRepository iconRepository;

    @Autowired
	private JDBCQueryAuditLogHelper wrap;
    
    @Autowired
    private DataSource ds;
	
    @Autowired
    private KMLService kmlService;
    
    @Autowired
    private KMLDao kmlDao;
    
    @Autowired
    private GroupManager groupManager;
    
    @Autowired
    private CommonUtil martiUtil;

    @Override
    protected void initalizeEsapiServlet() {
        log = Logger.getLogger(MissionKMLServlet.class.getCanonicalName());
        requiredHttpParameters = new HashMap<String, HttpParameterConstraints>();
        optionalHttpParameters = new HashMap<String, HttpParameterConstraints>();
        
        optionalHttpParameters.put(QueryParameter.startTime.name(), 
                new HttpParameterConstraints(Regex.Timestamp, MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(QueryParameter.endTime.name(), 
                new HttpParameterConstraints(Regex.Timestamp, MartiValidatorConstants.DEFAULT_STRING_CHARS));
        optionalHttpParameters.put(QueryParameter.interval.name(), 
        		new HttpParameterConstraints(Regex.Double, MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(QueryParameter.refreshRate.name(), 
                new HttpParameterConstraints(MartiValidatorConstants.Regex.NonNegativeInteger, MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put(QueryParameter.format.name(), 
                new HttpParameterConstraints(MartiValidatorConstants.Regex.SafeString, MartiValidatorConstants.SHORT_STRING_CHARS));
        optionalHttpParameters.put("uid",
                new HttpParameterConstraints(MartiValidatorConstants.Regex.MartiSafeString, MartiValidatorConstants.DEFAULT_STRING_CHARS));

        optionalHttpParameters.put(QueryParameter.multiTrackThreshold.name(), 
				new HttpParameterConstraints(MartiValidatorConstants.Regex.NonNegativeInteger, MartiValidatorConstants.SHORT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.extendedData.name(), 
				new HttpParameterConstraints(MartiValidatorConstants.Regex.SafeString, MartiValidatorConstants.SHORT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.optimizeExport.name(), 
				new HttpParameterConstraints(MartiValidatorConstants.Regex.SafeString, MartiValidatorConstants.SHORT_STRING_CHARS));
		optionalHttpParameters.put(QueryParameter.groups.name(), 
                new HttpParameterConstraints(MartiValidatorConstants.Regex.MartiSafeString, MartiValidatorConstants.DEFAULT_STRING_CHARS));
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

    	initAuditLog(request);

    	cotDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));

    	// Process request parameters
    	Map<String, String[]> httpParameters = validateParams("MissionKMLServlet", request, response, 
    			requiredHttpParameters, optionalHttpParameters);

    	if (httpParameters == null)
    		return;

    	String intervalParam = getParameterValue(httpParameters, QueryParameter.interval.name());
    	String startParam = getParameterValue(httpParameters, QueryParameter.startTime.name());
    	String endParam = getParameterValue(httpParameters, QueryParameter.endTime.name());
    	String uidParam = getParameterValue(httpParameters, "uid");

    	String multiTrackThreshold = getParameterValue(httpParameters, "multiTrackThreshold");
    	if (multiTrackThreshold == null || multiTrackThreshold.trim().length() == 0) {
    		multiTrackThreshold = "0";
    	}

    	String extendedData = getParameterValue(httpParameters, "extendedData");
    	String optimizeExport = getParameterValue(httpParameters, "optimizeExport");

    	if (validator != null) {
    		HttpParameterConstraints constraints = null;
    		try {

    			//logger.info("Parsing starting start time");
    			constraints = optionalHttpParameters.get(QueryParameter.startTime.name());
    			if (constraints != null) {
    				startParam = validator.getValidInput(CONTEXT, startParam, constraints.validationPattern.name(), 
    						constraints.maximumLength, true);
    			}
    			constraints = optionalHttpParameters.get(QueryParameter.endTime.name());
    			if (constraints != null) {
    				endParam = validator.getValidInput(CONTEXT, endParam, 
    						constraints.validationPattern.name(), constraints.maximumLength, true);
    			}
    			constraints = optionalHttpParameters.get(QueryParameter.interval.name());
    			if (constraints != null) {
    				intervalParam = validator.getValidInput(CONTEXT, intervalParam, constraints.validationPattern.name(),
    						constraints.maximumLength, true);
    			}
    			constraints = optionalHttpParameters.get("uid");
    			if (constraints != null) {
    				uidParam = validator.getValidInput(CONTEXT, uidParam,
    						constraints.validationPattern.name(), constraints.maximumLength, true);
    			}

    			constraints = optionalHttpParameters.get(QueryParameter.multiTrackThreshold.name());
    			if (constraints != null) {
    				multiTrackThreshold = validator.getValidInput(CONTEXT, multiTrackThreshold, constraints.validationPattern.name(),
    						constraints.maximumLength, true);
    			}

    			// TODO: validate groups

    		} catch (ValidationException ex) {
    			log.warning(ex.getMessage());
    			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " 
    					+ ((Regex)constraints.validationPattern).name() + " format detected.");
    			return;
    		} catch (IntrusionException ex) {
    			log.severe(ex.getMessage());
    			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " 
    					+ ((Regex)constraints.validationPattern).name() + " format detected.");
    			return;
    		} catch (Exception ex) {
    			log.severe(ex.getMessage());
    			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
    					"Error parsing HTTP request parameters.");
    		}
    	} 

    	Double interval = null;
    	if (intervalParam != null) {
    		try {
    			interval = Double.parseDouble(intervalParam);
    		} catch (NumberFormatException ex) {
    			log.warning("Invalid numeric string for parameter 'interval', double value is required");
    			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric format for time interval.");
    		}
    	}

    	Integer multiTrackThresholdAsInteger = null;
    	if (multiTrackThreshold != null) {
    		try {
    			multiTrackThresholdAsInteger = Integer.parseInt(multiTrackThreshold, 10);
    		} catch (NumberFormatException ex) {
    			log.warning("Invalid numeric string for parameter 'multiTrachThreshold', integer value required");
    			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid numeric format for time interval.");
    		}
    	}

    	boolean includeExtendedData = false;
    	if (extendedData != null && extendedData.equalsIgnoreCase("true")) {
    		includeExtendedData = true;
    	}

    	boolean optimizeExportAsBoolean = true; //Default to true to maintain backward compatibility
    	if (optimizeExport != null && optimizeExport.equalsIgnoreCase("false")) {
    		optimizeExportAsBoolean = false; 
    	}

    	AllowedFormat format = AllowedFormat.kml;
    	String formatParam = getParameterValue(httpParameters, QueryParameter.format.name());

    	if (formatParam != null && formatParam.compareToIgnoreCase("kmz") == 0) {
    		format = AllowedFormat.kmz;
    	}

    	String[] gv = request.getParameterValues("groups");

    	NavigableSet<Group> filterGroups = new ConcurrentSkipListSet<>();
    	if (gv != null) {

    		// populate the specified group with their bit vector
    		for (String groupName : gv) {
    			filterGroups.add(groupManager.hydrateGroup(new Group(groupName, Direction.OUT)));
    		}
    	}

    	NavigableSet<Group> userGroups = martiUtil.getUserGroups(request);

    	logger.debug("groups for current user: " + userGroups);

    	if (!martiUtil.isAdmin()) {
    		// validate membership in the requested filter groups. Let admin filter on any group.
    		for (Group filterGroup : filterGroups) {

    			Group outGroup = new Group(filterGroup.getName(), filterGroup.getDirection());

    			if (!userGroups.contains(outGroup)) {
    				throw new GroupForbiddenException("requested group " + outGroup.getName() + " not accessible to current user");
    			}
    		}
    	}

    	String filterGroupVector;

    	/*
    	 * If no groups are specified in the request:
    	 * 
    	 * for admin role, get messages from all groups
    	 * 
    	 * for non-admin role, filter messages according to the user's own group membership
    	 * 
    	 */
    	if (filterGroups.isEmpty()) {

    		logger.debug("no groups specified");

			// Get group vector for the user associated with this session
			filterGroupVector = martiUtil.getGroupBitVector(request);

    	} else {
    		filterGroupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(filterGroups));
    	}

    	log.fine("filterGroupVector: " + filterGroupVector);

    	switch(format) {
    	case kmz:
    		response.setContentType("application/vnd.google-earth.kmz");
    		break;
    	case kml: // fallthrough to default
    	default:
    		response.setContentType("application/vnd.google-earth.kml+xml");
    		break;
    	}

    	// perform query, leaving XPath evaluation and sorting to be done by workers in a thread pool
    	String trackQuery = "SELECT uid, ST_X(event_pt), ST_Y(event_pt), "
    			+ "point_hae, cot_type, servertime, point_le, detail, point_ce "
    			+ "FROM cot_router "
    			//                + "r inner join cot_image i on r.id = i.cot_id "
    			+ "WHERE servertime BETWEEN ? AND ? and cot_type != 'b-t-f' and cot_type != 'b-f-t-r' and cot_type != 'b-f-t-a'";


    	if (uidParam != null) {
    		trackQuery += " and uid = ?";
    	}

    	// Include group membership
    	trackQuery += RemoteUtil.getInstance().getGroupAndClause();

    	//        DbQueryWrapper wrap = new DbQueryWrapper();
    	OutputStream responseOutputStream = null;
    	ZipOutputStream zip = null;

    	logger.trace("processor count: " + Runtime.getRuntime().availableProcessors());

    	Set<String> uids = new HashSet<>();

    	try {

    		TimeInterval periodOfInterest = decideTimeInterval(startParam, endParam, interval);

    		response.setHeader("Content-Disposition", "filename=" + cotDateFormat.format(periodOfInterest.begin)
    		+ "." + format.toString());
    		if (uidParam != null) {
    			uids = kmlDao.getUidsHavingImages(periodOfInterest.begin, periodOfInterest.end, filterGroupVector);
    			log.finest("uids with images: " + uids);
    		}

    		logger.debug("DbQueryWrapper: " + wrap);

    		try (Connection connection = ds.getConnection(); PreparedStatement sqlQuery = wrap.prepareStatement(trackQuery, connection)) {
    			sqlQuery.setTimestamp(1, periodOfInterest.begin);
    			sqlQuery.setTimestamp(2, periodOfInterest.end);
    			
    			if (uidParam != null) {
    				sqlQuery.setString(3, uidParam);
    				sqlQuery.setString(4, filterGroupVector);
    	    	} else {
    	    		sqlQuery.setString(3, filterGroupVector);
    	    	}

    			long dbStart = System.currentTimeMillis();
    			try (ResultSet results = wrap.doQuery(sqlQuery)) {
    				long dbDuration = System.currentTimeMillis() - dbStart;

    				if (results == null || results.isClosed()) {
    					throw new RuntimeException("Database error");
    				}

    				long kmlInitStart = System.currentTimeMillis();
    				Kml kml = new Kml();
    				Document doc = kml.createAndSetDocument();

    				// populating static styles
    				Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
    				KmlUtils.initStyles(seenUrls, doc);

    				if (includeExtendedData) {
    					initKMLExtendedData(doc);
    				}

    				long kmlInitDuration = System.currentTimeMillis() - kmlInitStart;

    				// Use a concurrent set for sorted, processed CoT objects. Sorting will be performed upon insertion in the set by the worker threads, with the specified Comparator, which sorts by uid, then servertime, accounting for nulls if they are encountered.
    				Set<CotElement> cotResultSet = new ConcurrentSkipListSet<>(new Comparator<CotElement>() {
    					public int compare(CotElement thisCot, CotElement thatCot) {
    						if (thisCot == null) {
    							if (thatCot == null) {
    								return 0;
    							}

    							return -1;
    						}

    						if (thatCot == null) {
    							return 1;
    						}


    						if (thisCot.uid == null) {
    							if (thatCot.uid == null) {
    								return 0;
    							}
    							return -1;
    						}

    						if (thatCot.uid == null) {
    							return 1;
    						}

    						// if the uids are equal, then compare the servertimes
    						if (thisCot.uid.equals(thatCot.uid)) {
    							if (thisCot.servertime == null) {
    								if (thatCot.servertime == null) {
    									return 0;
    								}

    								return 1;
    							}

    							if (thatCot.servertime == null) {
    								return 1;
    							}

    							return thisCot.servertime.compareTo(thatCot.servertime);

    						}

    						return thisCot.uid.compareTo(thatCot.uid);
    					}
    				});

    				// Adds found CotElements to cotResultSet
    				long cotFetchAndDeserializeDuration = parseResultsForCotElements(cotResultSet, results);

    				long kmlStart = System.currentTimeMillis();
    				LinkedList<CotElement> qrs = new LinkedList<>();

    				String uid = null;

    				// use relative urls only for KMZ

    				String imagePath = "api/cot/images/uid/"; 

    				String baseUrl = format.equals(AllowedFormat.kmz) ? imagePath : KmlUtils.getBaseUrl(request) + "/" + imagePath;

    				Map<String, byte[]> images = new HashMap<>();

    				for (CotElement cot : cotResultSet) {
    					if (uid == null) {
    						uid = cot.uid;
    						logger.trace("processing track for uid: " + uid);
    					}

    					if (cot.uid.equals(uid)) {
    						qrs.add(cot);
    					} else {

    						if (format.equals(AllowedFormat.kmz)) {
    							// progressively build image url list.
    							KmlUtils.getImageUrlList(qrs, images, Constants.KML_MAX_IMAGE_COUNT, imagePath, kmlDao, uids);
    						}
    						if (uidParam == null) {
    							buildMissionFeatures(qrs, doc, seenUrls, periodOfInterest.end, baseUrl, images, format, true, includeExtendedData, multiTrackThresholdAsInteger, optimizeExportAsBoolean);
    						} else {
    							buildMissionFeatures(qrs, doc, seenUrls, periodOfInterest.end, baseUrl, images, format, false, includeExtendedData, multiTrackThresholdAsInteger, optimizeExportAsBoolean);
    						}

    						logger.trace("processing track for uid: " + uid);
    						uid = cot.uid;
    						qrs = new LinkedList<>();
    						qrs.add(cot);
    					}
    				}

    				if (qrs != null && !qrs.isEmpty()) {

    					if (format.equals(AllowedFormat.kmz)) {
    						// progressively build image url list.
    						KmlUtils.getImageUrlList(qrs, images, Constants.KML_MAX_IMAGE_COUNT, imagePath, kmlDao, uids);
    					}
    					if (uidParam == null) {
    						buildMissionFeatures(qrs, doc, seenUrls, periodOfInterest.end, baseUrl, images, format, true, includeExtendedData, multiTrackThresholdAsInteger, optimizeExportAsBoolean);
    					} else {
    						buildMissionFeatures(qrs, doc, seenUrls, periodOfInterest.end, baseUrl, images, format, false, includeExtendedData, multiTrackThresholdAsInteger, optimizeExportAsBoolean);
    					}
    				}

    				log.finest("kmz images: " + images);

    				long kmlDuration = System.currentTimeMillis() - kmlStart;

    				long kmlGenStart = System.currentTimeMillis();
    				responseOutputStream = response.getOutputStream();

    				long kmlGenDuration = System.currentTimeMillis() - kmlGenStart;

    				logger.debug("mission kml timings kmlInitDuration: " + kmlInitDuration 
    						+ " dbDuration: " + dbDuration 
    						+ " retrieve, and deserialize CoT results: " + cotFetchAndDeserializeDuration 
    						+ " kmlDuration (including track processing): " + kmlDuration 
    						+ " kmlGenDuration: " + kmlGenDuration 
    						+ " total duration: " 
    						+ (kmlInitDuration + cotFetchAndDeserializeDuration + dbDuration + kmlDuration + kmlGenDuration));

    				if (format.equals(AllowedFormat.kmz)) {
    					zip = new ZipOutputStream(responseOutputStream);

    					// marshal jak -> doc.kml
    					zip.putNextEntry(new ZipEntry("doc.kml"));
    					kml.marshal(zip);
    					zip.closeEntry();

    					byte[] inputBuffer = new byte[8192];
    					ServletContext context = this.getServletContext();
    					Set<String> iconUrls = new HashSet<String>(seenUrls.size());

    					for(Association<String,String> urls : seenUrls) {
    						String iconUrl = urls.getValue();

    						if (iconUrls.contains(iconUrl)) continue;
    						iconUrls.add(iconUrl);

    						// if this is a custom / default iconset icon, get the icon bytes from the icon repository service instead of directly from the app context

    						InputStream is = null;

    						if (!Strings.isNullOrEmpty(iconUrl) && iconUrl.startsWith(IconsetIconApi.ICON_API_PATH)) {
    							try {
    								logger.trace("custom icon detected");

    								if (iconRepository == null) {
    									logger.error("icon repository is null - unable to get custom icon bytes");
    									continue;
    								}

    								if (iconUrl.length() > IconsetIconApi.ICON_API_PATH.length()) {
    									String iconPath = iconUrl.substring(IconsetIconApi.ICON_API_PATH.length() + 1);

    									logger.trace("iconPath: " + iconPath);

    									IconParts iconParts = Icon.parseIconPath(iconPath);

    									Icon icon = iconRepository.findIconByIconsetUidAndGroupAndName(iconParts.iconsetUid, iconParts.group, iconParts.name);

    									if (icon.getBytes() != null && icon.getBytes().length > 0) {
    										is = new ByteArrayInputStream(icon.getBytes());
    									}

    								} else {
    									throw new IllegalArgumentException("invalid iconUrl: " + iconUrl);
    								}

    							} catch (Exception e) {
    								logger.error("exception getting custom icon bytes", e);
    							}

    						} else {
    							logger.trace("built-in icon detected");

    							is = context.getResourceAsStream(iconUrl);

    							if (is == null) {
    								// returns null if path could not be found					    
    								log.warning("Couldn't find resource for " + iconUrl);
    								continue;
    							}
    						}

    						BufferedInputStream ins = new BufferedInputStream(is);
    						zip.putNextEntry(new ZipEntry(iconUrl));
    						try {
    							int nread = 0;
    							while((nread = ins.read(inputBuffer)) != -1){
    								zip.write(inputBuffer, 0, nread);
    							}
    						} catch (IOException e) {
    							log.severe(iconUrl + " " + e.getMessage());
    						} finally {
    							try {
    								zip.closeEntry();
    							} catch (IOException ex) {
    								log.warning("Failed to close ZIP entry for " + iconUrl + " " + ex.getMessage());
    							} 
    							try {
    								if (is != null ) {
    									is.close();
    								}
    							} catch (IOException ex) {
    								log.severe(iconUrl + " " + ex.getMessage());
    							}
    						}
    					}

    					// put images into zip file
    					for(Entry<String, byte[]> image : images.entrySet()) {
    						try {
    							zip.putNextEntry(new ZipEntry(image.getKey()));
    							zip.write(image.getValue());
    						} catch (Exception e) {
    							log.warning("exception writing zip file entry for image " + image + " " + e.getMessage());
    						} finally {
    							try {
    								zip.closeEntry();
    							} catch (IOException ex) {
    								log.finer("Failed to close ZIP entry for " + image + " " + ex.getMessage());
    							} 
    						}
    					}

    				} else {
    					// non zip output
    					kml.marshal(responseOutputStream);
    				}

    			} catch (GroupForbiddenException ex) {
    				log.warning(ex.getMessage());
    				response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
    			} catch (SQLException ex) {
    				log.severe(ex.getMessage());
    				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing database query");
    			} catch (ZipException ex) {
    				log.severe(ex.getMessage());
    				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating kmz file");
    			} catch (KMLServletParameterException ex) {
    				log.warning(ex.getMessage());
    				response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
    			} catch (Exception ex) {
    				log.severe(ex.getMessage());
    				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Exception generating KML" + ex.getMessage());
    			} finally {
    				if (zip != null) {
    					try {
    						zip.flush();
    						zip.close();
    					} catch (Exception e) {}
    				} else if (responseOutputStream != null) {
    					try {
    						responseOutputStream.flush();
    						responseOutputStream.close();
    					} catch (IOException e) {}
    				}
    			}
    		} catch (SQLException | NamingException ee) {
    			log.warning(ee.getMessage());
    			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid timestamp format");
    		} 
    	} catch (ParseException ex) {
    		log.warning(ex.getMessage());
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp format");
    	}
    }

    /**
     * Populates the kml doc with the timeseries output of a particular uid.
     * @param qrs A nonempty list of cot containers for a particular uid, ordered by time ascending
     */    
    public void buildMissionFeatures(LinkedList<CotElement> qrs, Document doc, Set<Association<String,String>> seenUrls,
                                        Timestamp maxStartTime, String baseUrl, Map<String, byte[]> images, AllowedFormat format,
                                        boolean includeDescription, boolean includeExtendedData, int multiTrackThreshold, boolean optimizeExport)
            throws SQLException {
        CotElement last = qrs.getLast();
        String cotType = last.cottype;
        
        boolean imageTag = true;
        
        try {
            String imageUrl = baseUrl + URLEncoder.encode(last.uid, "UTF-8");
            if (log.isLoggable(Level.FINEST)) {
				log.finest("key: " + ESAPI.encoder().encodeForURL(imageUrl));
			}
            
            if (format.equals(AllowedFormat.kmz) && !images.containsKey(imageUrl)) {
                imageTag = false;
            }
        } catch (UnsupportedEncodingException | EncodingException e) { }

        if (cotType.startsWith("b-m-r")) {
            // route
            List<Placemark> ps = buildTimeseriesLineString(qrs, maxStartTime);
            
            for(Placemark p : ps) {
                if (includeDescription) {
                    p.withDescription(KmlUtils.buildDescription(last, Constants.KML_MAX_IMAGE_COUNT, baseUrl, "", imageTag));
                }
                doc.addToFeature(p); 
            }
        } else {
            // everything else
            Placemark p = KmlUtils.buildOptimizedTrack(qrs, includeExtendedData, multiTrackThreshold, optimizeExport);
            
            // Prepend star to name if there are any images
            if (format.equals(AllowedFormat.kmz) && imageTag) {
                p.setName("* " + p.getName());
            }
            if (includeDescription) {
                p.withDescription(KmlUtils.buildDescription(last, Constants.KML_MAX_IMAGE_COUNT, baseUrl, "", imageTag));
            }
            doc.addToFeature(p);
        }

        if (!seenUrls.contains(last.urls)) {
            doc.addToStyleSelector(KmlUtils.buildStyle(last));
            seenUrls.add(last.urls);
        }
    }

    protected List<Placemark> buildTimeseriesLineString(LinkedList<CotElement> qrs, Timestamp maxStartTime) {
        List<Association<CotElement,List<Coord>>> qrsAndRoutes = 
        		new ArrayList<Association<CotElement,List<Coord>>>(qrs.size());
        for(CotElement qr : qrs)
            qrsAndRoutes.add(new Association(qr, kmlService.parseRouteFromDetail(qr.detailtext)));
        return KmlUtils.buildTimeseriesLineString(qrsAndRoutes, maxStartTime);
    }

    /**
     * Partially parses a ResultSet into a list of CotElements, which is a timeseries for a single uid.
     * 
     * @param results A nonempty ResultSet, where the cursor is pointing at a valid row entry whose uid
     * is the beginning of a new grouping. Leaves the cursor either pointing at a new uid grouping, or
     * advanced past the end of the results.
     * @return A list of query results with a uniform uid, or an empty list. The empty list being returned is equivalent to the ResultSet cursor being depleted.
     * <p>
     * This method implements a groupby operation on the result set, returning a logical grouping of results by uid,
     * where the common uid is the uid of the first valid cot result, as found from the given cursor position.
     */
    protected LinkedList<CotElement> parseUidGroup(ResultSet results) 
            throws SQLException {
        LinkedList<CotElement> qrs = new LinkedList<>();

        // seek to the first valid query result -- need a baseline uid for comparison
        CotElement cursor = seekValid(results); 
        if (cursor != null) {
            // didn't reach the end of the row -- get baseline uid out
            String uid = cursor.uid;
            do {
                // run and bunch into qrs, until we find a new uid, or the cursor has run off the tracks
                qrs.add(cursor);
            } while(results.next() 
                    && (cursor = seekValid(results)) != null 
                    && uid.equals(cursor.uid));
        }

        return qrs;
    }

    /**
     * Returns the first valid CotElement that can be parsed out of the ResultSet, 
     * or null if none can be successfully parsed before encountering the end of the stream. 
     *
     * @param results Assumes that the given ResultSet is pointing at a valid row. Leaves the ResultSet pointing at the element that was just parsed, if any was.
     * @return Returns a valid CotElement ref, or null if none can be found. Null is equivalent to the ResultSet cursor being depleted.
     */
    private CotElement seekValid(ResultSet results, BlockingQueue<CotElement> queue) throws SQLException {
        CotElement qr = null;

        while (qr == null && results.next()) {
            qr = kmlService.deserializeFromResultSet(results);

            logger.debug("parsed cotelement: " + qr);

            try {
                if (qr != null) {
                    queue.add(qr);
                }
            } catch (Throwable t) {
                logger.warn("exception enqueuing cot element", t);
            }

        } 


        return qr;
    }

    private CotElement seekValid(ResultSet results) throws SQLException {
        CotElement qr = null;
        while (qr == null && results.next()) {
            qr = kmlService.parseFromResultSet(results);
        } 

        return qr;
    }

    private class CotParserWorker implements Runnable {
        private final BlockingQueue<CotElement> queue;
        private final Set<CotElement> outputSet;
        private final AtomicBoolean producerComplete;

        private KMLDao dao = null;

        private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CotParserWorker.class);

        CotParserWorker(BlockingQueue<CotElement> queue, Set<CotElement> cotResultSet, AtomicBoolean producerComplete, KMLDao dao) {
            this.queue = queue;
            this.producerComplete = producerComplete;
            this.outputSet = cotResultSet;
            this.dao = dao;
        }

        @Override
        public void run() {
            // wait for element to be available, and poll producerComplete

            logger.trace("getting KML dao");

            if (dao == null) {
	            try {
	                dao = SpringContextBeanForApi.getSpringContext().getBean(KMLDao.class);
	            } catch (Exception e) {
	                logger.warn("exception initializing worker", e);
	                return;
	            }
            }

            while(!(producerComplete.get() && queue.isEmpty())) {
                logger.trace("producer not complete - blocking waiting for CotElement to process. queue depth: " + queue.size() + " set size: " + outputSet.size());

                try {
                    CotElement cot = queue.poll(500, TimeUnit.MILLISECONDS);

                    logger.trace("cot parse worker processing cot " + cot);

                    if (cot != null) {
                        dao.parse(cot);
                        outputSet.add(cot);
                    } else {
                        logger.trace("skipping null CotElement");
                    }

                } catch (Exception e) {
                    logger.debug("exception during CoT result parsing", e);
                }
            }

            logger.debug("producer completion detected - consumer thread terminating");
        }
    }
    
    private TimeInterval decideTimeInterval(String startTime, String endTime, Double interval) 
    	throws ParseException, KMLServletParameterException {
    	
    	String startString = (startTime == null) ? "" : startTime;
    	String endString = (endTime == null) ? "" : endTime;
    	long elapsedMilliseconds = (interval == null) ? 0l : Math.round(interval * 60.0d * 60.0d * 1000.0d);
    	
    	long currentTime = System.currentTimeMillis();
    	if (elapsedMilliseconds > currentTime) {
    		throw new KMLServletParameterException("Time interval is excessively long.");
    	}
    	
    	Timestamp start = null;
    	Timestamp end = null;
    	
    	// Parse input
    	if (!startString.isEmpty()) {
    		start = new Timestamp(cotDateFormat.parse(startString).getTime());
    	}
    	
    	if (!endString.isEmpty()) {
    		 end = new Timestamp(cotDateFormat.parse(endString).getTime());
    	} 
    	
    	if (start != null && end != null && interval != null) {
    		// User specified start, stop, and the interval.
    		// Can only get all three if the interval happens to be exactly right.
    		if (end.getTime() - elapsedMilliseconds != start.getTime()) {
    			throw new KMLServletParameterException("Don't specify all three of start time, end time, and time interval.");
    		}
    	} else if (start == null && end == null && elapsedMilliseconds <= 0) {
    		throw new KMLServletParameterException("No valid time interval specified.");
    	} else if (start == null && end == null && elapsedMilliseconds > 0) {
    		// Interval only. Count backward starting from now.
    		end = new Timestamp(currentTime);
    		start = new Timestamp(end.getTime() - elapsedMilliseconds);
    	} else if (start == null && elapsedMilliseconds > 0) {
    		// End time and interval only. Count backward from end time.
    		start = new Timestamp(end.getTime() - elapsedMilliseconds);
    	} else if (end == null && elapsedMilliseconds > 0) {
    		// Start time only. Count forward from start time.
    		end = new Timestamp(start.getTime() + elapsedMilliseconds);
    	} else if (start == null || end == null) {
    		// should never happen
    		log.severe("Logic error parsing HTTP input parameters for time limits");
    	}
    
    	return new TimeInterval(start, end);
    }
    
    public long parseResultsForCotElements(Set<CotElement> cotResultSet, ResultSet results) throws SQLException, InterruptedException {
		// use at least two threads in the thread pool, more per processsor to leverage multicore
		int numThreads = Runtime.getRuntime().availableProcessors() + 1;

		// create an executor service
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		BlockingQueue<CotElement> queue = new LinkedBlockingQueue<>();
		
		// flag to track producer (result set streaming from database) processing completion
		AtomicBoolean producerComplete = new AtomicBoolean(false); 

		// create worker threads to process each cot record 
		for (int i = 0; i < numThreads; i++) {
			logger.debug("creating CoT worker " + i);
			executor.execute(new CotParserWorker(queue, cotResultSet, producerComplete, kmlDao));
		}

		logger.debug("fetching and deserializing cot data");

		long cotFetchAndDeserializeStart = System.currentTimeMillis();

		// deserialize all cot results
		CotElement cursor = seekValid(results, queue);
		if (cursor != null) {
			// didn't reach the end of the row -- get baseline uid out
			while(results.next() && (cursor = seekValid(results, queue)) != null);
		}
		long cotFetchAndDeserializeDuration = System.currentTimeMillis() - cotFetchAndDeserializeStart;
		while(queue.size() != 0) {
			logger.debug("Still processing...");
			try { Thread.sleep(200); } catch(InterruptedException e) {};
		}
		producerComplete.set(true);

		logger.debug("cot queue depth: " + queue.size());

		// request thread executor shutdown
		executor.shutdown();

		// block waiting for thread completion
		while(!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
			logger.trace("awaiting thread pool termination");
		}

		logger.debug("cot set size: " + cotResultSet.size());
		return cotFetchAndDeserializeDuration;
    }
    
    public void initKMLExtendedData(Document doc) {
		Schema schema = doc.createAndAddSchema();
		schema.setId("trackschema");

		SimpleArrayField speed = new SimpleArrayField();
		speed.setDisplayName("Speed m/s");
		speed.setName("speed");
		speed.setType("double");

		SimpleArrayField ce = new SimpleArrayField();
		ce.setDisplayName("Circular Error (m)");
		ce.setName("ce");
		ce.setType("double");

		SimpleArrayField le = new SimpleArrayField();
		le.setDisplayName("Linear Error (m)");
		le.setName("le");
		le.setType("double");

		schema.addToSchemaExtension(speed);
		schema.addToSchemaExtension(ce);
		schema.addToSchemaExtension(le);
    }
}
