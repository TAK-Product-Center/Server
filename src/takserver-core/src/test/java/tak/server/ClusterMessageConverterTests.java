package tak.server;

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dom4j.Document;
import org.dom4j.DocumentException;
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

import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.google.gson.Gson;

import tak.server.cluster.ClusterMessageWrapper;
import tak.server.cot.CotEventContainer;
import tak.server.messaging.MessageConverter;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class ClusterMessageConverterTests {

	private static final Logger logger = LoggerFactory.getLogger(ClusterMessageConverterTests.class);

	@Autowired
	private MessageConverter converter;

	private static String CHAT_TO_MISSION = "<event version=\"2.0\" uid=\"GeoChat.ANDROID-358982072593830.Green.2b4fb2c4-300d-41e6-9df3-f21b597b87e3\" type=\"b-t-f\" time=\"2017-06-19T17:13:53.047Z\" start=\"2017-06-19T17:13:53.047Z\" stale=\"2017-06-20T17:13:53.047Z\" how=\"h-g-i-g-o\"><point lat=\"0.0\" lon=\"0.0\" hae=\"9999999.0\" ce=\"9999999\" le=\"9999999\"/><detail><__chat id=\"Green\" chatroom=\"Green\"><chatgrp uid0=\"ANDROID-358982072593830\" id=\"Green\"/></__chat><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\"/><remarks time=\"2017-06-19T17:13:53.047Z\" source=\"BAO.F.ATAK.ANDROID-358982072593830\">hey there</remarks><precisionlocation geopointsrc=\"???\" altsrc=\"???\"/><_flow-tags_ marti1=\"2017-06-19T17:13:53Z\"/><marti><dest mission=\"jimmy\"/></marti></detail></event>";

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
	public void serializeMessageTest() throws DocumentException, ParserConfigurationException, SAXException {

		CotEventContainer chatCot = null;

		for (int i = 0; i < 10; i++) {
			chatCot = new CotEventContainer(parseXML(CHAT_TO_MISSION));
		}

		NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
		groups.add(new Group("__ANON__", Direction.IN));

		chatCot.setContext(Constants.GROUPS_KEY, groups);

		byte[] clusterMessage = converter.cotToDataMessage(chatCot);

		logger.debug("clusterMessage: " + new String(clusterMessage));
	}

	@Test
	public void deserializeMessageTest() throws DocumentException, ParserConfigurationException, SAXException, RemoteException {

		CotEventContainer chatCot = null;
		chatCot = new CotEventContainer(parseXML(CHAT_TO_MISSION));

		NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
		groups.add(new Group("__ANON__", Direction.IN));

		chatCot.setContext(Constants.GROUPS_KEY, groups);

		byte[] clusterMessageBytes = converter.cotToDataMessage(chatCot);

		CotEventContainer chatCotDeserialized = converter.dataMessageToCot(clusterMessageBytes, false);

		org.junit.Assert.assertTrue("GeoChat.ANDROID-358982072593830.Green.2b4fb2c4-300d-41e6-9df3-f21b597b87e3".equals(chatCotDeserialized.getUid()));
	}
}
