package com.bbn.marti.test.shared.data.templates;

import com.bbn.marti.test.shared.data.protocols.ProtocolProfiles;

/**
 * Created on 7/21/16.
 */
public enum ReferencedProtocolProfilesInputTemplate_A26D0F85 {
	//////////////////////////
	// Begin Generated ProtocolProfiles
	////////////////////////// 68EBC0C6-82C3-4C2F-9779-9191B5B432EC
	dummy_protocol("iShouldNotExist");


	public static String generateReferenceLine(ProtocolProfiles sourceProfile) {
		String name = sourceProfile.getValue();
		return "    " + name + "(\"" + name + "\"),";
	}
	////////////////////////// BD6A4745-76B4-42DD-AE35-5C2251DD6301
	// End Generated ProtocolProfiles
	//////////////////////////

	private final String protocolProfileIdentifier;
	private ProtocolProfiles protocolProfile;


	ReferencedProtocolProfilesInputTemplate_A26D0F85(String protocolProfileIdentifier) {
		this.protocolProfileIdentifier = protocolProfileIdentifier;
	}

	public ProtocolProfiles getProtocolProfile() {
		if (protocolProfile == null) {
			protocolProfile = ProtocolProfiles.getInputByValue(protocolProfileIdentifier);
		}
		return protocolProfile;
	}
}
