package tak.server.config;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bbn.marti.sync.EnterpriseSyncService;
import com.bbn.marti.sync.JDBCEnterpriseSyncService;

import tak.server.Constants;

/*
 * services that are only used in separate messaging process
 */
@Configuration
@EnableAutoConfiguration(exclude = {WebMvcAutoConfiguration.class, WebSocketMessagingAutoConfiguration.class, WebSocketServletAutoConfiguration.class, SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@Profile({Constants.MESSAGING_PROFILE_NAME})
public class MessagingOnlyConfiguration {

	@Bean
	public EnterpriseSyncService enterpriseSyncService() {
		return new JDBCEnterpriseSyncService();
	}

	@Bean
	public TomcatServletWebServerFactory noOpContainerFactory() throws RemoteException {

		return new TomcatServletWebServerFactory() {
			@Override
			public WebServer getWebServer(ServletContextInitializer... initializers) {

				return new WebServer() {
					@Override
					public void start() throws WebServerException { }

					@Override
					public void stop() throws WebServerException { }

					@Override
					public int getPort() {
						return -1;
					}
				};
			}
		};
	}

	@Bean
	ServletContext noOpServletContext() {
		return new ServletContext() {

			@Override
			public String getContextPath() {
				return null;
			}

			@Override
			public ServletContext getContext(String uripath) {
				return null;
			}

			@Override
			public int getMajorVersion() {
				return 0;
			}

			@Override
			public int getMinorVersion() {
				return 0;
			}

			@Override
			public int getEffectiveMajorVersion() {
				return 0;
			}

			@Override
			public int getEffectiveMinorVersion() {
				return 0;
			}

			@Override
			public String getMimeType(String file) {
				return null;
			}

			@Override
			public Set<String> getResourcePaths(String path) {
				return null;
			}

			@Override
			public URL getResource(String path) throws MalformedURLException {
				return null;
			}

			@Override
			public InputStream getResourceAsStream(String path) {
				return null;
			}

			@Override
			public RequestDispatcher getRequestDispatcher(String path) {
				return null;
			}

			@Override
			public RequestDispatcher getNamedDispatcher(String name) {
				return null;
			}

			@Override
			public Servlet getServlet(String name) throws ServletException {
				return null;
			}

			@Override
			public Enumeration<Servlet> getServlets() {
				return null;
			}

			@Override
			public Enumeration<String> getServletNames() {
				return null;
			}

			@Override
			public void log(String msg) { }

			@Override
			public void log(Exception exception, String msg) { }

			@Override
			public void log(String message, Throwable throwable) { }

			@Override
			public String getRealPath(String path) {
				return null;
			}

			@Override
			public String getServerInfo() {
				return null;
			}

			@Override
			public String getInitParameter(String name) {
				return null;
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				return null;
			}

			@Override
			public boolean setInitParameter(String name, String value) {
				return false;
			}

			@Override
			public Object getAttribute(String name) {
				return null;
			}

			@Override
			public Enumeration<String> getAttributeNames() {
				return null;
			}

			@Override
			public void setAttribute(String name, Object object) { }

			@Override
			public void removeAttribute(String name) { }

			@Override
			public String getServletContextName() {
				return null;
			}

			@Override
			public Dynamic addServlet(String servletName, String className) {
				return null;
			}

			@Override
			public Dynamic addServlet(String servletName, Servlet servlet) {
				return null;
			}

			@Override
			public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
				return null;
			}

			@Override
			public Dynamic addJspFile(String jspName, String jspFile) {
				return null;
			}

			@Override
			public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
				return null;
			}

			@Override
			public ServletRegistration getServletRegistration(String servletName) {
				return null;
			}

			@Override
			public Map<String, ? extends ServletRegistration> getServletRegistrations() {
				return null;
			}

			@Override
			public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
				return null;
			}

			@Override
			public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
				return null;
			}

			@Override
			public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {

				return null;
			}

			@Override
			public <T extends Filter> T createFilter(Class<T> c) throws ServletException {

				return null;
			}

			@Override
			public FilterRegistration getFilterRegistration(String filterName) {

				return null;
			}

			@Override
			public Map<String, ? extends FilterRegistration> getFilterRegistrations() {

				return null;
			}

			@Override
			public SessionCookieConfig getSessionCookieConfig() {

				return null;
			}

			@Override
			public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {


			}

			@Override
			public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {

				return null;
			}

			@Override
			public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {

				return null;
			}

			@Override
			public void addListener(String className) {


			}

			@Override
			public <T extends EventListener> void addListener(T t) {


			}

			@Override
			public void addListener(Class<? extends EventListener> listenerClass) {


			}

			@Override
			public <T extends EventListener> T createListener(Class<T> c) throws ServletException {

				return null;
			}

			@Override
			public JspConfigDescriptor getJspConfigDescriptor() {

				return null;
			}

			@Override
			public ClassLoader getClassLoader() {

				return null;
			}

			@Override
			public void declareRoles(String... roleNames) {


			}

			@Override
			public String getVirtualServerName() {

				return null;
			}

			@Override
			public int getSessionTimeout() {

				return 0;
			}

			@Override
			public void setSessionTimeout(int sessionTimeout) {


			}

			@Override
			public String getRequestCharacterEncoding() {

				return null;
			}

			@Override
			public void setRequestCharacterEncoding(String encoding) {


			}

			@Override
			public String getResponseCharacterEncoding() {

				return null;
			}

			@Override
			public void setResponseCharacterEncoding(String encoding) {


			}

		};
	}

	@Bean
	public AsyncTaskExecutor asyncExecutor() {

		int numProc = Runtime.getRuntime().availableProcessors();

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(numProc * 3);
		executor.setMaxPoolSize(numProc * 32);
		executor.setQueueCapacity(1024 * 32);
		executor.setThreadNamePrefix("takserver-messaging-async-executor-");
		executor.initialize();

		return executor;

	}
}
