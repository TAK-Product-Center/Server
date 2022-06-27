package com.bbn.marti.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;

public class KmlParser {

    private static final Logger logger = LoggerFactory.getLogger(KmlParser.class);

    private static ThreadLocal<Unmarshaller> unmarshallerThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<SAXParserFactory> saxParserFactoryThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<SAXParser> saxParserThreadLocal = new ThreadLocal<>();

    public static Kml unmarshal(String input) {
        try {
            if (unmarshallerThreadLocal.get() == null) {
                unmarshallerThreadLocal.set(JAXBContext.newInstance(new Class[]{ Kml.class }).createUnmarshaller());
            }

            if (saxParserFactoryThreadLocal.get() == null) {
                saxParserFactoryThreadLocal.set(SAXParserFactory.newInstance());
                saxParserFactoryThreadLocal.get().setNamespaceAware(true);
                saxParserFactoryThreadLocal.get().setValidating(false);

                //Disable XXE - taken from https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
                saxParserFactoryThreadLocal.get().setFeature("http://xml.org/sax/features/external-general-entities", false);
                saxParserFactoryThreadLocal.get().setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                saxParserFactoryThreadLocal.get().setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            }

            if (saxParserThreadLocal.get() == null) {
                saxParserThreadLocal.set(saxParserFactoryThreadLocal.get().newSAXParser());
            }

            InputSource inputSource = new InputSource(new StringReader(input));
            SAXSource saxSource = new SAXSource(saxParserThreadLocal.get().getXMLReader(), inputSource);
            Kml kml = (Kml)unmarshallerThreadLocal.get().unmarshal(saxSource);
            return kml;

        } catch (Exception e) {
            logger.error("exception parsing kml! ", e);
        }

        return null;
    }
}
