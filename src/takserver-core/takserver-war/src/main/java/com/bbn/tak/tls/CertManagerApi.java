

package com.bbn.tak.tls;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.CertificateConfig;
import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.TAKServerCAConfig;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.tak.tls.CertManager.CERTTYPE;
import com.bbn.tak.tls.CertManager.CertKey;
import com.bbn.tak.tls.Service.CertManagerService;
import com.bbn.tak.tls.repository.TakCertRepository;

import tak.server.Constants;


/**
 * Created on 6/8/16.
 */
@RestController
@Profile({Constants.API_PROFILE_NAME, Constants.MONOLITH_PROFILE_NAME})
public class CertManagerApi extends BaseRestController {

    public class PEMCertKey {
        String certPEM;
        String keyPEM;

        public PEMCertKey(String cert, String key) {
            certPEM = cert;
            keyPEM = key;
        }

        public String getPEM() { return certPEM; }
        public String getKeyPEM() { return keyPEM; }

    }

    public static final Logger logger = LoggerFactory.getLogger(CertManagerApi.class);

    @Autowired
    private CertManager certManager;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private SubscriptionManagerLite subMgr;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private TakCertRepository takCertRepository;

    @Autowired
    private CertManagerService certManagerService;

    private static final String DEFAULT_PASSWORD = "atakatak";
    
