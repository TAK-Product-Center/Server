package com.bbn.marti.remote.util;

import com.bbn.marti.remote.exception.TakException;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 3/29/2018.
 *
 * Use a regex to extract the common name from a distinguished name
 *
 */
public class CommonNameExtractor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Pattern dnPattern;

    public CommonNameExtractor(String regex) {
        Objects.requireNonNull(regex, "CN extraction regex");

        setDnRegex(regex);
    }

    public String extractCommonName(String dn) {
        String commonName = dn;

        try {

            Matcher matcher = dnPattern.matcher(dn);

            if (!matcher.find()) {
                throw new TakException("No matching pattern was found in DN: " + dn );
            }

            if (matcher.groupCount() < 1) {
                throw new IllegalArgumentException("Regular expression must contain a group ");
            }

            commonName = matcher.group(1);

        } catch (Exception e) {
            logger.warn("exception extracting common name from DN. Using full DN as common name.", e);
        }

        logger.debug("Extracted common name is '" + commonName + "'");

        if (Strings.isNullOrEmpty(commonName)) {
            throw new TakException("empty common name in dn - invalid");
        }

        return commonName;
    }

    /**
     * Sets the regular expression which will by used to extract the common name from the DN.
     * <p>
     * It should contain a single group; for example the default expression
     * "CN=(.*?)(?:,|$)" matches the common name field. So "CN=Jimi Hendrix, OU=..." will
     * give a user name of "Jimi Hendrix".
     * <p>
     * The matches are case insensitive. So "emailAddress=(.?)," will match
     * "EMAILADDRESS=jimi@hendrix.org, CN=..." giving a user name "jimi@hendrix.org"
     *
     * @param dnRegex the regular expression to find in the DN
     */
    public void setDnRegex(String dnRegex) {
        if (Strings.isNullOrEmpty(dnRegex)) {
            throw new IllegalArgumentException("DN Regular expression may not be null or empty");
        }
        dnPattern = Pattern.compile(dnRegex, Pattern.CASE_INSENSITIVE);
    }
}


