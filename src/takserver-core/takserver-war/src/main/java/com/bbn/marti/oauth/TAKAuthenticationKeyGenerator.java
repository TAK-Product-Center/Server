package com.bbn.marti.oauth;

import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TAKAuthenticationKeyGenerator extends DefaultAuthenticationKeyGenerator {

    private final String KEY = "key";
    private final String NONCE = "nonce";

    @Override
    public String extractKey(final OAuth2Authentication authentication) {
        final Map<String, String> values = new LinkedHashMap<>(2);
        values.put(KEY, super.extractKey(authentication));
        values.put(NONCE, UUID.randomUUID().toString());
        return generateKey(values);
    }
}
