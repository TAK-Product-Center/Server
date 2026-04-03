package com.bbn.marti.oauth;

import com.bbn.marti.config.Oauth.AuthServer;
import com.bbn.marti.config.Oauth.OpenIdDiscoveryConfiguration;
import com.bbn.marti.config.Tls;
import com.bbn.marti.remote.config.CoreConfigFacade;
import okhttp3.OkHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.*;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class OAuthUtils {
    public static AuthServer processTrustedAuthServerConfig(OpenIdDiscoveryConfiguration config)
            throws NoSuchAlgorithmException, KeyManagementException {

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<String, String>();

        RestTemplate restTemplate;

        if (config.isTrustAllCerts()) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            Tls tlsConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getTls();
            SSLContext sslContext = SSLContext.getInstance(tlsConfig.getContext());
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true);
            restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory(builder.build()));

        } else {
            restTemplate = new RestTemplate(new OkHttp3ClientHttpRequestFactory());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                config.getConfigurationUri(), HttpMethod.GET,
                null,
                String.class);

        // validate the response
        if (tokenResponse.getStatusCode() != HttpStatus.OK) {
            throw new IllegalStateException("token endpoint returned " + tokenResponse.getStatusCodeValue());
        }

        try {
            JSONObject response = (JSONObject) new JSONParser().parse(tokenResponse.getBody());

            if (!response.containsKey("issuer")) {
                throw new IllegalStateException("missing issuer in response");
            }

            if (!response.containsKey("authorization_endpoint")) {
                throw new IllegalStateException("missing authorization_endpoint in response");
            }

            if (!response.containsKey("authorization_endpoint")) {
                throw new IllegalStateException("missing authorization_endpoint in response");
            }

            if (!response.containsKey("token_endpoint")) {
                throw new IllegalStateException("missing token_endpoint in response");
            }

            if (!response.containsKey("jwks_uri")) {
                throw new IllegalStateException("missing jwks_uri in response");
            }
            if (!response.containsKey("scopes_supported")) {
                throw new IllegalStateException("missing scopes_supported in response");
            }

            if (!response.containsKey("claims_supported")) {
                throw new IllegalStateException("missing claims_supported in response");
            }

            if (!response.containsKey("grant_types_supported")) {
                throw new IllegalStateException("missing grant_types_supported in response");
            }

            AuthServer convertedConfig = new AuthServer();

            convertedConfig.setName(config.getName());
            convertedConfig.setClientId(config.getClientId());
            convertedConfig.setSecret(config.getSecret());
            convertedConfig.setRedirectUri(config.getRedirectUri());
            convertedConfig.setAccessTokenName(config.getAccessTokenName());
            convertedConfig.setRefreshTokenName(config.getRefreshTokenName());
            convertedConfig.setAuthEndpoint(response.get("authorization_endpoint").toString());
            convertedConfig.setTokenEndpoint(response.get("token_endpoint").toString());
            convertedConfig.setTrustAllCerts(config.isTrustAllCerts());
            StringBuilder scopeBuilder = new StringBuilder();

            org.json.simple.JSONArray scopes = ( org.json.simple.JSONArray) response.get("scopes_supported");
            for (Object scope : scopes){
                switch(scope.toString()){
                    case "openid":
                    case "email":
                    case "profile":
                        if (scopeBuilder.length() == 0){
                            scopeBuilder = new StringBuilder((String)scope);
                        } else {
                            scopeBuilder.append(" ").append((String)scope);
                        }

                    default:
                        break;
                }
            }
            convertedConfig.setScope(scopeBuilder.toString());

            tokenResponse = restTemplate.exchange(
                    response.get("jwks_uri").toString(), HttpMethod.GET,
                    null,
                    String.class);

            if (tokenResponse.getStatusCode() != HttpStatus.OK) {
                throw new IllegalStateException("token endpoint returned " + tokenResponse.getStatusCodeValue());
            }

            response = (JSONObject) new JSONParser().parse(tokenResponse.getBody());
            if (!response.containsKey("keys")) {
                throw new IllegalStateException("missing keys in jwks_uri response");
            }

            if ( response.get("keys") instanceof JSONObject) {
                org.json.simple.JSONObject jwks_key = (org.json.simple.JSONObject) response.get("keys");
                String publicKey = convertJWKtoRSA(jwks_key);
                convertedConfig.setIssuer("");
                convertedConfig.getKey().add(publicKey);
            } else if( response.get("keys") instanceof JSONArray) {
                org.json.simple.JSONArray jwks_keys = (org.json.simple.JSONArray) response.get("keys");

                convertedConfig.setIssuer("");
                for (Object key : jwks_keys) {
                    if ( key instanceof org.json.simple.JSONObject) {
                        if (( (JSONObject)key).get("use") != null
                        && ( (JSONObject)key).get("use").equals("sig")) {
                            String publicKey = convertJWKtoRSA((JSONObject) key);
                            convertedConfig.getKey().add(publicKey);
                        }
                    }
                }
            }
            return convertedConfig;
        } catch (ParseException exception) {
            throw new IllegalStateException("could not parse token response", exception);
        }
    }

    public static String convertJWKtoRSA(JSONObject jwk)  {
        String n = jwk.get("n").toString(); // Modulus, Base64url encoded
        String e = jwk.get("e").toString(); // Public Exponent, Base64url encoded

        // Decode Base64url to BigIntegers
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger publicExponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));

        // Create RSAPublicKeySpec
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Get DER encoded public key
            byte[] derPublicKey = publicKey.getEncoded();

            // Base64 encode the DER bytes
            String base64Encoded = Base64.getEncoder().encodeToString(derPublicKey);

            // Construct PEM string
            StringBuilder pem = new StringBuilder();

            // Split into 64-character lines
            int i = 0;
            while (i < base64Encoded.length()) {
                pem.append(base64Encoded.substring(i, Math.min(i + 64, base64Encoded.length())));
                i += 64;
            }
            return pem.toString();
        } catch (NoSuchAlgorithmException ex){
            throw new IllegalStateException("System could not find RSA encryption algorithm", ex);
        }  catch (InvalidKeySpecException ex){
            throw new IllegalStateException(String.format("The provided JWKS is not properly formated %s", keySpec), ex);
        }
    }
}
