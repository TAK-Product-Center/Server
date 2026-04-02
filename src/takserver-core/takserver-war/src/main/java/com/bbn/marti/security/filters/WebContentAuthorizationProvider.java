package com.bbn.marti.security.filters;

import com.bbn.marti.config.WebContent;
import com.bbn.marti.remote.config.CoreConfigFacade;

public class WebContentAuthorizationProvider {
    public synchronized String getContentFilters() {
        final WebContent content = CoreConfigFacade.getInstance().getRemoteConfiguration().getWebContent();

        if (content == null) {
            return "";
        }

        StringBuilder contentFilterPatterns = new StringBuilder();
        String pattern = "";
        for (var filter : content.getAccessFilter()) {
            if (contentFilterPatterns.isEmpty()) {
                pattern = String.format("%s/* ", filter.getFolder());
            } else {
                pattern = String.format(", %s/*", filter.getFolder());
            }
            contentFilterPatterns.append(pattern);
        }
        return contentFilterPatterns.toString();
    }
}
