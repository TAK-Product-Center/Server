package tak.server.oidc;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import okhttp3.OkHttpClient;

/**
 * The HTTP client TAK Server uses to talk to an OIDC provider (discovery, JWKS, token endpoint).
 *
 * Trust comes from, in order: the provider's own truststore when configured (mirrors {@code ldapsTruststoreFile};
 * this is how a tactical CA gets trusted, T16), else the JVM default trust store. {@code trustAllCerts} is kept
 * because upstream has it, but it disables both certificate and hostname verification and is logged as a WARN
 * with the provider name every time a client is built; the MR proposes removing it.
 *
 * Hostname verification is never disabled except by {@code trustAllCerts}. Redirects are not followed: a
 * discovery or token endpoint that redirects is misconfigured, and following it would let a plaintext hop in.
 */
final class OidcHttp {

    private static final Logger logger = LoggerFactory.getLogger(OidcHttp.class);

    private static final long TIMEOUT_SECONDS = 10;

    private OidcHttp() {
    }

    static RestTemplate restTemplate(OidcProviderConfig p) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false);

        if (p.isTrustAllCerts()) {
            logger.warn("oidc provider '{}': trustAllCerts=true disables TLS certificate and hostname verification "
                    + "for the IdP connection. Do not run this outside a lab. Configure truststoreFile instead.", p.getName());
            X509TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext ctx = sslContext(new TrustManager[] { trustAll });
            builder.sslSocketFactory(ctx.getSocketFactory(), trustAll).hostnameVerifier((h, s) -> true);
        } else if (p.getTruststoreFile() != null) {
            X509TrustManager tm = trustManager(p);
            SSLContext ctx = sslContext(new TrustManager[] { tm });
            builder.sslSocketFactory(ctx.getSocketFactory(), tm);
        }
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(builder.build()));
    }

    private static SSLContext sslContext(TrustManager[] managers) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, managers, new java.security.SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException("unable to build an SSL context for the OIDC provider", e);
        }
    }

    private static X509TrustManager trustManager(OidcProviderConfig p) {
        try {
            KeyStore ks = KeyStore.getInstance(p.getTruststoreType());
            try (InputStream in = new FileInputStream(p.getTruststoreFile())) {
                ks.load(in, p.getTruststorePassword() == null ? null : p.getTruststorePassword().toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    return (X509TrustManager) tm;
                }
            }
            throw new IllegalStateException("no X509TrustManager from truststore");
        } catch (Exception e) {
            throw new IllegalStateException("oidc provider '" + p.getName() + "': cannot load truststore "
                    + p.getTruststoreFile() + ": " + e.getMessage(), e);
        }
    }
}
