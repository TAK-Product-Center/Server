

package com.bbn.tak.tls;

import static com.bbn.tak.tls.Constants.CERTBITS;
import static com.bbn.tak.tls.Constants.KEY_TYPE;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs10.PKCS10;
import sun.security.pkcs10.PKCS10Attribute;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.*;


/**
 * Created on 6/8/16.
 */
public class CertManager {

    public static class CertKey {
        public X509Certificate cert;
        public PrivateKey key;

        public CertKey(X509Certificate signedCertificate, PrivateKey privKey) {
            cert = signedCertificate;
            key = privKey;
        }
    }

    public enum CERTTYPE { CA, SERVER, CLIENT };

    public CertManager() throws NoSuchProviderException, NoSuchAlgorithmException {

    }

    public CertKey makeCA(String name, long validity, String signatureAlg) throws Exception {
        CertAndKeyGen certGen = new CertAndKeyGen(KEY_TYPE, signatureAlg, null);
        certGen.generate(CERTBITS);
        PrivateKey privKey = certGen.getPrivateKey();
        X509Certificate cabase = certGen.getSelfCertificate(new X500Name(name), validity);
        return new CertKey(signCertificate(cabase, new CertKey(cabase, privKey), CERTTYPE.CA), privKey);
    }

    public CertKey makeServerCert(String name, long validity, String signatureAlg) throws InvalidKeyException,
            IOException, NoSuchAlgorithmException, CertificateException, SignatureException, NoSuchProviderException {
        CertAndKeyGen certGen = new CertAndKeyGen(KEY_TYPE, signatureAlg, null);
        certGen.generate(CERTBITS);
        PrivateKey privKey = certGen.getPrivateKey();
        X509Certificate serverBase = certGen.getSelfCertificate(new X500Name(name), validity);
        return new CertKey(serverBase, privKey);
    }

    public CertKey makeClientCert(String name, long validity, String signatureAlg) throws InvalidKeyException,
            IOException, NoSuchAlgorithmException, CertificateException, SignatureException, NoSuchProviderException {
        CertAndKeyGen certGen = new CertAndKeyGen(KEY_TYPE, signatureAlg, null);
        certGen.generate(CERTBITS);
        PrivateKey privKey = certGen.getPrivateKey();
        // need to turn validity from days into seconds
        X509Certificate clientBase = certGen.getSelfCertificate(new X500Name(name), validity * 60 * 60 *24);
        return new CertKey(clientBase, privKey);
    }

    public X509Certificate generateCert(CertAndKeyGen certGen, String name, long validity) throws IOException, NoSuchAlgorithmException,
            CertificateException, NoSuchProviderException, SignatureException, InvalidKeyException {
        // generate it with 2048 bits
        certGen.generate(CERTBITS);

        // add the certificate information, currently only valid for one year.
        X509Certificate cert = certGen.getSelfCertificate(
                // enter your details according to your application
                new X500Name(name), validity);
        return cert;
    }

    public static X509Certificate signCertificate(PKCS10 request, CertKey issuerCertficate, CERTTYPE type,
                                                  long validityNotBeforeOffsetMinutes,  long validity,
                                                  String signatureAlg, boolean addChannelsExtUsage,
                                                  String ocspResponder) throws Exception {
        PublicKey pub = request.getSubjectPublicKeyInfo();
        X509CertInfo info = new X509CertInfo();
        long now = new Date().getTime();
        long validMs = validity * 1000 * 60 * 60 * 24;
        long validityNotBeforeOffsetMS = validityNotBeforeOffsetMinutes * 60 * 1000;
        info.set(X509CertInfo.VALIDITY, new CertificateValidity(
                new Date(now - validityNotBeforeOffsetMS), new Date(now + validMs)));
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(
                new java.util.Random().nextInt() & 0x7fffffff));
        info.set(X509CertInfo.VERSION,
                new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.ALGORITHM_ID,
                new CertificateAlgorithmId(AlgorithmId.get(signatureAlg)));
        info.set(X509CertInfo.ISSUER,
                new X500Name(issuerCertficate.cert.getSubjectDN().toString()));
        info.set(X509CertInfo.KEY, new CertificateX509Key(request.getSubjectPublicKeyInfo()));
        info.set(X509CertInfo.SUBJECT, new X500Name(
                request.getSubjectName().getName()));

