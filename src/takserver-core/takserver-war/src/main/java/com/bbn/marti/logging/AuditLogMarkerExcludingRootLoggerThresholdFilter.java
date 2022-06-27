

package com.bbn.marti.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import tak.server.Constants;

/*
 * 
 * Custom Logback marker filter class, to filter audit log messages.
 * 
 */
public class AuditLogMarkerExcludingRootLoggerThresholdFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (event.getMarker() != null && event.getMarker().equals(MarkerFactory.getMarker(Constants.AUDIT_LOG_MARKER))) {

            Level rootLogLevel = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).getLevel();
            
//            System.out.println("root level: " + rootLogLevel + " event level: " + event.getLevel());

            // only let messages from the audit log into the application log if the log level is higher
            if (!rootLogLevel.isGreaterOrEqual(event.getLevel())) {
//                System.out.println("NEUTRAL");
                return FilterReply.NEUTRAL;
            } else {
//                System.out.println("DENY");
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }

    public void start() {
        super.start();
    }
}
