package com.bbn.marti.oauth;

import com.bbn.marti.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class TAKJwtAccessTokenConverter extends JwtAccessTokenConverter {

    private static final Logger logger = LoggerFactory.getLogger(TAKJwtAccessTokenConverter.class);

    public TAKJwtAccessTokenConverter() {
        try {
            PublicKey publicKey = JwtUtils.getInstance().getPublicKey();
            PrivateKey privateKey = JwtUtils.getInstance().getPrivateKey();
            setKeyPair(new KeyPair(publicKey, privateKey));
        } catch (Exception e) {
            logger.error("exception in TAKJwtAccessTokenConverter!", e);
        }
    }

}
