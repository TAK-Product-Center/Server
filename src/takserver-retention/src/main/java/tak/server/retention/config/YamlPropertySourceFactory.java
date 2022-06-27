package tak.server.retention.config;


import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

public class YamlPropertySourceFactory implements PropertySourceFactory {


    private static final Logger logger = LoggerFactory.getLogger(YamlPropertySourceFactory.class);

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource) throws IOException {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());
        factory.afterPropertiesSet();
        Properties loadedProperties = factory.getObject();
        logger.info("LOADING YAML PROPERTIES Name " + name + " loadedProperties = " + loadedProperties);
        return new PropertiesPropertySource((StringUtils.isNotBlank(name)) ? name : encodedResource.getResource().getFilename(), loadedProperties);
    }

}
