package tak.server.federation.hub.broker;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;

public class FederationHubServerConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(FederationHubServerConfigManager.class);

	private final String configFile;
	private FederationHubServerConfig config = null;
	
	FederationHubServerConfigManager(String configFile) {
		this.configFile = configFile;
		
		config = loadConfig();
		 
		if (Strings.isNullOrEmpty(config.getId())) {
			config.setId(UUID.randomUUID().toString().replace("-", ""));
			saveConfig(config);
		}
	}
	
	public FederationHubServerConfig getConfig() {
		return config;
	}
	
	private FederationHubServerConfig loadConfig() {
		try {
			if (getClass().getResource(configFile) != null) {
				// It's a resource.
				config = new ObjectMapper(new YAMLFactory()).readValue(getClass().getResourceAsStream(configFile),
						FederationHubServerConfig.class);
			} else {
				// It's a file.
				config = new ObjectMapper(new YAMLFactory()).readValue(new FileInputStream(configFile),
						FederationHubServerConfig.class);	
			}
		} catch (Exception e) {
			logger.error("Error loading broker config", e);
		}
		
		return config;
	}

	public FederationHubServerConfig saveConfig(FederationHubServerConfig config) {
		
		try {
			ObjectMapper om = new ObjectMapper(new YAMLFactory());
			om.writeValue(new File(configFile), config);
		} catch (Exception e) {
			logger.error("Error writing broker config", e);
		}
		
		config = loadConfig();
		
		return config;
	}
}
