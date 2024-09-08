package tak.server.federation;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

public class TokenAuthCredential extends CallCredentials {
	public static final String BEARER_TYPE = "Bearer";

	public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization",
			ASCII_STRING_MARSHALLER);

	private final String token;

	public TokenAuthCredential(String token) {
		this.token = token;
	}

	@Override
	public void applyRequestMetadata(final RequestInfo requestInfo, final Executor executor,
			final MetadataApplier metadataApplier) {

		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Metadata headers = new Metadata();
					headers.put(AUTHORIZATION_METADATA_KEY, String.format("%s %s", BEARER_TYPE, token));
					metadataApplier.apply(headers);
				} catch (Throwable e) {
					metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e));
				}
			}
		});
	}
}