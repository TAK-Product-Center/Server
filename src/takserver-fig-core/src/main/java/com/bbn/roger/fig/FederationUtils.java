package com.bbn.roger.fig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import jakarta.xml.bind.DatatypeConverter;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class FederationUtils {

    private static final String CONNECTION_REFUSED_MSG = "connection refused, check network connectivity to federate";
    private static final String CONNECTION_CLOSED_MSG = " Network closed for unknown reason, check if remote federate is down";
    private static final String CA_TRUST_FAILURE_MSG_LOCAL = "Unable to establish trusted connection, ensure that correct CA public key ca.pem for federate is loaded in Federate Certificate Authorities";
    private static final String CA_TRUST_FAILURE_MSG_REMOTE = "Unable to establish trusted connection, ensure that admin of remote TAK Server has added CA public key ca.pem in their Federate Certificate Authorities";
    private static final String CONNECTION_DISABLED_MSG = "Connection is disabled, check Enable Connection";
    private static final String OUTGOING_DELETED_FROM_UI = "Outgoing deleted from UI";
    private static final String FEDERATE_HOSTNAME_NOT_RESOLVED = "Remote federate host name can't be resolved";
    private static final String CA_TRUSTSTORE_EMPTY = "Unable to establish trusted connection - Federate Certificate Authorities list is empty, add ca.pem for remote federate to Federate Certificate Authorities";
  

    /**
     * Saves a file from the classpath resources in src/main/resources/certs as a file on the
     * filesystem.
     *
     * @param name  name of a file in src/main/resources/certs.
     */
    public static File loadCert(String name) throws IOException {
        InputStream in = FederationUtils.class.getResourceAsStream(name);
        File tmpFile = File.createTempFile(name, "");
        tmpFile.deleteOnExit();

        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile));
        try {
            int b;
            while ((b = in.read()) != -1) {
                writer.write(b);
            }
        } finally {
            writer.close();
        }

        return tmpFile;
    }
    
    /**
     * Loads an X.509 certificate from the classpath resources in src/main/resources/certs.
     *
     * @param resourceName  name of a file in src/main/resources/certs.
     */
    public static X509Certificate loadX509CertResource(String resourceName) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        InputStream in = FederationUtils.class.getResourceAsStream("/certs/" + resourceName);
        try {
            return (X509Certificate) cf.generateCertificate(in);
        } finally {
            in.close();
        }
    }

    /**
     * Loads an X.509 certificate from a file
     *
     * @param caFilename  name of cert file
     */
    public static X509Certificate loadX509CertFile(String caFilename) throws CertificateException, IOException {

        if (Strings.isNullOrEmpty(caFilename)) {
            throw new IllegalArgumentException("empty ca file name");
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        InputStream is = new FileInputStream(caFilename);
        try {
            return (X509Certificate) cf.generateCertificate(is);
        } finally {
            is.close();
        }
    }

    public static String authorityFromHostAndPort(String host, int port) {
        try {
            return new URI(null, null, host, port, null, null, null).getAuthority();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid host or port: " + host + " " + port, ex);
        }
    }
    
    // Get the hash of the given byte array, using the provided hash function
    public static String getBytesHash(byte[] bytes, HashFunction hf, boolean withColon) {
        HashCode hash;
      
        hash = hf.newHasher().putBytes(bytes).hash();
        
        hash.asBytes();

        String fingerprint = DatatypeConverter.printHexBinary(hash.asBytes());
        
        if (!withColon) {
            return fingerprint.toLowerCase();
        }

        StringBuilder fpBuilder = new StringBuilder();

        for (int i = 0; i < fingerprint.length(); i++) {
            if (i > 0 && i % 2 == 0) {
                fpBuilder.append(':');
            }

            fpBuilder.append(fingerprint.charAt(i));
        }

        return fpBuilder.toString();
    }

    // Get the hash of the encoded cert bytes, using the provided hash function
    public static String getCertFingerprint(X509Certificate cert, HashFunction hf) throws CertificateEncodingException {
        return getBytesHash(cert.getEncoded(), Hashing.sha256(), true);
    }

    // Get the SHA-256 hash of the bytes of an X.509 cert
    public static String getCertSHA256Fingerprint(X509Certificate cert) throws CertificateEncodingException {
        return getCertFingerprint(cert, Hashing.sha256());
    }

    // Get the SHA-256 hash of the given byte array
    public static String getBytesSHA256(byte[] bytes) {
        return getBytesHash(bytes, Hashing.sha256(), false);
    }

    // find the matching root cause and add helpful message
    public static String getHumanReadableErrorMsg(Throwable t) {

        if (t.getMessage()!= null && OUTGOING_DELETED_FROM_UI.contains(t.getMessage())) {
            return OUTGOING_DELETED_FROM_UI;
        }

        String rootCause = Throwables.getRootCause(t).getMessage();
        if (rootCause != null) {
            if (rootCause.contains("Connection refused")) {
                return CONNECTION_REFUSED_MSG;
            } else if (rootCause.contains("Network closed for unknown reason")) {
                return CONNECTION_CLOSED_MSG;
            } else if (rootCause.contains("Channel shutdownNow invoked")) {
                return CONNECTION_DISABLED_MSG;
            } else if (rootCause.contains("valid certification path")) {
                return CA_TRUST_FAILURE_MSG_LOCAL;
            } else if (rootCause.toLowerCase().contains("nodename nor")) { // federate host name can't be resolved
                return FEDERATE_HOSTNAME_NOT_RESOLVED;
            } else if (rootCause.toLowerCase().contains("TLSV1_ALERT_INTERNAL_ERROR".toLowerCase())) { // this happens when our CA cert is not present in remote federate CA trust store
                return CA_TRUST_FAILURE_MSG_REMOTE;
            } else if (rootCause.toLowerCase().contains("trustAnchors parameter must be non-empty".toLowerCase())) {
                return CA_TRUSTSTORE_EMPTY;
            } else {
                return rootCause;
            }
        } else {
            return "Unexpected exception: root cause message is null";
        }
    }
}
