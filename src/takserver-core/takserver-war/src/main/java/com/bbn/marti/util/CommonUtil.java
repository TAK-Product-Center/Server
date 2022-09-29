package com.bbn.marti.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.Marshaller;

import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.FederationManager;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.groups.User;
import com.bbn.marti.remote.socket.ChatMessage;
import com.bbn.marti.remote.socket.SituationAwarenessMessage;
import com.bbn.marti.remote.socket.TakMessage;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.Metadata.Field;
import com.bbn.marti.util.spring.RequestHolderBean;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import tak.server.Constants;

/*
 * Shared utility functions
 * 
 * 
 */
public class CommonUtil {

	private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

	@Autowired
	private GroupManager groupManager;

	@Autowired
	private FederationManager federationConfigurator;

	@Autowired
	private RequestHolderBean requestBean;

	@Autowired
	private Marshaller marshaller;

	@Autowired
	private CoreConfig config;

	@Autowired
	private Validator validator;


	public NavigableSet<Group> getAllInOutGroups() {
		ConcurrentSkipListSet<Group> allInOutGroups = new ConcurrentSkipListSet<>(groupManager.getAllGroups());
		for (Group group : allInOutGroups) {
			Group incp = group.getCopy();
			incp.setDirection(Direction.IN);
			allInOutGroups.add(incp);
		}
		return allInOutGroups;
	}


	/*
	 * Get the group set for the authenticated for an active HttpServletRequest
	 * 
	 */
	public NavigableSet<Group> getGroupsFromRequest(HttpServletRequest request) throws RemoteException {
		return getGroupsFromSessionId(request.getSession().getId());
	}

	/*
	 * Get the group vector corresponding to the authenticated for the currently
	 * active HttpServletRequest
	 * 
	 */
	public NavigableSet<Group> getGroupsFromActiveRequest() throws RemoteException {
		return getGroupsFromSessionId(requestBean.getRequest().getSession().getId());
	}

