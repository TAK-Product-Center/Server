package tak.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import com.bbn.marti.AltitudeConverter;
import com.bbn.marti.dao.kml.KMLDao;
import com.bbn.marti.service.kml.KMLServiceImpl;
import com.bbn.marti.util.Coord;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import tak.server.cot.CotElement;

public class KMLServiceTests {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(KMLServiceTests.class);
    
	private static String LINE_DETAIL_STRING = "<event><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,0,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,0,0\"/></event>";
	
	private static String RANGE_DETAIL_STRING = "<event><range value=\"4\"/><bearing value=\"6\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,0,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"10,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,10,0\"/><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\" point=\"0,0,0\"/></event>";
	
	@Before
	public void init() {
        MockitoAnnotations.initMocks(this);
		
		when(altitudeConverter.haeToMsl(anyDouble(), anyDouble(), anyDouble())).thenReturn(0.0);
		
		when(dao.getCotElements(eq("p-m-r"), anyInt(), anyString())).thenReturn(Arrays.asList(createCotElement("p-m-r", LINE_DETAIL_STRING)));
		when(dao.getCotElements(eq("b-m-r"), anyInt(), anyString())).thenReturn(Arrays.asList(createCotElement("b-m-r", LINE_DETAIL_STRING)));
		when(dao.getCotElements(eq("u-d-r"), anyInt(), anyString())).thenReturn(Arrays.asList(createCotElement("u-d-r", LINE_DETAIL_STRING)));
		when(dao.getCotElements(eq("u-rb-a"), anyInt(), anyString())).thenReturn(Arrays.asList(createCotElement("u-rb-a", RANGE_DETAIL_STRING)));

		logger.info("Finished initalizing services in KML Tests");

	}
	
	@Test
	public void buildPoint() {
		logger.info("Running buildPoint");

		Kml kml = service.process("p-m-r", 0, null);
		Feature kmlFeature = kml.getFeature();
		assertTrue(kmlFeature instanceof Document);

		Document document = (Document) kmlFeature;
		List<Feature> features = document.getFeature();
		assertEquals(1, features.size());
		assertTrue(features.get(0) instanceof Placemark);

		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof Point);
		
