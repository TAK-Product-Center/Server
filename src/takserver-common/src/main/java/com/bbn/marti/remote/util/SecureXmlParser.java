package com.bbn.marti.remote.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public final class SecureXmlParser {
    private SecureXmlParser(){}

    protected static final Logger logger = LoggerFactory.getLogger(SecureXmlParser.class);

	private static ThreadLocal<DocumentBuilderFactory> documentBuilderFactoryThreadLocal = new ThreadLocal<>();
	private static ThreadLocal<DocumentBuilder> documentBuilderThreadLocal = new ThreadLocal<>();

	public static Document makeDocument(InputStream inputStream){
		Document doc = null;
    	try {
			DocumentBuilderFactory dbf = documentBuilderFactoryThreadLocal.get();
			DocumentBuilder db = documentBuilderThreadLocal.get();

			if (dbf == null) {
				dbf = DocumentBuilderFactory.newInstance();
				dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
				dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
				dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

				dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				dbf.setXIncludeAware(false);
				dbf.setExpandEntityReferences(false);

				documentBuilderFactoryThreadLocal.set(dbf);
			}

			if (db == null) {
				db = dbf.newDocumentBuilder();
				documentBuilderThreadLocal.set(db);
			} else {
				db.reset();
			}

			doc = db.parse(inputStream);
		}
		catch (ParserConfigurationException e){
    		logger.debug("Unable to configure parser for unmarshalling: " + e);
		}
		catch (SAXException e){
			logger.debug("Error parsing XML document: " + e);
		}
		catch (IOException e){
			logger.debug("Error with file: " + e);
		}
		return doc;
    }

    public static Document makeDocument(String xml){
		Document doc = null;
		try {
			doc = makeDocument(new ByteArrayInputStream(xml.getBytes("UTF-8")));
		}
		catch (UnsupportedEncodingException e){
			logger.debug("XML isn't encoded with UTF-8" + e);
		}
		return doc;
	}
}