	/*
	 * Get the group vector corresponding to the authenticated for the currently
	 * active HttpServletRequest
	 * 
	 */
	public NavigableSet<Group> getGroupsFromSessionId(String sessionId) throws RemoteException {

		if (Strings.isNullOrEmpty(sessionId)) {
			throw new IllegalArgumentException("empty HTTP session ID");
		}

		if (isAdmin()) {
			return getAllInOutGroups();
		}

		NavigableSet<Group> groups = new ConcurrentSkipListSet<>();

		try {

			if (requestBean.isFederate()) {

				if (logger.isDebugEnabled()) {
					logger.debug("special federate role " + Constants.FEDERATE_ROLE + " matched in request");
				}

				HttpServletRequest request = requestBean.getRequest();

				if (request.getSession().getAttribute(Constants.X509_CERT_FP) != null && request.getSession().getAttribute(Constants.X509_CERT_FP) instanceof String) {
					String fp = (String) request.getSession().getAttribute(Constants.X509_CERT_FP);

					if (logger.isDebugEnabled()) {
						logger.debug("cert fingerprint: " + fp);
					}

					for (Federate fed : federationConfigurator.getConfiguredFederates()) {
						if (fed != null && fed.getId() != null && !Strings.isNullOrEmpty(fp) && fed.getId().equals(fp)) {

							if (logger.isDebugEnabled()) {
								logger.debug("match found for cert fingerprint in federation config " + fed.getName());
							}

							for (String inGroupName : fed.getInboundGroup()) {
								groups.add(groupManager
										.hydrateGroup(new Group(inGroupName, Direction.IN)));
							}

							// start here

							for (String outGroupName : fed.getInboundGroup()) {
								groups.add(groupManager.hydrateGroup(new Group(outGroupName, Direction.OUT)));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception matching federate groups", e);
			}
		}

		if (groups.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("no federate groups - getting groups for connection id");
			}

			groups = groupManager.getGroupsByConnectionId(sessionId);
		}

		if (!config.getRemoteConfiguration().getAuth().isX509UseGroupCache()) {
			if (groups.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("no federate or user groups - using default __ANON__ group");
				}

				groups.add(groupManager.getGroup("__ANON__", Direction.IN));
				groups.add(groupManager.getGroup("__ANON__", Direction.OUT));
			}
		}

		return groups;
	}

	/*
	 * Get the user from the currently active HttpServletRequest
	 * 
	 */
	public User getUserFromActiveRequest() throws RemoteException {

		return groupManager.getUserByConnectionId(requestBean.getRequest().getSession().getId());
	}

	/*
	 * Get the group vector corresponding to the authenticated for the currently
	 * active HttpServletRequest
	 *
	 */
	public String getGroupBitVector(HttpServletRequest request) throws RemoteException {
		return getGroupBitVector(request, null);
	}

	/*
	 * Get the group vector corresponding to the authenticated for the currently
	 * active HttpServletRequest. If direction is provided, filter results to
	 * only those matching the direction
	 */
	public String getGroupBitVector(HttpServletRequest request, Direction direction) throws RemoteException {

		if (isAdmin()) {
			return RemoteUtil.getInstance().getBitStringAllGroups();
		}

		NavigableSet<Group> groups = getGroupsFromRequest(request);

		if (direction != null) {
			groups.removeIf(g -> g.getDirection() != direction);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("group list: " + groups);
		}

		String groupVector = RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(groups));

		return groupVector;
	}

	public String getGroupVectorBitString() {
		return getGroupVectorBitString(requestBean.getRequest());
	}

	public String getGroupVectorBitString(HttpServletRequest request) {
		return getGroupVectorBitString(request, null);
	}

	public String getGroupVectorBitString(HttpServletRequest request, Direction direction) {

		String groupVector = null;

		try {
			// Get group vector for the user associated with this session
			groupVector = getGroupBitVector(request, direction);

			if (logger.isDebugEnabled()) {
				logger.debug("groups bit vector: " + groupVector);
			}
		} catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e.getMessage(), e);
		}

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}

		return groupVector;
	}

	public String getGroupVectorBitString(String sessionId) {

		String groupVector = null;

		try {
			// Get group vector for the user associated with this session
			return RemoteUtil.getInstance().bitVectorToString(RemoteUtil.getInstance().getBitVectorForGroups(getGroupsFromSessionId(sessionId)));

		} catch (Exception e) {
			logger.debug("exception getting group membership for current web user " + e.getMessage(), e);
		}

		if (Strings.isNullOrEmpty(groupVector)) {
			throw new IllegalStateException("empty group vector");
		}

		return groupVector;
	}

	public NavigableSet<Group> getUserGroups(HttpServletRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("no active HttpServletRequest");
		}

		if (isAdmin()) {
			return getAllInOutGroups();
		}

		User user = groupManager.getUserByConnectionId(requestBean.getRequest().getSession().getId());

		return groupManager.getGroups(user);
	}

	public NavigableSet<Group> getUserGroups(String sessionId) {

		if (Strings.isNullOrEmpty(sessionId)) {
			throw new IllegalArgumentException("empty sessionId");
		}

		if (isAdmin()) {
			return getAllInOutGroups();
		}

		User user = groupManager.getUserByConnectionId(sessionId);

		return groupManager.getGroups(user);
	}

	public StringBuilder parseAddresses(TakMessage message, AtomicInteger added) {
		StringBuilder marti = new StringBuilder("<marti>");

		added.set(0);

		// build up address set
		for (String address : message.getAddresses()) {
			SimpleEntry<String, String> resolved = resolveChatAddress(address);
			if (!resolved.getKey().equals("group")) {
				added.incrementAndGet();
				marti.append("<dest " + resolved.getKey() + "=\"" + resolved.getValue() + "\"/>");
			}
		}

		marti.append("</marti>");

		return marti;
	}

	public String chatToCot(ChatMessage chat) {

		Objects.requireNonNull(chat, "chat message");

		String uid = !Strings.isNullOrEmpty(chat.getFrom()) ? chat.getFrom() : UUID.randomUUID().toString();
		String time = DateUtil.toCotTime(chat.getTimestamp());
		String start = DateUtil.toCotTime(chat.getTimestamp());
		String stale = DateUtil.toCotTime(chat.getTimestamp() + 86400000); // add one day tRo now.
		String how = "h-g-i-g-o"; // human garbage-in, garbage-out
		double hae = 0.0;
		int ce = 9999999;
		int le = 9999999;

		AtomicInteger added = new AtomicInteger(0);
		StringBuilder marti = parseAddresses(chat, added);
		String randomUUID = UUID.randomUUID().toString();
		String sendCallsign;
		if(Strings.isNullOrEmpty(chat.getSenderCallsign())){
			sendCallsign = uid;
		}
		else{
			sendCallsign = chat.getSenderCallsign();
		}
		String result =
				"<event version='2.0' uid='" + uid + "." + UUID.randomUUID().toString() + "' type='b-t-f' time='"+time+"' start='"+start+"' stale='"+stale+"' how='"+how+"'>"
						+ "<point lat='" + chat.getLat() + "' lon='" + chat.getLon() + "' hae='"+hae+"' ce='"+ce+"' le='"+le+"' />"
						+ "<detail>"
						+ "<__chat parent='RootContactGroup' groupOwner='false' chatroom='" + uid + "' id='" + uid + "' senderCallsign='" + sendCallsign + "'>";

		//Fill in uids in chatgrp (this is the way ATAK needs it)
		//uids are listed for every contact participating in this conversation even if its only 1 to 1 communication
		int uidCounter = 0;
		result += "<chatgrp ";
		for(String converUid : chat.getConversationUids()){
			result += "uid" + uidCounter + "='" + converUid +"' ";
			uidCounter++;
		}
		result += "/></__chat>";
		if (added.get() > 0) {
			result += marti.toString();
		}

		result += "<remarks time='" + time + "' source='" + uid + "'>" + chat.getBody() + "</remarks>";
		result += "</detail>"
				+ "</event>";
		return result;
	}

	public String specialChatroomChatToCot(ChatMessage chat, String specialChatroom) {
		Objects.requireNonNull(chat, "chat message");

		String uid = !Strings.isNullOrEmpty(chat.getFrom()) ? chat.getFrom() : UUID.randomUUID().toString();
		String time = DateUtil.toCotTime(chat.getTimestamp());
		String start = DateUtil.toCotTime(chat.getTimestamp());
		String stale = DateUtil.toCotTime(chat.getTimestamp() + 86400000); // add one day tRo now.
		String how = "h-g-i-g-o"; // human garbage-in, garbage-out
		double hae = chat.getHae() == null ? 0.0 : chat.getHae();
		int ce = 9999999;
		int le = 9999999;

		AtomicInteger added = new AtomicInteger(0);
		StringBuilder marti = parseAddresses(chat, added);
		String sendCallsign;
		if(Strings.isNullOrEmpty(chat.getSenderCallsign())){
			sendCallsign = uid;
		}
		else{
			sendCallsign = chat.getSenderCallsign();
		}
		String randomUUID = UUID.randomUUID().toString();
		String result =
				"<event version='2.0' uid='" + uid + "." +  specialChatroom + "." + randomUUID + "' type='b-t-f' time='"+time+"' start='"+start+"' stale='"+stale+"' how='"+how+"'>"
						+ "<point lat='" + chat.getLat() + "' lon='" + chat.getLon() + "' hae='"+hae+"' ce='"+ce+"' le='"+le+"' />"
						+ "<detail>"
						+ "<__chat parent='RootContactGroup' groupOwner='false' chatroom='" + specialChatroom + "' id='" + specialChatroom + "' senderCallsign='" + sendCallsign + "'>"
						+ "<chatgrp uid0='" + chat.getFrom() + "' uid1='" + specialChatroom + "' id='" + specialChatroom + "'/></__chat>";

		if (added.get() > 0) {
			result += marti.toString();
		}

		result += "<remarks time='" + time + "' source='" + uid + "'>" + chat.getBody() + "</remarks>";
		result += "</detail>"
				+ "</event>";
		return result;   	
	}

	public String getSpecialChatroom(ChatMessage chatMessage){
		for(String address : chatMessage.getAddresses()){
			SimpleEntry<String, String> entry = resolveChatAddress(address);
			if(entry.getKey().equals("chatroom")){
				if(entry.getValue().equalsIgnoreCase(SpecialChatrooms.ALL_STREAMING.name)){
					return SpecialChatrooms.ALL_STREAMING.name;
				}
				else if(entry.getValue().equalsIgnoreCase(SpecialChatrooms.ALL_CHAT.name)){
					return SpecialChatrooms.ALL_CHAT.name;
				}
			}
		}
		return null;
	}
	public boolean isAllStreamingChat(ChatMessage chat) {
		for(String address : chat.getAddresses()) {
			SimpleEntry<String, String> entry = resolveChatAddress(address);
			if(entry.getKey().equals("chatroom") && entry.getValue().toLowerCase(Locale.ENGLISH).equals("all streaming")) {
				return true;
			}
		}
		return false;
	}
	public String saToCot(SituationAwarenessMessage sa) {

		Objects.requireNonNull(sa, "SituationAwarenessMessage");

		AtomicInteger added = new AtomicInteger(0);
		StringBuilder marti = parseAddresses(sa, added);

		String uid = sa.getUid() == null ? java.util.UUID.randomUUID().toString() : sa.getUid();
		String time = DateUtil.toCotTime(new Date().getTime());
		String start = DateUtil.toCotTime(new Date().getTime());
		String stale = DateUtil.toCotTime(new Date().getTime() + 86400000); // add one day to now.
		String how = sa.getHow() == null ? "h-g-i-g-o" : sa.getHow();
		double hae = sa.getHae() == null ? 0.0 : sa.getHae();
		double ce = sa.getCe() == null ? 9999999 : sa.getCe();
		double le = sa.getLe() == null ? 9999999 : sa.getLe();
		String result =
				"<event version='2.0' uid='"+uid+"' type='"+sa.getType()+"' time='"+time+"' start='"+start+"' stale='"+stale+"' how='"+how+"'>"
						+ "<point lat='"+sa.getLat()+"' lon='"+sa.getLon()+"' hae='"+hae+"' ce='"+ce+"' le='"+le+"' />"
						+ "<detail>";

		if (added.get() > 0) {
			result += marti.toString();
		}

		if(sa.getCallsign() != null) {
			result += "<contact callsign='"+sa.getCallsign() + "' ";
			if(sa.getPhoneNumber() != null) {
				result += "phone='" + sa.getPhoneNumber() + "' ";
			}
			if(sa.getTakv() != null) {
				result += "endpoint='*:-1:stcp' ";
			}
			result += "/>";
		}
		if(sa.getGroup() != null) {
			result += "<__group name='" + sa.getGroup() + "' role='" + sa.getRole() + "'/>";
		}
		if(sa.getTakv() != null) {
			String platform = sa.getTakv().split(":")[0];
			String version = sa.getTakv().split(":")[1];
			result += "<takv platform='" + platform + "' version='" + version + "'/>";
		}
		if(sa.getIconsetPath() != null) {
			result += "<usericon iconsetpath='" + sa.getIconsetPath() + "'/>";
		}
		if(sa.getColor() != null) {
			result += "<color argb='" + sa.getColor() + "' />";
		}
		if(sa.getPersistent() != null) {
			result += "<archive>" + sa.getPersistent() + "<archive>";
		}
		if(sa.getRemarks() != null) {
			result += "<remarks>" + sa.getRemarks() + "</remarks>";
		}
		if(sa.getDetailJson() != null && !sa.getDetailJson().equals("")) {
			String detailJson = sa.getDetailJson();
			//Recursively build XML details
			result += recursiveXMLDetailBuilder(new JSONObject(detailJson));
		}

		result += "</detail>"
				+ "</event>";
		return result;
	}

	private String recursiveXMLDetailBuilder(JSONObject obj) {
		StringBuilder sb = new StringBuilder();
		if(obj.names() != null) {
			for (int i = 0; i < obj.names().length(); i++) {
				String key = obj.names().getString(i);
				if (obj.optJSONArray(key) != null) {
					JSONArray tmpArray = obj.getJSONArray(key);
					for (int j = 0; j < tmpArray.length(); j++) {
						sb.append(helpBuildXMLFromJson(tmpArray.getJSONObject(j), key));
					}
				} else {
					JSONObject subObj = obj.getJSONObject(key);
					sb.append(helpBuildXMLFromJson(subObj, key));
				}
			}
		}
		return sb.toString();
	}

	private String helpBuildXMLFromJson(JSONObject obj, String key) {
		StringBuilder sb = new StringBuilder();
		List<String> subStrings = new ArrayList<>();
		sb.append("<" + key);
		//Special case for LineStyle and PolyStyle for ATAK
		if(key.equalsIgnoreCase("LineStyle") || key.equalsIgnoreCase("PolyStyle")){
			sb.append(">");
			for(int i = 0; i < obj.names().length(); i++){
				String name = obj.names().getString(i);
				Object val = obj.get(name);
				sb.append("<" + name + ">" + val.toString() + "</" + name + ">");
			}
			sb.append("</" + key + ">");
			return sb.toString();
		}
		else {
			for (int j = 0; j < obj.names().length(); j++) {
				String name = obj.names().getString(j);
				Object val = obj.get(name);
				if (obj.optJSONObject(name) != null) {
					subStrings.add(helpBuildXMLFromJson(obj.getJSONObject(name), name));
					continue;
				}
				sb.append(" " + name + "='" + val.toString() + "'");
			}
			if (subStrings.size() != 0) {
				for (String s : subStrings) {
					sb.append(s);
				}
				int index = sb.indexOf("<", 1);
				sb.insert(index, ">");
				sb.append("</" + key + ">");
			} else {
				sb.append("/>");
			}
			return sb.toString();
		}
	}

	// validate and parse a chat address
	public SimpleEntry<String, String> resolveChatAddress(String address) {
		Objects.requireNonNull(address, "address");

		List<String> parts = Lists.newArrayList(Splitter.on(":").split(address));

		if (parts.size() < 2) {
			throw new IllegalArgumentException("invalid chat address: " + address);
		}

		String kind = parts.get(0);
		String arg = parts.get(1);
		if (!kind.toLowerCase(Locale.ENGLISH).equals("mission") && !kind.toLowerCase(Locale.ENGLISH).equals("uid")
				&& !kind.toLowerCase(Locale.ENGLISH).equals("group") && !kind.toLowerCase(Locale.ENGLISH).equals("chatroom")) {
			throw new IllegalArgumentException("invalid chat address type " + kind);
		}

		return new SimpleEntry<>(kind.toLowerCase(), arg);
	}

	public static ScheduledExecutorService newScheduledExecutor(String name, int size) {
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-%1$d").build();

		return new ScheduledThreadPoolExecutor(size, threadFactory);
	}

	public boolean isAdmin() {
		return requestBean.isAdmin();
	}

	public static String getFileTransferCotMessage(
			String uid, String shaHash, String callsign, String filename, String url, long sizeInBytes, String[] contacts)
	{
		String time = DateUtil.toCotTime(System.currentTimeMillis()); // now
		String staleTime = DateUtil.toCotTime(System.currentTimeMillis()+100000); // 100 seconds from now
		String cot = "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>"
				+ "<event version='2.0' uid='"+uid+"' type='b-f-t-r' time='"+time+"' start='"+time+"' stale='"+staleTime+"' how='h-e'>"
				+ "<point lat='0.0' lon='0.0' hae='9999999.0' ce='9999999' le='9999999' />"
				+ "<detail>"
				+ "<fileshare sha256='"+shaHash+"' senderUid='"+uid+"' name='"+filename+"' filename='"+filename+"' senderUrl='"+url+"' sizeInBytes='"+sizeInBytes+"' senderCallsign='"+callsign+"'/>"
				+ "<marti>";
		for(String contact : contacts) {
			cot += "<dest uid='" + contact + "'/>";
		}
		//+ "<ackrequest uid='02629afa-ab2e-44a8-9048-2f517b72b221' ackrequested='true' tag='MP-Grizzly' endpoint='192.168.1.9:4242:tcp'/>"
		//+ "<precisionlocation geopointsrc='???' altsrc='???'/>"
		cot += "</marti>"
				+ "</detail>"
				+ "</event>";
		return cot;
	}

	public static String SHA256(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
		msgDigest.update(data);
		byte[] mdbytes = msgDigest.digest();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		String shaHash = sb.toString();
		return shaHash;
	}

	public String toXml(Object object) {
		String xml;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			marshaller.marshal(object, new StreamResult(os));
			os.close();
		} catch (IOException ex) {
			logger.error("Exception closing output stream!", ex);
		}

		xml = new String(os.toByteArray());

		// strip off the <?xml header elements
		xml = xml.replace(xml.substring(0, xml.indexOf(">") + 1),"");

		return xml;
	}

	public boolean hasAccessWriteOnly(Set<Group> groups, String groupName) {

		if (Strings.isNullOrEmpty(groupName)) {
			return false;
		}

		boolean in = false;
		boolean out = false;
		for (Group group : groups) {
			if (group.getName().equals(groupName)) {
				if (group.getDirection().equals(Direction.OUT)) {
					out = true;
				} else if (group.getDirection().equals(Direction.IN)) {
					in = true;
				}
			}
		}

		if (in && !out) {
			return true;
		}

		return false;
	}

	// use custom ESAPI validator to validate enterprise sync Metadata object
	public void validateMetadata(Metadata metadata) throws ValidationException, IntrusionException {

		if (metadata == null) {
			throw new IllegalArgumentException("null metadata");
		}

		Map<Field, String[]> fields = metadata.getFields();

		if (validator != null) {
			for (Field field : metadata.getFields().keySet()) {
				String[] values = fields.get(field);
				if (values != null) {
					for (String value : values) {
						validator.getValidInput("Metadata validation (" + field.toString() + ")",
								value, field.validationType.name(), field.maximumLength, true);

						if (logger.isTraceEnabled()) {
							logger.trace("validated " + field);
						}
					}
				}
			}
		}
	}
}