		Point point = (Point) placemark.getGeometry();
		assertEquals(1, point.getCoordinates().size());
		
	}
	
	@Test
	public void buildLineString() {
		logger.info("Running buildLineString");

		Kml kml = service.process("b-m-r", 0, null);
		Feature kmlFeature = kml.getFeature();
		assertTrue(kmlFeature instanceof Document);

		Document document = (Document) kmlFeature;
		List<Feature> features = document.getFeature();
		assertEquals(1, features.size());
		assertTrue(features.get(0) instanceof Placemark);

		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof LineString);
		
		LineString lineString = (LineString) placemark.getGeometry();
		assertEquals(4, lineString.getCoordinates().size());
		
	}
	
	@Test
	public void buildRectangle() {
		logger.info("Running buildRectangle");

		Kml kml = service.process("u-d-r", 0, null);
		Feature kmlFeature = kml.getFeature();
		assertTrue(kmlFeature instanceof Document);

		Document document = (Document) kmlFeature;
		List<Feature> features = document.getFeature();
		assertEquals(1, features.size());
		assertTrue(features.get(0) instanceof Placemark);

		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof Polygon);
		
		Polygon polygon = (Polygon) placemark.getGeometry();
		
		Boundary boundary = polygon.getOuterBoundaryIs();
		
		LinearRing linearRing = (LinearRing) boundary.getLinearRing();
		assertEquals(5, linearRing.getCoordinates().size());
		
	}
	
	@Test
	public void buildRangeAndBearing() {
		logger.info("Running buildRangeAndBearing");

		Kml kml = service.process("u-rb-a", 0, null);
		Feature kmlFeature = kml.getFeature();
		assertTrue(kmlFeature instanceof Document);

		Document document = (Document) kmlFeature;
		List<Feature> features = document.getFeature();
		assertEquals(1, features.size());
		assertTrue(features.get(0) instanceof Folder);

		Folder folder = (Folder) features.get(0);
		features = folder.getFeature();
		assertEquals(3, features.size());
		
		// Check line from start to end
		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof LineString);
		
		LineString lineString = (LineString) placemark.getGeometry();
		assertEquals(2, lineString.getCoordinates().size());

		// Check start 
		placemark = (Placemark) features.get(1);
		assertTrue(placemark.getGeometry() instanceof Point);
		
		Point point = (Point) placemark.getGeometry();
		assertEquals("Start", placemark.getName());
		assertEquals(1, point.getCoordinates().size());
		assertEquals(lineString.getCoordinates().get(0), point.getCoordinates().get(0));
	
		// Check end 
		placemark = (Placemark) features.get(2);
		assertTrue(placemark.getGeometry() instanceof Point);
		
		point = (Point) placemark.getGeometry();
		assertEquals("End", placemark.getName());
		assertEquals(1, point.getCoordinates().size());
		assertEquals(lineString.getCoordinates().get(1), point.getCoordinates().get(0));
	}
	
	@Test
	public void buildAll() {
		LinkedList<CotElement> cotElements = new LinkedList<>();
		cotElements.add(createCotElement("p-m-r", LINE_DETAIL_STRING));
		cotElements.add(createCotElement("b-m-r", LINE_DETAIL_STRING));
		cotElements.add(createCotElement("u-d-r", LINE_DETAIL_STRING));
		cotElements.add(createCotElement("u-rb-a", RANGE_DETAIL_STRING));
		
		Kml kml = service.process(cotElements);
		Feature kmlFeature = kml.getFeature();
		assertTrue(kmlFeature instanceof Document);

		Document document = (Document) kmlFeature;
		List<Feature> features = document.getFeature();
		assertEquals(4, features.size());

		// Point
		assertTrue(features.get(0) instanceof Placemark);

		Placemark placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof Point);
		
		Point point = (Point) placemark.getGeometry();
		assertEquals(1, point.getCoordinates().size());

		// Line String
		assertTrue(features.get(1) instanceof Placemark);

		placemark = (Placemark) features.get(1);
		assertTrue(placemark.getGeometry() instanceof LineString);
		
		LineString lineString = (LineString) placemark.getGeometry();
		assertEquals(4, lineString.getCoordinates().size());
		
		// Polygon
		assertTrue(features.get(2) instanceof Placemark);

		placemark = (Placemark) features.get(2);
		assertTrue(placemark.getGeometry() instanceof Polygon);
		
		Polygon polygon = (Polygon) placemark.getGeometry();
		
		Boundary boundary = polygon.getOuterBoundaryIs();
		
		LinearRing linearRing = (LinearRing) boundary.getLinearRing();
		assertEquals(5, linearRing.getCoordinates().size());
		
		// Range and Bearing
		assertTrue(features.get(3) instanceof Folder);

		Folder folder = (Folder) features.get(3);
		features = folder.getFeature();
		assertEquals(3, features.size());
		

		placemark = (Placemark) features.get(0);
		assertTrue(placemark.getGeometry() instanceof LineString);
		
		lineString = (LineString) placemark.getGeometry();
		assertEquals(2, lineString.getCoordinates().size());

		// Check start 
		placemark = (Placemark) features.get(1);
		assertTrue(placemark.getGeometry() instanceof Point);
		
		point = (Point) placemark.getGeometry();
		assertEquals("Start", placemark.getName());
		assertEquals(1, point.getCoordinates().size());
		assertEquals(lineString.getCoordinates().get(0), point.getCoordinates().get(0));
	
		// Check end 
		placemark = (Placemark) features.get(2);
		assertTrue(placemark.getGeometry() instanceof Point);
		
		point = (Point) placemark.getGeometry();
		assertEquals("End", placemark.getName());
		assertEquals(1, point.getCoordinates().size());
		assertEquals(lineString.getCoordinates().get(1), point.getCoordinates().get(0));

	}
	
	@Test
	public void parseRouteFromDetail() {
		logger.info("Running parseRouteFromDetail");
		
		List<Coord> routeCoords = service.parseRouteFromDetail(LINE_DETAIL_STRING);
		assertEquals(4, routeCoords.size());

		// 10, 0, 0
		Coord coord1 = routeCoords.get(0);
		assertEquals(10, coord1.getLat(), .0001);
		assertEquals(0, coord1.getLon(), .0001);
		assertEquals(0, coord1.getAlt(), .0001);

		// 10, 10, 0
		Coord coord2 = routeCoords.get(1);
		assertEquals(10, coord2.getLat(), .0001);
		assertEquals(10, coord2.getLon(), .0001);
		assertEquals(0, coord2.getAlt(), .0001);
		
		// 0, 10, 0
		Coord coord3 = routeCoords.get(2);
		assertEquals(0, coord3.getLat(), .0001);
		assertEquals(10, coord3.getLon(), .0001);
		assertEquals(0, coord3.getAlt(), .0001);
		
		// 0, 0, 0
		Coord coord4 = routeCoords.get(3);
		assertEquals(0, coord4.getLat(), .0001);
		assertEquals(0, coord4.getLon(), .0001);
		assertEquals(0, coord4.getAlt(), .0001);
	}
	
	public CotElement createCotElement(String cotType, String detailText) {
		CotElement element = new CotElement();
		element.uid = UUID.randomUUID().toString();
		element.lon = 10.0;
		element.lat = 32.0;
		element.hae = "0.0";
		element.cottype = cotType;
		element.servertime = new Timestamp(16522101109000L);
		element.le = 9999999;
		element.detailtext = detailText;
		element.cotId = null;
		element.how = "h-g-i-g-o";
		element.staletime = new Timestamp(16522101109000L);
		element.ce = 9999999;
		element.geom = element.lon + "," + element.lat; 
		return element;
	}
	
	@InjectMocks
	private KMLServiceImpl service;

	@Mock
	private AltitudeConverter altitudeConverter;
	
	@Mock
	private KMLDao dao;
	
	@Mock
	private ResultSet results;
}
