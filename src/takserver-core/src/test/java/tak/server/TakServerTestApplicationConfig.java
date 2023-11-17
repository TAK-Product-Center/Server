package tak.server;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.bbn.marti.config.Input;
import com.bbn.marti.remote.ServerInfo;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.ConcurrentMultiHashMap;
import com.bbn.marti.service.SubscriptionStore;
import com.bbn.marti.sync.api.PropertiesApi;
import com.bbn.marti.sync.service.PropertiesService;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;
import com.bbn.marti.util.spring.SpringContextBeanForApi;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import tak.server.ignite.IgniteConfigurationHolder;
import tak.server.messaging.MessageConverter;

@Configuration
public class TakServerTestApplicationConfig {

	public static final String JARG_KEY_IGNITE_TEST_LOG_FILE = "tak.server.test.logging.targetPath";

	@Bean
	Marshaller jaxbMarshaller() {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();

		marshaller.setClassesToBeBound(Input.class);

		return marshaller;
	}

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);

		return mapper;
	}

	@Bean
	MessageConverter clusterMessageConverter() {
		return new MessageConverter();
	}

	@MockBean
	GroupManager groupManager;

	@Bean
	SpringContextBeanForApi springContextBean() {
		return new SpringContextBeanForApi();
	}

	@Bean
	MessagingDependencyInjectionProxy coreSpringContextBean() {
		return new MessagingDependencyInjectionProxy();
	}
	
	@Bean
	MessageConverter converter() {
		return new MessageConverter();
	}

	@Bean
	ServerInfo serverInfo() {
		return new ServerInfo() {
			
			private String id = UUID.randomUUID().toString().replace("-", "");

			@Override
			public String getServerId() {
				return id;
			}

			@Override
			public String getSubmissionTopic() {
				return Constants.SUBMISSION_TOPIC_BASE + getServerId();
			}

			@Override
			public String getTakMessageTopic() {
				return Constants.TAK_MESSAGE_TOPIC_BASE + getServerId();
			}
			
			@Override
			public String getNatsURL() {
				return "";
			}

			@Override
			public String getNatsClusterId() {
				return "";
			}

			@Override
			public boolean isCluster() {
				return false;
			}
		};
	}
	
	@Bean 
	Ignite ignite() {
		IgniteConfigurationHolder.getInstance().setConfiguration(IgniteConfigurationHolder.getInstance().getIgniteConfiguration(Constants.MESSAGING_PROFILE_NAME, "127.0.0.1", false, false, true, false, 47500, 100, 47100, 100, 0, 600000, 524288000, 524288000));
		IgniteConfiguration igniteConfiguration = IgniteConfigurationHolder.getInstance().getConfiguration();
		String logPath = System.getProperty(JARG_KEY_IGNITE_TEST_LOG_FILE, null);

		if (logPath != null && new File(logPath).isDirectory()) {
			logPath = Paths.get(logPath).resolve(Constants.MESSAGING_PROFILE_NAME + "-ignite.log").toAbsolutePath().toString();
			LoggerFactory.getLogger(TakServerTestApplicationConfig.class).warn("Redirecting ignite logs to '" + logPath + "'.");

			Logger logger = (Logger) LoggerFactory.getLogger("org.apache.ignite");
			LoggerContext lc = logger.getLoggerContext();

			FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
			fileAppender.setPrudent(true);
			fileAppender.setFile(logPath);
			fileAppender.setContext(lc);

			PatternLayout pl = new PatternLayout();
			pl.setPattern("%d{yyyy-MM-dd-HH:mm:ss.SSS} [%thread] %logger{36} - %msg%n");
			pl.setContext(lc);
			pl.start();

			fileAppender.setLayout(pl);
			fileAppender.start();

			logger.setAdditive(false);
			logger.addAppender(fileAppender);

			igniteConfiguration.setGridLogger(new Slf4jLogger(logger));
		}

		return Ignition.getOrStart(igniteConfiguration);
	}
	
	@Bean 
	SubscriptionStore subscriptionStore() {		
		return new SubscriptionStore();
	}
	
	@Bean
	public PropertiesService propertiesService() {
		return new PropertiesService() {

			@Override
			public List<String> findAllUids() {
				List<String> uids = Arrays.asList("1234","2358");
				return uids;
			}

			@Override
			public Map<String, Collection<String>> getKeyValuesByUid(String uid) {
				if(uid == "1234") {
					Multimap<String, String> uidKvMap = new ConcurrentMultiHashMap<String, String>();
					uidKvMap.put("Key1", "value1");
					uidKvMap.put("Key1", "value2");
					uidKvMap.put("Key2", "value0");
					uidKvMap.put("Key2", "value1");
					return uidKvMap.asMap();
				} else {
					return null;
				}
			}

			@Override
			public List<String> getValuesByKeyAndUid(String uid, String key) {
				if(uid == "1234" && key == "Key1") {
					List<String> values = new ArrayList<String>();
					values.add("value1");
					values.add("value2");
					return values;
				} else {
					return null;
				}
			}

			@Override
			public void putKeyValue(String uid, String key, String value) {
				
			}

			@Override
			public void deleteKey(String uid, String key) {
				
			}

			@Override
			public void deleteAllKeysByUid(String uid) {
				
			}
			
		};
	}
	
	@Bean 
	PropertiesApi propertiesApi() {		
		return new PropertiesApi();
	}
	
}
