package tak.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.Constants;

public class ActiveProfiles {
	private static final Logger logger = LoggerFactory.getLogger(ActiveProfiles.class);

	// Profiles are ways that the same Spring framework can bootstrap the different microservices.  Since they are
	// *NOT* in common to each other, they are moved here since this facade will be local to each process.
	String profile = "";
	boolean monolithProfileActive = false;
	boolean messagingProfileActive = false;
	boolean apiProfileActive = false;
	boolean configProfileActive = false;
	boolean pluginsProfileActive = false;
	boolean retentionProfileActive = false;
	boolean consoleProfileActive = false;

	// Singleton instance
	private static ActiveProfiles instance = null;


	private ActiveProfiles() {
		String profilesActive = System.getProperty("spring.profiles.active");
		if (profilesActive != null) {
			// Due to initialization order, can't use spring environment and therefore
			// ProfileTracker class yet, so look at the expected system property.
			String lcProfilesActive = profilesActive.toLowerCase();

			if (lcProfilesActive.contains(tak.server.Constants.MONOLITH_PROFILE_NAME)) {
				monolithProfileActive = true;
				profile = tak.server.Constants.MONOLITH_PROFILE_NAME;
			}
			if (lcProfilesActive.contains(tak.server.Constants.MESSAGING_PROFILE_NAME)) {
				messagingProfileActive = true;
				profile = tak.server.Constants.MESSAGING_PROFILE_NAME;
			}
			if (lcProfilesActive.contains(tak.server.Constants.API_PROFILE_NAME)) {
				apiProfileActive = true;
				profile = tak.server.Constants.API_PROFILE_NAME;
			}
			if (lcProfilesActive.contains(Constants.PLUGINS_ENABLED_PROFILE_NAME)) {
				pluginsProfileActive = true;
				profile = Constants.PLUGINS_ENABLED_PROFILE_NAME;
			}
			if (lcProfilesActive.contains(Constants.RETENTION_PROFILE_NAME)) {
				retentionProfileActive = true;
				profile = Constants.RETENTION_PROFILE_NAME;
			}
			if (lcProfilesActive.contains(tak.server.Constants.CONFIG_PROFILE_NAME)) {
				configProfileActive = true;
				profile = tak.server.Constants.CONFIG_PROFILE_NAME;
			}

			if (lcProfilesActive.contains("consolelog")) {
				consoleProfileActive = true;
			}
		}
	}

	public static ActiveProfiles getInstance() {
		if (instance == null) {
			synchronized (ActiveProfiles.class) {
				if (instance == null) {
					try {
						instance = new ActiveProfiles();
					} catch (Exception e) {
						if (logger.isErrorEnabled()) {
							logger.error("Exception instantiating ActiveProfiles : {} ", e);
						}
					}
				}
			}
		}

		return instance;
	}

	public String getProfile() {
		return profile;
	}

	public boolean isMonolithProfileActive() {
		return monolithProfileActive;
	}

	public boolean isMessagingProfileActive() {
		return messagingProfileActive;
	}

	public boolean isApiProfileActive() {
		return apiProfileActive;
	}

	public boolean isConfigProfileActive() {
		return configProfileActive;
	}

	public boolean isPluginProfileActive() {
		return pluginsProfileActive;
	}

	public boolean isRetentionProfileActive() {
		return retentionProfileActive;
	}

	public boolean isConsoleProfileActive() {
		return consoleProfileActive;
	}
}