package tak.server;

import java.io.StringReader;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.bbn.cot.CotParserCreator;

import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.proto.StreamingProtoBufHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TakServerTestApplicationConfig.class})
public class ProtoAndCotMessageConversionTests {

	private static final Logger logger = LoggerFactory.getLogger(ProtoAndCotMessageConversionTests.class);
	
	private CotParser parser = CotParserCreator.newInstance();

	private StreamingProtoBufHelper cotProtoConverter = new StreamingProtoBufHelper();

	private static String CHAT_TO_MISSION = "<event version=\"2.0\" uid=\"GeoChat.ANDROID-358982072593830.Green.2b4fb2c4-300d-41e6-9df3-f21b597b87e3\" type=\"b-t-f\" time=\"2017-06-19T17:13:53.047Z\" start=\"2017-06-19T17:13:53.047Z\" stale=\"2017-06-20T17:13:53.047Z\" how=\"h-g-i-g-o\"><point lat=\"0.0\" lon=\"0.0\" hae=\"9999999.0\" ce=\"9999999\" le=\"9999999\"/><detail><__chat id=\"Green\" chatroom=\"Green\"><chatgrp uid0=\"ANDROID-358982072593830\" id=\"Green\"/></__chat><link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"ANDROID-358982072593830\"/><remarks time=\"2017-06-19T17:13:53.047Z\" source=\"BAO.F.ATAK.ANDROID-358982072593830\">hey there</remarks><precisionlocation geopointsrc=\"???\" altsrc=\"???\"/><_flow-tags_ marti1=\"2017-06-19T17:13:53Z\"/><marti><dest mission=\"jimmy\"/></marti></detail></event>";
	
	private static String SA = "<event version=\"2.0\" uid=\"ANDROID-352413144215585\" type=\"a-f-G-U-C\" how=\"m-g\" time=\"2020-02-12T13:16:07Z\" start=\"2020-02-12T13:16:05Z\" stale=\"2020-02-12T13:16:50Z\"><point lat=\"40.255716\" lon=\"-72.045306\" hae=\"-22.22983896651138\" ce=\"4.9\" le=\"9999999.0\"/><detail><__group name=\"Dark Blue\" role=\"Team Member\"/><precisionlocation geopointsrc=\"GPS\" altsrc=\"GPS\"/><status battery=\"32\"/><takv device=\"SAMSUNG SM-G975U1\" platform=\"ATAK-CIV\" os=\"29\" version=\"3.12.0-45691.45691-CIV\"/><track speed=\"0.0\" course=\"344.72362164876733\"/><contact endpoint=\"*:-1:stcp\" phone=\"19999999999\" callsign=\"coolata\"/><uid Droid=\"coolata\"/></detail></event>";
	
	private SAXReader getSAXReader() throws ParserConfigurationException, SAXException {

		SAXParserFactory factory = SAXParserFactory.newInstance();

		SAXParser parser = factory.newSAXParser();

		SAXReader reader = new SAXReader(parser.getXMLReader());
		reader.setValidation(false);

		return reader;
	}
	
	@SuppressWarnings("unused")
	private Document parseXML(String xml) throws DocumentException, ParserConfigurationException, SAXException {
		return getSAXReader().read(new InputSource(new StringReader(xml)));
	}
	
	@Test
	public void parseCotChat() throws DocumentException  {
		
		CotEventContainer cot = new CotEventContainer(parser.parse(CHAT_TO_MISSION));
		
		org.junit.Assert.assertEquals(cot.getUid(), "GeoChat.ANDROID-358982072593830.Green.2b4fb2c4-300d-41e6-9df3-f21b597b87e3");
	}
	
	@Test
	public void parseCotSA() throws DocumentException  {
		
		CotEventContainer cot = new CotEventContainer(parser.parse(SA));
		
		org.junit.Assert.assertTrue("ANDROID-352413144215585".equals(cot.getUid()));
		org.junit.Assert.assertTrue("coolata".equals(cot.getCallsign()));
	}
	
	@Test
	public void ParseProtoSAFromString() throws DocumentException, ParserConfigurationException, SAXException  {
		
		TakMessage sa = cotProtoConverter.cot2protoBuf(new CotEventContainer(parser.parse(SA)));
		
		org.junit.Assert.assertNotNull(sa.getCotEvent());
		
		org.junit.Assert.assertTrue("ANDROID-352413144215585".contentEquals(sa.getCotEvent().getUid()));
		
		org.junit.Assert.assertNotNull(sa.getCotEvent().getDetail());
		
		org.junit.Assert.assertNotNull(sa.getCotEvent().getDetail().getContact());
		
		org.junit.Assert.assertNotNull(sa.getCotEvent().getDetail().getXmlDetail());
		
		String xmlDetail = sa.getCotEvent().getDetail().getXmlDetail();
		
		// can't parse this because it contains two tags
//		Document detailDoc = parseXML(xmlDetail);

		// so just consider the xmlDetail string as a whole
		org.junit.Assert.assertTrue(xmlDetail.contains("coolata"));
	}
}
