package tak.server;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bbn.marti.remote.socket.SituationAwarenessMessage;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.MessageConversionUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import tak.server.cot.CotEventContainer;

// This test can be updated to use the Spring JUnit TestRunner and use beans from the ServerConfiguration and XML Spring config
public class RepeaterMessageConversionTest {
	
	// mapper configuration
	private ObjectMapper getObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
	    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	    
	    return mapper;
	}
	
	private SAXReader getSAXReader() throws ParserConfigurationException, SAXException {

		SAXParserFactory factory = SAXParserFactory.newInstance();

		SAXParser parser = factory.newSAXParser();

		SAXReader reader = new SAXReader(parser.getXMLReader());
		reader.setValidation(false);

		return reader;
	}
	
	private Document parseXML(String xml) throws DocumentException, ParserConfigurationException, SAXException {
		return getSAXReader().read(new InputSource(new StringReader(xml)));
	}

	String alert911 = "<event version=\"2.0\" uid=\"16173334444-9-1-1\" type=\"b-a-o-tbl\" time=\"2018-12-17T22:47:15.109Z\" start=\"2018-12-17T22:47:15.109Z\" stale=\"2018-12-17T22:47:25.109Z\" how=\"m-g\"><point lat=\"42.389128\" lon=\"-71.147716\" hae=\"6.1\" ce=\"6.0\" le=\"9999999.0\"/><detail><link uid=\"ANDROID-358982072593830\" type=\"a-f-G-U-C\" relation=\"p-p\"/><contact callsign=\"BOB-Alert\"/><emergency type=\"911 Alert\">BOB</emergency><precisionlocation altsrc=\"???\" geopointsrc=\"???\"/></detail></event>";
	
	Logger logger = LoggerFactory.getLogger(RepeaterMessageConversionTest.class);
	
	@Test
	public void parseCotAlert() throws DocumentException, ParserConfigurationException, SAXException {
		
		CotEventContainer alert911cot = new CotEventContainer(parseXML(alert911));
		
		logger.debug("alert911cot: " + alert911cot);
		
	}
	
	@Test
	public void convertCotToJsonAndBack() throws DocumentException, ParserConfigurationException, SAXException, IOException {
		
		CotEventContainer alert911cot = new CotEventContainer(parseXML(alert911));
		
		SituationAwarenessMessage alert911sa = MessageConversionUtil.saMessageFromCot(alert911cot);
		
		logger.debug("alert911sa: " + alert911sa);
		
		logger.debug("detail json: " + alert911sa.getDetailJson());
		
		ObjectMapper mapper = getObjectMapper();
		
		String alert911json = mapper.writeValueAsString(alert911sa);
		
		logger.debug("alert911json: " + alert911json);
		
		SituationAwarenessMessage alert911saFromJson = mapper.readValue(alert911json, SituationAwarenessMessage.class);
		
		String alert911cotFromJson = new CommonUtil().saToCot(alert911saFromJson);
		
		logger.debug("alert911cotFromJson: " + alert911cotFromJson);
	}
}
