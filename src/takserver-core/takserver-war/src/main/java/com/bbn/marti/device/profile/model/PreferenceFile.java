package com.bbn.marti.device.profile.model;

import java.util.HashMap;
import java.util.Map;


public class PreferenceFile extends ProfileFile {

    private String filename;
    private HashMap<String, String> preferences = new HashMap<>();

    public PreferenceFile(String filename) {
        this.filename = filename;
    }

    public void addPreference(String key, String value) {
        preferences.put(key, value);
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public byte[] getData() {

        String prefs =
                "<?xml version='1.0' standalone='yes'?>" +
                    "<preferences>" +
                        "<preference version=\"1\" name=\"com.atakmap.app.civ_preferences\">";

        for (Map.Entry<String, String> preference : preferences.entrySet()) {
            prefs += "<entry key=\"" + preference.getKey() + "\" class=\"class java.lang.String\">"
                    + preference.getValue() + "</entry>";
        }

        prefs += "</preference></preferences>";
        return prefs.getBytes();
    }
}
