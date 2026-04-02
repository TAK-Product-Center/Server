package com.bbn.marti.takcl.connectivity;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.connectivity.missions.CustomHttpLoggingInterceptor;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.AbstractServerProfile;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tak.server.api.client.SubmissionApiApi;
import tak.server.api.client.invoker.ApiClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TakserverClient {

    private static final Map<AbstractServerProfile, TakserverClient> instanceMap = new HashMap<>();

    private ApiClient apiClient;

    public SubmissionApiApi submissionApi;

    private final Logger logger = LoggerFactory.getLogger(TakserverClient.class);

    private TakserverClient(AbstractServerProfile serverProfile) {
        synchronized (this) {

            try {
                ImmutableUsers admin = ImmutableUsers.valueOf(serverProfile.getAdminuserIdentifier());

                SSLHelper.TakClientSslContext tcsc = new SSLHelper.TakClientSslContext(admin);

                CustomHttpLoggingInterceptor loggingInterceptor = new CustomHttpLoggingInterceptor(logger::info);
                loggingInterceptor.setLevel(CustomHttpLoggingInterceptor.Level.BODY);

                tcsc.init();

                apiClient = new ApiClient();
                OkHttpClient.Builder builder = apiClient.getOkBuilder();

                builder
                    .sslSocketFactory(tcsc.getSslSocketFactory(), tcsc.getTrustManager())
                    .addInterceptor(loggingInterceptor)
                    .callTimeout(100, TimeUnit.SECONDS)
                    .connectTimeout(100, TimeUnit.SECONDS)
                    .readTimeout(100, TimeUnit.SECONDS)
                    .writeTimeout(100, TimeUnit.SECONDS)
                    .hostnameVerifier((s, sslSession) -> {
                        // TODO: Not this
                        return true;
                    });

                submissionApi = apiClient.createService(SubmissionApiApi.class);
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static TakserverClient getInstance(AbstractServerProfile serverProfile) {
        synchronized (instanceMap) {
            if (!instanceMap.containsKey(serverProfile)) {
                TakserverClient client = new TakserverClient(serverProfile);
                instanceMap.put(serverProfile, client);
            }
        }
        return instanceMap.get(serverProfile);
    }

    public synchronized SubmissionApiApi getSubmissionApi() {
        if (submissionApi == null) {
            throw new IllegalStateException("The Takserver ApiClient failed to initialize!");
        }
        return submissionApi;
    }
}
