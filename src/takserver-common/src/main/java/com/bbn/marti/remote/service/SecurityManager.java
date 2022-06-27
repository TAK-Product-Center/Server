package com.bbn.marti.remote.service;

import java.util.Collection;
import java.util.HashMap;

import com.bbn.marti.remote.AuthenticationConfigInfo;
import com.bbn.marti.remote.SecurityConfigInfo;

/**
 */
public interface SecurityManager {
	
	AuthenticationConfigInfo getAuthenticationConfig();

	void modifyAuthenticationConfig(AuthenticationConfigInfo info);

	SecurityConfigInfo getSecurityConfig();

	void modifySecurityConfig(SecurityConfigInfo info);

	Collection<Integer> getNonSecurePorts();

	HashMap<String, Boolean> verifyConfiguration();
}
