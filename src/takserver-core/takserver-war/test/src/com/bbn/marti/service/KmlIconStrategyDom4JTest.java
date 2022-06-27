package com.bbn.marti.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.model.kml.CotElement;
import com.bbn.marti.service.kml.KmlIconStrategyDom4J;

/**
 * Unit tests for KmlIconStrategy
 * 
 * 
 */

public class KmlIconStrategyDom4JTest {

    KmlIconStrategyDom4J iconStrategy = null;
	
	String detailIconAndColor = "<detail><contact callsign='B 1'/><status readiness='true'/><usericon iconsetpath='COT_MAPPING_SPOTMAP/b-m-p-s-m/-16776961'/><color argb='-16776961'/><archive/><precisionlocation geopointsrc='???' altsrc='???'/></detail>";

	Logger logger = LoggerFactory.getLogger(KmlIconStrategyDom4JTest.class);
	
	@Before
	public void setup() {
		iconStrategy = new KmlIconStrategyDom4J();
	}

	@Test
    public void testAssignIcon() {
        
        CotElement cotElement = new CotElement();
        
        cotElement.detailtext = detailIconAndColor;
        cotElement.cottype = "b-m-p-s-m";
        
        iconStrategy.assignIcon(cotElement);
        
        assertEquals(cotElement.iconArgbColor, new Long(-16776961));
        assertEquals(cotElement.iconSetPath, "COT_MAPPING_SPOTMAP/b-m-p-s-m/-16776961");
    }
}
