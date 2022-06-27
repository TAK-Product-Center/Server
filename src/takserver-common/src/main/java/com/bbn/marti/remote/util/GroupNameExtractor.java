package com.bbn.marti.remote.util;

/**
 * Created on 3/29/2018.
 *
 * Use a regex to extract the group name from the DN.
 *
 */
public class GroupNameExtractor extends CommonNameExtractor {

    public GroupNameExtractor() {
        super("CN=(.*?)(?:,|$)");
    }

    public GroupNameExtractor(String regex) {
        super(regex);
    }

    public String extractGroupName(String groupDN) {
        return extractCommonName(groupDN);
    }
}