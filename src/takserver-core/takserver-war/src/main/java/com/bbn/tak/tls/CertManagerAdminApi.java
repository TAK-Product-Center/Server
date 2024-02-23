package com.bbn.tak.tls;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.bbn.marti.config.CertificateSigning;
import com.bbn.marti.config.TAKServerCAConfig;
import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.tak.tls.repository.TakCertRepository;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tak.server.Constants;


@RestController
public class CertManagerAdminApi extends BaseRestController {

    public static final Logger logger = LoggerFactory.getLogger(CertManagerAdminApi.class);

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private TakCertRepository takCertRepository;

    @Autowired
    SubscriptionManagerLite subscriptionManager;

    @RequestMapping(value = "/certadmin/cert", method = RequestMethod.GET)
    ApiResponse<List<TakCert>> getAll(@RequestParam(value = "username", required = false) String username) throws Exception {
        try {
            List<TakCert> certs;

            if (username == null) {
                certs = takCertRepository.findAll(Sort.by(Sort.Direction.ASC, "clientUid", "id"));
            } else {
                certs = takCertRepository.findAllByUserDn(username);
            }

            return new ApiResponse<List<TakCert>>(Constants.API_VERSION, TakCert.class.getSimpleName(), certs);
        } catch (Exception e) {
            logger.error("exception in getAll!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/active", method = RequestMethod.GET)
    ApiResponse<List<TakCert>> getActive() throws Exception {
        try {
            List<TakCert> certs = takCertRepository.getActive();
            return new ApiResponse<List<TakCert>>(Constants.API_VERSION, TakCert.class.getSimpleName(), certs);
        } catch (Exception e) {
            logger.error("exception in getActive!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/replaced", method = RequestMethod.GET)
    ApiResponse<List<TakCert>> getReplaced() throws Exception {
        try {
            List<TakCert> certs = takCertRepository.getReplaced();
            return new ApiResponse<List<TakCert>>(Constants.API_VERSION, TakCert.class.getSimpleName(), certs);
        } catch (Exception e) {
            logger.error("exception in getReplaced!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/expired", method = RequestMethod.GET)
    ApiResponse<List<TakCert>> getExpired() throws Exception {
        try {
            List<TakCert> certs = takCertRepository.findAllByExpirationDateIsLessThanEqual(new Date());
            return new ApiResponse<List<TakCert>>(Constants.API_VERSION, TakCert.class.getSimpleName(), certs);
        } catch (Exception e) {
            logger.error("exception in getExpired!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/revoked", method = RequestMethod.GET)
    ApiResponse<List<TakCert>> getRevoked() throws Exception {
        try {
            List<TakCert> certs = takCertRepository.findAllByRevocationDateIsLessThanEqual(new Date());
            return new ApiResponse<List<TakCert>>(Constants.API_VERSION, TakCert.class.getSimpleName(), certs);
        } catch (Exception e) {
            logger.error("exception in getRevoked!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/download/{ids}", method = RequestMethod.GET)
    ResponseEntity<byte[]> downloadCertificates(
            @PathVariable("ids") String ids) throws IOException {
        try {
            List<Long> certIds = new ArrayList<>();
            for (String id : Arrays.asList(ids.split(","))) {
                certIds.add(Long.parseLong(id));
            }

            List<TakCert> certs = takCertRepository.findAllById(certIds);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(baos);
            ZipOutputStream zos = new ZipOutputStream(bos);

            Integer ndx = 1;
            for (TakCert cert : certs) {
                zos.putNextEntry(new ZipEntry((ndx++).toString() + "_" + cert.getUserDn() + "_ClientCert.pem"));
                zos.write(cert.getCertificate().getBytes());
                zos.closeEntry();
            }
            zos.close();

            response.setContentType("application/zip");
            response.setContentLength(baos.size());

            String date = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date());
            String filename = "ClientCertExport_" + date + ".zip";
            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + filename);

            return new ResponseEntity<byte[]>(baos.toByteArray(), HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in downloadCertificates!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/delete/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity deleteCertificates(
            @PathVariable("ids") String ids) throws IOException {
        try {
            List<Long> certIds = new ArrayList<>();
            for (String id : Arrays.asList(ids.split(","))) {
                certIds.add(Long.parseLong(id));
            }

            List<Long> temp = takCertRepository.deleteByIds(certIds);

            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in deleteCertificates!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/revoke/{ids}", method = RequestMethod.DELETE)
    public ResponseEntity revokeCertificates(
            @PathVariable("ids") String ids) throws IOException {
        try {
            for (String id : Arrays.asList(ids.split(","))) {

                TakCert cert = takCertRepository.findOneById(Long.decode(id));
                if (cert == null) {
                    logger.error("unable to find cert for " + id);
                    continue;
                }

                takCertRepository.revokeByHash(cert.getHash());

                revokeCertificate(cert);
            }

            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in revokeCertificates!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/{hash}", method = RequestMethod.GET)
    ApiResponse<TakCert> getCertificate(
            @PathVariable("hash") @NotNull String hash) throws IOException{
        try {
            TakCert cert = takCertRepository.findOneByHash(hash);

            if (cert == null) {
                throw new NotFoundException();
            }

            return new ApiResponse<TakCert>(Constants.API_VERSION, TakCert.class.getSimpleName(), cert);
        } catch (Exception e) {
            logger.error("exception in getCertificate!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/certadmin/cert/{hash}/download", method = RequestMethod.GET)
    ResponseEntity<String> downloadCertificate(
            @PathVariable("hash") @NotNull String hash) throws IOException {
        try {
            TakCert cert = takCertRepository.findOneByHash(hash);

            if (cert == null) {
                throw new NotFoundException();
            }

            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + cert.getUserDn() + "_ClientCert.pem");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            return new ResponseEntity<String>(cert.getCertificate(), headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in downloadCertificate!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    private void revokeCertificate(TakCert cert) throws IOException, InterruptedException {
        try {
            //
            // save the pem file to a temporary directory
            //
            File tempFile = File.createTempFile("revoke-", ".pem");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempFile));
            bufferedWriter.write(cert.getCertificate());
            bufferedWriter.flush();
            bufferedWriter.close();

            //
            // grab the CertificateSigning config element
            //
            CertificateSigning certificateSigning = CoreConfigFacade.getInstance().getRemoteConfiguration().getCertificateSigning();
            if (certificateSigning == null) {
                throw new TakException("Couldn't find CertificateSigning in CoreConfig.xml!");
            }

            //
            // grab the TAKServerCAConfig config element
            //
            TAKServerCAConfig takServerCAConfig = certificateSigning.getTAKServerCAConfig();
            if (takServerCAConfig == null) {
                throw new TakException("Couldn't find TAKServerCAConfig in CoreConfig.xml!");
            }

            //
            // build up the call to revokeCert.sh
            //
            StringBuilder revokeCmd = new StringBuilder("./revokeCert.sh  ");
            revokeCmd.append(tempFile.getCanonicalPath().replace(".pem", ""));
            revokeCmd.append(" ");
            revokeCmd.append(takServerCAConfig.getCAkey());
            revokeCmd.append(" ");
            revokeCmd.append(takServerCAConfig.getCAcertificate());

            if (logger.isDebugEnabled()) {
                logger.debug("revoking certificate : " + revokeCmd.toString());
            }

            //
            // execute the revoke call in /opt/tak/certs and wait for it to complete
            //
            Process process = Runtime.getRuntime().exec(
                    revokeCmd.toString(), null, new File("/opt/tak/certs/"));
            process.waitFor();

            //
            // cleanup the temp pem file
            //
            tempFile.delete();

        } catch (Exception e) {
            logger.error("exception calling revokeCert.sh!", e);
        }

        try {
            subscriptionManager.deleteSubscriptionssByCertificate(cert.getX509Certificate());
        } catch (Exception e) {
            logger.error("exception disconnecting users!", e);
        }

    }

    @RequestMapping(value = "/certadmin/cert/{hash}", method = RequestMethod.DELETE)
    ApiResponse<TakCert> revokeCertificate(
            @PathVariable("hash") @NotNull String hash) throws IOException {
        try {
            TakCert cert = takCertRepository.findOneByHash(hash);
            if (cert == null) {
                throw new NotFoundException();
            }

            takCertRepository.revokeByHash(hash);

            revokeCertificate(cert);

            return new ApiResponse<TakCert>(Constants.API_VERSION, TakCert.class.getSimpleName(), cert);

        } catch (Exception e) {
            logger.error("exception in revokeCertificate!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }
}
