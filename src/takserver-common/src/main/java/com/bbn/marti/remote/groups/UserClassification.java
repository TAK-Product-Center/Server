package com.bbn.marti.remote.groups;

import java.util.List;

public class UserClassification {

    private String country;
    private List<String> classifications;
    private List<String> accms;
    private List<String> sciControls;

    public UserClassification(String country, List<String> classifications, List<String> accms, List<String> sciControls) {
        this.country = country;
        this.classifications = classifications;
        this.accms = accms;
        this.sciControls = sciControls;
    }

    public String getCountry() {
        return country;
    }

    public List<String> getClassifications() {
        return classifications;
    }

    public List<String> getAccms() {
        return accms;
    }

    public List<String> getSciControls() {
        return sciControls;
    }
}
