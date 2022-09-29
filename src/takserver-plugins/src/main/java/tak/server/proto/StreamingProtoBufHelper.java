package tak.server.proto;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Date;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.remote.util.DateUtil;

import atakmap.commoncommo.protobuf.v1.ContactOuterClass.Contact;
import atakmap.commoncommo.protobuf.v1.Cotevent.CotEvent;
import atakmap.commoncommo.protobuf.v1.DetailOuterClass.Detail;
import atakmap.commoncommo.protobuf.v1.GroupOuterClass.Group;
import atakmap.commoncommo.protobuf.v1.Precisionlocation.PrecisionLocation;
import atakmap.commoncommo.protobuf.v1.StatusOuterClass.Status;
import atakmap.commoncommo.protobuf.v1.Takmessage.TakMessage;
import atakmap.commoncommo.protobuf.v1.TakvOuterClass.Takv;
import atakmap.commoncommo.protobuf.v1.TrackOuterClass.Track;
import tak.server.cot.CotEventContainer;
import tak.server.util.NumericUtil;


/**
 * Created on 5/31/2018.
 */
public class StreamingProtoBufHelper {

    private static final Logger logger = LoggerFactory.getLogger(StreamingProtoBufHelper.class);
    private static final StreamingProtoBufHelper instance = new StreamingProtoBufHelper();
    public static StreamingProtoBufHelper getInstance() { return instance; }

    public final static byte MAGIC = (byte)0xbf;
    public final static String TAK_PROTO_VERSION = "1";

    private static final String CONTACT = "contact";
    private static final String GROUP = "__group";
    private static final String PRECISION_LOCATION = "precisionlocation";
    private static final String STATUS = "status";
    private static final String TAKV = "takv";
    private static final String TRACK = "track";

    public StreamingProtoBufHelper() {}

