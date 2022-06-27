package com.bbn.marti.cot;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CotSerializationTests {
	
	private static final Logger logger = LoggerFactory.getLogger(CotSerializationTests.class);
	
	private static String CONCENTRIC_CIRCLES_COT_XML = "<detail><contact callsign=\"Drawing Circle 4\"/><shape><ellipse major=\"6.783474303271526\" minor=\"6.783474303271526\" angle=\"360\"/><ellipse major=\"13.566948606543052\" minor=\"13.566948606543052\" angle=\"360\"/><ellipse major=\"20.35042290981458\" minor=\"20.35042290981458\" angle=\"360\"/><ellipse major=\"27.133897213086104\" minor=\"27.133897213086104\" angle=\"360\"/><ellipse major=\"33.91737151635763\" minor=\"33.91737151635763\" angle=\"360\"/><ellipse major=\"40.70084581962916\" minor=\"40.70084581962916\" angle=\"360\"/><ellipse major=\"47.484320122900684\" minor=\"47.484320122900684\" angle=\"360\"/><ellipse major=\"54.26779442617221\" minor=\"54.26779442617221\" angle=\"360\"/><link uid=\"4b4f1620-5073-4303-a6da-dadc3611fda5.Style\" type=\"b-x-KmlStyle\" relation=\"p-c\"><Style><LineStyle><color>ffffffff</color><width>2.0</width></LineStyle><PolyStyle><color>00ffffff</color></PolyStyle></Style></link></shape><color argb=\"-1\"/><precisionlocation altsrc=\"???\" geopointsrc=\"Calc\"/><_flow-tags_ TAKServer6bc41eef=\"2018-08-08T18:04:57Z\"/></detail>";
	private static String COT_SHAPE_CIRCLE = "<detail><contact callsign=\"Drawing Circle 1\"/><shape><ellipse major=\"182752.91852647165\" minor=\"182752.91852647165\" angle=\"360\"/><link uid=\"7020dbf0-c347-4e31-8a25-7ab5baefbfe7.Style\" type=\"b-x-KmlStyle\" relation=\"p-c\"><Style><LineStyle><color>ffffffff</color><width>2.0</width></LineStyle><PolyStyle><color>00ffffff</color></PolyStyle></Style></link></shape><color argb=\"-1\"/><precisionlocation altsrc=\"???\" geopointsrc=\"Calc\"/><_flow-tags_ TAKServer054c1aa7=\"2018-07-26T18:28:40Z\"/></detail>"; 
	private static String COT_TELESTRATION = "<detail><contact/><title title=\"Freehand 1\"/><link line=\"&lt;?xml version='1.0' encoding='UTF-8' standalone='yes'?&gt;&lt;event version='2.0' uid='99a1fd13-0c62-4fb8-bd87-4e2d21927ddb' type='u-d-f' time='2018-07-26T18:31:04.811Z' start='2018-07-26T18:31:04.811Z' stale='2018-07-27T18:31:04.811Z' how='h-e'&gt;&lt;point lat='34.15199190157241' lon='-89.81985480163021' hae='9999999.0' ce='9999999.0' le='9999999.0' /&gt;&lt;detail&gt;&lt;contact callsign='Freehand 3'/&gt;&lt;link point='34.15199190157241,-89.81985480163021'/&gt;&lt;link point='33.715945619140086,-87.5875794561014'/&gt;&lt;link point='32.87739507600101,-87.22108648892502'/&gt;&lt;link point='26.135448709162823,-86.6213707244546'/&gt;&lt;link point='25.632318383279376,-86.02165495998418'/&gt;&lt;link point='25.93419657880944,-84.98881114339622'/&gt;&lt;link point='28.952978534110123,-79.35814646586832'/&gt;&lt;link point='29.154230664463505,-78.4252552766921'/&gt;&lt;link point='24.76022581841473,-79.3914640083389'/&gt;&lt;link point='22.64707844970426,-79.35814646586832'/&gt;&lt;link point='22.110406102095247,-79.09160612610368'/&gt;&lt;link point='21.674359819662925,-78.15871493692747'/&gt;&lt;link point='21.640817797937366,-74.96023085975185'/&gt;&lt;link point='22.378742275899754,-72.72795551422304'/&gt;&lt;link point='22.44582631935088,-72.39478008951724'/&gt;&lt;link point='22.31165823244863,-72.22819237716435'/&gt;&lt;link point='21.171229493779478,-72.49473271692898'/&gt;&lt;link point='20.164968842012584,-72.9611783115171'/&gt;&lt;link point='18.185989560204362,-74.39383263775201'/&gt;&lt;link point='17.213270930163034,-75.8598045064575'/&gt;&lt;link point='17.079102843260785,-76.45952027092792'/&gt;&lt;link point='17.280354973614152,-77.95880968210399'/&gt;&lt;link point='17.917653386399863,-79.82459206045642'/&gt;&lt;link point='18.957456059892316,-84.05591995421999'/&gt;&lt;link point='19.594754472678012,-85.85506724763127'/&gt;&lt;link point='20.3326789506404,-87.35435665880735'/&gt;&lt;link point='21.13768747205392,-88.28724784798356'/&gt;&lt;link point='22.68062047142982,-89.12018640974804'/&gt;&lt;link point='24.793767840140298,-89.78653725915963'/&gt;&lt;link point='26.73920510022296,-90.21966531127715'/&gt;&lt;link point='28.18151203442217,-90.28630039621832'/&gt;&lt;link point='29.389024816542445,-90.153030226336'/&gt;&lt;link point='31.133209946271727,-89.61994954680674'/&gt;&lt;link point='32.87739507600101,-88.52047064527761'/&gt;&lt;strokeColor value='-65536'/&gt;&lt;fillColor value='16711680'/&gt;&lt;strokeWeight value='6.0'/&gt;&lt;precisionlocation altsrc='???' geopointsrc='???'/&gt;&lt;/detail&gt;&lt;/event&gt;\"/><link line=\"&lt;?xml version='1.0' encoding='UTF-8' standalone='yes'?&gt;&lt;event version='2.0' uid='1e31f5a3-8243-4f6b-98cc-a569c09e9c6f' type='u-d-f' time='2018-07-26T18:31:04.812Z' start='2018-07-26T18:31:04.812Z' stale='2018-07-27T18:31:04.812Z' how='h-e'&gt;&lt;point lat='30.16049131623039' lon='-89.0202337823363' hae='9999999.0' ce='9999999.0' le='9999999.0' /&gt;&lt;detail&gt;&lt;contact callsign='Freehand 4'/&gt;&lt;link point='30.16049131623039,-89.0202337823363'/&gt;&lt;link point='31.602798250429608,-89.45336183445383'/&gt;&lt;link point='32.03884453286193,-89.35340920704209'/&gt;&lt;link point='34.05136583639572,-87.88743733833661'/&gt;&lt;strokeColor value='-65536'/&gt;&lt;fillColor value='16711680'/&gt;&lt;strokeWeight value='6.0'/&gt;&lt;precisionlocation altsrc='???' geopointsrc='???'/&gt;&lt;/detail&gt;&lt;/event&gt;\"/><link line=\"&lt;?xml version='1.0' encoding='UTF-8' standalone='yes'?&gt;&lt;event version='2.0' uid='95369ddd-d640-49c8-b012-f5af99f39b52' type='u-d-f' time='2018-07-26T18:31:04.813Z' start='2018-07-26T18:31:04.813Z' stale='2018-07-27T18:31:04.813Z' how='h-e'&gt;&lt;point lat='20.601015124444906' lon='-85.58852690786664' hae='9999999.0' ce='9999999.0' le='9999999.0' /&gt;&lt;detail&gt;&lt;contact callsign='Freehand 4'/&gt;&lt;link point='20.601015124444906,-85.58852690786664'/&gt;&lt;link point='21.875611950016307,-84.38909537892579'/&gt;&lt;link point='23.85459123182453,-81.69037443880886'/&gt;&lt;link point='25.095646035670363,-79.99117977280932'/&gt;&lt;link point='25.397524231200435,-79.72463943304469'/&gt;&lt;link point='25.162730079121488,-80.05781485775049'/&gt;&lt;link point='23.82104921009897,-81.52378672645597'/&gt;&lt;link point='20.0643427768359,-84.72227080363157'/&gt;&lt;link point='19.058082125069006,-85.28866902563142'/&gt;&lt;link point='18.45432573400886,-85.42193919551374'/&gt;&lt;link point='18.42078371228331,-84.688953261161'/&gt;&lt;link point='19.460586385775763,-82.78985334033798'/&gt;&lt;link point='24.927935927042547,-75.19345365704591'/&gt;&lt;link point='25.665860405004935,-73.82743441575215'/&gt;&lt;link point='25.330440187749304,-73.72748178834041'/&gt;&lt;link point='23.01604068868545,-74.89359577481069'/&gt;&lt;link point='16.743682626005153,-78.65847807398615'/&gt;&lt;link point='14.999497496275865,-79.42478155080947'/&gt;&lt;link point='14.6976193007458,-79.32482892339773'/&gt;&lt;link point='15.670337930787127,-77.65895179986877'/&gt;&lt;link point='20.0643427768359,-71.86169940998798'/&gt;&lt;link point='21.74144386311405,-69.49615389457685'/&gt;&lt;link point='22.07686408036968,-68.69653287528294'/&gt;&lt;link point='19.192250211971256,-71.12871347563524'/&gt;&lt;link point='16.240552300121706,-73.4276239061052'/&gt;&lt;link point='13.590732583802222,-75.49331153928112'/&gt;&lt;link point='12.450303845133064,-76.29293255857502'/&gt;&lt;link point='14.194488974862352,-74.49378526516374'/&gt;&lt;link point='16.91139273463297,-71.76174678257624'/&gt;&lt;link point='19.796006603031394,-68.66321533281237'/&gt;&lt;link point='21.976238015192997,-65.76458913787198'/&gt;&lt;link point='15.670337930787127,-72.76127305669362'/&gt;&lt;link point='15.267833670080378,-73.4276239061052'/&gt;&lt;link point='15.536169843884878,-73.36098882116404'/&gt;&lt;link point='17.17972890843747,-72.66132042928189'/&gt;&lt;link point='20.835809276523847,-71.16203101810581'/&gt;&lt;link point='26.001280622260566,-69.2629310972828'/&gt;&lt;link point='26.53795296986958,-68.82980304516526'/&gt;&lt;strokeColor value='-65536'/&gt;&lt;fillColor value='16711680'/&gt;&lt;strokeWeight value='6.0'/&gt;&lt;precisionlocation altsrc='???' geopointsrc='???'/&gt;&lt;/detail&gt;&lt;/event&gt;\"/><precisionlocation altsrc=\"???\" geopointsrc=\"???\"/><_flow-tags_ TAKServer054c1aa7=\"2018-07-26T18:31:05Z\"/></detail>";
	private static String COT_TELESTRATION_LINE = "<detail><contact callsign='Freehand 3'/><link point='34.15199190157241,-89.81985480163021'/><link point='33.715945619140086,-87.5875794561014'/><link point='32.87739507600101,-87.22108648892502'/><link point='26.135448709162823,-86.6213707244546'/><link point='25.632318383279376,-86.02165495998418'/><link point='25.93419657880944,-84.98881114339622'/><link point='28.952978534110123,-79.35814646586832'/><link point='29.154230664463505,-78.4252552766921'/><link point='24.76022581841473,-79.3914640083389'/><link point='22.64707844970426,-79.35814646586832'/><link point='22.110406102095247,-79.09160612610368'/><link point='21.674359819662925,-78.15871493692747'/><link point='21.640817797937366,-74.96023085975185'/><link point='22.378742275899754,-72.72795551422304'/><link point='22.44582631935088,-72.39478008951724'/><link point='22.31165823244863,-72.22819237716435'/><link point='21.171229493779478,-72.49473271692898'/><link point='20.164968842012584,-72.9611783115171'/><link point='18.185989560204362,-74.39383263775201'/><link point='17.213270930163034,-75.8598045064575'/><link point='17.079102843260785,-76.45952027092792'/><link point='17.280354973614152,-77.95880968210399'/><link point='17.917653386399863,-79.82459206045642'/><link point='18.957456059892316,-84.05591995421999'/><link point='19.594754472678012,-85.85506724763127'/><link point='20.3326789506404,-87.35435665880735'/><link point='21.13768747205392,-88.28724784798356'/><link point='22.68062047142982,-89.12018640974804'/><link point='24.793767840140298,-89.78653725915963'/><link point='26.73920510022296,-90.21966531127715'/><link point='28.18151203442217,-90.28630039621832'/><link point='29.389024816542445,-90.153030226336'/><link point='31.133209946271727,-89.61994954680674'/><link point='32.87739507600101,-88.52047064527761'/><strokeColor value='-65536'/><fillColor value='16711680'/><strokeWeight value='6.0'/><precisionlocation altsrc='???' geopointsrc='???'/></detail>";
	private static String MARKER_WITH_ATTACHMENT = "<detail><fileshare senderUid=\"ANDROID-358982072593830\" senderUrl=\"https://192.168.43.68:8443/Marti/sync/content?hash=1264f50b0a2e4646181b73940a1a4d279913f59486fc79ca70ded477151f4cda\" filename=\"20180817_165502.jpg.zip\" sizeInBytes=\"1056484\" sha256=\"1264f50b0a2e4646181b73940a1a4d279913f59486fc79ca70ded477151f4cda\" name=\"20180817_165502.jpg\" senderCallsign=\"PSS5\"/><ackrequest uid=\"f911502c-8da6-447b-8ecd-4ccfc896a619\" endpoint=\"10.187.124.61:4242:tcp\" ackrequested=\"true\" tag=\"20180817_165502.jpg\"/><precisionlocation altsrc=\"GPS\" geopointsrc=\"GPS\"/><_flow-tags_ TAKServer2d6a88bb=\"2018-08-17T20:57:39Z\"/></detail>";
	
	
	@Test
	public void cotXmlToJson() {

		try {
			logger.debug("CONCENTRIC_CIRCLES_COT_XML: " + CONCENTRIC_CIRCLES_COT_XML );
			JSONObject xmlJSONObj = XML.toJSONObject(CONCENTRIC_CIRCLES_COT_XML);
			String jsonPrettyPrintString = xmlJSONObj.toString();
			logger.debug("CONCENTRIC_CIRCLE_COT_XML as JSON: " + jsonPrettyPrintString);
		} catch (JSONException je) {
			logger.debug("exception converting to JSON", je);
		}
		
		try {
			logger.debug("COT_SHAPE_XML: " + COT_SHAPE_CIRCLE);
			JSONObject xmlJSONObj = XML.toJSONObject(COT_SHAPE_CIRCLE);
			String jsonPrettyPrintString = xmlJSONObj.toString();
			logger.debug("COT_SHAPE_XML as JSON: " + jsonPrettyPrintString);
		} catch (JSONException je) {
			logger.error("exception converting to JSON", je);
		}
		
		try {
			logger.debug("COT_SHAPE_CIRCLE: " + COT_SHAPE_CIRCLE);
			JSONObject xmlJSONObj = XML.toJSONObject(COT_SHAPE_CIRCLE);
			String jsonPrettyPrintString = xmlJSONObj.toString();
			logger.debug("COT_SHAPE_CIRCLE as JSON: " + jsonPrettyPrintString);
		} catch (JSONException je) {
			logger.error("exception converting to JSON", je);
		}
		
		try {
			logger.debug("COT_TELESTRATION: " + COT_TELESTRATION);
			JSONObject xmlJSONObj = XML.toJSONObject(COT_TELESTRATION);
			String jsonPrettyPrintString = xmlJSONObj.toString();
			logger.debug("COT_TELESTRATION as JSON: " + jsonPrettyPrintString);
		} catch (JSONException je) {
			logger.error("exception converting to JSON", je);
		}
		
		try {
			logger.debug("COT_TELESTRATION_LINE: " + COT_TELESTRATION_LINE);
			JSONObject xmlJSONObj = XML.toJSONObject(COT_TELESTRATION_LINE);
			String jsonPrettyPrintString = xmlJSONObj.toString();
			logger.debug("COT_TELESTRATION_LINE as JSON: " + jsonPrettyPrintString);
		} catch (JSONException je) {
			logger.error("exception converting to JSON", je);
		}
		
		try {
			logger.debug("MARKER_WITH_ATTACHMENT: " + MARKER_WITH_ATTACHMENT);
			JSONObject xmlJSONObj = XML.toJSONObject(MARKER_WITH_ATTACHMENT);
			String jsonPrettyPrintString = xmlJSONObj.toString();
			logger.debug("MARKER_WITH_ATTACHMENT as JSON: " + jsonPrettyPrintString);
		} catch (JSONException je) {
			logger.error("exception converting to JSON", je);
		}
	}
}
