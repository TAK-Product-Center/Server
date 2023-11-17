package tak.server.plugins;

/**
 * Plugin CoreConfigApi
 * <p>
 * This is intended to have reduced functionality as exposing the entire CoreConfig to all plugins is
 * unnecessary
 */
public interface PluginCoreConfigApi {

	public class Tls {
		private final String keystore;
		private final String keystoreFile;
		private final String keystorePass;
		private final String truststore;
		private final String truststoreFile;
		private final String truststorePass;
		private final String context;
		private final String keymanager;

		public Tls(String keystore, String keystoreFile, String keystorePass,
		           String truststore, String truststoreFile, String truststorePass,
		           String context, String keymanager) {
			this.keystore = keystore;
			this.keystoreFile = keystoreFile;
			this.keystorePass = keystorePass;
			this.truststore = truststore;
			this.truststoreFile = truststoreFile;
			this.truststorePass = truststorePass;
			this.context = context;
			this.keymanager = keymanager;
		}

		public String getKeystore() {
			return keystore;
		}

		public String getKeystoreFile() {
			return keystoreFile;
		}

		public String getKeystorePass() {
			return keystorePass;
		}

		public String getTruststore() {
			return truststore;
		}

		public String getTruststoreFile() {
			return truststoreFile;
		}

		public String getTruststorePass() {
			return truststorePass;
		}

		public String getContext() {
			return context;
		}

		public String getKeymanager() {
			return keymanager;
		}
	}

	public class Security {
		private final Tls tls;

		public Security(Tls tls) {
			this.tls = tls;
		}

		public Tls getTls() {
			return tls;
		}
	}

	public Security getSecurity() throws Exception;
}
