package com.bbn.cot.filter;

import com.bbn.marti.config.GeospatialFilter;

import tak.server.cot.CotEventContainer;

import org.dom4j.Node;

import java.util.Arrays;
import java.util.List;

/**
 * Drops the CoT event if not found within GeospatialFilter
 */
public class GeospatialEventFilter implements CotFilter {

    private GeospatialFilter filter;

    private static final List<String> noFilterTypes = Arrays.asList(
            "b-a-o-tbl",    // NineOneOne
                "b-a-o-can",     // Cancel
                "b-a-g",         // GeoFenceBreach
                "b-a-o-pan",     // RingTheBell
                "b-a-o-opn");    // TroopsInContact


    public GeospatialEventFilter(GeospatialFilter filter) {
        this.filter = filter;
    }

    private boolean noFilter(CotEventContainer c) {

        // check the noFilterList
        if (noFilterTypes.contains(c.getType())) {
            return true;
        }

        // see if the cot event is for a TAK device
        Node name = c.getDocument().selectSingleNode("/event/detail/__group/@name");
        Node role = c.getDocument().selectSingleNode("/event/detail/__group/@role");
        if (name != null && role != null &&
                name.getText() != null && role.getText() != null) {
            return true;
        }

        return false;
    }

    public CotEventContainer filter(CotEventContainer c) {

        if (noFilter(c)) {
            return c;
        }

        // if we dont have any filters, return the event
        if (filter.getBoundingBox() == null || filter.getBoundingBox().size() == 0) {
            return c;
        }

        // get the current coordinates as doubles
        double latitude = Double.parseDouble(c.getLat());
        double longitude = Double.parseDouble(c.getLon());

        // dont apply filters to points without location info
        if (latitude == 0 && longitude == 0) {
            return c;
        }

        // iterate over the filters
        for (GeospatialFilter.BoundingBox bbox : filter.getBoundingBox()) {

            boolean validLongitude = false;
            if (bbox.getMaxLongitude() > bbox.getMinLongitude()) {
                validLongitude = longitude >= bbox.getMinLongitude() && longitude <= bbox.getMaxLongitude();
            } else {
                validLongitude = longitude >= bbox.getMinLongitude() || longitude <= bbox.getMaxLongitude();
            }

            // return the cot event if found within one of the inputs filters
            if (validLongitude &&
                    latitude >= bbox.getMinLatitude() && latitude <= bbox.getMaxLatitude()) {
                return c;
            }
        }

        // return null to indicate event should be dropped
        return null;
    }
}
