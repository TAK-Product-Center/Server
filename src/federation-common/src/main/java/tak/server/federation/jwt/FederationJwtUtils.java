package tak.server.federation.jwt;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import tak.server.federation.hub.broker.FederationHubServerConfig;
import tak.server.federation.hub.ui.FederationHubUIConfig;

public class FederationJwtUtils {
	private static final Logger logger = LoggerFactory.getLogger(FederationJwtUtils.class);

	private ThreadLocal<JwtParser> jwtRsaParser = new ThreadLocal<>();
	private ThreadLocal<JwtParser> jwtHmacParser = new ThreadLocal<>();
	private PrivateKey privateKey = null;
	private SecretKeySpec secretKeySpec = null;
	private PublicKey publicKey = null;
	private boolean keysLoaded = false;
	
	private static String keyStoreType = "";
	private static String keyStoreFile = "";
	private static String keyStorePass = "";
	
	private static FederationJwtUtils instance = null;

	private KeyPair loadKeyPair(String keyStoreType, String keyStoreFile, String keyStorePass) {
		try {
			KeyStore keyStore = KeyStore.getInstance(keyStoreType);
			try (InputStream is = new FileInputStream(keyStoreFile)) {
				keyStore.load(is, keyStorePass.toCharArray());
			}
			
			PrivateKey privateKeyTmp = null;
			PublicKey publicKeyTmp = null;

			PrivateKey search = null;
			Enumeration<String> aliases = keyStore.aliases();
			while (aliases.hasMoreElements() && privateKeyTmp == null) {
				String alias = aliases.nextElement();
				privateKeyTmp = (PrivateKey) keyStore.getKey(alias, keyStorePass.toCharArray());
				if (privateKeyTmp != null) {
					Certificate cert = keyStore.getCertificate(alias);
					publicKeyTmp = cert.getPublicKey();
				}
			}

			KeyPair keyPair = new KeyPair(publicKeyTmp, privateKeyTmp);
			return keyPair;
		} catch (Exception e) {
			logger.error("exception in loadKeyPair", e);
			return null;
		}
	}

	private void loadKeys() {

		if (keysLoaded) {
			return;
		}

		try {
			//
			// load keys from tls keystore
			KeyPair keyPair = loadKeyPair(keyStoreType, keyStoreFile, keyStorePass);
			if (keyPair == null) {
				logger.info("loadKeyPair returned null");
				return;
			}

			publicKey = keyPair.getPublic();
			privateKey = keyPair.getPrivate();
			secretKeySpec = new SecretKeySpec(privateKey.getEncoded(), privateKey.getAlgorithm());

			if (privateKey == null) {
				logger.info("FedHubJwtUtils unable to find PrivateKey in keystore!");
				return;
			}

			keysLoaded = true;

			jwtRsaParser.remove();
			jwtHmacParser.remove();

		} catch (Exception e) {
			logger.error("Exception in loadKeys!", e);
		}
	}
	
	public static FederationJwtUtils getInstance(String keyStoreFileP,  String keyStorePassP, String keyStoreTypeP) {
		if (instance == null) {
			keyStoreFile = keyStoreFileP;
			keyStorePass = keyStorePassP;
			keyStoreType = keyStoreTypeP;
			instance = new FederationJwtUtils();
			instance.loadKeys();
		}
		return instance;
	}

	public static FederationJwtUtils getInstance(FederationHubUIConfig fedHubConfig) {
		if (instance == null) {
			keyStoreFile = fedHubConfig.getKeystoreFile();
			keyStorePass = fedHubConfig.getKeystorePassword();
			keyStoreType = fedHubConfig.getKeystoreType();
			instance = new FederationJwtUtils();
			instance.loadKeys();
		}
		return instance;
	}

	public static FederationJwtUtils getInstance(FederationHubServerConfig fedHubConfig) {
		if (instance == null) {
			keyStoreFile = fedHubConfig.getKeystoreFile();
			keyStorePass = fedHubConfig.getKeystorePassword();
			keyStoreType = fedHubConfig.getKeystoreType();
			instance = new FederationJwtUtils();
			instance.loadKeys();
		}
		return instance;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	private JwtParser getParser(SignatureAlgorithm signatureAlgorithm, Key key) {

		JwtParser parser = null;
		if (signatureAlgorithm == SignatureAlgorithm.RS256) {
			parser = jwtRsaParser.get();
			if (parser == null) {
				parser = Jwts.parser();
				parser.setSigningKey(key);
				jwtRsaParser.set(parser);
			}
		} else if (signatureAlgorithm == SignatureAlgorithm.HS256) {
			parser = jwtHmacParser.get();
			if (parser == null) {
				parser = Jwts.parser();
				parser.setSigningKey(key.getEncoded());
				jwtHmacParser.set(parser);
			}
		}

		return parser;
	}

	public Claims parseClaim(String token) {
		JwtParser jwtParser = getParser(SignatureAlgorithm.HS256, privateKey);
		return jwtParser.parseClaimsJws(token).getBody();
	}

	public String createFedhubToken(Map<String, Object> attributes, long expiration) {
		List<String> clientGroups = new ArrayList<>();
		clientGroups.add((String) attributes.get("clientGroup"));

		return createFedhubToken((String) attributes.get("clientFingerprint"), clientGroups, expiration);
	}

	public String createFedhubToken(String clientFingerprint, List<String> clientGroups, long expiration) {
		try {
			Date now = new Date();

			JwtBuilder builder = Jwts.builder().setIssuedAt(now).signWith(SignatureAlgorithm.HS256, secretKeySpec)
					.claim("federation", "federation claim val").claim("clientFingerprint", clientFingerprint)
					.claim("clientGroups", clientGroups);

			if (expiration > 0)
				builder.setExpiration(new Date(expiration));

			return builder.compact();

		} catch (Exception e) {
			logger.error("Exception in createFedhubToken!", e);
			return null;
		}
	}
	
	public String createFederationToken(String name, long expiration) {
		try {
			Date now = new Date();

			JwtBuilder builder = Jwts.builder().setIssuedAt(now).signWith(SignatureAlgorithm.HS256, secretKeySpec)
					.claim("name", name)
					.claim("expiration", expiration);

			if (expiration > 0)
				builder.setExpiration(new Date(expiration));

			return builder.compact();

		} catch (Exception e) {
			logger.error("Exception in createFederationToken!", e);
			return null;
		}
	}
	
	public String createFederationTokenFromClientCert(String fingerprint, String caFingerprint, String principalDN, String issuerDN, long expiration) {
		try {
			Date now = new Date();

			JwtBuilder builder = Jwts.builder().setIssuedAt(now).signWith(SignatureAlgorithm.HS256, secretKeySpec)
					.claim("fingerprint", fingerprint)
					.claim("caFingerprint", caFingerprint)
					.claim("principalDN", principalDN)
					.claim("issuerDN", issuerDN)
					.claim("expiration", expiration);

			if (expiration > 0)
				builder.setExpiration(new Date(expiration));

			return builder.compact();

		} catch (Exception e) {
			logger.error("Exception in createFederationToken!", e);
			return null;
		}
	}
}
