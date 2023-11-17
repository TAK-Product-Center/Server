package com.bbn.marti.citrap;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Document;

import com.bbn.marti.citrap.reports.ReportType;
import com.bbn.marti.citrap.reports.ReportsType;
import com.bbn.marti.config.Citrap;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.SecureXmlParser;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

public class CITrapReportService {

    private static final String REPORTMATCH = "<report";
    private static final String CI_TRAP_MISSION = "citrap";
    
    private static final Logger logger = LoggerFactory.getLogger(CITrapReportService.class);
    
    @Autowired
    private com.bbn.marti.citrap.PersistenceStore persistenceStore;
    
    @Autowired
    private CITrapReportNotifications ciTrapReportNotifications;

    public static ReportType deserializeReport(String reportTypeXml) throws JAXBException {
        Document doc = SecureXmlParser.makeDocument(reportTypeXml);
        JAXBContext jaxbContext = JAXBContext.newInstance(ReportType.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ReportType report = (ReportType) jaxbUnmarshaller.unmarshal(doc);
        return report;
    }

    public static String serializeReportAsXml(ReportType report) throws  JAXBException {
        JAXBContext jc = JAXBContext.newInstance(ReportType.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(report, writer);
        return writer.toString();
    }

    public String serializeReportsAsXml(List<ReportType> reports) throws  JAXBException {
        ReportsType reportsType = new ReportsType();
        reportsType.getReport().addAll(reports);
        JAXBContext jc = JAXBContext.newInstance(ReportsType.class);
        Marshaller marshaller = jc.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(reportsType, writer);
        return writer.toString();
    }

    public static JSONObject createJsonObjectFromReport(ReportType report) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", report.getId());
        jsonObject.put("type", report.getType());
        jsonObject.put("title", report.getTitle());
        jsonObject.put("userCallsign", report.getUserCallsign());
        jsonObject.put("userDescription", report.getUserDescription());
        jsonObject.put("dateTime", report.getDateTime());
        jsonObject.put("dateTimeDescription", report.getDateTimeDescription());
        jsonObject.put("location", report.getLocation());
        jsonObject.put("locationDescription", report.getLocationDescription());
        jsonObject.put("eventScale", report.getEventScale());
        jsonObject.put("importance", report.getImportance());
        if (report.getStatus() != null) {
            jsonObject.put("status", report.getStatus());
        }
        return jsonObject;
    }

    public String serializeReportsAsJson(List<ReportType> reports) {
        JSONArray jsonArray = new JSONArray();
        for (ReportType report : reports) {
            JSONObject jsonReport = CITrapReportService.createJsonObjectFromReport(report);
            jsonArray.add(jsonReport);
        }
        return jsonArray.toJSONString();
    }

    public boolean reportExists(String id, String groupVector) throws Exception {
        com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);
        return persistenceStore.getReportAttrString(id, "uid", groupVector) != null &&
                syncStore.getContentByUid(id, groupVector) != null;
    }

    public ReportType addReport(
            byte[] reportMP, String clientUid, String groupVector,
            MissionService missionService,
            SubscriptionManagerLite subscriptionManager,
            NavigableSet<Group> groups,
            Citrap config) throws Exception {
        //
        // extract out the report from the mission package
        //
        String contents = CITrapReportService.getReportXmlFromReportMp(reportMP);
        if (contents == null) {
            logger.error("addReport: getReportXmlFromReportMp failed!");
            return null;
        }

        ReportType report = deserializeReport(contents);

        // create a new id if we need to
        if (report.getId() == null) {
            // create a new uid for the report if we need to
            String uid = UUID.randomUUID().toString();
            report.setId(uid);
        }
        else if (reportExists(report.getId(), groupVector)) {
            return updateReport(reportMP, clientUid, groupVector, report.getId(), missionService);
        }

        //
        // serialize the report back out to ensure that the xml has the id
        //
        contents = serializeReportAsXml(report);
        reportMP = replaceReportInMp(contents.getBytes(), reportMP);

        if (persistenceStore.createReport(report, contents, groupVector) == 0) {
            logger.error("addReport: createReport failed!");
            return null;
        }

        Metadata metadata = addReportToEnterpriseSync(reportMP, groupVector, report.getId());
        if (metadata == null) {
            logger.error("addReport: addReportToEnterpriseSync failed!");
            return null;
        }

        // add the report to the top level ci-trap mission
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());
        missionService.addMissionContent(CI_TRAP_MISSION, content, clientUid, groupVector);

