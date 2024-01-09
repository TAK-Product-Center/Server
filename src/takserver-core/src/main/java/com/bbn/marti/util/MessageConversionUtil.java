

package com.bbn.marti.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.dom4j.Attribute;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.bbn.cot.filter.StreamingEndpointRewriteFilter;
import com.bbn.marti.config.Input;
import com.bbn.marti.nio.channel.ChannelHandler;
import com.bbn.marti.nio.channel.base.AbstractBroadcastingChannelHandler;
import com.bbn.marti.remote.ContactManager;
import com.bbn.marti.remote.InputMetric;
import com.bbn.marti.remote.RemoteContact;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.socket.ChatMessage;
import com.bbn.marti.remote.socket.MissionChange;
import com.bbn.marti.remote.socket.SituationAwarenessMessage;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.service.TransportCotEvent;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import tak.server.cot.CotEventContainer;
import tak.server.util.NumericUtil;

public class MessageConversionUtil {
	
	private static MessageConversionUtil utilInstance = null;

	public static MessageConversionUtil getInstance() {
		if (utilInstance == null) {
			synchronized (MessageConversionUtil.class) {
				if (utilInstance == null) {
					utilInstance = SpringContextBeanForApi.getSpringContext().getBean(MessageConversionUtil.class);
				}
			}
		}
		
		return utilInstance;
	}

    private static final Logger logger = LoggerFactory.getLogger(MessageConversionUtil.class);
    
    private ContactManager contactManager;
    
    @Autowired
    private SpringContextBeanForApi springContextBean;
    
    @SuppressWarnings("static-access")
    @EventListener({ContextRefreshedEvent.class})
    private void init() {
    	contactManager = springContextBean.getSpringContext().getBean(ContactManager.class);
    }

    public static final String generateUid() {
        return UUID.randomUUID().toString();
    }

