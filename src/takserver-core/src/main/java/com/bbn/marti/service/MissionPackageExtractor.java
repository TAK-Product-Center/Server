package com.bbn.marti.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.NavigableSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.naming.NamingException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.json.JSONObject;
import org.json.XML;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.socket.MissionPackageEntry;
import com.bbn.marti.remote.socket.MissionPackageMessage;
import com.bbn.marti.remote.socket.TakMessage;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.Metadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import tak.server.Constants;
import tak.server.cot.CotEventContainer;
import tak.server.messaging.Messenger;

public class MissionPackageExtractor {

	private static final Logger logger = LoggerFactory.getLogger(MissionPackageExtractor.class.getSimpleName());

	public static final String B_F_T_R = "b-f-t-r";
	private static final String CALLSIGN = "callsign";

	private MimetypesFileTypeMap mtft;
	private MissionPackageMessage mpm;

	private int autoExtractSizeLimitMB = -1;
	private long autoExtractSizeLimitBytes;

	@Autowired
	private EnterpriseSyncService enterpriseSyncService;

	@Autowired
	private Messenger<TakMessage> takMessenger;

	@Autowired
	private CoreConfig coreConfig;

	public void missionPackageFromCot(CotEventContainer cot, NavigableSet<Group> groups) {

		Document cotDoc = cot.getDocument();

		try {
			String filename = ((Attribute) cotDoc.selectSingleNode("//event/detail/fileshare/@filename")).getValue();

			if (Strings.isNullOrEmpty(filename)) {
				logger.warn("empty file name in mission package notification", cot);
				return;
			}

			mpm = new MissionPackageMessage();
			mpm.setCotType(cot.getType());

			if (logger.isDebugEnabled()) {
				logger.debug("trying to convert CoT message " + cot + " to socket MissionPackage message");
			}

			mpm.setFilename(filename);
			mpm.setSenderCallSign(cot.getCallsign());
			String destCallSign = ((Attribute) cotDoc.selectSingleNode("//event/detail/marti/dest/@callsign")).getValue();
			mpm.setDestCallSign(destCallSign);
			mpm.getAddresses().add("callsign:"+ destCallSign);
			String size = ((Attribute) cotDoc.selectSingleNode("//event/detail/fileshare/@sizeInBytes")).getValue();
			mpm.setSizeInBytes(Long.parseLong(size));
			mpm.setSenderUrl(((Attribute) cotDoc.selectSingleNode("//event/detail/fileshare/@senderUrl")).getValue());
			mpm.setSha256Hash(((Attribute) cotDoc.selectSingleNode("//event/detail/fileshare/@sha256")).getValue());

		} catch (Exception e) {
			logger.warn("mission package announcement exception" + e.getMessage());
			return;
		}

		unzip(mpm, groups);

		try {
			mpm.getGroups().addAll(groups);

			if (cot.getContextValue(Constants.TOPICS_KEY) != null && cot.getContextValue(Constants.TOPICS_KEY) instanceof Set<?>) {
				@SuppressWarnings("unchecked")
				Set<String> explicitTopics = (Set<String>) cot.getContextValue(Constants.TOPICS_KEY);
				mpm.getTopics().addAll(explicitTopics);
			}

			takMessenger.send(mpm);
		} catch (Exception e) {
			logger.warn("Error sending distributed TakMessage", e);

		}
	}

	public void unzip(MissionPackageMessage mpm, NavigableSet<Group> groups) {
		if (autoExtractSizeLimitMB == -1) {
			loadSizeLimit();
		}

		// if the size is too large, then don't unpack it
		if (mpm.getSizeInBytes() > autoExtractSizeLimitBytes) {
			String message = "MissionPackage file exceeds server's Mission Package auto-extract size limit of "
					+ autoExtractSizeLimitMB + " MB! (limit is set in server's conf/context.xml)";
			logger.warn(message);
			mpm.setUnPacked(false);
			return;

		}

		if (logger.isDebugEnabled()) {
			logger.debug("auto extract size limit in MB: " + autoExtractSizeLimitMB);
		}
		byte[] contents;

		String packageUid = mpm.getSha256Hash();
		MissionPackageEntry missionPackageEntry;

		boolean[] bitVectorForGroups = RemoteUtil.getInstance().getBitVectorForGroups(groups);
		String groupVector = RemoteUtil.getInstance().bitVectorToString(bitVectorForGroups);
		try {
			contents = enterpriseSyncService.getContentByUid(packageUid, groupVector);
			ByteArrayInputStream bis = new ByteArrayInputStream(contents);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));

