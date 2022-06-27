package com.bbn.marti.dao.kml;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.model.kml.CotElement;
import com.bbn.marti.service.kml.IconStrategy;

import static org.mockito.Mockito.*;


/**
 * Unit tests for JDBCCachingKMLDao
 * 
 * 
 */

public class JDBCCachingKMLDaoTest {

	String detailCallsign = "<detail><contact callsign=\"Cerberus.GBL.1400 Blue\"/><supplementalData originator_id=\"17757\"/><color red=\"0\" green=\"0\" blue=\"255\"/><_flow-tags_ marti_lite=\"2014-09-10T20:30:43.580Z\"/></detail>";
	String callsign = "Cerberus.GBL.1400 Blue";
	
	JDBCCachingKMLDao dao;
	
	String detailIconAndColor = "<detail><contact callsign='B 1'/><status readiness='true'/><usericon iconsetpath='COT_MAPPING_SPOTMAP/b-m-p-s-m/-16776961'/><color argb='-16776961'/><archive/><precisionlocation geopointsrc='???' altsrc='???'/></detail>";

	Logger logger = LoggerFactory.getLogger(JDBCCachingKMLDaoTest.class);
	
	@SuppressWarnings("unchecked")
    @Before
	public void setup() {
		dao = new JDBCCachingKMLDao();
		dao.setIconStrategy(mock(IconStrategy.class));
	}

	@Test
	public void testParseDetails() {
		
		CotElement cotElement = new CotElement();
		
		cotElement.detailtext = detailCallsign;
		
		dao.parseDetailText(cotElement);
		
		System.out.println("callsign: " + cotElement.callsign);
		
		assertEquals(cotElement.callsign, callsign);
	}
}