    public TakMessage cot2protoBuf(CotEventContainer cot) {
        try {
        	        	
            if (cot == null) {
                logger.error("cot2protoBuf called with null CotEventContainer");
                return null;
            }

            if (cot.getDocument() == null) {
                logger.error("cot2protoBuf unable to get document for CotEventContainer");
                return null;
            }

            Element root = cot.getDocument().getRootElement();
            if (root == null) {
                logger.error("cot2protoBuf failed to get root element");
                return null;
            }

            //
            // event
            //
            CotEvent.Builder cotEventBuilder = CotEvent.newBuilder();
            Attribute type = root.attribute("type");
            if (type == null) {
                logger.error("cot2protoBuf failed to find CotEvent.type!");
            } else {
                cotEventBuilder.setType(type.getText());
            }

            Attribute uid = root.attribute("uid");
            if (uid == null) {
                logger.error("cot2protoBuf failed to find CotEvent.uid!");
            } else {
                cotEventBuilder.setUid(uid.getText());
            }

            Attribute how = root.attribute("how");
            if (how == null) {
                logger.error("cot2protoBuf failed to find CotEvent.how!");
            } else {
                cotEventBuilder.setHow(how.getText());
            }
            
            long timeLong = cot.getTimeLong();
            if (timeLong != -1L) {
            	cotEventBuilder.setSendTime(timeLong);
            } else {
            	 Attribute time = root.attribute("time");
                 if (time == null) {
                     logger.error("cot2protoBuf failed to find CotEvent.time!");
                 } else {
                     cotEventBuilder.setSendTime(DateUtil.millisFromCotTimeStr(time.getText()));
                 }
            }
            
            long startLong = cot.getStartLong();
            if (startLong != -1L) {
            	cotEventBuilder.setStartTime(startLong);
            } else {
            	 Attribute start = root.attribute("start");
                 if (start == null) {
                     logger.error("cot2protoBuf failed to find CotEvent.start!");
                 } else {
                     cotEventBuilder.setStartTime(DateUtil.millisFromCotTimeStr(start.getText()));
                 }
            }
            
            long staleLong = cot.getStaleLong();
            if (staleLong != -1L) {
            	cotEventBuilder.setStaleTime(staleLong);
            } else {
            	 Attribute stale = root.attribute("stale");
                 if (stale == null) {
                     logger.error("cot2protoBuf failed to find CotEvent.stale!");
                 } else {
                     cotEventBuilder.setStaleTime(DateUtil.millisFromCotTimeStr(stale.getText()));
                 }
            }
            
            Date submitTime = cot.getSubmissionTime();
            long submitMillis;
            if (submitTime != null && (submitMillis = submitTime.getTime()) > 0) {
            	cotEventBuilder.setSubmissionTime(submitMillis);
            } 
            
            long creationTime = cot.getCreationTime();
            if (creationTime > 0) {
            	cotEventBuilder.setCreationTime(creationTime);
            } 
            
            Attribute opex = root.attribute("opex");
            if (opex != null) {
                cotEventBuilder.setOpex(opex.getText());
            }

            Attribute qos = root.attribute("qos");
            if (qos != null) {
                cotEventBuilder.setQos(qos.getText());
            }

            Attribute access = root.attribute("access");
            if (access != null) {
                cotEventBuilder.setAccess(access.getText());
            }

            //
            // point
            //
            Element point = root.element("point");
            if (point == null) {
                logger.error("cot2protoBuf found message without a point!");
            } else {
            	double latD = cot.getLatDouble();
            	if (latD != -1.0) {
            		cotEventBuilder.setLat(latD);
            	} else {
            		 Attribute lat = point.attribute("lat");
                     if (lat == null) {
                         logger.error("cot2protoBuf failed to find CotEvent.lat!");
                     } else {
                         cotEventBuilder.setLat(NumericUtil.parseDoubleOrDefault(lat.getText(), 0));
                     }
            	}
            	
            	double lonD = cot.getLonDouble();
            	if (lonD != -1.0) {
            		cotEventBuilder.setLon(lonD);
            	} else {
            		Attribute lon = point.attribute("lon");
                    if (lon == null) {
                        logger.error("cot2protoBuf failed to find CotEvent.lon!");
                    } else {
                        cotEventBuilder.setLon(NumericUtil.parseDoubleOrDefault(lon.getText(), 0));
                    }
            	}
            	
            	double haeD = cot.getHaeDouble();
            	if (haeD != -1.0) {
            		cotEventBuilder.setHae(haeD);
            	} else {
            		Attribute hae = point.attribute("hae");
                    if (hae == null) {
                        logger.error("cot2protoBuf failed to find CotEvent.hae!");
                    } else {
                        cotEventBuilder.setHae(NumericUtil.parseDoubleOrDefault(hae.getText(), 0));
                    }
            	}
            	
            	double ceD = cot.getCeDouble();
            	if (ceD != -1.0) {
            		cotEventBuilder.setCe(ceD);
            	} else {
            		Attribute ce = point.attribute("ce");
                    if (ce == null) {
                        logger.error("cot2protoBuf failed to find CotEvent.ce!");
                    } else {
                        cotEventBuilder.setCe(NumericUtil.parseDoubleOrDefault(ce.getText(), 999999));
                    }
            	}
            	
            	double leD = cot.getLeDouble();
            	if (leD != -1.0) {
            		cotEventBuilder.setLe(leD);
            	} else {
            		Attribute le = point.attribute("le");
                    if (le == null) {
                        logger.error("cot2protoBuf failed to find CotEvent.le!");
                    } else {
                        cotEventBuilder.setLe(NumericUtil.parseDoubleOrDefault(le.getText(), 999999));
                    }
            	}
            }

            //
            // detail
            //
            Element detailElement = root.element("detail");
            if (detailElement != null) {
                detailElement = detailElement.createCopy();
                Detail.Builder detailBuilder = Detail.newBuilder();

                //
                // contact
                //
                Element contactElement = detailElement.element(CONTACT);
                if (contactElement != null) {

                    Attribute callsign = contactElement.attribute("callsign");
                    Attribute endpoint = contactElement.attribute("endpoint");

                    if (callsign != null) {
                        if ((endpoint == null && contactElement.attributeCount() == 1) ||
                            (endpoint != null && contactElement.attributeCount() == 2)) {

                            Contact.Builder contactBuilder = Contact.newBuilder();
                            contactBuilder.setCallsign(callsign.getText());

                            if (endpoint != null && endpoint.getText().length() > 0) {
                                contactBuilder.setEndpoint(endpoint.getText());
                            }

                            Contact contact = contactBuilder.build();
                            detailBuilder.setContact(contact);

                            detailElement.remove(contactElement);
                        }
                    }
                }

                //
                // group
                //
                Element groupElement = detailElement.element(GROUP);
                if (groupElement != null) {

                    Attribute name = groupElement.attribute("name");
                    Attribute role = groupElement.attribute("role");

                    if (name != null && role != null
                            && groupElement.attributeCount() == 2) {

                        Group.Builder groupBuilder = Group.newBuilder();
                        groupBuilder.setName(name.getText());
                        groupBuilder.setRole(role.getText());

                        Group group = groupBuilder.build();
                        detailBuilder.setGroup(group);

                        detailElement.remove(groupElement);
                    }
                }

                //
                // precision location
                //
                Element precisionLocationElement = detailElement.element(PRECISION_LOCATION);
                if (precisionLocationElement != null) {

                    Attribute geopointsrc = precisionLocationElement.attribute("geopointsrc");
                    Attribute altsrc = precisionLocationElement.attribute("altsrc");

                    if (geopointsrc != null && altsrc != null
                            && precisionLocationElement.attributeCount() == 2) {

                        PrecisionLocation.Builder precisionLocationBuilder = PrecisionLocation.newBuilder();
                        precisionLocationBuilder.setGeopointsrc(geopointsrc.getText());
                        precisionLocationBuilder.setAltsrc(altsrc.getText());

                        PrecisionLocation precisionLocation = precisionLocationBuilder.build();
                        detailBuilder.setPrecisionLocation(precisionLocation);

                        detailElement.remove(precisionLocationElement);
                    }
                }

                //
                // status
                //
                Element statusElement = detailElement.element(STATUS);
                if (statusElement != null) {

                    Attribute battery = statusElement.attribute("battery");
                    if (battery != null
                            && statusElement.attributeCount() == 1) {

                        Status.Builder statusBuilder = Status.newBuilder();
                        
                        int batteryI = cot.getBatteryInt();
                    	if (batteryI != -1) {
                    		statusBuilder.setBattery(batteryI);
                    	} else {
                    		statusBuilder.setBattery(NumericUtil.parseIntOrDefault(battery.getText(), 0));
                    	}

                        Status status = statusBuilder.build();
                        detailBuilder.setStatus(status);

                        detailElement.remove(statusElement);
                    }
                }

                //
                // takv
                //
                Element takvElement = detailElement.element(TAKV);
                if (takvElement != null) {

                    Attribute device = takvElement.attribute("device");
                    Attribute platform = takvElement.attribute("platform");
                    Attribute os = takvElement.attribute("os");
                    Attribute version = takvElement.attribute("version");

                    if (device != null && platform != null && os != null && version != null
                            && takvElement.attributeCount() == 4) {

                        Takv.Builder takvBuilder = Takv.newBuilder();
                        takvBuilder.setDevice(device.getText());
                        takvBuilder.setPlatform(platform.getText());
                        takvBuilder.setOs(os.getText());
                        takvBuilder.setVersion(version.getText());

                        Takv takv = takvBuilder.build();
                        detailBuilder.setTakv(takv);

                        detailElement.remove(takvElement);
                    }
                }

                //
                // track
                //
                Element trackElement = detailElement.element(TRACK);
                if (trackElement != null) {

                    Attribute speed = trackElement.attribute("speed");
                    Attribute course = trackElement.attribute("course");

                    if (speed != null && course != null &&
                            trackElement.attributeCount() == 2) {

                        Track.Builder trackBuilder = Track.newBuilder();
                        
                        double speedD = cot.getSpeedDouble();
                    	if (speedD != -1.0) {
                    		trackBuilder.setSpeed(speedD);
                    	} else {
                    		trackBuilder.setSpeed(NumericUtil.parseDoubleOrDefault(speed.getText(), 0));
                    	}
                    	
                    	double courseD = cot.getCourseDouble();
                     	if (courseD != -1.0) {
                     		trackBuilder.setCourse(courseD);
                     	} else {
                     		trackBuilder.setCourse(NumericUtil.parseDoubleOrDefault(course.getText(), 0));
                     	}

                        Track track = trackBuilder.build();
                        detailBuilder.setTrack(track);

                        detailElement.remove(trackElement);
                    }
                }

                //
                // xmlDetail
                //
                if (detailElement.elements().size() != 0) {
                    StringBuilder xmlDetail = new StringBuilder();
                    for (Object subElement : detailElement.elements()) {
                        xmlDetail.append(((Element)subElement).asXML());
                    }
                    detailBuilder.setXmlDetail(xmlDetail.toString());
                }

                Detail detail = detailBuilder.build();
                cotEventBuilder.setDetail(detail);
            }

            CotEvent cotEvent = cotEventBuilder.build();

            TakMessage.Builder takMessageBuilder = TakMessage.newBuilder();
            takMessageBuilder.setCotEvent(cotEvent);
            TakMessage takMessage = takMessageBuilder.build();

            return takMessage;

        } catch (Exception e) {
            logger.error("exception in cot2protoBuf!", e);
            return null;
        }
    }
    
