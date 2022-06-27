package com.bbn.marti.takcl.connectivity.missions;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

import static okhttp3.internal.platform.Platform.INFO;

/**
 * Duplicate of the standard okhttp HttpLoggingInterceptor which is licensed under the apache license:
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * The purpose is to enable variable body logging so that binary data doesn't flood the logs
 */
public final class CustomHttpLoggingInterceptor implements Interceptor {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	public enum Level {
		/**
		 * No logs.
		 */
		NONE,
		/**
		 * Logs request and response lines.
		 *
		 * <p>Example:
		 * <pre>{@code
		 * --> POST /greeting http/1.1 (3-byte body)
		 *
		 * <-- 200 OK (22ms, 6-byte body)
		 * }</pre>
		 */
		BASIC,
		/**
		 * Logs request and response lines and their respective headers.
		 *
		 * <p>Example:
		 * <pre>{@code
		 * --> POST /greeting http/1.1
		 * Host: example.com
		 * Content-Type: plain/text
		 * Content-Length: 3
		 * --> END POST
		 *
		 * <-- 200 OK (22ms)
		 * Content-Type: plain/text
		 * Content-Length: 6
		 * <-- END HTTP
		 * }</pre>
		 */
		HEADERS,
		/**
		 * Logs request and response lines and their respective headers and bodies (if present).
		 *
		 * <p>Example:
		 * <pre>{@code
		 * --> POST /greeting http/1.1
		 * Host: example.com
		 * Content-Type: plain/text
		 * Content-Length: 3
		 *
		 * Hi?
		 * --> END POST
		 *
		 * <-- 200 OK (22ms)
		 * Content-Type: plain/text
		 * Content-Length: 6
		 *
		 * Hello!
		 * <-- END HTTP
		 * }</pre>
		 */
		BODY
	}

	public interface Logger {
		void log(String message);

		/**
		 * A {@link Logger} defaults output appropriate for the current platform.
		 */
		Logger DEFAULT = message -> Platform.get().log(INFO, message, null);
	}

	public CustomHttpLoggingInterceptor() {
		this(Logger.DEFAULT);
	}

	public CustomHttpLoggingInterceptor(Logger logger) {
		this.logger = logger;
	}

	private final Logger logger;

	private volatile Set<String> headersToRedact = Collections.emptySet();

	public void redactHeader(String name) {
		Set<String> newHeadersToRedact = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		newHeadersToRedact.addAll(headersToRedact);
		newHeadersToRedact.add(name);
		headersToRedact = newHeadersToRedact;
	}

	private volatile Level level = Level.NONE;

	/**
	 * Change the level at which this interceptor logs.
	 */
	public CustomHttpLoggingInterceptor setLevel(Level level) {
		if (level == null) throw new NullPointerException("level == null. Use Level.NONE instead.");
		this.level = level;
		return this;
	}

	public Level getLevel() {
		return level;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Level level = this.level;

		Request request = chain.request();
		if (level == Level.NONE) {
			return chain.proceed(request);
		}


		boolean logBody = level == Level.BODY;

		boolean logHeaders = logBody || level == Level.HEADERS;

		RequestBody requestBody = request.body();
		boolean hasRequestBody = requestBody != null;

		Connection connection = chain.connection();
		String requestStartMessage = "--> "
				+ request.method()
				+ ' ' + request.url()
				+ (connection != null ? " " + connection.protocol() : "");
		if (!logHeaders && hasRequestBody) {
			requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
		}
		logger.log(requestStartMessage);

		if (logHeaders) {
			if (hasRequestBody) {
				// Request body headers are only present when installed as a network interceptor. Force
				// them to be included (when available) so there values are known.
				if (requestBody.contentType() != null) {
					logger.log("Content-Type: " + requestBody.contentType());
				}
				if (requestBody.contentLength() != -1) {
					logger.log("Content-Length: " + requestBody.contentLength());
				}
			}

			Headers headers = request.headers();
			for (int i = 0, count = headers.size(); i < count; i++) {
				String name = headers.name(i);
				// Skip headers from the request body as they are explicitly logged above.
				if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
					logHeader(headers, i);
				}
			}

			if (!logBody || !hasRequestBody) {
				logger.log("--> END " + request.method());
			} else if (bodyHasUnknownEncoding(request.headers())) {
				logger.log("--> END " + request.method() + " (encoded body omitted)");
			} else if (requestBody.isDuplex()) {
				logger.log("--> END " + request.method() + " (duplex request body omitted)");
			} else {
				Buffer buffer = new Buffer();
				requestBody.writeTo(buffer);

				Charset charset = UTF8;
				MediaType contentType = requestBody.contentType();
				if (contentType != null) {
					charset = contentType.charset(UTF8);
				}

				logger.log("");
				if (isPlaintext(buffer, charset)) {
					logger.log(buffer.readString(charset));
					logger.log("--> END " + request.method()
							+ " (" + requestBody.contentLength() + "-byte body)");
				} else {
					logger.log("--> END " + request.method() + " (binary "
							+ requestBody.contentLength() + "-byte body omitted)");
				}
			}
		}

