package com.bbn.marti.remote;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.Map;

public class AuditLogJsonLayout extends JsonLayout {

    public AuditLogJsonLayout() {
        this.setIncludeMDC(false);
    }

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        map.putAll(event.getMDCPropertyMap());
        super.addCustomDataToJsonMap(map, event);
    }
}
