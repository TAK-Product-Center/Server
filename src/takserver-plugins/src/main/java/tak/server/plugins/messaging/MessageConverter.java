package tak.server.plugins.messaging;

import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atakmap.commoncommo.protobuf.v1.MessageOuterClass.Message;
import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import tak.server.cot.CotEventContainer;
import tak.server.cot.CotParser;
import tak.server.proto.StreamingProtoBufHelper;

public class MessageConverter {
	
	private static final Logger logger = LoggerFactory.getLogger(MessageConverter.class);
	
	private static final String MARTI_SUBTAG = "/detail/marti";
	private static final String DEST_SUBTAG = "/detail/marti/dest";
	
	private StreamingProtoBufHelper cotProtoConverter = new StreamingProtoBufHelper();
	
	private ThreadLocal<CotParser> cotParser = new ThreadLocal<>();
	
	private CotParser cotParser() {
		if (cotParser.get() == null) {
			cotParser.set(new CotParser(false));
		}
		
		return cotParser.get();
	}

	// Convert CoT message string to proto encoding
	public Message cotStringToDataMessage(String cotString, Set<String> groups, String source) throws DocumentException {
		
		if (source == null || source.isEmpty()) {
			throw new IllegalArgumentException("source must be specified for message");
		}

		final TakMessage sa = cotProtoConverter.cot2protoBuf(new CotEventContainer(cotParser().parse(cotString)));

		final Message.Builder mb = Message.newBuilder();

		mb.setPayload(sa);

		if (groups != null) {
			groups.forEach((group) -> mb.addGroups(group));
		}

		mb.setSource(source);

		return mb.build();		
	}
	
	public Document parseXml(String xmlString) throws DocumentException {
		return cotParser().parse(xmlString);
	}
	
	public Document replaceXmlDetailDestCallsignsAndUids(String xmlDetail, List<String> callsigns, List<String> uids) throws DocumentException {
		
		xmlDetail = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><detail>" + xmlDetail + "</detail>";

		
			
		Document detailDoc = parseXml(xmlDetail);
		
		return replaceDestCallsignsAndUids(detailDoc, callsigns, uids);
	}
	
	private Document replaceDestCallsignsAndUids(Document detailDoc, List<String> callsigns, List<String> uids) {
		
		Element martiElem = (Element) detailDoc.selectSingleNode(MARTI_SUBTAG);
		
		if (martiElem != null) {
			martiElem.detach();
		}
		
	    Element martiElement = DocumentHelper.createElement("marti");
	    
	    if (callsigns != null) {
	    	for (String callsign : callsigns) {
	    		martiElement.add(getCallsignDest(callsign));
	    	}
	    }
	    
	    if (uids != null) {
	    	for (String uid : uids) {
	    		martiElement.add(getUidDest(uid));
	    	}
	    }
	    
	    detailDoc.getRootElement().add(martiElement);
	    
	    if (logger.isDebugEnabled()) {
	    	logger.debug("updated detail: " + detailDoc.getRootElement().getDocument().asXML());
	    }
	    
		return detailDoc.getRootElement().getDocument();
	}
	
	private Element getCallsignDest(String val) {
		Element destElement = DocumentHelper.createElement("dest");
		
		destElement.addAttribute("callsign", val);
		
		return destElement;
	}
	
	private Element getUidDest(String val) {
		Element destElement = DocumentHelper.createElement("dest");
		
		destElement.addAttribute("uid", val);
		
		return destElement;
	}

}
