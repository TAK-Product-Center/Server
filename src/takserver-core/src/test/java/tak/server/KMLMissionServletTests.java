package tak.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import com.bbn.marti.AltitudeConverter;
import com.bbn.marti.LatestKMLServlet.AllowedFormat;
import com.bbn.marti.MissionKMLServlet;
import com.bbn.marti.dao.kml.KMLDao;
import com.bbn.marti.service.kml.KMLServiceImpl;
import com.bbn.marti.util.Coord;
import com.bbn.marti.util.KmlUtils;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.IconStyle;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Schema;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import de.micromata.opengis.kml.v_2_2_0.gx.MultiTrack;
import de.micromata.opengis.kml.v_2_2_0.gx.SimpleArrayField;
import de.micromata.opengis.kml.v_2_2_0.gx.Track;
import tak.server.cot.CotElement;
import tak.server.util.Association;

public class KMLMissionServletTests {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KMLMissionServletTests.class);
    
	private static String LINE_DETAIL_STRING = "<event><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,0,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,0,0\"/></event>";
	
	private static String RANGE_DETAIL_STRING = "<event><range value=\"4\"/><bearing value=\"6\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,0,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,0,0\"/></event>";
	
	@Before
	public void init() {
        MockitoAnnotations.initMocks(this);
		
        coords = new ArrayList<>();
        coords.add(new Coord(10, 0, 0));
        coords.add(new Coord(10, 10, 0));
        coords.add(new Coord(0, 10, 0));
        coords.add(new Coord(0, 0, 0));
        
        when(service.parseRouteFromDetail(LINE_DETAIL_STRING)).thenReturn(coords);
        
		try {
			when(results.next()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
			when(service.deserializeFromResultSet(results)).thenReturn(createCotElement("u-rb-a", RANGE_DETAIL_STRING)).thenReturn(createCotElement("u-d-r", LINE_DETAIL_STRING));
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Offset timestamps by exactly report time threshold so when optimizeExport is true, skip additional tracks
		qrsOptimized = new LinkedList<>();
		qrsOptimized.add(createCotElement("p-m-r", LINE_DETAIL_STRING));
		qrsOptimized.add(createCotElement("b-m-r", LINE_DETAIL_STRING, baseTime + reportTimeThreshold));
		qrsOptimized.add(createCotElement("u-d-r", LINE_DETAIL_STRING, baseTime + (reportTimeThreshold * 2)));
		qrsOptimized.add(createCotElement("u-rb-a", RANGE_DETAIL_STRING, baseTime + (reportTimeThreshold * 3)));
		
		// Offset timestamps by greater than report time threshold so every cot gets a separate track
		qrsUnoptimized = new LinkedList<>();
		qrsUnoptimized.add(createCotElement("p-m-r", LINE_DETAIL_STRING));
		qrsUnoptimized.add(createCotElement("b-m-r", LINE_DETAIL_STRING, baseTime + overReportTimeThreshold));
		qrsUnoptimized.add(createCotElement("u-d-r", LINE_DETAIL_STRING, baseTime + (overReportTimeThreshold * 2)));
		qrsUnoptimized.add(createCotElement("u-rb-a", RANGE_DETAIL_STRING, baseTime + (overReportTimeThreshold * 3)));
		
		logger.info("Finished initalizing services in KML Tests");

	}

	@SuppressWarnings("unchecked")
	@Test
	public void buildMissionFeaturesTimeseriesLineString() {
		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();
		String baseUrl = "api/cot/images/uid/";
		Map<String, byte[]> images = new HashMap<>();
		LinkedList<CotElement> qrs = (LinkedList<CotElement>) qrsOptimized.clone();
		
		// populating static styles
		Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
		KmlUtils.initStyles(seenUrls, doc);

		// Add "b-m-r" cot to force TimeseriesLineString path
		qrs.add(createCotElement("b-m-r", LINE_DETAIL_STRING, baseTime + (reportTimeThreshold * 4)));
		
		try {
			missionServlet.buildMissionFeatures(qrs, doc, seenUrls, new Timestamp(baseTime * 2), baseUrl, images, AllowedFormat.kmz, false, true, 1, true);
		} catch (Exception e) {
			logger.info("Error is: {}", e.getMessage());
			fail(e.getMessage());
		}
		
		List<Feature> features = doc.getFeature();
		assertEquals(5, features.size());
		assertTrue(features.get(0) instanceof Placemark);
		
		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof LineString);
		
		LineString lineString = (LineString) placemark.getGeometry();
		assertEquals(4, lineString.getCoordinates().size());
	}
	
	@Test
	public void buildMissionFeaturesUnoptimizedTrack() {
		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();
		String baseUrl = "api/cot/images/uid/";
		Map<String, byte[]> images = new HashMap<>();

		// populating static styles
		Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
		KmlUtils.initStyles(seenUrls, doc);
		
		try {
			missionServlet.buildMissionFeatures(qrsUnoptimized, doc, seenUrls, null, baseUrl, images, AllowedFormat.kmz, false, true, 1, true);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		List<Feature> features = doc.getFeature();
		assertEquals(1, features.size());
		assertTrue(features.get(0) instanceof Placemark);
		
		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof MultiTrack);
		
		MultiTrack multiTrack = (MultiTrack) placemark.getGeometry();
		assertEquals(AltitudeMode.ABSOLUTE, multiTrack.getAltitudeMode());
		List<Track> tracks = multiTrack.getTrack();
		assertEquals(4, tracks.size());
		
		Track track = tracks.get(0);
		List<String> trackCoords = track.getCoord();
		assertEquals(1, trackCoords.size());
		assertEquals(qrsUnoptimized.get(0).geom.replace(',', ' '), trackCoords.get(0));
		
	}
	
	@Test
	public void buildMissionFeaturesOptimizedTrack() {
		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();

		// populating static styles
		Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
		KmlUtils.initStyles(seenUrls, doc);
		String baseUrl = "api/cot/images/uid/";
		Map<String, byte[]> images = new HashMap<>();

		try {
			missionServlet.buildMissionFeatures(qrsOptimized, doc, seenUrls, null, baseUrl, images, AllowedFormat.kmz, false, true, 1, true);
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		List<Feature> features = doc.getFeature();
		assertEquals(1, features.size());
		assertTrue(features.get(0) instanceof Placemark);
		
		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof MultiTrack);
		
		MultiTrack multiTrack = (MultiTrack) placemark.getGeometry();
		assertEquals(AltitudeMode.ABSOLUTE, multiTrack.getAltitudeMode());
		List<Track> tracks = multiTrack.getTrack();
		assertEquals(1, tracks.size());
		
		Track track = tracks.get(0);
		List<String> trackCoords = track.getCoord();
		assertEquals(1, trackCoords.size());
		assertEquals(qrsOptimized.get(0).geom.replace(',', ' '), trackCoords.get(0));

	}
	
	@Test
	public void parseResultsForCotElements() {
		
		try {
			missionServlet.parseResultsForCotElements(cotResultSet, results);
		}
		catch (Exception e) {
			logger.info("Error parsing results for cot elements", e);
			fail(e.getMessage());
		}
		
		assertEquals(2, cotResultSet.size());
	}
	
	@Test
	public void initStyles() {
		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();

		// populating static styles
		Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
		KmlUtils.initStyles(seenUrls, doc);

		List<StyleSelector> styleSelectors = doc.getStyleSelector();
		assertEquals(14, styleSelectors.size());
		
		for (StyleSelector styleSelector: styleSelectors) {
			assertTrue(styleSelector instanceof Style);
			Style style = (Style) styleSelector;
			if (style.getId().equals("magenta")) {
				LineStyle lineStyle = style.getLineStyle();
				assertEquals("8fff00ff", lineStyle.getColor());
				IconStyle iconStyle = style.getIconStyle();
				assertEquals("icons/team_magenta.png", iconStyle.getIcon().getHref());
			}
			if (style.getId().equals("white")) {
				LineStyle lineStyle = style.getLineStyle();
				assertEquals("8fffffff", lineStyle.getColor());
				IconStyle iconStyle = style.getIconStyle();
				assertEquals("icons/team_white.png", iconStyle.getIcon().getHref());
			}
		}
	}
	
	@Test
	public void initKMLExtendedData() {
		Kml kml = new Kml();
		Document doc = kml.createAndSetDocument();
		missionServlet.initKMLExtendedData(doc);
		
		List<Schema> schemas = doc.getSchema();
		assertEquals(1, schemas.size());
		
		List<Object> schemaExtension = schemas.get(0).getSchemaExtension();
		assertEquals(3, schemaExtension.size());
		
		SimpleArrayField schemaField = (SimpleArrayField)schemaExtension.get(0);
		assertEquals("speed", schemaField.getName());
		assertEquals("Speed m/s", schemaField.getDisplayName());
		
		schemaField = (SimpleArrayField)schemaExtension.get(1);
		assertEquals("ce", schemaField.getName());
		assertEquals("Circular Error (m)", schemaField.getDisplayName());
		
		schemaField = (SimpleArrayField)schemaExtension.get(2);
		assertEquals("le", schemaField.getName());
		assertEquals("Linear Error (m)", schemaField.getDisplayName());
	}
	
	public CotElement createCotElement(String cotType, String detailText, Long timeStamp) {
		CotElement element = new CotElement();
		element.uid = UUID.randomUUID().toString();
		element.lon = 10.0;
		element.lat = 32.0;
		element.hae = "0.0";
		element.cottype = cotType;
		element.servertime = new Timestamp(timeStamp);
		element.le = 9999999;
		element.detailtext = detailText;
		element.cotId = null;
		element.how = "h-g-i-g-o";
		element.staletime = new Timestamp(timeStamp);
		element.ce = 9999999;
		element.geom = element.lon + "," + element.lat;
		element.speed = 10.0;
		element.course = 11.0;
		element.msl = 10.0;
		return element;

	}
	
	public CotElement createCotElement(String cotType, String detailText) {
		return createCotElement(cotType, detailText, baseTime);
	}

	@InjectMocks
	private MissionKMLServlet missionServlet;
	
	@Mock
	private KMLServiceImpl service;
	
	@Mock
	private AltitudeConverter altitudeConverter;
	
	@Mock
	private KMLDao dao;
	
	@Mock
	private ResultSet results;
	
	private LinkedList<CotElement> qrsOptimized;
	private LinkedList<CotElement> qrsUnoptimized;
	
	private List<Coord> coords;
	
	private Long baseTime = 1552210110900L;
	private Long reportTimeThreshold = 60000L;
	private Long overReportTimeThreshold = reportTimeThreshold + 1;
	
	private Set<CotElement> cotResultSet = new ConcurrentSkipListSet<>(new Comparator<CotElement>() {
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
}
