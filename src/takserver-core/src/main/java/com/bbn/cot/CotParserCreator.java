

package com.bbn.cot;

import com.bbn.marti.service.DistributedConfiguration;

import tak.server.cot.CotParser;

// Simple wrapper that looks at configuration, and creates parser
public class CotParserCreator {
		
	public static CotParser newInstance() {
		
		return new CotParser(DistributedConfiguration.getInstance().getRemoteConfiguration().getSubmission() == null ? false : DistributedConfiguration.getInstance().getRemoteConfiguration().getSubmission().isValidateXml());
	}
}
