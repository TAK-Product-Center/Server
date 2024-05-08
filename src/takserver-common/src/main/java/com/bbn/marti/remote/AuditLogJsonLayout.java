package com.bbn.marti.remote;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

public class AuditLogJsonLayout extends SeparatedJsonLayout {

    public AuditLogJsonLayout() {
        super();
        this.setIncludeMDC(false);
    }

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        map.putAll(event.getMDCPropertyMap());
        super.addCustomDataToJsonMap(map, event);
    }
}
