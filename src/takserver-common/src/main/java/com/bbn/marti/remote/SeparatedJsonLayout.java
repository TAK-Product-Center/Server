package com.bbn.marti.remote;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;


public class SeparatedJsonLayout extends JsonLayout {

    private boolean doubleSpaced = false;

    @Override
    public String doLayout(ILoggingEvent event) {
        // Perform the default JSON layout
        String jsonLayout = super.doLayout(event);

        // Conditionally append an extra line after each log
        jsonLayout = super.isAppendLineSeparator() && doubleSpaced ? jsonLayout + System.lineSeparator() : jsonLayout;

        return jsonLayout;
    }

    public boolean isDoubleSpaced() {
        return doubleSpaced;
    }

    public void setDoubleSpaced(boolean doubleSpaced) {
        this.doubleSpaced = doubleSpaced;
    }
}
