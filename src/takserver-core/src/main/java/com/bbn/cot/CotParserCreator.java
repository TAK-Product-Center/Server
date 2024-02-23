

package com.bbn.cot;

import com.bbn.marti.remote.config.CoreConfigFacade;

import tak.server.cot.CotParser;

// Simple wrapper that looks at configuration, and creates parser
public class CotParserCreator {
		
	public static CotParser newInstance() {
		
		return new CotParser(CoreConfigFacade.getInstance().getRemoteConfiguration().getSubmission() == null ? false : CoreConfigFacade.getInstance().getRemoteConfiguration().getSubmission().isValidateXml());
	}
}