		long startNs = System.nanoTime();
		Response response;
		try {
			response = chain.proceed(request);
		} catch (Exception e) {
			logger.log("<-- HTTP FAILED: " + e);
			throw e;
		}
		long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

		ResponseBody responseBody = response.body();
		long contentLength = responseBody.contentLength();
		String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
		logger.log("<-- "
				+ response.code()
				+ (response.message().isEmpty() ? "" : ' ' + response.message())
				+ ' ' + response.request().url()
				+ " (" + tookMs + "ms" + (!logHeaders ? ", " + bodySize + " body" : "") + ')');

		if (logHeaders) {
			Headers headers = response.headers();
			for (int i = 0, count = headers.size(); i < count; i++) {
				logHeader(headers, i);
			}

			if (!logBody || !HttpHeaders.hasBody(response)) {
				logger.log("<-- END HTTP");
			} else if (bodyHasUnknownEncoding(response.headers())) {
				logger.log("<-- END HTTP (encoded body omitted)");
			} else {
				BufferedSource source = responseBody.source();
				source.request(Long.MAX_VALUE); // Buffer the entire body.
				Buffer buffer = source.getBuffer();

				Long gzippedLength = null;
				if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
					gzippedLength = buffer.size();
					try (GzipSource gzippedResponseBody = new GzipSource(buffer.clone())) {
						buffer = new Buffer();
						buffer.writeAll(gzippedResponseBody);
					}
				}

				Charset charset = UTF8;
				MediaType contentType = responseBody.contentType();
				if (contentType != null) {
					charset = contentType.charset(UTF8);
				}

				if (!isPlaintext(buffer, charset)) {
					logger.log("");
					logger.log("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
					return response;
				}

				if (contentLength != 0) {
					logger.log("");
					logger.log(buffer.clone().readString(charset));
				}

				if (gzippedLength != null) {
					logger.log("<-- END HTTP (" + buffer.size() + "-byte, "
							+ gzippedLength + "-gzipped-byte body)");
				} else {
					logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
				}
			}
		}

		return response;
	}

	private void logHeader(Headers headers, int i) {
		String value = headersToRedact.contains(headers.name(i)) ? "██" : headers.value(i);
		logger.log(headers.name(i) + ": " + value);
	}

	/**
	 * Returns true if the body in question probably contains human readable text. Uses a small sample
	 * of code points to detect unicode control characters commonly used in binary file signatures.
	 */
	static boolean isPlaintext(Buffer buffer, Charset charset) {
		try {
			Buffer prefix = new Buffer();
			long byteCount = buffer.size() < 64 ? buffer.size() : 64;
			buffer.copyTo(prefix, 0, byteCount);

			boolean isNotByteArray = true;

			try {
				String value = prefix.readString(charset);

				int valueLength = value.length();

				if (value.charAt(0) == '[') {
					char lastChar = value.charAt(value.length() - 1);
					int startIdx = 1;
					int endIdx = (lastChar == ']' || lastChar == ',' || lastChar == '-') ? valueLength - 1 : valueLength;

					String[] values = value.substring(startIdx, endIdx).split(",");
					for (String component : values) {
						try {
							Integer.parseInt(component);
						} catch (Exception e) {
							break;
						}
					}
					isNotByteArray = false;
				}
			} catch (Exception e) {
				// Pass
			}

			for (int i = 0; i < 16; i++) {
				if (prefix.exhausted()) {
					break;
				}
				int codePoint = prefix.readUtf8CodePoint();
				if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
					return false;
				}
			}


			return isNotByteArray;
		} catch (EOFException e) {
			return false; // Truncated UTF-8 sequence.
		}
	}

	private static boolean bodyHasUnknownEncoding(Headers headers) {
		String contentEncoding = headers.get("Content-Encoding");
		return contentEncoding != null
				&& !contentEncoding.equalsIgnoreCase("identity")
				&& !contentEncoding.equalsIgnoreCase("gzip");
	}
}