			// copy all files over to the new mp
			ZipEntry entry;
			String filename;

			while ((entry = zis.getNextEntry()) != null) {
				missionPackageEntry = new MissionPackageEntry();
				filename = entry.getName();
				filename = FilenameUtils.getName(filename);
				if (logger.isDebugEnabled()) {
					logger.debug(" adding zip entry adding entry to list " + filename);
				}
				missionPackageEntry.setName(filename);
				byte[] data = IOUtils.toByteArray(zis);
				missionPackageEntry.setSizeInBytes(Long.valueOf(data.length));

				// "real" mission packages don't contain a callsign parameter in the manifest
				if (filename.contains("manifest.xml")) {
					setRealMissionPackageFlag(data);
				}

				if (!mpm.isRealMissionPackage() && filename.contains(".cot")) {
					mpm.setInnerCotJson(parseInnerCot(data));
				}
				mpm.addMissionPackageEntry(missionPackageEntry);
				Metadata metadata = uploadFile(data, filename, groupVector);
				// should we throw an error here????
				try {
					if (metadata != null) {
						missionPackageEntry.setHash(metadata.getHash());
					}
				} catch (IllegalArgumentException e) {
					logger.error(" no hash " + e.getMessage());
				}
			}
			zis.close();
			bis.close();
			if (logger.isDebugEnabled()) {
				logger.debug(" mission package file set includes : " + mpm.getMissionPackageEntries());
			}

		} catch (SQLException | NamingException |  IOException e) {
			logger.error("Exception opening zipfile " + mpm.getFilename() + e.getMessage());
		}
	}

	private void loadSizeLimit() {
		autoExtractSizeLimitMB = coreConfig.getRemoteConfiguration().getNetwork().getMissionPackageAutoExtractSizeLimitMB();

		autoExtractSizeLimitBytes = ((long) autoExtractSizeLimitMB) * 1000000L;
	}

	private void setRealMissionPackageFlag(byte[] data)  {
		SAXReader reader = new SAXReader();
		Document doc = null;
		try {
			doc = reader.read(new ByteArrayInputStream(data));
			for (Object node : doc.selectNodes("//MissionPackageManifest/Configuration/Parameter/@name")) {
				String nodeName = ((Attribute) node).getValue();
				if (CALLSIGN.equals(nodeName)) {
					mpm.setRealMissionPackage(false);
				}
			}
		} catch (DocumentException e) {
			logger.error(e.getMessage());
		}
	}

	private JsonNode parseInnerCot(byte[] innerCot) {
		JSONObject tmpObj = XML.toJSONObject(new String(innerCot));
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = null;
		try {
			jsonNode = mapper.readTree(tmpObj.getJSONObject("event").toString());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return jsonNode;
	}

	private Metadata uploadFile(byte[] data, String filename, String groupVector) {
		if (mtft == null) {
			mtft = new MimetypesFileTypeMap();

		}
		if (Strings.isNullOrEmpty((filename))) {
			throw new IllegalArgumentException("empty filename");
		}
		if (data.length <= 0) {
			throw new IllegalArgumentException(
					"Mission Package entry does not contain a positive size argument.");
		}


		String mimeType = mtft.getContentType(filename);
		Metadata metaData = new Metadata();
		metaData.set(Metadata.Field.DownloadPath, filename);
		metaData.set(Metadata.Field.Name, filename);
		metaData.set(Metadata.Field.MIMEType, mimeType);
		Metadata uploadedMetadata = null;

		try {
			uploadedMetadata = enterpriseSyncService.insertResource(metaData, data, groupVector);
		} catch (SQLException | NamingException | ValidationException | IOException e) {
			logger.warn(e.getMessage());
		}
		return uploadedMetadata;
	}

}
