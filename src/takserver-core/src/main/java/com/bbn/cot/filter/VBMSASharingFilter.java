package com.bbn.cot.filter;

import com.bbn.marti.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.service.SubmissionService;
import com.bbn.marti.remote.util.SpringContextBeanForApi;
import com.google.common.base.Strings;

import com.bbn.marti.remote.config.CoreConfigFacade;
import tak.server.Constants;
import tak.server.cot.CotEventContainer;

/*
 * 
 * The purpose of this class is to filter out SA messages that didn't come come from a data feed.
 *  
 */
public class VBMSASharingFilter {
	private static final Logger logger = LoggerFactory.getLogger(VBMSASharingFilter.class);
	
	private static VBMSASharingFilter instance = null;
	
	public static VBMSASharingFilter getInstance() {
		if (instance == null) {
			synchronized (VBMSASharingFilter.class) {
				if (instance == null) {
					instance = SpringContextBeanForApi.getSpringContext().getBean(VBMSASharingFilter.class);
				}
			}
		}
		return instance;
	}
	
	public CotEventContainer filter(CotEventContainer cot) {
		if (cot == null) return cot;
		
		if (SubmissionService.getInstance().isControlMessage(cot.getType())) return cot;
				
		// came from a data feed				
		String dataFeedUid = (String) cot.getContextValue(Constants.DATA_FEED_UUID_KEY);
		if (!Strings.isNullOrEmpty(dataFeedUid)) {
			return cot;
		}

		Configuration config = CoreConfigFacade.getInstance().getRemoteConfiguration();
		if (config.getVbm().isEnabled()) {
			if (config.getVbm().isDisableChatSharing()) {
				if (isChatMessage(cot)) {
					return null;
				}
			}
			if (config.getVbm().isDisableSASharing()) {
				if (!isChatMessage(cot)) {
					return null;
				}
			}
		}
		
		return cot;
	}
	
	private boolean isChatMessage(CotEventContainer cot) {
		return "b-t-f".equals(cot.getType());
	}
}