    @RequestMapping(value = "/tls/makeClientKeyStore", method = RequestMethod.GET)
    ResponseEntity<byte[]> makeKeyStore(@RequestParam(value = "cn", required = false) String cn,
                                        @RequestParam(value = "password", required = false,
                                                defaultValue = DEFAULT_PASSWORD) String password) throws Exception {
        if (cn == null || cn.isEmpty()) {
            cn = getHttpUser();
        }

        X509Certificate[] signingCertChain = subMgr.getSigningCertChain();
        X509Certificate signingCert = signingCertChain[0];
        String dn = verifyCN(cn, signingCert);
        if (dn == null) {
            logger.error("Can't make certificate for cn="+cn);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Can't make certificate for cn="+cn);
            return null;
        }
        try {
            PrivateKey sk = subMgr.getSigningKey();
            logger.error("signing key: format: " +sk.getFormat()+ "; alg: " + sk.getAlgorithm());
            CertKey ccert = makeClientCert(dn, new CertKey(signingCert, subMgr.getSigningKey()));
            logger.error("client signing key: format: " +ccert.key.getFormat()+ "; alg: " + ccert.key.getAlgorithm());

            X509Certificate cert = ccert.cert;

            List<X509Certificate> chain = new ArrayList<X509Certificate>();
            chain.add(cert);
            chain.addAll(Arrays.asList(signingCertChain));
            X509Certificate[] chainArray = chain.toArray(new X509Certificate[chain.size()]);

            KeyStore keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(null, null);
            keyStore.setCertificateEntry(cn, cert);
            keyStore.setKeyEntry(cn, ccert.key, password.toCharArray(), chainArray);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            keyStore.store(bos, password.toCharArray());
            byte[] result = bos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<byte[]>(result, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Exception making client certificate for: " + cn + " on behalf of " + getHttpUser(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    protected String verifyCN(String cn, X509Certificate issuer) {
        logger.warn("generating client cert for: " + getHttpUser());
        if (cn.compareTo(getHttpUser()) == 0) {
            logger.warn("cn matched username");
            String issuerDn = issuer.getSubjectDN().getName();
            logger.warn(" issuer dn: " + issuerDn);
            String dn = "CN=" + cn + issuerDn.substring(issuerDn.indexOf(','));
            return dn;
        }
        return null;
    }

    private CertKey makeClientCert(String dn, CertKey signingCert) throws Exception {

        CertificateSigning certificateSigningConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getCertificateSigning();
        if (certificateSigningConfig == null) {
            throw new TakException("CertificateSigning element not found in CoreConfig!");
        }

        TAKServerCAConfig takServerCAConfig = certificateSigningConfig.getTAKServerCAConfig();
        if (takServerCAConfig == null) {
            throw new TakException("TAKServerCAConfig element not found in CoreConfig!");
        }

        CertKey clientCert = certManager.makeClientCert(
                new X500Principal(dn).getName(), subMgr.getSigningValidity(), takServerCAConfig.getSignatureAlg());
        CertKey signedClient =
                new CertKey(certManager.signCertificate(clientCert.cert, signingCert, CERTTYPE.CLIENT), clientCert.key);
        return signedClient;
    }

/*
    @RequestMapping(value = "/tls/makeClient", method = RequestMethod.GET)
    PEMCertKey makeClientCert(@RequestParam(value = "cn", required = true) String CN) throws Exception {
        if (validity == null) {
            validity = DEFAULT_VALIDITY;
        }
        subMgr.

        TakCert tcert = certRepository.findOneBySubjectDn(new X500Principal(issuerDN).getName());
        StoredCertKey caKey = keyRepository.findOneByCertificateId(tcert.getId());
        CertKey caCert = new CertKey(tcert.getX509Certificate(), caKey.getPrivateKey());

        CertKey clientCert = certManager.makeClientCert(new X500Principal(CN).getName(), validity);
        CertKey signedClient =
                new CertKey(certManager.signCertificate(clientCert.cert, caCert, CERTTYPE.CLIENT), clientCert.key);

        TakCert saved = certRepository.save(new TakCert(signedClient.cert, getHttpUser(), getHttpUser(), new Date()));
        keyRepository.save(new StoredCertKey(saved.getId(), signedClient.key));
        return new PEMCertKey(certToPEM(signedClient.cert, true), keyToPEM(signedClient.key, true));
    }

    @RequestMapping(value = "/tls/signServer", method = RequestMethod.POST)
    ResponseEntity<String> signServerCert(@RequestParam(value = "base64CSR", required = true) String base64CSR,
                                          @RequestParam(value = "issuerDN", required = true) String issuerDN)
            throws Exception {
        return signCert(base64CSR, issuerDN, CERTTYPE.SERVER);
    }

    protected ResponseEntity<String> signCert(String base64CSR, String issuerDN, CERTTYPE type) throws Exception {
        String bareRequest;
        if (base64CSR.startsWith(Constants.CSRBEGIN)) {
            bareRequest = base64CSR.replace(Constants.CSRBEGIN, "").replace(Constants.CSREND, "").trim();
        } else {
            bareRequest = base64CSR;
        }

        byte [] bytes = Base64.decode(bareRequest.getBytes());

        TakCert caTCert = certRepository.findOneBySubjectDn(new X500Principal(issuerDN).getName());
        StoredCertKey caKey = keyRepository.findOneByCertificateId(caTCert.getId());
        CertKey caCert = new CertKey(caTCert.getX509Certificate(), caKey.getPrivateKey());

        X509Certificate signedCert = certManager.signCertificate(bytes, caCert, type, DEFAULT_VALIDITY);
        certRepository.save(new TakCert(signedCert, getHttpUser(), getHttpUser(), new Date()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<String>(Util.certToPEM(signedCert, true), headers, HttpStatus.OK);
    }
*/
    private CertificateConfig getCertificateConfig() {
        try {
            CertificateSigning certificateSigningConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getCertificateSigning();
            if (certificateSigningConfig == null) {
                throw new TakException("CertificateSigning element not found in CoreConfig!");
            }

            CertificateConfig config = certificateSigningConfig.getCertificateConfig();
            if (config == null) {
                throw new TakException("CertificateConfig element not found in CoreConfig!");
            }

            return  config;

        } catch (Exception e) {
            logger.error("exception in getCertificateConfig!", e);
            return null;
        }
    }

    @RequestMapping(value = "/tls/config", method = RequestMethod.GET)
    ResponseEntity<String> getConfig()
            throws Exception {

        try {
            CertificateConfig config = getCertificateConfig();
            if (config == null) {
                throw new TakException("getCertificateConfig failed!");
            }

            // instantiate a jaxb context to serialize out the configuration
            JAXBContext jc = JAXBContext.newInstance(CertificateConfig.class);
            Marshaller marshaller = jc.createMarshaller();
            StringWriter writer = new StringWriter();

            // create a root node (not present in the xsd generated classes)
            QName qName = new QName("com.bbn.marti.config", "certificateConfig");
            JAXBElement<CertificateConfig> root = new JAXBElement<>(qName, CertificateConfig.class, config);

            // serialize and return the certificate configuration
            marshaller.marshal(root, writer);
            String xml = writer.toString();
            return ResponseEntity.ok().body(xml);

        } catch (Exception e) {
            logger.error("Exception in getConfig!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/tls/signClient", method = RequestMethod.POST)
    ResponseEntity<byte[]> signClientCert(
            @RequestParam(value = "clientUid", defaultValue = "") String clientUid,
            @RequestParam(value = "version", required = false) String version,
            @RequestBody String base64CSR)
            throws Exception {

        try {
            // TAK 4.4 clients that support Channels will pass in the version parameter
            TakCert cert = certManagerService.signClient(clientUid, version != null, base64CSR);
            if (cert == null) {
                throw new TakException("signClient returned null!");
            }

            takCertRepository.save(cert);

            //
            // package up the signed cert and the ca into a keystore and return to the client
            //
            KeyStore keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(null, null);
            keyStore.setCertificateEntry("signedCert", cert.getX509Certificate());

            int ndx = 0;
            for (X509Certificate ca : cert.getX509CertificateChain()) {
                keyStore.setCertificateEntry("ca" + ndx++, ca);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            keyStore.store(bos, DEFAULT_PASSWORD.toCharArray());
            byte[] p12 = bos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(p12, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Exception in signClient!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/tls/signClient/v2", method = RequestMethod.POST)
    ResponseEntity<String> signClientCertV2(
            @RequestParam(value = "clientUid", defaultValue = "") String clientUid,
            @RequestParam(value = "version", required = false) String version,
            @RequestBody String base64CSR,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {

        try {
            TakCert cert = certManagerService.signClient(clientUid, version != null, base64CSR);
            if (cert == null) {
                throw new TakException("signClient returned null!");
            }

            takCertRepository.save(cert);

            String accept = request.getHeader("Accept");
            if (accept != null) {
                accept = accept.toLowerCase();
            }

            String result = "";
            if (accept == null ||
                    (accept.contains("*/*") || accept.contains("application/json") || accept.length() == 0)) {

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode rootNode = mapper.createObjectNode();

                String clientCertPem = Util.certToPEM(cert.getX509Certificate(), false);
                rootNode.put("signedCert", clientCertPem);

                int ndx = 0;
                for (X509Certificate ca : cert.getX509CertificateChain()) {
                    String caPem = Util.certToPEM(ca, false);
                    rootNode.put("ca" + ndx++, caPem);
                }

                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                response.addHeader("Content-Type", "application/json");

            } else if (accept.contains("application/xml")) {

                StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                xml.append("<enrollment>");
                xml.append("<signedCert>");
                xml.append(Util.certToPEM(cert.getX509Certificate(), false));
                xml.append("</signedCert>");
                for (X509Certificate ca : cert.getX509CertificateChain()) {
                    xml.append("<ca>");
                    xml.append(Util.certToPEM(ca, false));
                    xml.append("</ca>");
                }
                xml.append("</enrollment>");

                result = xml.toString();
                response.addHeader("Content-Type", "application/xml");

            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            return new ResponseEntity<String>(result, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Exception in signClientCertV2!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    protected String getHttpUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
