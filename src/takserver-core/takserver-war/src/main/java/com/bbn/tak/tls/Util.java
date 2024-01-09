

package com.bbn.tak.tls;

import sun.security.pkcs10.PKCS10;

import jakarta.xml.bind.DatatypeConverter;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Created on 6/23/16.
 */
public class Util {

    private static String toPEM(byte[] bytes, boolean includeHeader, String header, String footer) {
        String encoded = DatatypeConverter.printBase64Binary(bytes);

        StringWriter sw = new StringWriter();
        if (includeHeader) sw.write(header + "\n");
        sw.write(encoded.replaceAll("(.{64})", "$1\n"));

        if (includeHeader) {
            if (encoded.length() % 64 != 0) {
                sw.write("\n");
            }

            sw.write(footer + "\n");
        }

        return sw.toString();
    }

    public static String certToPEM(X509Certificate cert, boolean includeHeader) throws CertificateEncodingException {
        return toPEM(cert.getEncoded(), includeHeader, Constants.CERTBEGIN, Constants.CERTEND);
    }

    public static String keyToPEM(PrivateKey key, boolean includeHeader) throws CertificateEncodingException {
        return toPEM(key.getEncoded(), includeHeader, Constants.KEYBEGIN, Constants.KEYEND);
    }

    public static String csrToPEM(PKCS10 csr, boolean includeHeader) throws CertificateEncodingException {
        return toPEM(csr.getEncoded(), includeHeader, Constants.CSRBEGIN, Constants.CSREND);
    }
}
