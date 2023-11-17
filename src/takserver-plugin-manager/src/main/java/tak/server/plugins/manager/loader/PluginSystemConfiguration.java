package tak.server.plugins.manager.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains the configuration for a plugin loaded from an associated configuration file capable of loading and saving the
 * file and getting or setting the properties contained within.
 */
public class PluginSystemConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PluginSystemConfiguration.class);

    private Map<String, Object> configProperties;

    static final String PLUGIN_CONFIG_BASE = "conf/plugins/";

    static final String[] RESERVED_KEYWORDS = {"server", "tak", "system"};
    public static final String ARCHIVE_ENABLED_PROPERTY = "system.archive";
    public static final String PLUGIN_ENABLED_PROPERTY = "system.enable";

    public PluginSystemConfiguration() {
        configProperties = new HashMap<>();
    }

    /**
     * Initializes the Plugin Configuration. If the configuration file cannot be found in the expected location, an empty
     * file will be generated in its place
     *
     * @param clazz plugin class of the configuration
     */
    public PluginSystemConfiguration(Class clazz) {
        this(clazz.getName());
    }

    /**
     * Initializes the Plugin Configuration. If the configuration file cannot be found in the expected location, an empty
     * file will be generated in its place
     *
     * @param pluginClassName name of the class
     */
    public PluginSystemConfiguration(String pluginClassName) {
        String configFileName = PLUGIN_CONFIG_BASE + pluginClassName + ".yaml";
        File configFile = new File(configFileName);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                logger.error("Unable to create new configuration file for {}", pluginClassName, e);
            }
        }
        try (InputStream inputStream = Files.newInputStream(configFile.toPath());) {
        	LoaderOptions options = new LoaderOptions();
        	Yaml yaml = new Yaml(new SafeConstructor(options));
            configProperties = yaml.load(inputStream);
        } catch (Exception e) {
            logger.error("Unable to load configuration file for: {}", pluginClassName, e);
        }
        if (configProperties == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("The config for {} was empty. Creating an empty properties map.", pluginClassName);
            }
            configProperties = new HashMap<>();
        }
    }

    /**
     * Searches for the specified configuration property, if it exists, from the configuration list and returns null if no
     * such property is found
     *
     * @param key name of the property to return
     * @return object containing the value of the property, null if the property does not exist
     */
    public <T> T getProperty(String key) {
        String[] propChain = key.split("\\.");
        Object currentProperty = configProperties;
        for (String prop : propChain) {
            if (currentProperty instanceof Map) {
                currentProperty = ((Map) currentProperty).get(prop);
            } else {
                logger.error("no such property: {}", key);
                return null;
            }
        }
        return (T) currentProperty;
    }

    /**
     * Checks if a configuration file contains a specified property
     *
     * @param key name of the property
     * @return true if the config contains the property, false if it does not
     */
    public boolean containsProperty(String key) {
        String[] propChain = key.split("\\.");
        Object currentProperty = configProperties;
        for (String prop : propChain) {
            if (currentProperty instanceof Map) {
                currentProperty = ((Map<?, ?>) currentProperty).get(prop);
            } else {
                return false;
            }
        }
        return currentProperty != null;
    }

    /**
     * Gets a list of all properties
     *
     * @return the list of properties represented as strings
     */
    public List<String> getProperties() {
        List<String> properties = new ArrayList<>();
        for (Map.Entry<String, Object> entry : configProperties.entrySet()) {
            String prop = entry.getKey();
            properties.add(prop);
            if (entry.getValue() instanceof Map) {
                properties.addAll(getProperties(prop, (Map<String, ?>) entry.getValue()));
            }
        }
        return properties;
    }

    /**
     * Gets a base property and all sub properties associated with it
     *
     * @param baseProp base property to be retrieved
     * @param map      a map containing the associated sub properties
     * @return the list of sub properties
     */
    private List<String> getProperties(String baseProp, Map<String, ?> map) {
        List<String> props = new ArrayList<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String subProp = baseProp + "." + entry.getKey();
            props.add(subProp);
            if (entry.getValue() instanceof Map) {
                props.addAll(getProperties(subProp, (Map<String, ?>) entry.getValue()));
            }
        }
        return props;
    }

    /**
     * Sets a property within a configuration file so long as it does not conflict with an existing configuration.
     *
     * @param key   name of the new property to set
     * @param value value to be assigned to the new property
     * @throws Exception throws an exception if the key already exists within an existing configuration
     */
    public void setProperty(String key, Object value) throws Exception {

        logger.info("Calling setProperty: key {}, value {}", key, value);

        List<String> propChain = Arrays.asList(key.split("\\."));
        Object current = configProperties;
        for (int i = 0; i < propChain.size() - 1; i++) {
            String prop = propChain.get(i);
            if (current instanceof Map) {
                if (((Map) current).containsKey(prop)) {
                    current = ((Map) current).get(prop);
                } else {
                    ((Map) current).put(prop, new HashMap<>());
                    current = ((Map<?, ?>) current).get(prop);
                }
            } else {
                throw new Exception("Key creates conflicts with existing configuration");
            }
        }
        String lastKey = propChain.get(propChain.size() - 1);
        if (current instanceof Map) {
            ((Map) current).put(lastKey, value);
        } else {
            throw new Exception("Key creates conflicts with existing configuration");
        }
    }

    /**
     * Saves a plugin configuration to a given plugin class
     *
     * @param clazz plugin class to save the configuration to
     * @throws IOException if there is nothing to be saved within the configuration, an exception will be thrown
     */
    public void save(Class clazz) throws IOException {
        save(clazz.getName());
    }

    /**
     * Saves a plugin configuration to a given plugin class. The save location and file path are determined by the name
     * of the class
     *
     * @param pluginClassName name of the class to save the configuration to
     * @throws IOException if there is nothing to be saved within the configuration, an exception will be thrown
     */
    public void save(String pluginClassName) throws IOException {
        synchronized (this) {
            String configFileName = PLUGIN_CONFIG_BASE + pluginClassName + ".yaml";

            logger.info("Saving plugin configuration to {}", configFileName);

            File configFile = new File(configFileName);
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                try {
                    configFile.createNewFile();
                } catch (IOException e) {
                    logger.error("Unable to create configuration file {}", configFileName, e);
                }
            }
            if (configProperties == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Nothing to save in plugin configuration {}", configFileName);
                }
                return;
            }

            try (Writer writer = new FileWriter(configFile);) {
                Yaml yaml = new Yaml();
                yaml.dump(configProperties, writer);
                logger.info("Configuration data {} saved for plugin {}", yaml.dump(configProperties), pluginClassName);
            }
        }
    }
}