	private static ThreadLocal<SAXReader> reader =
		    new ThreadLocal<SAXReader>() {
		        @Override public SAXReader initialValue() {
		            return new SAXReader();
		        }
		    };

    public CotEventContainer proto2cot(TakMessage takMessage) {
        try {
            CotEvent cotEvent = takMessage.getCotEvent();
            Document document = DocumentHelper.createDocument();

            //
            // event
            //
            Element eventElement = document.addElement("event");
            eventElement.addAttribute("version", "2.0")
                    .addAttribute("uid", cotEvent.getUid())
                    .addAttribute("type", cotEvent.getType())
                    .addAttribute("how", cotEvent.getHow())
                    .addAttribute("time", DateUtil.toCotTime(cotEvent.getSendTime()))
                    .addAttribute("start", DateUtil.toCotTime(cotEvent.getStartTime()))
                    .addAttribute("stale", DateUtil.toCotTime(cotEvent.getStaleTime()));
            
            String opex = cotEvent.getOpex();
            if (opex != null && opex.length() > 0) {
                eventElement.addAttribute("opex", opex);
            }

            String qos = cotEvent.getQos();
            if (qos != null && qos.length() > 0) {
                eventElement.addAttribute("qos", qos);
            }

            String access = cotEvent.getAccess();
            if (access != null && access.length() > 0) {
                eventElement.addAttribute("access", access);
            }

            //
            // point
            //
            eventElement.addElement("point")
                    .addAttribute("lat", Double.toString(cotEvent.getLat()))
                    .addAttribute("lon", Double.toString(cotEvent.getLon()))
                    .addAttribute("hae", Double.toString(cotEvent.getHae()))
                    .addAttribute("ce", Double.toString(cotEvent.getCe()))
                    .addAttribute("le", Double.toString(cotEvent.getLe()));

            double speed = -1.0;
            double course = -1.0;
            int battery = -1;
            
            //
            // detail
            //
            Detail detail = cotEvent.getDetail();
            if (detail != null) {
                Element detailElement = eventElement.addElement("detail");

                //
                // contact
                //
                if (detail.hasContact()) {
                    Contact contact = detail.getContact();
                    Element contactElement = detailElement.addElement(CONTACT)
                            .addAttribute("callsign", contact.getCallsign());

                    if (contact.getEndpoint() != null && contact.getEndpoint().length() > 0) {
                        contactElement.addAttribute("endpoint", contact.getEndpoint());
                    }
                }

                //
                // group
                //
                if (detail.hasGroup()) {
                    Group group = detail.getGroup();
                    detailElement.addElement(GROUP)
                            .addAttribute("name", group.getName())
                            .addAttribute("role", group.getRole());
                }

                //
                // precision location
                //
                if (detail.hasPrecisionLocation()) {
                    PrecisionLocation precisionLocation = detail.getPrecisionLocation();
                    detailElement.addElement(PRECISION_LOCATION)
                            .addAttribute("geopointsrc", precisionLocation.getGeopointsrc())
                            .addAttribute("altsrc", precisionLocation.getAltsrc());
                }

                //
                // status
                //
                if (detail.hasStatus()) {
                    Status status = detail.getStatus();
                    battery = status.getBattery();
                    detailElement.addElement(STATUS)
                            .addAttribute("battery", Integer.toString(battery));
                }

                //
                // takv
                //
                if (detail.hasTakv()) {
                    Takv takv = detail.getTakv();
                    detailElement.addElement(TAKV)
                            .addAttribute("device", takv.getDevice())
                            .addAttribute("platform", takv.getPlatform())
                            .addAttribute("os", takv.getOs())
                            .addAttribute("version", takv.getVersion());
                }

                //
                // track
                //
                if (detail.hasTrack()) {
                    Track track = detail.getTrack();
                    course = track.getCourse();
                    speed = track.getSpeed();
                    detailElement.addElement(TRACK)
                            .addAttribute("speed", Double.toString(speed))
                            .addAttribute("course", Double.toString(course));
                }

                //
                // xmlDetail
                //
                String xmlDetail = detail.getXmlDetail();
                if (xmlDetail != null && xmlDetail.length() > 0) {
                    xmlDetail = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><detail>" + xmlDetail + "</detail>";
                    Document doc = reader.get().read(new ByteArrayInputStream(xmlDetail.getBytes()));
                    Element xmlDetailElement = doc.getRootElement();
                    for (Object subElement : xmlDetailElement.elements()) {

                        String name = ((Element)subElement).getName();

                        // if we see one of the currently defined detail types appear in the xmlDetail section
                        // then the xmlDetail contents shall override whatever appeared in the proto message.
                        if(0 == name.compareTo(CONTACT)
                        || 0 == name.compareTo(GROUP)
                        || 0 == name.compareTo(PRECISION_LOCATION)
                        || 0 == name.compareTo(STATUS)
                        || 0 == name.compareTo(TAKV)
                        || 0 == name.compareTo(TRACK)) {
                            Element existing = detailElement.element(name);
                            if (existing != null) {
                                // go ahead and delete what came from the explicit proto message
                                detailElement.remove(existing);
                            }
                        }

                        detailElement.add(((Element) subElement).createCopy());
                    }
                }
            }

            CotEventContainer cotEventContainer = new CotEventContainer(document);
            
            cotEventContainer.setTimeLong(cotEvent.getSendTime());
            cotEventContainer.setStartLong(cotEvent.getStartTime());
            cotEventContainer.setStaleLong(cotEvent.getStaleTime());
            
            if (cotEvent.getSubmissionTime() > 0) {
            	cotEventContainer.setSubmissionTime(new Date(cotEvent.getSubmissionTime()));
            }
            
            
            if (cotEvent.getCreationTime() > 0) {
            	cotEventContainer.setCreationTime(cotEvent.getCreationTime());
            }
                        
            cotEventContainer.setLatDouble(cotEvent.getLat());
            cotEventContainer.setLonDouble(cotEvent.getLon());
            cotEventContainer.setHaeDouble(cotEvent.getHae());
            cotEventContainer.setCeDouble(cotEvent.getCe());
            cotEventContainer.setLeDouble(cotEvent.getLe());
            cotEventContainer.setBatteryInt(battery);
            cotEventContainer.setSpeedDouble(speed);
            cotEventContainer.setCourseDouble(course);
                        
            return cotEventContainer;

        } catch (Exception e) {
            logger.error("exception in proto2cot!", e);
            return null;
        }
    }

    public int readVarint(ByteBuffer buffer) {
        int next = 0;
        int nextShift = 0;
        while (buffer.remaining() > 0) {
            byte b = buffer.get();
            if ((b & 0x80) == 0) {
                next = next| (b << nextShift);
                return next;
            } else {
                next |= (b & 0x7F) << nextShift;
                nextShift += 7;
            }
        }
        return -1;
    }
}
