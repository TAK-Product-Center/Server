

package com.bbn.marti.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.atakmap.coremap.maps.coords.DistanceCalculations;
import de.micromata.opengis.kml.v_2_2_0.*;
import de.micromata.opengis.kml.v_2_2_0.Container;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.atakmap.android.icons.Icon2525bTypeResolver;
import com.bbn.marti.dao.kml.KMLDao;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import de.micromata.opengis.kml.v_2_2_0.gx.MultiTrack;
import de.micromata.opengis.kml.v_2_2_0.gx.SimpleArrayData;
import de.micromata.opengis.kml.v_2_2_0.gx.Track;
import tak.server.Constants;
import tak.server.cot.CotElement;
import tak.server.util.Association;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class KmlUtils {
    public static final double MAX_VALID_ALTITUDE_METERS = 1000000d; // The atmosphere is about 1000 km thick
    public static Logger log = Logger.getLogger(KmlUtils.class.getCanonicalName());
    public static SimpleDateFormat cotDateFormat = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
    
    public static final String MEDEVAC_URL = "icons/damaged.png";
    
    // location on classpath of mil-std-2525b icon files
    private static final String MIL_2525_CLASSPATH_LOC = "classpath:mil-std-2525b/**";
    
    private static List<String> mil2525filenames = new ArrayList<>();
    
    static {

      try {
        PathMatchingResourcePatternResolver pathMatcher = new PathMatchingResourcePatternResolver();

        Resource[] milResources;

        milResources = pathMatcher.getResources(MIL_2525_CLASSPATH_LOC);

        if (milResources == null || milResources.length == 0) {
          throw new IllegalStateException("no MIL-STD-2525B icons found at " + MIL_2525_CLASSPATH_LOC);
        }

        for (Resource milResource : milResources) {
          mil2525filenames.add(milResource.getFilename().replace("mil-std-2525b/", ""));
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public static String rgbToKmlHex(int r, int g, int b) {
        Color c = new Color(r,g,b);
        String rr = Integer.toHexString(c.getRed());
        if(rr.length() < 2) { rr = "0" + rr; }
        String gg = Integer.toHexString(c.getGreen());
        if(gg.length() < 2) { gg = "0" + gg; }
        String bb = Integer.toHexString(c.getBlue());
        if(bb.length() < 2) { bb = "0" + bb; }
        return "8f" + bb + gg + rr;
    }

    /**
     * Checks the list of query results and finds the appropriate altitude mode based on validity of MSL data.
     * @param qrs
     * @return <code>AltitudeMode.ABSOLUTE</code> if all MSL altitudes in the result list are valid,
     * <code>AltitudeMode.CLAMP_TO_GROUND</code> if any MSL altitudes are invalid.
     * @see buildAltitudeMode(double)
     */
    public static AltitudeMode buildAltitudeMode(List<CotElement> qrs) {
        for (CotElement qr : qrs) {
            AltitudeMode mode;
            if ((mode = buildAltitudeMode(qr.msl)) != AltitudeMode.ABSOLUTE)
                return mode;
        }

        return AltitudeMode.ABSOLUTE;
    }

    /**
     * Applies simple heuristics to determine if the altitude data should be displayed in ABSOLUTE or CLAMP_TO_GROUND mode.
     * Invalid data is that for which MSL altitude is unknown (due to missing geoid data) or where the HAE
     * (Height Above Ellipsoid) is excessively high.
     */
    public static AltitudeMode buildAltitudeMode(double msl) {
        if (Double.isNaN(msl) || msl > MAX_VALID_ALTITUDE_METERS) {
            return AltitudeMode.CLAMP_TO_GROUND;
        } else {
            return AltitudeMode.ABSOLUTE;
        }
    }

    public static void initStyleUrls(Set<String> seenStyleUrls, Container c) {
        Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
        initStyles(seenUrls, c);

        for(Association<String,String> urls : seenUrls) {
            seenStyleUrls.add(urls.getKey());
        }
    }

    public static void initIconUrls(Set<String> seenIconUrls, Container c) {
        Set<Association<String,String>> seenUrls = new HashSet<Association<String,String>>();
        initStyles(seenUrls, c);

        for(Association<String,String> urls : seenUrls) {
            seenIconUrls.add(urls.getValue());
        }
    }

    public static void initStyles(Set<Association<String,String>> seenUrls, Container c) {
        initTeamStyles(seenUrls, c);
        initMedevacStyle(seenUrls, c);
    }

    public static void initTeamStyles(Set<Association<String,String>> seenUrls, Container c) {
        HashMap<String,String> teams = new HashMap<String,String>();
        teams.put("white", rgbToKmlHex(255, 255, 255));
        teams.put("yellow", rgbToKmlHex(255, 255, 0));
        teams.put("orange", rgbToKmlHex(255, 165, 0));
        teams.put("magenta", rgbToKmlHex(255, 0, 255));
        teams.put("red", rgbToKmlHex(255, 0, 0));
        teams.put("maroon", rgbToKmlHex(176, 48, 96));
        teams.put("purple", rgbToKmlHex(160, 32, 240));
        teams.put("darkblue", rgbToKmlHex(0, 0, 139));
        teams.put("blue", rgbToKmlHex(0, 0, 255));
        teams.put("cyan", rgbToKmlHex(0, 255, 255));
        teams.put("teal", rgbToKmlHex(0, 134, 139));
        teams.put("green", rgbToKmlHex(0, 255, 0));
        teams.put("darkgreen", rgbToKmlHex(0, 100, 0));

        for(String styleUrl : teams.keySet()) {
            String iconUrl = String.format("icons/team_%s.png", styleUrl);
            c.addToStyleSelector(buildStyle(styleUrl, iconUrl, "7fffaaff", teams.get(styleUrl), 2));
            seenUrls.add(new Association<String,String>(styleUrl, iconUrl));
        }
    }

    public static void initMedevacStyle(Set<Association<String,String>> seenUrls, Container c) {
        String styleUrl = "medevac";
        String iconUrl = "icons/damaged.png";
        c.addToStyleSelector(buildStyle(styleUrl, iconUrl, "ffffffff", null, 2));
        seenUrls.add(new Association<String,String>(styleUrl, iconUrl));
    }


    /*
     * generate the KML description field
     * 
     *  @param qr the CoT object
     *  @param maxImages the maximum number of image links to include.
     * 
     */
    public static String buildDescription(CotElement qr, int maxImages, String baseUrl, String urlSuffix, boolean imageTag) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("null baseUrl");
        }

        String location = "";

        try {
            // if the hae altitude is 9999999, ignore.
            location = String.format("%.5f", qr.lat) + ", " + String.format("%.5f",  qr.lon) + (qr.hae.equals("9999999") ? "" : ", " + Math.round(qr.msl));
        } catch (Exception e) { }


        String remarks = "";
        try {
            if (qr.detailtext != null) {
                final String opening_remarks = "<remarks>";
                final String closing_remarks = "</remarks>";

                int remarks_start = qr.detailtext.indexOf(opening_remarks);
                if (remarks_start > -1) {
                    int remarks_end = qr.detailtext.indexOf(
                            closing_remarks,remarks_start + opening_remarks.length());
                    if (remarks_end > -1) {
                        remarks = qr.detailtext.substring(remarks_start + opening_remarks.length(), remarks_end);
                    }
                }
                if (remarks.length() > 0) {
                    remarks = "<br>" + remarks;
                }
            }

        } catch (Exception e) { }

        return "Type: " + qr.cottype + "<br> Time: " + qr.prettytime + " (" + qr.servertime + ")<br> " + location + remarks;
    }

    public static void getImageUrlList(List<CotElement> events, Map<String, byte[]> images, int maxImages, String baseUrl, KMLDao kmlDao, Set<String> uids) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("null baseUrl");
        }

        for (CotElement event : events) {
            // only look for images for uids known to be associated with images
            if (event != null && uids.contains(event.uid)) {
                try {
                    log.finest("building image url for uid " + event.uid);

                    String imageUrl = baseUrl + URLEncoder.encode(event.uid, "UTF-8");

                    // only do the database query / cache lookup for images for this uid if necessary
                    if (!images.containsKey(imageUrl)) {
                        images.put(imageUrl, getImageBytesForUid(event.uid, kmlDao));
                    }
                } catch (Exception e) {
                    log.fine("exception generating image url");
                }
            }
        }
    }

    public static void buildAndPopulateStyle(CotElement qr, Container styleParent) {
        styleParent.addToStyleSelector(buildStyle(qr));
    }

    public static Style buildStyle(CotElement qr) {

        String type = qr.cottype;
        Style s;

        log.fine("building style for cottype: " + type + " CotElement " + qr);

        if(type.startsWith("b-m-r")) {
            // route
            s = buildStyle(qr.styleUrl, qr.iconUrl, "7fffaaff", rgbToKmlHex(0, 255, 0), 4);
        } else if (type.startsWith("u-d-r")) {
            s = buildPolygonStyle(qr.styleUrl, "7fffaaff", "8fffffff", 4);
        }  else {
            // non-route -- primary case
            s = buildStyle(qr.styleUrl, qr.iconUrl, "7fffaaff", "8fffffff", 4);
        }

        return s;
    }

    public static Style buildStyle(String styleUrl, String iconUrl, 
            String labelColor, String lineColor, int lineWidth) {
        Style s = new Style();

        if(labelColor == null)
            labelColor = "7fffaaff";
        if(lineColor == null)
            lineColor = "8fffffff";

        s.withId(styleUrl)
        .withIconStyle(new IconStyle()
        .withScale(1.0)
        .withIcon(new BasicLink()
        .withHref(iconUrl)))
        .withLabelStyle(new LabelStyle()
        .withColor(labelColor)
        .withScale(1.5))
        .withLineStyle(new LineStyle()
        .withColor(lineColor)
        .withWidth(lineWidth));

        return s;
    }

    public static Style buildPolygonStyle(String styleUrl,
            String labelColor, String lineColor, int lineWidth) {
        Style s = new Style();

        if(labelColor == null)
            labelColor = "7fffaaff";
        if(lineColor == null)
            lineColor = "8fffffff";

        s.withId(styleUrl)
                .withLabelStyle(new LabelStyle()
                        .withColor(labelColor)
                        .withScale(1.5))
                .withLineStyle(new LineStyle()
                        .withColor(lineColor)
                        .withWidth(lineWidth))
                .withPolyStyle(new PolyStyle()
                        .withFill(false)
                        .withOutline(true));

        return s;
    }

    /**
     * Returns a Placemark containing no geometry, but with the name and style url initialized.
     */
    public static Placemark buildBasePlacemark(CotElement qr) {
        Placemark p = (new Placemark())
                .withName(qr.getCallsign())
                .withStyleUrl("#"+qr.styleUrl)
                .withId(qr.uid); //potential issue here; kml schema does not like spaces in ID values for Placemark
        return p;
    }

    /**
     * Placemark for any kind of cot point
     */
    public static Placemark buildPoint(CotElement qr) {
        Placemark p = buildBasePlacemark(qr);
        p.createAndSetPoint()
        .withAltitudeMode(buildAltitudeMode(qr.msl))
        .addToCoordinates(qr.geom);

        return p;
    }

    /**
     * Placemark for a cot rectangle
     */
    public static Placemark buildRectangle(CotElement qr, List<Coord> coords) {
        Placemark p = buildBasePlacemark(qr);
        Polygon polygon = p.createAndSetPolygon();
        Boundary boundary = polygon.createAndSetOuterBoundaryIs();
        LinearRing linearRing = boundary.createAndSetLinearRing();

        for (Coord coord : coords) {
            linearRing.addToCoordinates(coord.getLon(), coord.getLat());
        }

        Coord first = coords.get(0);
        linearRing.addToCoordinates(first.getLon(), first.getLat());

        return p;
    }

    public static Feature buildRangeAndBearing(CotElement qr, Document kmlDoc) {
        try {

            org.dom4j.Document doc = DocumentHelper.parseText(qr.detailtext);
            if (doc == null) {
                return null;
            }

            Element detail = doc.getRootElement();
            if (detail == null) {
                return null;
            }

            Element rangeElement = detail.element("range");
            if (rangeElement == null) {
                return null;
            }

            Element bearingElement = detail.element("bearing");
            if (bearingElement == null) {
                return null;
            }

            double range = Double.parseDouble(rangeElement.attribute("value").getStringValue());
            double bearing = Double.parseDouble(bearingElement.attribute("value").getStringValue());
            CotElement point2 = DistanceCalculations.computeDestinationPoint(qr, bearing, range);

            Folder folder = new Folder()
                    .withName(qr.getCallsign());

            Placemark line = folder.createAndAddPlacemark();
            LineString ls = line.createAndSetLineString();
            ls.addToCoordinates(qr.lon, qr.lat);
            ls.addToCoordinates(point2.lon, point2.lat);

            Placemark start = folder.createAndAddPlacemark();
            start.setName("Start");
            start.createAndSetPoint()
                    .addToCoordinates(qr.lon, qr.lat);

            Placemark end = folder.createAndAddPlacemark();
            end.setName("End");
            end.createAndSetPoint()
                    .addToCoordinates(point2.lon, point2.lat);

            return folder;
        } catch (Exception e) {
            log.fine("Exception in buildRangeAndBearing! " + e.getMessage());
            return null;
        }
    }

    /**
     * Placemark for a cot route
     */
    public static Placemark buildLineString(CotElement qr, List<Coord> route) {
        Placemark p = buildBasePlacemark(qr);

        LineString ls = p.createAndSetLineString();
        for (Coord c : route) {
            if (Double.isNaN(c.getAlt())) {
                ls.addToCoordinates(c.getLon(), c.getLat());
            } else {
                ls.addToCoordinates(c.getLon(), c.getLat(), c.getAlt());
            }
        }

        return p;
    }
    /**
     * Build a Placemark containing the track geometry formed by the timeseries of the query result list.
     * <p>
     * The list should be chronologically sorted, with the least recent at the front.
     * The name of the placemark is the callsign/uid associated with the most recent query result.
     */
    public static Placemark buildOptimizedTrack(LinkedList<CotElement> qrs, boolean includeExtendedData, int multiTrackThreshold, boolean optimizeExport) {

        long reportTimeThreshold = 0;
        
        if (multiTrackThreshold == 0) {
        	reportTimeThreshold = Integer.MAX_VALUE;
        } else {
        	reportTimeThreshold = multiTrackThreshold * 60 * 1000;
        }

        String uid = "";
        if (qrs != null && !qrs.isEmpty() && qrs.get(0) != null) {
            uid = qrs.get(0).uid;
        }
        log.finest("processing track for uid " + uid + " list size: " + qrs.size());


        Placemark p = buildBasePlacemark(qrs.getLast());
        PeekingIterator<CotElement> iter = new PeekingIterator<CotElement>(qrs.iterator());
        Date lastReportTime = new Date(0L);
        
        MultiTrack mt = p.createAndSetMultiTrack();
        mt.setInterpolate(false);
        mt.withAltitudeMode(buildAltitudeMode(qrs));
        AltitudeMode am = mt.getAltitudeMode();
        
        Track t = null;
        
        SimpleArrayData speed = null;
        SimpleArrayData ce = null;
        SimpleArrayData le = null;

        while (iter.hasNext()) {
        	CotElement qr = iter.next();
            log.fine("add to track for uid " + qr.uid);

        	if (qr.servertime.getTime() - lastReportTime.getTime() > reportTimeThreshold) {
	            t = mt.createAndAddTrack();
	            t.setAltitudeMode(am);

	            if (includeExtendedData) {
		            ExtendedData extendedData = t.createAndSetExtendedData();
		            SchemaData schemaData = extendedData.createAndAddSchemaData();
		            
		            schemaData.setSchemaUrl("#trackschema");
		            
		            speed = new SimpleArrayData();
		            ce = new SimpleArrayData();
		            le = new SimpleArrayData();
		            
		            speed.setName("speed");
		            ce.setName("ce");
		            le.setName("le");
		            
		            schemaData.addToSchemaDataExtension(speed);
		            schemaData.addToSchemaDataExtension(ce);
		            schemaData.addToSchemaDataExtension(le);
	            }
	            lastReportTime = qr.servertime;
        	} else {
        		lastReportTime = qr.servertime;
        	}
            
            t.addToWhen(getUtcTimestamp(qr.servertime));
            t.addToCoord(qr.geom.replace(',', ' ')); // <when> element uses spaces instead of commas

            if (includeExtendedData) {
	            if (!Double.isNaN(qr.course) && qr.course != 9999999.0) {
	            	t.addToAngles(String.valueOf(qr.course));
	            } else {
	            	t.addToAngles("");
	            }
	            
	            if (!Double.isNaN(qr.speed) && qr.speed != 9999999.0) {
	            	speed.addToValue(String.valueOf(qr.speed));
	            } else {
	            	speed.addToValue("");
	            }
	            
	            if (!Double.isNaN(qr.ce) && qr.ce != 9999999.0) {
	            	ce.addToValue(String.valueOf(qr.ce));
	            } else {
	            	ce.addToValue("");
	            }
	            
	            if (!Double.isNaN(qr.le) && qr.le != 9999999.0) {
	            	le.addToValue(String.valueOf(qr.le));
	            } else {
	            	le.addToValue("");
	            }
            }
            
            if (optimizeExport) {
	            // advance iterator until a point is found that is outside of some threshold for difference
	            while (iter.hasNext()) {
	                CotElement finger = iter.peek();
	                if (withinThreshold(qr, finger, lastReportTime, reportTimeThreshold)) {
	                	lastReportTime = finger.servertime;
	                    iter.next();
	                } else {
	                    break;
	                }
	            }
            }
        }

        return p;
    }

    public static boolean withinThreshold(CotElement qr1, CotElement qr2, Date lastReportTime, long reportTimeThreshold) {
        return (qr1.lat == qr2.lat && qr1.lon == qr2.lon && Math.abs(qr1.msl - qr2.msl) <= 0.5 &&
        		(qr2.servertime.getTime() - lastReportTime.getTime() <= reportTimeThreshold));
    }

    public static Placemark buildTrack(LinkedList<CotElement> qrs) {
        Placemark p = buildBasePlacemark(qrs.getLast());

        Track t = p.createAndSetTrack();
        t.withAltitudeMode(buildAltitudeMode(qrs));

        for (CotElement qr : qrs) {
            t.addToWhen(cotDateFormat.format(qr.servertime));
            t.addToCoord(qr.geom.replace(',', ' '));
        }

        return p;
    }

    public static List<Placemark> buildOptimizedTimeseriesPoint(List<CotElement> qrs, Timestamp stop) {
        List<Placemark> output = new ArrayList(qrs.size());
        PeekingIterator<CotElement> iter = new PeekingIterator<CotElement>(qrs.iterator());

        while (iter.hasNext()) {
            CotElement qr = iter.next();
            Placemark p = buildPoint(qr);

            // advance iterator until a new point is found
            while (iter.hasNext()) {
                CotElement finger = iter.peek();
                if (qr.geom.equals(finger.geom)) {
                    iter.next();
                } else {
                    break;
                }
            }

            Timestamp end = iter.hasNext() ? iter.peek().servertime : stop;
            p.withTimePrimitive(new TimeSpan()
            .withBegin(cotDateFormat.format(qr.servertime))
            .withEnd(cotDateFormat.format(end)));

            output.add(p);				
        }

        return output;
    }

    public static List<Placemark> buildTimeseriesPoint(List<CotElement> qrs, Timestamp stop) {
        List<Placemark> output = new ArrayList(qrs.size());
        PeekingIterator<CotElement> iter = new PeekingIterator<CotElement>(qrs.iterator());

        while (iter.hasNext()) {
            CotElement qr = iter.next();
            Placemark p = buildPoint(qr);

            Timestamp end = iter.hasNext() ? iter.peek().servertime : stop;
            p.withTimePrimitive(new TimeSpan()
            .withBegin(cotDateFormat.format(qr.servertime))
            .withEnd(cotDateFormat.format(end)));
            output.add(p);
        }

        return output;
    }

    public static List<Placemark> buildTimeseriesLineString(List<Association<CotElement,List<Coord>>> qrsAndCoords, Timestamp stop) {
        List<Placemark> output = new ArrayList(qrsAndCoords.size());
        PeekingIterator<Association<CotElement,List<Coord>>> iter = new PeekingIterator<Association<CotElement,List<Coord>>>(qrsAndCoords.iterator());

        while (iter.hasNext()) {
            Association<CotElement,List<Coord>> assoc = iter.next();
            CotElement qr = assoc.getKey();
            Placemark p = buildLineString(qr, assoc.getValue());
            Timestamp end = iter.hasNext() ? iter.peek().getKey().servertime : stop;

            p.withTimePrimitive(new TimeSpan()
            .withBegin(cotDateFormat.format(qr.servertime))
            .withEnd(cotDateFormat.format(end)));
            output.add(p);
        }
        return output;
    }

    /*
     * extract the base url from a request
     * 
     */
    public static String getBaseUrl(HttpServletRequest request) 
            throws ServletException, IOException {

        return  request.getScheme()
                + "://"
                + request.getServerName()
                + ":"
                + request.getServerPort()
                + request.getContextPath()
        		+ "/Marti";
    }

    /*
     * 
     * combine all images together (vertically) that are associated with a uid, so that they can be referenced by one <img> tag in KML
     * 
     */
    public static byte[] getImageBytesForUid(String uid, KMLDao kmlDao) {

        if (Strings.isNullOrEmpty(uid)) {
            throw new IllegalArgumentException("empty uid");
        }

        // get a list of cotIds for images for this uid
        List<Integer> cotIds = kmlDao.getImageCotIdsByUid(uid, Constants.KML_MAX_IMAGE_COUNT);

        return getImageBytesForCotIds(cotIds, kmlDao);
    }

    public static byte[] getImageBytesForCotIds(List<Integer> cotIds, KMLDao kmlDao) {

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        if (cotIds.isEmpty()) {
            return getEmptyImage();
        }

        log.finest("cotIds: " + cotIds);

        List<byte[]> images = new ArrayList<>();

        for (Integer cotId : cotIds) {
            byte[] image = kmlDao.getImageBytesByCotId(cotId);
            images.add(image);
        }

        // no images
        if (images.isEmpty()) {
            return getEmptyImage();
        }

        // if there's only one image, don't re-render it
        if (images.size() == 1) {
            return images.get(0);
        }

        // combine the images
        List<BufferedImage> bufferedImages = new ArrayList<>();

        int width = 0;
        int height = 0;

        for (byte[] image : images) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(image));

                bufferedImages.add(bufferedImage);

                // get max width
                if (bufferedImage.getWidth() > width) {
                    width = bufferedImage.getWidth();
                }

                // sum the height
                height += bufferedImage.getHeight();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        log.finest("final image width: " + width + " height: " + height);

        // use the image type of the first image
        BufferedImage combinedImage = new BufferedImage(width, height, bufferedImages.get(0).getType());

        Graphics graphics = combinedImage.getGraphics();

        int x = 0;
        int y = 0;

        for (BufferedImage bufferedImage : bufferedImages) {
            log.finest("drawing image at (" + x + ", " + y + ")");

            graphics.drawImage(bufferedImage, x, y, null);
            y += bufferedImage.getHeight();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // write combined image to byte array output stream
        try {
            ImageIO.write(combinedImage, "JPEG", out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] outBytes = out.toByteArray();

        log.finest("total image size (bytes): " + outBytes.length);

        return outBytes;
    }

    private static byte[] getEmptyImage() {
        // create empty image in case there are no cotIds
        BufferedImage empty = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);

        ByteArrayOutputStream emptyOut = new ByteArrayOutputStream();

        try {
            ImageIO.write(empty, "JPEG", emptyOut);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] emptyImage = emptyOut.toByteArray();
        return emptyImage;
    }
    
    /*
     * get the timestamp as UTC
     */
    private static String getUtcTimestamp(java.sql.Timestamp timestamp) {
        SimpleDateFormat df = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date(timestamp.getTime()));
    }
    
    public static String get2525BIconUrl(String cotType, boolean isMedevac, String groupName) {
        
        String iconUrl = "";
        
        // first look for medevac
        if (isMedevac) {
            log.finer("medevac icon");
            return KmlUtils.MEDEVAC_URL;
        }
        
        // then group
        if (!Strings.isNullOrEmpty(groupName)) {
            groupName = groupName.replace(" ", "");
            
            return "icons/team_" + groupName.toLowerCase() + ".png";
        }
        
        // then default to cotType
        if (Strings.isNullOrEmpty(cotType)) {
            throw new IllegalArgumentException("empty CoT type");
        }
        
        if(cotType.equals("b-m-p-s-p-i")) {
            iconUrl = "icons/bmpspi.png";
        } else if(cotType.startsWith("b-m-p-j")) {
            iconUrl = "icons/dip.png";
        } else if(cotType.startsWith("b-m-p") || cotType.startsWith("u-d-p") || cotType.startsWith("b-m-r") || cotType.startsWith("b-l")) {
            iconUrl = "icons/bmpw.png";
        } else if(cotType.startsWith("b-i-v")) {
            iconUrl = "icons/video.png";
        } else if(cotType.startsWith("b-d")) {
            iconUrl = "icons/b.png";
        } else if(cotType.contains("radsensor")) {
            iconUrl = "icons/radsensor.png";
        } else {
            try {
                String shrinkingType = cotType;
                while (iconUrl.isEmpty()) {
                    String bareicon = Icon2525bTypeResolver.mil2525bFromCotType(shrinkingType);
                    if (bareicon == null || bareicon.length() == 0) {
                        log.fine("Couldn't find icon for type: " + cotType);
                        bareicon = "none";
                    } else {
                        bareicon += ".png";
                        for (String f : mil2525filenames) {
                            //log.warning("Checking " + f + " against target: " + bareicon);
                            if (f.compareTo(bareicon) == 0) {
                                iconUrl = "mil-std-2525b/" + bareicon;
                                break;
                            }
                        }
                    }
                    if(iconUrl.isEmpty()) {
                        shrinkingType = shrinkingType.substring(0, shrinkingType.length() - 2);
                        if (shrinkingType.length() < 4) {
                            iconUrl = "icons/b.png";
                        }
                    }
                }
            } catch (Exception e) {
                log.severe(e.toString() + "; " + e.getMessage());
            }
        }
        
        return iconUrl;
    }

    public static Double[] parseSpatialCoordinates(String given) throws IllegalArgumentException {
    	String[] coordinateStrings = given.replaceAll(",", " ").split("\\s+");
    	List<Double> coordinates = new ArrayList<Double>();
    	for (String token : coordinateStrings) {
    		Double value = Double.parseDouble(token.trim());
    		coordinates.add(value);
    	} 
    	return coordinates.toArray(new Double[coordinates.size()]);
    }
    
    public static List<CotElement> trackToCot(Track track) {
    	
      	int size = track.getAngles().size();
      	if (track.getCoord().size() != size || track.getWhen().size() != size) {
      		log.severe("trackKmlToCot: track array size mismatch!");
      		return null;
      	}
      	
      	ArrayList<CotElement> cotElements = new ArrayList<CotElement>();
      	for (int ndx=0; ndx<size; ndx++) {
      		
      		CotElement cotElement = new CotElement();
      		cotElement.detailtext = "";

      		String angles = track.getAngles().get(ndx);
      		if (!Strings.isNullOrEmpty(angles)) {
                cotElement.course = Double.parseDouble(angles);
            }

      		String coord = track.getCoord().get(ndx);
      		// handle both comma and space delimiters
      		String delimiter = coord.contains(",") ? "," : " ";
      		String[] latlong = coord.split(delimiter);
      		cotElement.lon = Double.parseDouble(latlong[0]);
      		cotElement.lat = Double.parseDouble(latlong[1]);
      		if (latlong.length == 3) {
      			cotElement.hae = latlong[2];
      		}

      		//
      		// temporarily store the device time in servertime
       		//
      		
      		// try parsing with milliseconds
      		DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
      		String when = track.getWhen().get(ndx);
      		try {
	      		Date date = iso8601.parse(when);
	      		cotElement.servertime = new Timestamp(date.getTime());
      		} catch (ParseException e) {
      			
      			// try parsing just with seconds
          		iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
          		when = track.getWhen().get(ndx);
          		try {
    	      		Date date = iso8601.parse(when);
    	      		cotElement.servertime = new Timestamp(date.getTime());
          		} catch (ParseException e2) {
          			log.info("trackKmlToCot: error parsing date, " + when);
          		}
      		}
      		
      		cotElements.add(cotElement);
      	}
      	
      	ExtendedData extendedData = track.getExtendedData();
      	SchemaData schemaData = extendedData.getSchemaData().get(0);
      	
      	for (SimpleArrayData simpleArrayData : schemaData.getSchemaDataExtension()) {
      	
      		if (simpleArrayData.getValue().size() != size) {
          		log.severe("trackKmlToCot: track array size mismatch!");
          		return null;
      		}
      		
      		String name = simpleArrayData.getName();
	      	for (int ndx=0; ndx<size; ndx++) {
	      		CotElement cotElement = cotElements.get(ndx);
	      		String rawValue = simpleArrayData.getValue().get(ndx);
	      		
      			if (name.equals("speed") || name.equals("ce") || name.equals("le")) {
	      		
		      		double value = 0;
		      		try {
		      			value = Double.parseDouble(rawValue);
		      		} catch (NumberFormatException e) {
		      			log.info("trackKmlToCot: error parsing " + name + ", " + value);
		      		}
	      		
	      			if (name.equals("speed")) {
	      				cotElement.speed = value;
	      			} else if (name.equals("ce")) {
	      				cotElement.ce = value;
	      			} else if (name.equals("le")) {
	      				cotElement.le = value;
	      			} 
	      				      			
      			} else if (name.equals("geopointsrc")) {
      				cotElement.detailtext += " geopointsrc=\"" + rawValue + "\"";
      			} else if (name.equals("altsrc")) {
      				cotElement.detailtext += " altsrc=\"" + rawValue + "\"";
      			}
      		}
      	}

      	return cotElements;
    }
    
    public static List<CotElement> trackKmlToCot(Kml kml) {
    	
    	List<CotElement> cotElements = null;
    	
    	try {
	      	Document doc = (Document)kml.getFeature();
	      	
	      	List<Feature> features = doc.getFeature();
	      	if (features.get(0) instanceof Folder) {
	      		Folder folder = (Folder)features.get(0);
	      		features = folder.getFeature();
	      	}
	      	
	      	Placemark placemark = (Placemark)features.get(0);
	      	Geometry geometry = placemark.getGeometry();
	      	
	      	if (geometry instanceof Track) {
	      		cotElements = trackToCot((Track)geometry);
	      	} else if (geometry instanceof MultiTrack) {
	      		cotElements = new ArrayList<CotElement>();
		      	MultiTrack multiTrack = (MultiTrack)geometry;
		      	for (Track track : multiTrack.getTrack()) {
		      		List<CotElement> tmpCotElements = trackToCot(track);
			      	cotElements.addAll(tmpCotElements);
		      	}
	      	}
	      	
    	} catch (Exception e) {
      		log.severe("trackKmlToCot: exception!, " + e.toString() + "; " + e.getMessage());
      		cotElements = null;
    	}
      	
      	return cotElements;
    }
    
   
}
