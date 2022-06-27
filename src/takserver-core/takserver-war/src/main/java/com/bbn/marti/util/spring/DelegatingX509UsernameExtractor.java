package com.bbn.marti.util.spring;

import java.security.cert.X509Certificate;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;
import org.springframework.util.Assert;

import com.bbn.marti.remote.util.X509UsernameExtractor;

/*
 * 
 * Use a regex to extract the username from the DN. 
 * 
 */
public class DelegatingX509UsernameExtractor implements X509PrincipalExtractor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    protected X509UsernameExtractor extractor = new X509UsernameExtractor();

    public DelegatingX509UsernameExtractor() {
        setSubjectDnRegex("CN=(.*?)(?:,|$)");
    }

    public Object extractPrincipal(X509Certificate clientCert) {
        
        Objects.requireNonNull(clientCert, "X509 client certificate");
        
        return extractor.extractUsername(clientCert);
    }

    /**
     * Sets the regular expression which will by used to extract the user name from the
     * certificate's Subject DN.
     * <p>
     * It should contain a single group; for example the default expression
     * "CN=(.*?)(?:,|$)" matches the common name field. So "CN=Jimi Hendrix, OU=..." will
     * give a user name of "Jimi Hendrix".
     * <p>
     * The matches are case insensitive. So "emailAddress=(.?)," will match
     * "EMAILADDRESS=jimi@hendrix.org, CN=..." giving a user name "jimi@hendrix.org"
     *
     * @param subjectDnRegex the regular expression to find in the subject
     */
    public void setSubjectDnRegex(String subjectDnRegex) {
        Assert.hasText(subjectDnRegex, "Regular expression may not be null or empty");
        extractor.setDnRegex(subjectDnRegex);
    }

    public void setMessageSource(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }
}