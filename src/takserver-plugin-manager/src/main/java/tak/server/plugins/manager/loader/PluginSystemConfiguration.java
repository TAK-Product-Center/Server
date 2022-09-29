package tak.server.plugins.manager.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class PluginSystemConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PluginSystemConfiguration.class);
    
    private Map<String, Object> obj;
    
    static final String PLUGIN_CONFIG_BASE = "conf/plugins/";
    
    static final String[] RESERVED_KEYWORDS = { "server", "tak", "system" };
    public static final String ARCHIVE_ENABLED_PROPERTY = "system.archive";
    public static final String PLUGIN_ENABLED_PROPERTY = "system.enable";

    public PluginSystemConfiguration() {
        obj = new HashMap<String, Object>();
    }
    
    public PluginSystemConfiguration(Class clazz) {
        String configFileName = PLUGIN_CONFIG_BASE + clazz.getName() + ".yaml";
        File f = new File(configFileName);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try {
                f.createNewFile();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        try (InputStream inputStream = new FileInputStream(f);){    
            Yaml yaml = new Yaml();
            obj = yaml.load(inputStream);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        if (obj == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("The config was empty. Create empty properties map.");
            }
            obj = new HashMap<String, Object>();
        } 
    }
    
    public Object getProperty(String key) {
        List<String> propChain = Arrays.asList(key.split("\\."));
        Object cur = obj;
        for (String prop : propChain) {
            if (cur instanceof Map) {
                cur = ((Map) cur).get(prop);
            } else {
                logger.error("no such property: " + key);
                return null;
            }
        }
        return cur;
    }
    
    public boolean containsProperty(String property) {
        String[] propChain = property.split("\\.");
        Object cur = obj;
        for (String prop : propChain) {
            if (cur instanceof Map) {
                cur = ((Map) cur).get(prop);
            } else {
                return false;
            }
        }
        if (cur != null) {
            return true;
        } else {
            return false;
        }
    }
    
    public List<String> getProperties() {
        List<String> properties = new ArrayList<String>();
        for (String prop : obj.keySet()) {
            properties.add(prop);
            if (obj.get(prop) instanceof Map) {
                properties.addAll(this.getProperties(prop, (Map<String, ?>)obj.get(prop)));
            }
        }
        return properties;
    }
    
    private List<String> getProperties(String baseProp, Map<String, ?> map) {
        List<String> props = new ArrayList<String>();
        for (String prop : map.keySet()) {
            String subProp = baseProp + "." + prop;
            props.add(subProp);
            if (map.get(prop) instanceof Map) {
                props.addAll(this.getProperties(subProp, (Map<String, ?>) map.get(prop)));
            }
        }
        return props;
    }
    
    public void setProperty(String key, Object value) throws Exception{
    	
		logger.info("Calling setProperty: key {}, value {}", key, value);

        List<String> propChain = Arrays.asList(key.split("\\."));
        Object cur = obj;
        for (int i=0 ; i < propChain.size()-1 ; i++) {
        	String prop = propChain.get(i);
            if (cur instanceof Map) {
            	if (((Map) cur).containsKey(prop)) {
                    cur = ((Map) cur).get(prop);
            	}else {
                    ((Map) cur).put(prop, new HashMap<>());
                    cur = ((Map) cur).get(prop);
            	}
            } else {
                throw new Exception("Key creates conflicts with existing configuration");
            }
        }
        String lastKey = propChain.get(propChain.size()-1);
        if (cur instanceof Map) {
        	((Map) cur).put(lastKey,value);
        }else {
            throw new Exception("Key creates conflicts with existing configuration");
        }
    }

    public void save(Class clazz) throws IOException {
    	
    	synchronized (this) {
    		String configFileName = PLUGIN_CONFIG_BASE + clazz.getName() + ".yaml";
    		
    		logger.info("Saving plugin configuration to {}", configFileName);

            File f = new File(configFileName);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
            if (obj == null) {
            	 if (logger.isDebugEnabled()) {
                     logger.debug("Nothing to save in plugin configuration");
                 }
            	return;
            }
            
            try(Writer writer = new FileWriter(f);){
            	Yaml yaml = new Yaml();
                yaml.dump(obj, writer);
            }
		}
            	
    }
   
}