        // create a new mission for this report and add the report
        missionService.createMission(report.getId(), clientUid, groupVector, null, null, null, null, null, null,
                CI_TRAP_MISSION, null, null, null, null, false);
        missionService.addMissionContent(report.getId(), content, clientUid, groupVector);

        // subscribe for notifications to the new report
        try {
            missionService.missionSubscribe(report.getId(), clientUid, groupVector);
        } catch (JpaSystemException e) { } // DuplicateKeyException comes through as JpaSystemException due to transaction

        if (config != null && config.isEnableNotifications()) {
            // notify active users, within range, who are not part of the top level mission that there is a new report
            ciTrapReportNotifications.notifyNonMissionSubscribersWithinRange(
                    groupVector,
                    report,
                    CI_TRAP_MISSION,
                    metadata.getLongitude(),
                    metadata.getLatitude(),
                    subscriptionManager,
                    groups,
                    config);
        }

        return report;
    }

    public ReportType updateReport(
            byte[] reportMP, String clientUid, String groupVector, String id,
            MissionService missionService) throws Exception {
        //
        // extract out the report from the mission package
        //
        String contents = CITrapReportService.getReportXmlFromReportMp(reportMP);
        if (contents == null) {
            logger.error("updateReport: getReportXmlFromReportMp failed!");
            return null;
        }

        ReportType report = deserializeReport(contents);

        // if the report doesn't have an id, use the one from the request
        if (report.getId() == null) {
            report.setId(id);
        }

        // make sure the report ids match
        if (!id.equalsIgnoreCase(report.getId())) {
            logger.error("updateReport: attempt to update a report with a mismatched id!");
            return null;
        }

        if (persistenceStore.updateReport(report, contents, groupVector) == 0) {
            logger.error("updateReport: updateReport failed!");
            return null;
        }

        Metadata metadata = addReportToEnterpriseSync(reportMP, groupVector, id);
        if (metadata == null) {
            logger.error("updateReport: addReportToEnterpriseSync failed!");
            return null;
        }

        //
        // mark the report as updated on the report specific mission only. this will only notify users that
        // have retrieved this report already.
        //
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());
        missionService.addMissionContent(report.getId(), content, clientUid, groupVector);
        return report;
    }

    public boolean deleteReport(
            String id, String clientUid, String groupVector, MissionService missionService) throws  Exception {

        // delete the mission package from enterprise sync
        if (!deleteReportFromEnterpriseSync(id, groupVector)) {
            logger.error("deleteReport: deleteReportFromEnterpriseSync failed!");
        }

        // delete the report from the ci_trap database
        if (persistenceStore.deleteReport(id, groupVector) == 0) {
            logger.error("deleteReport: deleteReport failed!");
        }

        // remove the report from the top level ci-trap mission and delete the entire report specific mission
        missionService.deleteMissionContent(CI_TRAP_MISSION, null, id, clientUid, groupVector);
        missionService.deleteMission(id, clientUid, groupVector, true);
        return true;
    }

    private Metadata addReportToEnterpriseSync(byte[] reportMP, String groupVector, String id) throws Exception {
        // get the centroid of the location
        Double[] lonlat = persistenceStore.getCentroid(id);

        //
        // build up the metadata for adding to enterprise sync
        //
        Metadata toStore = new Metadata();
        toStore.set(Metadata.Field.Longitude, lonlat[0].toString());
        toStore.set(Metadata.Field.Latitude, lonlat[1].toString());
        toStore.set(Metadata.Field.Keywords, "citrap");
        toStore.set(Metadata.Field.DownloadPath, id + ".zip");
        toStore.set(Metadata.Field.Name, id);
        toStore.set(Metadata.Field.MIMEType, "application/zip");
        toStore.set(Metadata.Field.UID, new String[]{ id });
        // Get the user name from the request
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        if (userName != null) {
            toStore.set(Metadata.Field.SubmissionUser, userName);
        }

        //
        // add mission package to enterprise sync
        //
        com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);

        Metadata metadata = syncStore.insertResource(toStore, reportMP, groupVector);

        return metadata;
    }

    private static boolean deleteReportFromEnterpriseSync(String id, String groupVector) throws Exception {
        com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);

        List<Metadata> metadata = syncStore.getMetadataByUid(id, groupVector);
        if (metadata == null || metadata.size() == 0) {
            return false;
        }

        List<Integer> pks = new LinkedList<>();
        for (Metadata nextMeta : metadata) {
            pks.add(nextMeta.getPrimaryKey());
        }

        syncStore.delete(pks, groupVector);
        return true;
    }

    private static void copyZipInputStreamToOutputStream(ZipInputStream zis, OutputStream os) throws  IOException {
        int count;
        final int BUFFER = 2048;
        byte data[] = new byte[BUFFER];
        final byte[] BOM = DatatypeConverter.parseHexBinary("efbbbf");

        // skip the bom header
        byte header[] = new byte[3];
        if ((count = zis.read(header, 0, BOM.length)) != -1) {
            if (!Arrays.equals(header, BOM)) {
                os.write(header, 0, count);
            }
        }

        while ((count = zis.read(data, 0, BUFFER)) != -1) {
            os.write(data, 0, count);
        }
    }

    protected static String getReportXmlFromReportMp(byte[] reportMp) throws Exception {
        String reportXml = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(reportMp);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));

        // iterate across file in the zip archive
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {

            // only look at xml files
            if (!entry.getName().endsWith(".xml")) {
                continue;
            }

            // extract the contents
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copyZipInputStreamToOutputStream(zis, bos);
            String fileContents = new String(bos.toByteArray(), "UTF-8");
            bos.flush();
            bos.close();

            // bail if we find the report element
            if (fileContents.contains(REPORTMATCH))
            {
                reportXml = fileContents;
                break;
            }
        }

        zis.close();
        return reportXml;
    }

    protected static byte[] replaceReportInMp(byte[] report, byte[] mp) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(mp);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));

        String originalFilename = "TrapReport.xml";

        // iterate across file in the zip archive
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {

            // copy all non xml files over to the new archive
            if (!entry.getName().endsWith(".xml")) {
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(newEntry);
                copyZipInputStreamToOutputStream(zis, zos);
                zos.closeEntry();
                continue;
            }

            // extract the contents of the xml file
            ByteArrayOutputStream bosXml = new ByteArrayOutputStream();
            copyZipInputStreamToOutputStream(zis, bosXml);
            String fileContents = new String(bosXml.toByteArray(), "UTF-8");
            bosXml.flush();
            bosXml.close();

            // dont copy over the old report
            if (fileContents.contains(REPORTMATCH)) {
                originalFilename = entry.getName();
                continue;
            }

            // copy over any other non report xml
            ZipEntry newEntry = new ZipEntry(entry.getName());
            zos.putNextEntry(newEntry);
            zos.write(fileContents.getBytes());
            zos.closeEntry();
        }
        zis.close();
        bis.close();

        if (originalFilename == null) {
            logger.error("error in replaceReportInMp, report not found");
        }

        // add the new report
        ZipEntry newEntry = new ZipEntry(originalFilename);
        zos.putNextEntry(newEntry);
        zos.write(report);
        zos.closeEntry();

        zos.finish();
        zos.close();
        bos.flush();
        bos.close();
        return bos.toByteArray();
    }

    protected static byte[] addAttachmentToMp(String filename, byte[] contents, byte[] mp) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(mp);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bis));

        // copy all files over to the new mp
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            ZipEntry newEntry = new ZipEntry(entry.getName());
            zos.putNextEntry(newEntry);
            copyZipInputStreamToOutputStream(zis, zos);
            zos.closeEntry();
        }
        zis.close();
        bis.close();

        // add the new report
        ZipEntry newEntry = new ZipEntry(filename);
        zos.putNextEntry(newEntry);
        zos.write(contents);
        zos.closeEntry();

        zos.finish();
        zos.close();
        bos.flush();
        bos.close();
        return bos.toByteArray();
    }

    protected static String getFilename(String contentDisposition) {
        String[] filenameKv = contentDisposition.split("=");
        if (filenameKv == null || filenameKv.length != 2) {
            return null;
        }
        String filename = filenameKv[1];
        return filename;
    }
}
