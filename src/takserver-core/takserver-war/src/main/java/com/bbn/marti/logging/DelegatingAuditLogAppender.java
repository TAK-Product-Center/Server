

package com.bbn.marti.logging;

import ch.qos.logback.core.AppenderBase;

public class DelegatingAuditLogAppender<T> extends AppenderBase<T> {

//    private static final Logger destinationLogger = LoggerFactory.getLogger(AuditLogUtil.AUDIT_LOG_NAME);

    @Override
    protected void append(T t) {

        // send this log message to the audit log, to be logged along with the current username and roles for this thread
        AuditLogUtil.auditLog(t.toString());
        //        destinationLogger.debug("message from oorg " + t);
    }
}
