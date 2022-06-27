

package com.bbn.marti.logging;

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
public class AuditLogMarkerThresholdFilter extends Filter<ILoggingEvent> {

    Level level;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (event.getMarker() != null && event.getMarker().equals(MarkerFactory.getMarker(Constants.AUDIT_LOG_MARKER))) {
            return FilterReply.NEUTRAL;
        }

        if (event.getLevel().isGreaterOrEqual(level)) {
            return FilterReply.NEUTRAL;
        } else {
            return FilterReply.DENY;
        }
    }

    public void setLevel(String level) {
        this.level = Level.toLevel(level);
    }

    public void start() {
        if (this.level != null) {
            super.start();
        }
    }
}
