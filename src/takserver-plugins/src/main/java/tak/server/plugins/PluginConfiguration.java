package tak.server.plugins;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PluginConfiguration.class);

    private Map<String, Object> obj;
    private final File sourceFile;

    static final String PLUGIN_CONFIG_BASE = "conf/plugins/";

    static final String[] RESERVED_KEYWORDS = {"server", "tak", "system"};

    public PluginConfiguration() {
        obj = new HashMap<String, Object>();
        sourceFile = null;
    }

    public PluginConfiguration(Class<?> clazz) {
        String configFileName = PLUGIN_CONFIG_BASE + clazz.getName() + ".yaml";
        sourceFile = new File(configFileName);
        if (!sourceFile.exists()) {
            sourceFile.getParentFile().mkdirs();
            try {
                sourceFile.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        try (InputStream inputStream = new FileInputStream(sourceFile);) {
        	LoaderOptions options = new LoaderOptions();
        	Yaml yaml = new Yaml(new SafeConstructor(options));
            obj = yaml.load(inputStream);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        if (obj == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("The config was empty. Create empty properties map.");
            }
            obj = new HashMap<String, Object>();
        } else {
            // remove reserved keywords
            Arrays.asList(RESERVED_KEYWORDS).forEach(obj.keySet()::remove);
        }
    }

    public Object getProperty(String key) {
        String[] propChain = key.split("\\.");
        Object cur = obj;
        for (String prop : propChain) {
            if (cur instanceof Map) {
                cur = ((Map<?, ?>) cur).get(prop);
            } else {
                logger.error("No such property: {}", key);
                return null;
            }
        }
        return cur;
    }

    public boolean containsProperty(String property) {
        return getProperty(property) != null;
    }

    public List<String> getProperties() {
        List<String> properties = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String prop = entry.getKey();
            properties.add(prop);
            if (entry.getValue() instanceof Map) {
                properties.addAll(getProperties(prop, (Map<String, ?>) entry.getValue()));
            }
        }
        return properties;
    }

    private List<String> getProperties(String baseProp, Map<String, ?> map) {
        List<String> props = new ArrayList<String>();
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
     * Re-reads and returns the contents of the plugin's configuration file.
     *
     * @param pluginClazz Concrete implementation of a TAK Server plugin, {@link PluginBase}.
     * @return Refreshed plugin configuration that will reflect any changes made to the configuration file.
     * @since 4.9
     */
    public PluginConfiguration reloadPluginConfiguration(Class<?> pluginClazz) {
        return new PluginConfiguration(pluginClazz);
    }

    public <T> T parseConfigToObject(Class<T> objectType) throws DatabindException, IOException, StreamReadException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return parseConfigToObject(objectType, mapper);
    }

    public <T> T parseConfigToObject(Class<T> objectType, ObjectMapper customMapper) throws DatabindException, IOException, StreamReadException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper.readValue(sourceFile, objectType);
    }
}
