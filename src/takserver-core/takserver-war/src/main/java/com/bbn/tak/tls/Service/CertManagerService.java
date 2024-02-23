package com.bbn.tak.tls.Service;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.bbn.marti.config.CAType;
import com.bbn.marti.config.CertificateConfig;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.MicrosoftCAConfig;
import com.bbn.marti.config.NameEntry;
import com.bbn.marti.config.TAKServerCAConfig;
import com.bbn.marti.config.Tls;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.util.spring.MartiSocketUserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.jwt.JwtUtils;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.util.X509UsernameExtractor;
import com.bbn.tak.tls.CertManager;
import com.bbn.tak.tls.TakCert;
import com.bbn.tak.tls.WSTEP.WSTEPClient;

import sun.security.pkcs10.PKCS10;


/**
 * Created on 5/14/2018.
 */
public class CertManagerService {

    public static final Logger logger = LoggerFactory.getLogger(CertManagerService.class);

    @Autowired
    private CertManager certManager;

    @Autowired
    private SubscriptionManagerLite subMgr;

    private X509UsernameExtractor usernameExtractor = null;

    @EventListener({ContextRefreshedEvent.class})
    private void init()throws RemoteException {
        usernameExtractor = new X509UsernameExtractor(CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getDNUsernameExtractorRegex());
    }

