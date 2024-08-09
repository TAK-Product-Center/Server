package tak.server;

import java.io.CharArrayWriter;

import org.apache.catalina.valves.AbstractAccessLogValve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import com.bbn.marti.config.Logging;
import com.bbn.marti.remote.config.CoreConfigFacade;

@Component
public class CustomizeEmbeddedTomcatContainer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	private static final Logger httpAccessLogger = LoggerFactory.getLogger("http_access_logger");
	private static final Logger logger = LoggerFactory.getLogger(CustomizeEmbeddedTomcatContainer.class);

	@Override
	public void customize(TomcatServletWebServerFactory factory) {

		Logging loggingConf = CoreConfigFacade.getInstance().getRemoteConfiguration().getLogging();

		if (loggingConf != null) {
			logger.info("http access logging enabled: " + loggingConf.isHttpAccessEnabled());
		}


		if (loggingConf != null && loggingConf.isHttpAccessEnabled()) {

			TomcatSlf4jAccessValve accessLogValve = new TomcatSlf4jAccessValve();
			accessLogValve.setEnabled(true);

			/**
			 * for pattern format see https://tomcat.apache.org/tomcat-7.0-doc/api/org/apache/catalina/valves/AccessLogValve.html
			 */
			accessLogValve.setPattern("request: method=%m uri=\"%U\" response: statuscode=%s bytes=%b duration=%D(ms) client: remoteip=%a user=%u useragent=\"%{User-Agent}i\"");

			factory.addContextValves(accessLogValve);
		}
	}

	public static class TomcatSlf4jAccessValve extends AbstractAccessLogValve {

		@Override
		protected void log(CharArrayWriter message) {
			httpAccessLogger.info(message.toString());
		}

	}
}