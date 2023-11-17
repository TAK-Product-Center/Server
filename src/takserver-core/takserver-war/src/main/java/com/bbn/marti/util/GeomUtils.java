package com.bbn.marti.util;


import java.util.Arrays;
import com.bbn.marti.config.GeospatialFilter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GeomUtils {
	private static final GeometryFactory gf = new GeometryFactory();
	
	private static final Logger logger = LoggerFactory.getLogger(GeomUtils.class);
	
	// string format: lat long, lat long, lat lon, ...
	public static Polygon postgisBoundingPolygonToPolygon(String polyString) {
		Polygon polygon = null;
		
		try {
			Coordinate[] polygonPoints = Arrays.stream(polyString.split(","))
					.map(p->p.trim())
					.map(p->p.split(" "))
					.map(latlon-> new double[] {Double.valueOf(latlon[0]), Double.valueOf(latlon[1])})
					.map(latlon-> new Coordinate(latlon[1], latlon[0])) // flip lat lon so that its x,y instead of y,x
					.toArray(Coordinate[]::new);
			
			polygon = gf.createPolygon(polygonPoints);
		} catch (Exception e) {
			logger.error("Could not parse polygon string into a polygon", e);
		}
			
		return polygon;
	}
	
	public static boolean polygonContainsCoordinate(Polygon polygon, double lat, double lon) {
		Coordinate coord = new Coordinate(lon,lat); // flip lat lon so that its x,y instead of y,x
		
		return polygon.contains(gf.createPoint(coord));
	}

	// compute bbox from string and cache it for instant lookup next time
	public static GeospatialFilter.BoundingBox getBoundingBoxFromBboxString(String bbox) {

		String[] bboxArr = bbox.split(",");

		if (bboxArr.length != 4) return null;

		double maxLat, minLat, maxLong, minLong;
		try {
			maxLat = Double.valueOf(bboxArr[0]);
			minLong = Double.valueOf(bboxArr[1]);
			minLat = Double.valueOf(bboxArr[2]);
			maxLong = Double.valueOf(bboxArr[3]);
		} catch (Exception e) {
			return null;
		}

		GeospatialFilter.BoundingBox boundingBox = new GeospatialFilter.BoundingBox();
		boundingBox.setMaxLatitude(maxLat);
		boundingBox.setMinLongitude(minLong);
		boundingBox.setMinLatitude(minLat);
		boundingBox.setMaxLongitude(maxLong);

		return boundingBox;
	}

	public static boolean bboxContainsCoordinate(GeospatialFilter.BoundingBox boundingBox, double lat, double lon) {

		boolean validLongitude = false;
		if (boundingBox.getMaxLongitude() > boundingBox.getMinLongitude()) {
			validLongitude = lon >= boundingBox.getMinLongitude() && lon <= boundingBox.getMaxLongitude();
		} else {
			validLongitude = lon >= boundingBox.getMinLongitude() || lon <= boundingBox.getMaxLongitude();
		}

		// return the cot event if found within one of the inputs filters
		if (validLongitude &&
				lat >= boundingBox.getMinLatitude() && lat <= boundingBox.getMaxLatitude()) {
			return true;
		}

		return false;
	}
}
