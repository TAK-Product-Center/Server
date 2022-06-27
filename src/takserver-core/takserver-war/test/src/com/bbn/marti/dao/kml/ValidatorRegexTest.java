package com.bbn.marti.dao.kml;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for JDBCCachingKMLDao
 * 
 * 
 */

public class ValidatorRegexTest {

	@Test
	public void testCommonNameRegex() {
	    
	    String cnValid = "CN=testClient_4, OU=ATAK, O=BBN, L=Cambridge, ST=MA, C=US";
        String cnInvalid = "CN=testClient_function();_4, OU=ATAK, O=BBN, L=Cambridge, ST=MA, C=US";

	    String regexCommonName = "^[\\w\\s,=]*$";
	    
	    assertTrue(cnValid.matches(regexCommonName));
	    assertFalse(cnInvalid.matches(regexCommonName));
	}
}
