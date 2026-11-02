import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Container healthcheck for the distroless TAK Server images.
 *
 * The runtime image has no shell, no curl and no netcat on purpose, so the JVM is the only thing that can run a
 * HEALTHCHECK. Usage:
 *
 *   java -cp /opt/takserver/health Health tcp:127.0.0.1:8443 log:/opt/tak/logs/takserver-api.log=Tomcat started
 *
 * Exit 0 when every argument is satisfied, 1 otherwise.
 */
public final class Health {

	private static final int TIMEOUT_MS = 3000;

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("usage: Health tcp:<host>:<port> | log:<file>=<substring>");
			System.exit(2);
		}
		try {
			for (String arg : args) {
				if (arg.startsWith("tcp:")) {
					String hostPort = arg.substring(4);
					int colon = hostPort.lastIndexOf(':');
					try (Socket socket = new Socket()) {
						socket.connect(new InetSocketAddress(hostPort.substring(0, colon),
								Integer.parseInt(hostPort.substring(colon + 1))), TIMEOUT_MS);
					}
				} else if (arg.startsWith("log:")) {
					String spec = arg.substring(4);
					int eq = spec.indexOf('=');
					String file = spec.substring(0, eq);
					String needle = spec.substring(eq + 1);
					if (!contains(file, needle)) {
						System.err.println("unhealthy: " + file + " does not contain: " + needle);
						System.exit(1);
					}
				} else {
					System.err.println("unhealthy: unknown check " + arg);
					System.exit(2);
				}
			}
			System.exit(0);
		} catch (Exception e) {
			System.err.println("unhealthy: " + e);
			System.exit(1);
		}
	}

	private static boolean contains(String file, String needle) throws Exception {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(needle)) {
					return true;
				}
			}
		}
		return false;
	}
}
