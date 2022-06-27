package com.bbn.marti.remote.util;

import java.security.cert.X509Certificate;

/*
 * 
 * Use a regex to extract the username from the DN. 
 * 
 */
public class X509UsernameExtractor extends CommonNameExtractor {

    public X509UsernameExtractor() {
        super("CN=(.*?)(?:,|$)");
    }
    
    public X509UsernameExtractor(String regex) {
        super(regex);
    }

    public String extractUsername(X509Certificate clientCert) {
        String subjectDN = clientCert.getSubjectDN().getName();
        return extractCommonName(subjectDN);
    }
}