        return signCertificate(info, issuerCertficate, type, addChannelsExtUsage, ocspResponder);
    }

    public static X509Certificate signCertificate(X509Certificate certificate,
                                                  CertKey issuerCertificate,
                                                  CERTTYPE type) throws Exception {
        byte[] inCertBytes = certificate.getTBSCertificate();
        X509CertInfo info = new X509CertInfo(inCertBytes);
        return signCertificate(info, issuerCertificate, type, false, null);
    }

    public static X509Certificate signCertificate(X509CertInfo info,
                                                  CertKey issuerCertificate,
                                                  CERTTYPE type,
                                                  boolean addChannelsExtUsage,
                                                  String ocspResponder) throws Exception {
        X500Name issuerName = new X500Name(issuerCertificate.cert.getSubjectX500Principal().toString());
        String issuerSigAlg = issuerCertificate.cert.getSigAlgName();

        info.set(X509CertInfo.ISSUER, issuerName);
        CertificateExtensions exts = new CertificateExtensions();
        switch(type) {
            case CA:
                exts.set(BasicConstraintsExtension.NAME,new BasicConstraintsExtension(true, true, -1));
                exts.set(KeyUsageExtension.NAME, getCaKeyUsage());
                break;
            case SERVER:
                exts.set(KeyUsageExtension.NAME, getServerKeyUsage());
                exts.set(ExtendedKeyUsageExtension.NAME, getServerExtKeyUsage());
                break;
            case CLIENT:
                exts.set(KeyUsageExtension.NAME, getClientKeyUsage());
                exts.set(ExtendedKeyUsageExtension.NAME, getClientExtKeyUsage(addChannelsExtUsage));
                break;
        }

        if (ocspResponder != null) {
            LinkedList<AccessDescription> accessDescriptions = new LinkedList<>();
            accessDescriptions.add(new AccessDescription(
                    AccessDescription.Ad_OCSP_Id, new GeneralName(new URIName(ocspResponder))));
            exts.set(AuthorityInfoAccessExtension.NAME, new AuthorityInfoAccessExtension(accessDescriptions));
        }

        info.set(X509CertInfo.EXTENSIONS, exts);
        X509CertImpl outCert = new X509CertImpl(info);
        outCert.sign(issuerCertificate.key, issuerSigAlg);
        return outCert;
    }

    public static Object getCaKeyUsage() throws IOException {
        KeyUsageExtension ext = new KeyUsageExtension();
        ext.set(KeyUsageExtension.DIGITAL_SIGNATURE, true);
        ext.set(KeyUsageExtension.KEY_CERTSIGN, true);
        ext.set(KeyUsageExtension.CRL_SIGN, true);
        ext.set(KeyUsageExtension.NON_REPUDIATION, true);
        return ext;
    }

    public static Object getServerKeyUsage() throws IOException {
        KeyUsageExtension ext = new KeyUsageExtension();
        ext.set(KeyUsageExtension.DIGITAL_SIGNATURE, true);
        ext.set(KeyUsageExtension.KEY_ENCIPHERMENT, true);
        ext.set(KeyUsageExtension.KEY_AGREEMENT, true);
        ext.set(KeyUsageExtension.NON_REPUDIATION, true);
        return ext;
    }

    public static Object getClientKeyUsage() throws IOException {
        KeyUsageExtension ext = new KeyUsageExtension();
        ext.set(KeyUsageExtension.DIGITAL_SIGNATURE, true);
        ext.set(KeyUsageExtension.KEY_AGREEMENT, true);
        ext.set(KeyUsageExtension.NON_REPUDIATION, true);
        return ext;
    }

    private static final int[] serverAuthOidData = {1, 3, 6, 1, 5, 5, 7, 3, 1};
    private static final int[] clientAuthOidData = {1, 3, 6, 1, 5, 5, 7, 3, 2};
    private static final int[] enableChannelsOidData = {1, 2, 840, 113549, 1, 9, 7};

    public static Object getServerExtKeyUsage() throws IOException {
        ObjectIdentifier serverAuthObjId = new ObjectIdentifier(serverAuthOidData);
        ObjectIdentifier clientAuthObjId = new ObjectIdentifier(clientAuthOidData);
        Vector<ObjectIdentifier> obj = new Vector<>(2);
        obj.add(serverAuthObjId);
        obj.add(clientAuthObjId);
        ExtendedKeyUsageExtension ext = new ExtendedKeyUsageExtension(obj);
        return ext;
    }

    public static Object getClientExtKeyUsage(boolean addChannelsExtUsage) throws IOException {
        ObjectIdentifier clientAuthObjId = new ObjectIdentifier(clientAuthOidData);
        ObjectIdentifier enableChannelsObjId = new ObjectIdentifier(enableChannelsOidData);
        Vector<ObjectIdentifier> obj = new Vector<>(1);
        obj.add(clientAuthObjId);
        if (addChannelsExtUsage) {
            obj.add(enableChannelsObjId);
        }
        ExtendedKeyUsageExtension ext = new ExtendedKeyUsageExtension(obj);
        return ext;
    }

    public static PKCS10 generateCSR(CertKey cert, String signatureAlg) throws Exception {
        PKCS10 request = new PKCS10(cert.cert.getPublicKey());
        CertificateExtensions ext = new CertificateExtensions();
        request.getAttributes().setAttribute(X509CertInfo.EXTENSIONS,
                new PKCS10Attribute(PKCS9Attribute.EXTENSION_REQUEST_OID, ext));

        Signature signature = Signature.getInstance(signatureAlg);
        signature.initSign(cert.key);
        X500Name subject = new X500Name(cert.cert.getSubjectDN().toString());
        request.encodeAndSign(subject, signature);
        return request;
    }
}
