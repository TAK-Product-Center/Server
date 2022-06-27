

package com.bbn.marti.service.kml;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.ocpsoft.prettytime.PrettyTime;
import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.AltitudeConverter;
import com.bbn.marti.dao.kml.KMLDao;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.util.Coord;
import com.bbn.marti.util.KmlUtils;
import com.google.common.base.Strings;

import de.micromata.opengis.kml.v_2_2_0.BasicLink;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import tak.server.Constants;
import tak.server.cot.CotElement;

/*
 * 
 * In conjunction with the LatestKMLServlet, this class provides the web services layer to
 * provide KML data to clients. This is similar to the Spring MVC Controller role. This class is managed by Spring, and the Spring context is responsible for the scope of its dependencies.
 * 
 */

public class KMLServiceImpl implements KMLService {

	@Autowired
	private KMLDao kmlDao;

	@Autowired
	protected AltitudeConverter converter;

	@Autowired
	protected Validator validator = null;

	protected String servletContextPath = null;	
	
	public static final SimpleDateFormat sqlDateFormat = new SimpleDateFormat(Constants.SQL_DATE_FORMAT);

	PrettyTime prettyTimeFormat = new PrettyTime();

	private static final Logger logger = LoggerFactory.getLogger(KMLServiceImpl.class);

	// process the request handed over from LatestKMLServlet
	// Kml object contains the state
	/* (non-Javadoc)
	 * @see com.bbn.marti.service.kml.KMLService#process(java.lang.String, int, de.micromata.opengis.kml.v_2_2_0.Kml)
	 */
	@Override
	public Kml process(String cotType, int secAgo, String groupVector) {
	    
	    if (Strings.isNullOrEmpty(groupVector)) {
	        groupVector = RemoteUtil.getInstance().getBitStringAllGroups();
	    }

		try {
			// build, populate, and marshal kml object
			Kml kml = KmlFactory.createKml();

			logger.debug("process kml request for cotType: " + cotType + " secAgo: " + secAgo + " kml: " + kml);

			long dbStart = System.currentTimeMillis();
			List<CotElement> elements = kmlDao.getCotElements(cotType, secAgo, groupVector);
			long dbDuration = System.currentTimeMillis() - dbStart;

			long kmlDocStart = System.currentTimeMillis();
			logger.debug("kml cot element count: " + elements.size());
			
			Document doc = kml.createAndSetDocument();
			long kmlDocDuration = System.currentTimeMillis() - kmlDocStart;

			long kmlProcessStart = System.currentTimeMillis();
			buildFeatures(new LinkedList<CotElement>(elements), doc);
			long kmlProcessDuration = System.currentTimeMillis() - kmlProcessStart;
			
			logger.debug("latest kml db query duration: " + dbDuration + " kml doc create duration: " + kmlDocDuration + " kml processing duration: " + kmlProcessDuration + " total: " + (dbDuration + kmlDocDuration + kmlProcessDuration));

			return kml;

		} catch (Throwable t) {
			throw new KMLServiceException("exception fetching CoT KML", t);
		}
	}

