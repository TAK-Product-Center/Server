package com.bbn.marti.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.bbn.marti.dao.kml.IconRepository;
import com.bbn.marti.model.kml.CotElement;
import com.bbn.marti.model.kml.Icon;
import com.bbn.marti.service.kml.AbstractKmlIconStrategy;
import com.bbn.marti.service.kml.KmlIconStrategyJaxb;

import static org.mockito.Mockito.*;

/**
 * Unit tests for KmlIconStrategyJaxb
 * 
 * 
 */

public class KmlIconStrategyJaxbTest {

    AbstractKmlIconStrategy iconStrategy = null;
	
	String detailIconAndColorSpotmap = "<detail><contact callsign='B 1'/><status readiness='true'/><usericon iconsetpath='COT_MAPPING_SPOTMAP/b-m-p-s-m/-16776961'/><color argb='-16776961'/><archive/><precisionlocation geopointsrc='???' altsrc='???'/></detail>";
	String detailIconsetPath = "<detail><contact callsign='B 1'/><status readiness='true'/><usericon iconsetpath= '34ae1613-9645-4222-a9d2-e5f243dea2865/Animals/antelope.png' /><precisionlocation geopointsrc='???' altsrc='???'/></detail>";
	String iconsetUid = "34ae1613-9645-4222-a9d2-e5f243dea2865";
	String iconGroup = "Animals";
	String iconName = "antelope.png";

	Logger logger = LoggerFactory.getLogger(KmlIconStrategyJaxbTest.class);
	
	@Before
	public void setup() {
	    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
	    
	    // set log level for tests
	    root.setLevel(Level.ALL);
	    
		iconStrategy = new KmlIconStrategyJaxb();
		
		IconRepository mockRepository = mock(IconRepository.class); 
		
		iconStrategy.setIconRepository(mockRepository);
		
		when(mockRepository.findIconByIconsetUidAndGroupAndName( (String) any(), (String) any(), (String) any())).thenReturn(new Icon());
	}

	@Test
    public void testAssignIconSpotmap() {
        
        CotElement cotElement = new CotElement();
        
        cotElement.detailtext = detailIconAndColorSpotmap;
        cotElement.cottype = "b-m-p-s-m";
        
        long start = System.currentTimeMillis();
        iconStrategy.assignIcon(cotElement);
        long duration = System.currentTimeMillis() - start;
        
        logger.debug("icon assignment running time: " + duration);
        
        assertEquals(cotElement.iconArgbColor, new Long(-16776961));
        assertEquals(cotElement.iconSetPath, "COT_MAPPING_SPOTMAP/b-m-p-s-m/-16776961");
    }
	
	@Test
    public void testAssignIconIconset() {
        
        CotElement cotElement = new CotElement();
        
        cotElement.detailtext = detailIconsetPath;
        
        cotElement.cottype = "b-m-p-s-m";
        
        long start = System.currentTimeMillis();
        iconStrategy.assignIcon(cotElement);
        long duration = System.currentTimeMillis() - start;
        
        logger.debug("icon assignment running time: " + duration);
        
        assertEquals(cotElement.iconArgbColor, null);
        assertEquals(cotElement.iconSetUid, iconsetUid);
        assertEquals(cotElement.iconName, iconName);
        assertEquals(cotElement.iconGroup, iconGroup);
    }
}