    private boolean validateCSR(PKCS10 csr, CertificateConfig certificateConfig) {

        try {
            // convert the CSR's subject into a LdapName for easy searching
            LdapName ldapName = new LdapName(csr.getSubjectName().getName());

            // CSR should have 1 more RDN than required (CN)
            if (ldapName.getRdns().size() - 1 !=
                    certificateConfig.getNameEntries().getNameEntry().size()) {
                return false;
            }

            //
            // ensure the CSR has a CN and its equal to the currently logged in user
            //

            String cn = csr.getSubjectName().getCommonName();
            if (cn == null) {
                logger.error("Found CSR without CN!");
                return false;
            }

            if (getHttpUser().compareToIgnoreCase(cn) != 0) {
                logger.error("HttpUser didn't equal CN!");
                return false;
            }

            // loop over our required entries
            for (NameEntry nameEntry : certificateConfig.getNameEntries().getNameEntry()) {

                // search for the entry in the CSR
                boolean found = false;
                for (Rdn rdn : ldapName.getRdns()) {
                    found = rdn.getType().compareToIgnoreCase(nameEntry.getName()) == 0 &&
                            ((String) rdn.getValue()).compareToIgnoreCase(nameEntry.getValue()) == 0;
                    if (found) {
                        break;
                    }
                }

                // if we didn't find the required pair in the CSR, bail
                if (!found) {
                    logger.error("Required RDN not found in CSR!");
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("exception in validateCSR!", e);
            return false;
        }
    }

    public TakCert signClient(String clientUid, boolean addChannelsExtUsage, String base64CSR) {
        try {
            //
            // get the cert config from CoreConfig.xml
            //
            CertificateSigning certificateSigning = CoreConfigFacade.getInstance().getRemoteConfiguration().getCertificateSigning();
            if (certificateSigning == null) {
                throw new TakException("CertificateSigning element not found in CoreConfig!");
            }

            // extract the csr from the request
            String tempCsr = new String(base64CSR);
            tempCsr = tempCsr.replace("-----BEGIN CERTIFICATE REQUEST-----", "");
            tempCsr = tempCsr.replace("-----END CERTIFICATE REQUEST-----", "");

            byte[] bytes = Base64.decodeBase64(tempCsr.getBytes("UTF-8"));
            PKCS10 csr = new PKCS10(bytes);

            // get the list of required RDNs to compare against CSR from client
            CertificateConfig config = certificateSigning.getCertificateConfig();
            if (config == null) {
                throw new TakException("CertificateConfig element not found in CoreConfig!");
            }

            // ensure that the CSR has the required RDNs filled out
            if (!validateCSR(csr, config)) {
                throw new TakException("CSR validation failed!");
            }

            //
            // process the CSR and populate caChain and signedCert with the results
            //
            X509Certificate[] caChain;
            X509Certificate signedCert = null;

            // is the user authenticating to the enrollment endpoint via OAuth?
            String token = ((MartiSocketUserDetailsImpl)SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal()).getToken();

            if (certificateSigning.getCA() == CAType.TAK_SERVER) {

                TAKServerCAConfig takServerCAConfig = certificateSigning.getTAKServerCAConfig();
                if (takServerCAConfig == null) {
                    throw new TakException("TAKServerCAConfig element not found in CoreConfig!");
                }

                caChain = subMgr.getSigningCertChain();
                CertManager.CertKey signingCert = new CertManager.CertKey(caChain[0], subMgr.getSigningKey());
                long validityDays = subMgr.getSigningValidity();
                long validityNotBeforeOffsetMinutes = takServerCAConfig.getValidityNotBeforeOffsetMinutes();

                String responderUrl = null;
                Tls tls = CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getTls();
                if (tls.isEnableOCSP()) {
                    responderUrl = tls.getResponderUrl();
                }

                long now = new Date().getTime();
                long validMs = validityDays * 1000 * 60 * 60 * 24;
                long validityNotBeforeOffsetMS = validityNotBeforeOffsetMinutes * 60 * 1000;
                Date notBefore = new Date(now - validityNotBeforeOffsetMS);
                Date notAfter = new Date(now + validMs);

                // is the user authenticating to the enrollment endpoint via OAuth?
                if (token != null) {
                    // set the certificate expiration to match the token expiration
                    Claims claims = JwtUtils.getInstance().parseClaims(token, SignatureAlgorithm.RS256);
                    Integer exp = (Integer)claims.get("exp");
                    if (exp != null) {
                        notAfter = new Date(exp.longValue() * 1000);
                    }
                }

                signedCert = certManager.signCertificate(
                        csr, signingCert, CertManager.CERTTYPE.CLIENT, notBefore, notAfter,
                        takServerCAConfig.getSignatureAlg(), addChannelsExtUsage, responderUrl);

            } else if (certificateSigning.getCA() == CAType.MICROSOFT_CA) {

                MicrosoftCAConfig microsoftCAConfig = certificateSigning.getMicrosoftCAConfig();
                if (microsoftCAConfig == null) {
                    throw new TakException("MicrosoftCAConfig not found in CoreConfig!");
                }

                Tls tlsConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getSecurity().getTls();

                X509Certificate[] certs = WSTEPClient.submitCSR(tempCsr,
                        microsoftCAConfig.getTemplateName(),
                        microsoftCAConfig.getSvcUrl(),
                        microsoftCAConfig.getUsername(),
                        microsoftCAConfig.getPassword(),
                        microsoftCAConfig.getTruststore(),
                        microsoftCAConfig.getTruststorePass(),
                        microsoftCAConfig.isTrustAllHosts(),
                        tlsConfig.getContext());

                if (certs == null || certs.length < 2) {
                    throw new TakException("WSTEPClient.submitCSR failed!");
                }

                List<X509Certificate> caList = new LinkedList<>();
                for (int i = 0; i < certs.length; i++) {
                    if (certs[i].getExtendedKeyUsage() != null
                            &&  certs[i].getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2")) {
                        signedCert = certs[i];
                    } else {
                        caList.add(certs[i]);
                    }
                }

                caChain = caList.toArray(new X509Certificate[0]);

            } else {
                throw new TakException("Unknown CA type!");
            }

            // store the new cert in the database
            String username = usernameExtractor.extractUsername(signedCert);

            return new TakCert(signedCert, caChain,
                    getHttpUser(), username, new Date(), clientUid, token);

        } catch (Exception e) {
            logger.error("exception in signClient!", e);
            return null;
        }
    }

    protected String getHttpUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
