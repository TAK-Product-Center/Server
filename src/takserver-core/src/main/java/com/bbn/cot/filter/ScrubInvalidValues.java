

package com.bbn.cot.filter;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.service.DistributedConfiguration;

import tak.server.cot.CotEventContainer;

public class ScrubInvalidValues implements CotFilter {

  	private static String TRACK_XPATH = "/event/detail/track";
  	private static String POINT_XPATH = "/event/point";
  	
  	@Autowired
  	private DistributedConfiguration config;
  	
    @Override
	public CotEventContainer filter(final CotEventContainer cot) {

		if (config.getFilter().getScrubber() != null &&
			config.getFilter().getScrubber().isEnable()) {
			boolean drop = true;
			if (config.getFilter().getScrubber().getAction().compareToIgnoreCase("overwrite") == 0) {
				drop = false;
			}

			Element point = (Element) cot.getDocument().selectSingleNode(POINT_XPATH);
			Element track = (Element) cot.getDocument().selectSingleNode(TRACK_XPATH);

			// this could be elegant if we only dropped... then we could 'OR' all the checks together
			// since we might be wanting to preserve it though, we want to make sure all the scrub
			// operations actually happen.

			//check all the point values:
			boolean [] results = new boolean[7];
			int i = 0;
			results[i++] = isInvalidDouble(point.attribute("lat"), -91, 91, 0);
			results[i++] = isInvalidDouble(point.attribute("lon"), -181, 181, 0);
			results[i++] = isInvalidDouble(point.attribute("hae"), -99999, 99999999, 0);
			results[i++] = isInvalidDouble(point.attribute("le"), 0, 99999999, 0);
			results[i++] = isInvalidDouble(point.attribute("ce"), 0, 99999999, 0);

			//optional tags:
			if(track != null && track.attribute("course") != null) {
				results[i++] = isInvalidDouble(track.attribute("course"), 0, 360, 0);
			}
			if(track != null && track.attribute("speed") != null) {
				results[i++] = isInvalidDouble(track.attribute("speed"), 0, 999999, 0);
			}

			if (drop) {
				for(int j = 0; j<i; j++) {
					if(results[j] == true)
						return null;
				}
			}
		}
		return cot;
	}

	boolean isInvalidInt(Attribute attribute, int lowerBound, int upperBound, int overwriteValue) {
			try {
				int i = Integer.parseInt(attribute.getValue());
				if(i >= lowerBound && i <= upperBound) {
					return false;
				}
			} catch(Exception e) {
			}
			attribute.setValue(Integer.toString(overwriteValue));
			return true;
	}

	boolean isInvalidDouble(Attribute attribute, double lowerBound, double upperBound, double overwriteValue) {
		try {
                        //logger.warn("Checking attr: " + attribute.getName() + ": " + attribute.getValue());
			double i = Double.parseDouble(attribute.getValue());
			if(i >= lowerBound && i <= upperBound) {
				return false;
			}
                        //logger.warn(" FAILED Bounds check!");
		} catch(Exception e) {
		}
		//logger.error("Corrected value: " + attribute.getName() + ":" + attribute.getValue());
		attribute.setValue(Double.toString(overwriteValue));
		return true;
	}
}
