package tak.server;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import tak.server.cot.CotEventContainer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class XPathPerformanceTest {

	private static final Logger logger = LoggerFactory.getLogger(XPathPerformanceTest.class);

	private static String CHAT_TO_MISSION = "<event version=\"2.0\" uid=\"GeoChat.ANDROID-358982072593830.Green.2b4fb2c4-300d-41e6-9df3-f21b597b87e3\" type=\"b-t-f\" time=\"2017-06-19T17:13:53.047Z\" start=\"2017-06-19T17:13:53.047Z\" stale=\"2017-06-20T17:13:53.047Z\" how=\"h-g-i-g-o\"><point lat=\"0.0\" lon=\"0.0\" hae=\"9999999.0\" ce=\"9999999\" le=\"9999999\"/><detail><__chat id=\"Green\" chatroom=\"Green\"><chatgrp uid0=\"ANDROID-358982072593830\" id=\"Green\"/></__chat><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\"/><remarks time=\"2017-06-19T17:13:53.047Z\" source=\"BAO.F.ATAK.ANDROID-358982072593830\">hey there</remarks><precisionlocation geopointsrc=\"???\" altsrc=\"???\"/><_flow-tags_ marti1=\"2017-06-19T17:13:53Z\"/><marti><dest mission=\"jimmy\"/></marti></detail></event>";

	@Autowired
	private ObjectMapper objectMapper;

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


	@Test
	public void stringMatchTiming() {

		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			CHAT_TO_MISSION.contains("mission");
			long end = System.nanoTime();

			logger.info("string match mission success timing: " + (end - start) + " ns");
		}

		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			CHAT_TO_MISSION.contains("zission");
			long end = System.nanoTime();

			logger.info("string no-match timing: " + (end - start) + " ns");
		}

	}

	@Test
	public void stringMatchTimingWithLowercase() {

		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			CHAT_TO_MISSION.contains("zission");
			long end = System.nanoTime();

			logger.info("string no-match timing toLowerCase(): " + (end - start) + " ns");
		}

	}

	@Test
	public void missionXpathTiming() throws DocumentException, ParserConfigurationException, SAXException {

		CotEventContainer chatCot = null;

		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			chatCot = new CotEventContainer(parseXML(CHAT_TO_MISSION));
			long end = System.nanoTime();

			logger.info("CoT parse timing: " + (end - start) + " ns");
		}

		logger.info("chatCot: " + chatCot);

		for (int i = 0; i < 10; i++) {
			long start = System.nanoTime();
			List<Node> missionDests = chatCot.getDocument().selectNodes("/event/detail/marti/dest[@mission]");
			long end = System.nanoTime();

			logger.info("mission XPath timing: " + (end - start) + " ns");
		}



	}
}
