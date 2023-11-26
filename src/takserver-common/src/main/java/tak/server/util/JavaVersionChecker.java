package tak.server.util;

import org.slf4j.Logger;

import javax.annotation.Nullable;

public class JavaVersionChecker {

    @Nullable
    private static String innerCheck() {
        int targetFeatureVersion = 17;
        int runtimeFeatureVersion = Runtime.version().feature();

        if (runtimeFeatureVersion < targetFeatureVersion) {
            return "Unsupported java version detected! TAK Server requires Java version " +
                    targetFeatureVersion + " but Java version " + runtimeFeatureVersion + " detected!";
        }
        return null;
    }

    public static void check() {
        String msg = innerCheck();
        if (msg != null) {
            System.err.println(msg);
        }
    }

    public static void check(Logger logger) {
        String msg = innerCheck();
        if (msg != null) {
            logger.error(msg);
        }
    }
}
