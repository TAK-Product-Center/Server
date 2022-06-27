package com.bbn.marti.service;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

import com.bbn.marti.dao.kml.KMLDao;
import com.bbn.marti.model.kml.CotElement;
import com.bbn.marti.service.kml.KMLServiceImpl;

import de.micromata.opengis.kml.v_2_2_0.Kml;

/**
 * Unit tests for KMLServiceImplTest
 * 
 * 
 */

public class KMLServiceImplTest {

	KMLServiceImpl service = null;

	List<CotElement> cotElements = null;
	
	@Before
	public void setup() {
		
		service = new KMLServiceImpl();
	}

	@Test
	public void testProcess() throws Exception {
		
		// create mock DAO
		KMLDao dao = mock(KMLDao.class);
		
		service.setKmlDao(dao);
		
		List<CotElement> cotElements = new ArrayList<CotElement>();
		
		Mockito.when(dao.getCotElements("a-f", 0)).thenReturn(cotElements);
		
		Kml kml = service.process("a-f", 0);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		kml.marshal(os);
		
		os.flush();
		
		String output = new String(os.toByteArray(), "UTF-8");
		
		os.close();
		
		System.out.println("kml output: " + output);
		
		assertNotNull(output);
		
		// could do more assertions here by parsing the output XML, and checking XPath values
	}
}
