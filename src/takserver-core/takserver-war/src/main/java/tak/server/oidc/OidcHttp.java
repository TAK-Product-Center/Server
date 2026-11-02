package tak.server.oidc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

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
public final class OidcHttp {

    private static final Logger logger = LoggerFactory.getLogger(OidcHttp.class);

    private static final long TIMEOUT_SECONDS = 10;
    private static final long MAX_TOKEN_RESPONSE_BYTES = 64 * 1024;

    private OidcHttp() {
    }

    /** One HTTP response, body bounded, never logged by this class. */
    public static final class FormResponse {
        public final int status;
        public final String body;

        FormResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /**
     * POSTs a form to the token endpoint through the provider's own OkHttp client. Spring's {@code RestTemplate}
     * logs request bodies at DEBUG, which would put the authorization code, the client secret and the PKCE
     * verifier in the server log (T8). OkHttp logs nothing unless an interceptor is added, and none is.
     */
    public static FormResponse postForm(OkHttpClient client, String url, Map<String, String> fields) throws IOException {
        FormBody.Builder form = new FormBody.Builder();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (e.getValue() != null) {
                form.add(e.getKey(), e.getValue());
            }
        }
        Request request = new Request.Builder().url(url).post(form.build())
                .header("Accept", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                return new FormResponse(response.code(), "");
            }
            // Bound the body before materialising it: read at most the limit plus one byte.
            okio.BufferedSource source = body.source();
            if (source.request(MAX_TOKEN_RESPONSE_BYTES + 1)) {
                throw new IOException("token response larger than " + MAX_TOKEN_RESPONSE_BYTES + " bytes");
            }
            byte[] bytes = source.readByteArray();
            return new FormResponse(response.code(), new String(bytes, StandardCharsets.UTF_8));
        }
    }

    static RestTemplate restTemplate(OidcProviderConfig p) {
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(okHttpClient(p)));
    }

    static OkHttpClient okHttpClient(OidcProviderConfig p) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(3 * TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
        return builder.build();
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
