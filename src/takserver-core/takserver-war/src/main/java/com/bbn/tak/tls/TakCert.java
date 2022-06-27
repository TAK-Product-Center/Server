

package com.bbn.tak.tls;

import com.bbn.marti.remote.util.RemoteUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.Date;

import static com.bbn.tak.tls.Constants.CERT_TYPE;

/**
 * Created on 6/21/16.
 */
@Entity
@Table(name = "certificate")
@Cacheable
public class TakCert implements Serializable, Comparable<TakCert> {

    public static final Logger logger = LoggerFactory.getLogger(TakCert.class);

    private Long id;
    private String creatorDn;
    private String subjectDn;
    private String userDn;
    private X509Certificate certificate;
    private X509Certificate[] caChain;
    private String hash;
    private String clientUid;
    private Date issuanceDate;
    private Date expirationDate;
    private Date effectiveDate;
    private Date revocationDate;

    public TakCert(){

    }

    public TakCert(X509Certificate cert, X509Certificate[] caChain,
                   String creator, String userDn, Date issuanceDate, String clientUid)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        this.certificate = cert;
        this.subjectDn = cert.getSubjectX500Principal().getName();
        this.expirationDate = cert.getNotAfter();
        this.effectiveDate = cert.getNotBefore();
        this.issuanceDate = issuanceDate;
        this.creatorDn = creator;
        this.userDn = userDn;
        this.hash = RemoteUtil.getInstance().getCertSHA256Fingerprint(cert);
        this.clientUid = clientUid;
        this.caChain = caChain;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "creator_dn", unique = false, nullable = false, columnDefinition="VARCHAR")
    public String getCreatorDn() {
        return creatorDn;
    }

    public void setCreatorDn(String name) {
        this.creatorDn = name;
    }

    @Column(name = "subject_dn", unique = false, nullable = false, columnDefinition="VARCHAR")
    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn(String subjectDn) {
        this.subjectDn = subjectDn;
    }

    @Column(name = "user_dn", unique = false, nullable = true, columnDefinition="VARCHAR")
    public String getUserDn() {
        return userDn;
    }

    public void setUserDn(String user_dn) {
        this.userDn = user_dn;
    }

    @Column(name = "certificate", unique = true, nullable = false, columnDefinition="VARCHAR")
    public String getCertificate() {
        try {
            return Util.certToPEM(certificate, true);
        } catch (CertificateEncodingException cfe) {
            logger.error("CertificateEncodingException in getCertificate!", cfe);
            return "Error encoding certificate in PEM format!";
        }
    }

    public void setCertificate(String certificate) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance(CERT_TYPE);
            this.certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificate.getBytes()));
        } catch (CertificateException ce) {
            logger.error("CertificateException in getCertificate!", ce);
        }
    }

    @JsonIgnore
    @Transient
    public X509Certificate getX509Certificate() { return certificate; }

    @JsonIgnore
    @Transient
    public X509Certificate[] getX509CertificateChain()  { return caChain; }


    @Column(name = "hash", unique = false, nullable = false, columnDefinition="VARCHAR")
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Column(name = "client_uid", unique = false, columnDefinition="VARCHAR")
    public String getClientUid() {
        return clientUid;
    }

    public void setClientUid(String clientUid) {
        this.clientUid = clientUid;
    }

    @Transient
    public String getSerialNumber()  {
        return certificate.getSerialNumber().toString(16);
    }

    @Column(name = "issuance_date", unique = false, nullable = true, columnDefinition="DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = tak.server.Constants.COT_DATE_FORMAT)
    public Date getIssuanceDate() {
        return issuanceDate;
    }

    public void setIssuanceDate(Date issuanceDate) {
        this.issuanceDate = issuanceDate;
    }

    @Column(name = "expiration_date", unique = false, nullable = false, columnDefinition="DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = tak.server.Constants.COT_DATE_FORMAT)
    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Column(name = "effective_date", unique = false, nullable = false, columnDefinition="DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = tak.server.Constants.COT_DATE_FORMAT)
    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    @Column(name = "revocation_date", unique = false, nullable = true, columnDefinition="DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = tak.server.Constants.COT_DATE_FORMAT)
    public Date getRevocationDate() {
        return revocationDate;
    }

    public void setRevocationDate(Date revocationDate) {
        this.revocationDate = revocationDate;
    }


    @Override
    public int compareTo(TakCert takCert) {
        return 0;
    }
}