	@Override
	public Kml process(LinkedList<CotElement> cotElements) {

		try {
			// build, populate, and marshal kml object
			Kml kml = KmlFactory.createKml();

			long kmlDocStart = System.currentTimeMillis();
			logger.debug("kml cot element count: " + cotElements.size());

			Document doc = kml.createAndSetDocument();
			long kmlDocDuration = System.currentTimeMillis() - kmlDocStart;

			long kmlProcessStart = System.currentTimeMillis();
			buildFeatures(new LinkedList<CotElement>(cotElements), doc);
			long kmlProcessDuration = System.currentTimeMillis() - kmlProcessStart;

			logger.debug("mission kml total time: " + (kmlDocDuration + kmlProcessDuration));

			return kml;

		} catch (Throwable t) {
			throw new KMLServiceException("exception fetching CoT KML", t);
		}
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.service.kml.KMLService#buildFeatures(java.util.LinkedList, de.micromata.opengis.kml.v_2_2_0.Document)
	 */
	@Override
	public void buildFeatures(LinkedList<CotElement> cotElements, Document kmlDoc) {
		Set<String> seenStyleUrls = new HashSet<String>(cotElements.size());
		KmlUtils.initStyleUrls(seenStyleUrls, kmlDoc);
		
		logger.debug("seenStyleUrls: " + seenStyleUrls);

		for (CotElement cotElement : cotElements) {

			Feature feature;
			if (cotElement.cottype.startsWith("b-m-r")) {
				feature = buildLineString(cotElement);
			} else if (cotElement.cottype.startsWith("u-d-r")) {
				feature = KmlUtils.buildRectangle(cotElement,
						parseRouteFromDetail(cotElement.detailtext));
			} else if (cotElement.cottype.startsWith("u-rb-a")){
				feature = KmlUtils.buildRangeAndBearing(cotElement, kmlDoc);
			} else {
				feature = KmlUtils.buildPoint(cotElement);
			}

            String imagePath = "api/cot/images/uid/"; 
			
			// description added here for placemark code reuse in MissionKML
			feature.withDescription(KmlUtils.buildDescription(cotElement, Constants.KML_MAX_IMAGE_COUNT, imagePath, "", true));
			kmlDoc.addToFeature(feature);

			// get style url that this feature is going to link to, build it if we haven't already
			if (!cotElement.cottype.startsWith("u-rb-a") &&
					!seenStyleUrls.contains(cotElement.styleUrl)) {
				KmlUtils.buildAndPopulateStyle(cotElement, kmlDoc);
				seenStyleUrls.add(cotElement.styleUrl);
			}
		}
	}

	private Placemark buildLineString(CotElement qr) {
		return KmlUtils.buildLineString(qr, parseRouteFromDetail(qr.detailtext));
	}

	/* (non-Javadoc)
	 * @see com.bbn.marti.service.kml.KMLService#parseRouteFromDetail(java.lang.String)
	 */
	@Override
	public List<Coord> parseRouteFromDetail(String detailText) {
		List<Coord> result = new LinkedList<Coord>();
		org.dom4j.Document doc = null;

		try {
			doc = DocumentHelper.parseText(detailText);
		} catch (Exception e) {
			logger.warn("exception parsing detail field from CoT data", e);
		}

		if (doc != null) {
			Element detail = doc.getRootElement();
			if (detail != null) {
				List<Element> elemList = (List<Element>) detail.elements("link");
				for (Element e : elemList) {
					String pointStr = e.attributeValue("point");

					if (pointStr != null) {
						String[] points = pointStr.split(",");
						try {
							// parse args into doubles -- confirmed that parseDouble handles leading/trailing whitespace
							double lat = Double.parseDouble(points[0]);
							double lon = Double.parseDouble(points[1]);
							Double hae = (points.length > 2) ? Double.parseDouble(points[2]) : Double.NaN;
							Double msl = converter.haeToMsl(hae, lat, lon);

							// build and add new coordinate into route
							result.add(new Coord(lat, lon, msl));
						} catch (Exception expt) {
							// indexOutOfBounds (not enough coordinates in a point), NumberFormatException (coordinates couldn't be parsed into a double)
							logger.warn("exception parsing lat/long fields from CoT data", expt);
							// return empty list -- result var is potentially tainted with partial parsing
							return new LinkedList<Coord>();
						}
					}
				} 
			}
		}
		return result;
	}

	@Override
	public CotElement parseFromResultSet(ResultSet results) throws SQLException {
		if (results == null) {
			throw new IllegalArgumentException("null ResultSet");
		}
		
		if (kmlDao == null) {
			throw new IllegalStateException("null dao object");
		}
		
		return kmlDao.parseFromResultSet(results);
	}
	
	@Override
    public CotElement deserializeFromResultSet(ResultSet results) throws SQLException {
        if (results == null) {
            throw new IllegalArgumentException("null ResultSet");
        }
        
        if (kmlDao == null) {
            throw new IllegalStateException("null dao object");
        }
        
        return kmlDao.deserialize(results);
    }

    @Override
	public void setStyleUrlBase(Kml kml, String urlBase) {
		Feature feature = kml.getFeature();
		for (StyleSelector styleSelector : feature.getStyleSelector()) {
			if (styleSelector instanceof Style) {
				Style style = (Style)styleSelector;
				IconStyle iconStyle = style.getIconStyle();
				if (iconStyle == null) {
					continue;
				}

				BasicLink link = iconStyle.getIcon();
				if (link == null) {
					continue;
				}

				link.setHref(urlBase + "/" + link.getHref());
			}
		}
	}
}