    public static final void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.warn("Thread sleep interrupted", e);
        }
    }

    public static final void sleepSecs(int seconds) {
        sleepMillis(seconds * 1000);
    }

    public static class CotEndpoint {
        private final InetAddress address;
        private final int port;
        private final TransportCotEvent transport;

        public CotEndpoint(String address, int port, String transport) throws UnknownHostException {
            this.transport = TransportCotEvent.findByID(transport);
            this.address = InetAddress.getByName(address);
            this.port = port;
        }

        public TransportCotEvent transport() {
            return this.transport;
        }

        public InetAddress address() {
            return this.address;
        }

        public int port() {
            return this.port;
        }

        public InetSocketAddress socketAddress() {
            return new InetSocketAddress(address(), port());
        }
    }

    /**
     * Covers protocol:addr:port (option 1)
     * addr:port:protocol (option 2)
     */
    public static final CotEndpoint parseCotEndpoint(String endpointString) {
        CotEndpoint endpoint = null;

        String[] tokens = endpointString.split(":");

        if (tokens.length < 3) {
            logger.error("Malformed subscription endpoint: " + endpointString);
            return null;
        }

        try {
            // Try option #1
            endpoint = new CotEndpoint(tokens[1], Integer.parseInt(tokens[2]), tokens[0]);
        } catch (NumberFormatException e) {
            try {
                // Try option #2
                endpoint = new CotEndpoint(tokens[0], Integer.parseInt(tokens[1]), tokens[2]);
            } catch (NumberFormatException nfe) {
                // Both failed!
                return null;
            } catch (UnknownHostException uhe) {
                return null;
            }
        } catch (UnknownHostException uhe) {
            return null;
        }

        return endpoint;
    }

    public static boolean isValidPort(int port) {
        return (port > 0 && port <= 65535);
    }

    public static byte[] trimArray(byte[] src, int offset, int len) {
        if (offset == 0 && len == src.length) {
            return src;
        } else {
            byte[] dest = new byte[len];
            System.arraycopy(src, offset, dest, 0, len);
            return dest;
        }
    }

    public static RemoteContact contactFromCot(final CotEventContainer c) {
        return new RemoteContact()
                .setUid(c.getUid())
                .setContactName(c.getCallsign())
                .setEndpoint(c.getEndpoint())
                .setLastHeardFromMillis(System.currentTimeMillis());
    }

    public static String getConnectionId(AbstractBroadcastingChannelHandler handler) {

        String id = null;

        try {

            if (handler.getConnectionInfo() == null || Strings.isNullOrEmpty(handler.getConnectionInfo().getConnectionId())) {
                throw new IllegalStateException("AbstractBroadcastingChannelHandler " + handler + " does not contain a connectionId");
            }
            id = handler.getConnectionInfo().getConnectionId();

        } catch (Exception e) {
            logger.debug("exception getting connection id", e);
        }

        return id;
    }
    
   public static String getConnectionId(Input input) {
       // Only the name should be necessary, but the more information and unique info the better.
       return input.getName() + "-" + input.getProtocol() +  "-" + input.getPort();
   }

    public static String getConnectionId(SocketChannel socket) {
        String id = null;

        try {
            
        } catch (Exception e) {
            logger.debug("exception getting connection id", e);
        }

        return id;
    }
    
    public static String getConnectionId(io.netty.channel.socket.SocketChannel socket) {
        String id = null;

        try {
            id = Integer.valueOf(socket.hashCode()).toString();
        } catch (Exception e) {
            logger.debug("exception getting connection id", e);
        }

        return id;
    }

    public static String getIp(ChannelHandler handler) {
        String ip = "";

        try {

            ip = handler.host().toString();

        } catch (Exception e) {
            logger.debug("exception getting IP address from socket", e);
        }

        return ip;
    }


    public InputMetric getInputMetric(Input input) {
    	if (input == null) {
    		throw new IllegalArgumentException("null input object");
    	}
    	
    	return MessagingDependencyInjectionProxy.getInstance().submissionService().getMetric(input);
    }
    
    // attempt to get the CN in a robust way
    public static String getCN(String dn) {
        if (Strings.isNullOrEmpty(dn)) {
            throw new IllegalArgumentException("empty DN");
        }

        try {
            LdapName ldapName = new LdapName(dn);

            for(Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase("CN")) {

                    return rdn.getValue().toString();
                }
            }

            throw new TakException("No CN found in DN: " + dn);

        } catch (InvalidNameException e) {
            throw new TakException(e);
        }
    }

    public static boolean isStale(CotEventContainer data) {
        
        if (data == null) {
            return true;
        }

        if (Strings.isNullOrEmpty(data.getStale())) {
            logger.debug("empty stale time: " + data);
            return true;
        }

        long stale = DateUtil.millisFromCotTimeStr(data.getStale());

        logger.trace("stale time: " + stale);

        // The message is stale if the stale time is before the current time
        if (stale < System.currentTimeMillis()) {
            logger.debug("stale message: " + data);
            return true;
        }

        return false;
    }

    public static SituationAwarenessMessage saMessageFromCot(CotEventContainer cot) {

        logger.debug("trying to convert CoT message " + cot + " to socket sa message");

        SituationAwarenessMessage result = null;
        // required elements
        try {
            result = new SituationAwarenessMessage()
            .setUid(cot.getUid())
            .setType(cot.getType())
            .setHow(cot.getHow())
            .setLat(NumericUtil.parseDoubleOrDefault(cot.getLat(), 0))
            .setLon(NumericUtil.parseDoubleOrDefault(cot.getLon(), 0))
            .setCe(cot.getCe())
            .setLe(cot.getLe());
        } catch(Exception e) {
            return null;
        }

        // optional elements
        try {
            result.setCallsign(cot.getCallsign());
        } catch(Exception e) { /* ignore */ }
        try {
            result.setHae(cot.getHae());
        } catch(Exception e) { /* ignore */ }
        try {
            result.setStart(DateUtil.millisFromCotTimeStr(cot.getStart()));
        } catch(Exception e) { /* ignore */ }
        try {
            result.setTime(DateUtil.millisFromCotTimeStr(cot.getTime()));
        } catch(Exception e) { /* ignore */ }
        try {
            result.setStale(DateUtil.millisFromCotTimeStr(cot.getStale()));
        } catch(Exception e) { /* ignore */ }
        try {
            String group = ((Attribute)cot.getDocument().selectSingleNode("//detail/__group/@name")).getValue();
            if (!Strings.isNullOrEmpty(group)) {
                result.setGroup(group);
                result.getAddresses().add("group:" + group);
            }
        } catch(Exception e) { /* ignore */ }
        try {
            String role = ((Attribute)cot.getDocument().selectSingleNode("//detail/__group/@role")).getValue();
            if (!Strings.isNullOrEmpty(role)) {
                result.setRole(role);
                result.getAddresses().add("role:" + role);
            }
        } catch(Exception e) { /* ignore */ }
        try {
            if (cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY) != null) {
                for (String callsign : (List<String>) cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY)) {
                    result.getAddresses().add("callsign:" + callsign);
                }
            }
            
            if (cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY) != null) {
                for (String uid : (List<String>) cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY)) {
                    result.getAddresses().add("uid:" + uid);
                }
            }
        } catch(Exception e) { /* ignore */ }
        try {
            String missionName = ((Attribute)cot.getDocument().selectSingleNode("//detail/marti/dest/@mission")).getValue();
            if (!Strings.isNullOrEmpty(missionName)) {
                result.getMissions().add(missionName);
                result.getAddresses().add("mission:" + missionName);
            }

        } catch(Exception e) { }
        
        try {

            for (Object node : cot.getDocument().selectNodes("//detail/marti/dest/@uid")) {
                String uidDest = ((Attribute)node).getValue();
                logger.debug("uidDest: " + uidDest);

                if (!Strings.isNullOrEmpty(uidDest)) {
                    result.getUids().add(uidDest);
                    result.getAddresses().add("uid:" + uidDest);
                }
            }

        } catch(Exception e) { }
        try {
            result.setIconsetPath(((Attribute)cot.getDocument().selectSingleNode("//detail/usericon/@iconsetpath")).getValue());
        } catch(Exception e) { /* ignore */ }
        try {
            result.setColor(((Attribute)cot.getDocument().selectSingleNode("//detail/color/@argb")).getValue());
        } catch(Exception e) { /* ignore */ }
        try {
            result.setPhoneNumber(((Attribute)cot.getDocument().selectSingleNode("//detail/contact/@phone")).getValue());
        } catch(Exception e) { /* ignore */ }
        try {
            String platform = ((Attribute)cot.getDocument().selectSingleNode("//detail/takv/@platform")).getValue();
            String version = ((Attribute)cot.getDocument().selectSingleNode("//detail/takv/@version")).getValue();
            result.setTakv(platform +":"+version);
        } catch(Exception e) { /* ignore */ }
        try {
            result.setPersistent(Boolean.parseBoolean(cot.getDocument().selectSingleNode("//detail/archive[text()]").getStringValue()));
        } catch(Exception e) { /* ignore */ }
        try {
            result.setRemarks((cot.getDocument().selectSingleNode("//detail/remarks[text()]")).getStringValue());
        } catch(Exception e) { /* ignore */ }
        try {
            String missionName = ((Attribute)cot.getDocument().selectSingleNode("//detail/marti/dest/@mission")).getValue();
            if (!Strings.isNullOrEmpty(missionName)) {
                logger.debug("mission dest: " + missionName);
                result.getMissions().add(missionName);
            }

        } catch(Exception e) { /* ignore */ }
        
        try {

            for (Object node : cot.getDocument().selectNodes("//detail/marti/dest/@uid")) {
                String uidDest = ((Attribute)node).getValue();
                logger.debug("uidDest: " + uidDest);

                result.getUids().add(uidDest);
            }
            for (Object node : cot.getDocument().selectNodes("//detail/marti/dest/@callsign")) {
            	String callsignDest = ((Attribute)node).getValue();
            	result.getAddresses().add("callsign:"+ callsignDest);
            }

        } catch(Exception e) { }
        
        try {
        	String detailXML = cot.getDocument().selectSingleNode("//detail").asXML();
        	JSONObject tmpObj = XML.toJSONObject(detailXML);
        	ObjectMapper mapper = new ObjectMapper();
        	JsonNode jsonNode = mapper.readTree(tmpObj.getJSONObject("detail").toString());
        	result.setDetailJson(jsonNode);
        }
        catch(Exception e) {
        	
        }
        return result;
    }
    
    /*
     * TODO returning null for these is a bad practice, likely to lead to null pointer exceptions
     * 
     */
    public static MissionChange MissionChangeFromCot(CotEventContainer cot) {

        logger.trace("trying to convert CoT message " + cot + " to socket MissionChange message");

        MissionChange result = new MissionChange();
        
        try {
            
            // has to be a mission change CoT message
            if (!cot.getType().startsWith("t-x-m-c")) {
                return null;
            }
            
            result.setCotType(cot.getType());
        } catch(Exception e) {
            return null;
        }

        try {
            String missionName = ((Attribute) cot.getDocument().selectSingleNode("//event/detail/mission/@name")).getValue();
            
            if (Strings.isNullOrEmpty(missionName)) {
                logger.warn("empty mission name in mission change notification", cot);
                return null;
            }

            result.setMissionName(missionName);
            result.getMissions().add(missionName);
            
        } catch(Exception e) { 
            return null;
        }
        
        try {
            result.setTime(DateUtil.millisFromCotTimeStr(cot.getTime()));
        } catch(Exception e) {
            return null;
        }
        
        return result;
    }
    
    public ChatMessage chatMessageFromCot(CotEventContainer cotEvent) {
        try {
        	
        	// copy for thread safety
        	CotEventContainer cot = new CotEventContainer(cotEvent);
        	
            //<detail>
            //    <__chat id="Streaming" chatroom="All Streaming">
            //       <chatgrp uid0="Streaming" id="Streaming"/>
            //    </__chat>
            //    <link relation="p-p" type="a-f-G-U-C-I" uid="ANDROID-50:2e:5c:e5:4a:86"/>
            //    <remarks time="2015-07-06T20:18:58.283Z" source="BAO.F.ATAK.INTAC">at VDO</remarks>
            ChatMessage result = null;

            // required elements
            String cotRemarkSrc = cot.getDocument().selectSingleNode("//detail/remarks/@source").getStringValue();
            // In older versions of ATAK, this was the Callsign, now it is the UID
            String callsignOrUid = cotRemarkSrc.substring(cotRemarkSrc.lastIndexOf('.') + 1);
            result = new ChatMessage();

            Collection<RemoteContact> contactList = contactManager.getContactList();

            result.setFrom(callsignOrUid);
            result.setBody(cot.getDocument().selectSingleNode("//detail/remarks[text()]").getStringValue()); // get message body from "remarks" field
            try {
                result.setLat(Double.parseDouble(cot.getLat()));
            } catch (Exception e) {
                logger.debug("setting lat on chat message");
            }
            try {
                result.setLon(Double.parseDouble(cot.getLon()));
            } catch (Exception e) {
                logger.debug("setting lat on chat message");
            }
            
            try {
                String missionName = ((Attribute)cot.getDocument().selectSingleNode("//detail/marti/dest/@mission")).getValue();
                if (!Strings.isNullOrEmpty(missionName)) {
                    result.getMissions().add(missionName);
                    result.getAddresses().add("mission:" + missionName);
                }

            } catch(Exception e) { }
            
            try {

                for (Object node : cot.getDocument().selectNodes("//detail/marti/dest/@uid")) {
                    String uidDest = ((Attribute)node).getValue();
                    logger.debug("uidDest: " + uidDest);

                    if (!Strings.isNullOrEmpty(uidDest)) {
                        result.getUids().add(uidDest);
                        result.getAddresses().add("uid:" + uidDest);
                    }
                }

            } catch(Exception e) { }
            
            try {      
                String group = ((Attribute)cot.getDocument().selectSingleNode("//detail/__group/@name")).getValue();
                if (!Strings.isNullOrEmpty(group)) {
                    result.getAddresses().add("group:" + group);
                }
            } catch(Exception e) { /* ignore */ }
            
            try {
                String role = ((Attribute)cot.getDocument().selectSingleNode("//detail/__group/@role")).getValue();
                if (!Strings.isNullOrEmpty(role)) {
                    result.getAddresses().add("role:" + role);
                }
            } catch(Exception e) { /* ignore */ }
            
            try {
                if (cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY) != null) {
                    for (String callsign : (List<String>) cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_CALLSIGN_KEY)) {
                        result.getAddresses().add("callsign:" + callsign);
                    }
                }

                if (cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY) != null) {
                    for (String uid : (List<String>) cot.getContextValue(StreamingEndpointRewriteFilter.EXPLICIT_UID_KEY)) {
                        result.getAddresses().add("uid:" + uid);
                    }
                }
            } catch(Exception e) { /* ignore */ }

             try {
//              <detail><__chat id="TEST-98:0C:82:FB:A2:FF" chatroom="George">

                 //Need to map 'chatroom' from atak to a specific contact if possible, ATAK always uses chatroom to send messages even in 1 to 1
                String chatroom = ((Attribute)cot.getDocument().selectSingleNode("//detail/__chat/@chatroom")).getValue();
                boolean added = false;
                if (!Strings.isNullOrEmpty(chatroom)) {
                    for(RemoteContact contact : contactList){
                        //First try and match the chatroom id to a callsign
                        if (contact.getContactName().equalsIgnoreCase(chatroom)){
                            logger.trace("Matched chatroom id to contact name: " + contact);
                            result.getAddresses().add("uid:" + contact.getUid());
                            added = true;
                            break;
                        }

                        //logger.debug("Chatroom uid: " + chatroom + " == contact uid: " + contact.getUid() );
                        //Next try to match to a uid
                        if (contact.getUid().equals(chatroom)){
                            logger.trace("Matched chatroom id to contact uid: " + contact);
                            result.getAddresses().add("uid:" + chatroom);
                            added = true;
                            break;
                        }
                    }
                    //If chatroom id can't be matched to a contact, treat it as an actual chatroom
                    if (!added){
                        logger.trace("Didn't match chatroom to name or id, treating as normal chatroom");
                        result.getAddresses().add("chatroom:" + chatroom);
                    }
                }
            } catch(Exception e) { /* ignore */ }

            result.setTimestamp(DateUtil.millisFromCotTimeStr(cot.getTime()));
        
            return result;
        } catch (Exception e) {
            logger.debug(e.toString());
            logger.debug("message not parseable as chat");
            return null;
        }
    }
}
