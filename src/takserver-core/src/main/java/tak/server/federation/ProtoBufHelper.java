

package tak.server.federation;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atakmap.Tak.BINARY_TYPES;
import com.atakmap.Tak.BinaryBlob;
import com.atakmap.Tak.CRUD;
import com.atakmap.Tak.ContactListEntry;
import com.atakmap.Tak.FederatedEvent;
import com.atakmap.Tak.GeoEvent;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.service.DistributedSubscriptionManager;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.util.NumericUtil;

/**
 * Created on 10/29/15.
 */
public class ProtoBufHelper {

	static final ProtoBufHelper instance = new ProtoBufHelper();

	static final Logger logger = LoggerFactory.getLogger(ProtoBufHelper.class);

	public static ProtoBufHelper getInstance() { return instance; }

	ProtoBufHelper() {
	}

	public GeoEvent cot2protoBuf(CotEventContainer cot) {

		Element rootE = requireNonNull(cot, "CoT event XML object").getDocument().getRootElement();
		Element pointE = requireNonNull(rootE, "CoT event root XML element").element("point");

		Element detailE = null;

		if (rootE.element("detail") != null) {
			detailE = rootE.element("detail").createCopy();
		}

		GeoEvent.Builder geoBuilder = GeoEvent.newBuilder();

		try {
			geoBuilder.setSendTime(DateUtil.millisFromCotTimeStr(rootE.attribute("time").getText()));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting time", e);
		}

		try {
			geoBuilder.setStartTime(DateUtil.millisFromCotTimeStr(rootE.attribute("start").getText()));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting start", e);
		}

		try {
			geoBuilder.setStaleTime(DateUtil.millisFromCotTimeStr(rootE.attribute("stale").getText()));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting stale", e);
		}

		try {
			geoBuilder.setLat(NumericUtil.parseDoubleOrDefault(pointE.attribute("lat").getText(),0));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting lat", e);
		}

		try {
			geoBuilder.setLon(NumericUtil.parseDoubleOrDefault(pointE.attribute("lon").getText(), 0));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting lon", e);
		}

		try {
			geoBuilder.setHae(NumericUtil.parseDoubleOrDefault(pointE.attribute("hae").getText(), 0));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting hae", e);
		}

		try {
			geoBuilder.setCe(NumericUtil.parseDoubleOrDefault(pointE.attribute("ce").getText(), 999999));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting ce", e);
		}

		try {
			geoBuilder.setLe(NumericUtil.parseDoubleOrDefault(pointE.attribute("le").getText(), 999999));
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting le", e);
		}

		try {
			geoBuilder.setUid(rootE.attribute("uid").getText());
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting uid", e);
		}

		try {
			geoBuilder.setType(rootE.attribute("type").getText());
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting type", e);
		}

		try {
			geoBuilder.setCoordSource(rootE.attribute("how").getText());
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting how", e);
		}
		
		try {
			String feedUid = (String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY);
			if (!Strings.isNullOrEmpty(feedUid)) {
				geoBuilder.setFeedUid((String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("exception setting how", e);
		}
		

		if (detailE != null) {
			try {
				Element trackE = detailE.element("track");
				if (trackE != null && trackE.attribute("speed") != null && trackE.attribute("course") != null) {
					detailE.remove(trackE);
					geoBuilder.setSpeed(NumericUtil.parseDoubleOrDefault(trackE.attribute("speed").getText(), 0));
					geoBuilder.setCourse(NumericUtil.parseDoubleOrDefault(trackE.attribute("course").getText(), 0));
				}

				Element statusE = detailE.element("status");
				if (statusE != null && statusE.attribute("battery") != null) {
					detailE.remove(statusE);
					geoBuilder.setBattery(Integer.parseInt(statusE.attribute("battery").getText()));
				}

				Element plocE = detailE.element("precisionlocation");
				if (plocE != null && plocE.attribute("geopointsrc") != null && plocE.attribute("altsrc") != null) {
					detailE.remove(plocE);
					geoBuilder.setPloc(plocE.attribute("geopointsrc").getText());
					geoBuilder.setPalt(plocE.attribute("altsrc").getText());
				}

				Element imageE = detailE.element("image");
				if (imageE != null) {
					String imgStr = imageE.getText();
					System.err.println(imgStr);
					byte[] img = Base64.decodeBase64(imgStr);
					geoBuilder.setBinary(
							BinaryBlob.newBuilder()
							.setType(BINARY_TYPES.IMAGE)
							.setData(ByteString.copyFrom(img))
							.build());
					imageE.setText("");
				}

				List<String> callsignList = (List<String>) cot.getContextValue("explicitBrokeringCallsign");
				if (callsignList != null) {
					for(String s : callsignList) {
						geoBuilder.addPtpCallsigns(s);
					}
				}
				List<String> uidList = (List<String>) cot.getContextValue("explicitBrokeringUid");
				if (uidList != null) {
					for(String s : uidList) {
						geoBuilder.addPtpUids(s);
					}
				}
				Set<String> missionNamesList = (Set<String>) cot.getContextValue("explicitBrokeringMission");
				if (missionNamesList != null) {
					for(String s : missionNamesList) {
						geoBuilder.addMissionNames(s);
					}
				}
				geoBuilder.setOther(detailE.asXML());
			} catch (Exception e) {
				logger.warn("exception setting detail fields ", e);
			}
		}

		return geoBuilder.build();
	}

	public FederatedEvent delContact2protoBuf(CotEventContainer toSend) {
		Element link = (Element) toSend.getDocument().selectSingleNode("/event/detail/link");
		ContactListEntry contact = ContactListEntry.newBuilder()
				.setCallsign(link.attribute("type").getValue()) // because CoT links are stupid...
				.setUid(link.attribute("uid").getValue())
				.setOperation(CRUD.DELETE)
				.build();

		return FederatedEvent.newBuilder()
				.setContact(contact)
				.build();
	}

	// Note: had to remove some checks for existence of certain fields, didn't seem to exist in Proto3
	public CotEventContainer proto2cot(GeoEvent geo) {
		Document event = DocumentHelper.createDocument();
		Element rootE = event.addElement("event");
		rootE.addAttribute("version", "2.0")
		.addAttribute("uid", geo.getUid())
		.addAttribute("type", geo.getType())
		.addAttribute("how", geo.getCoordSource())
		.addAttribute("time",  DateUtil.toCotTime(geo.getSendTime()))
		.addAttribute("start", DateUtil.toCotTime(geo.getStartTime()))
		.addAttribute("stale", DateUtil.toCotTime(geo.getStaleTime()));
		rootE.addElement("point")
		.addAttribute("lat", Double.toString(geo.getLat()))
		.addAttribute("lon", Double.toString(geo.getLon()))
		.addAttribute("hae", Double.toString(geo.getHae()))
		.addAttribute("ce", Double.toString(geo.getCe()))
		.addAttribute("le", Double.toString(geo.getLe()));
		if (!Strings.isNullOrEmpty(geo.getOther())) {
			try {
				Document otherDoc = DocumentHelper.parseText(geo.getOther());
				Element detailE = otherDoc.getRootElement();
				detailE.addElement("track")
				.addAttribute("speed", Double.toString(geo.getSpeed()))
				.addAttribute("course", Double.toString(geo.getCourse()));
				detailE.addElement("status")
				.addAttribute("battery", Integer.toString(geo.getBattery()));
				detailE.addElement("precisionlocation")
				.addAttribute("geopointsrc", geo.getPloc())
				.addAttribute("altsrc", geo.getPalt());
				if (geo.hasBinary() && geo.getBinary().getType() == BINARY_TYPES.IMAGE &&
						detailE.element("image") != null) {
					detailE.element("image").setText(
							Base64.encodeBase64String(geo.getBinary().getData().toByteArray()));
				}

				rootE.add(detailE);

			} catch (DocumentException e) {
				logger.warn("exception converting proto to CoT", e);
			}
		}

		CotEventContainer rval = new CotEventContainer(event);

		if (geo.getPtpCallsignsCount() > 0) {
			List<String> l = new LinkedList<String>();
			for(int i = 0; i < geo.getPtpCallsignsCount(); ++i) {
				l.add(geo.getPtpCallsigns(i));
			}
			rval.setContextValue("explicitBrokeringCallsign", l);
		}

		if (geo.getPtpUidsCount() > 0) {
			List<String> l = new LinkedList<String>();
			for(int i = 0; i < geo.getPtpUidsCount(); ++i) {
				l.add(geo.getPtpUids(i));
			}
			rval.setContextValue("explicitBrokeringUid", l);
		}

		if (geo.getMissionNamesCount() > 0) {
			Set<String> l = new HashSet<String>();
			for(int i = 0; i < geo.getMissionNamesCount(); ++i) {
				l.add(geo.getMissionNames(i));
			}
			rval.setContextValue("explicitBrokeringMission", l);
		}
		
		if (!Strings.isNullOrEmpty(geo.getFeedUid())) {
			rval.setContextValue(Constants.DATA_FEED_UUID_KEY, geo.getFeedUid());
		}

		return rval;
	}

	public CotEventContainer protoBuf2delContact(ContactListEntry contact) {
		return DistributedSubscriptionManager.getInstance().makeDeleteMessage(contact.getUid(), contact.getCallsign());
	}

}
