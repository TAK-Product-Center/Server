package com.bbn.cot.filter;

import com.bbn.marti.config.GeospatialFilter;

import com.bbn.marti.util.GeomUtils;
import tak.server.cot.CotEventContainer;

import org.dom4j.Node;

import java.util.Arrays;
import java.util.List;

/**
 * Drops the CoT event if not found within GeospatialFilter
 */
public class GeospatialEventFilter implements CotFilter {

    private GeospatialFilter filter;
    private boolean noFilterCheckTypes = true;
    private boolean noFilterCheckTAKClient = true;
    private boolean noFilterCheckOrigin = true;

    private static final List<String> noFilterTypes = Arrays.asList(
                "b-a-o-tbl",    // NineOneOne
                "b-a-o-can",     // Cancel
                "b-a-g",         // GeoFenceBreach
                "b-a-o-pan",     // RingTheBell
                "b-a-o-opn");    // TroopsInContact


    public GeospatialEventFilter(GeospatialFilter filter) {
        this.filter = filter;
    }

    public GeospatialEventFilter(GeospatialFilter filter, boolean noFilterCheckTypes, boolean noFilterCheckTAKClient,
                                 boolean noFilterCheckOrigin) {
        this(filter);
        this.noFilterCheckTypes = noFilterCheckTypes;
        this.noFilterCheckTAKClient = noFilterCheckTAKClient;
        this.noFilterCheckOrigin = noFilterCheckOrigin;
    }

    private boolean noFilter(CotEventContainer c) {

        if (noFilterCheckTypes) {
            // check the noFilterList
            if (noFilterTypes.contains(c.getType())) {
                return true;
            }
        }

        if (noFilterCheckTAKClient) {
            // see if the cot event is for a TAK device
            Node name = c.getDocument().selectSingleNode("/event/detail/__group/@name");
            Node role = c.getDocument().selectSingleNode("/event/detail/__group/@role");
            if (name != null && role != null &&
                    name.getText() != null && role.getText() != null) {
                return true;
            }
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
        if (noFilterCheckOrigin) {
            if (latitude == 0 && longitude == 0) {
                return c;
            }
        }

        // iterate over the filters
        for (GeospatialFilter.BoundingBox bbox : filter.getBoundingBox()) {

            // return the cot event if found within one of the inputs filters
            if (GeomUtils.bboxContainsCoordinate(bbox, latitude, longitude)) {
                return c;
            }
        }

        // return null to indicate event should be dropped
        return null;
    }
}
