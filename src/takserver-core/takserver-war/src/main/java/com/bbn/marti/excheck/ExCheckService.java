package com.bbn.marti.excheck;


import java.io.StringReader;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.xml.sax.InputSource;

import com.bbn.marti.excheck.checklist.Checklist;
import com.bbn.marti.excheck.checklist.ChecklistColumn;
import com.bbn.marti.excheck.checklist.ChecklistColumnType;
import com.bbn.marti.excheck.checklist.ChecklistColumns;
import com.bbn.marti.excheck.checklist.ChecklistDetails;
import com.bbn.marti.excheck.checklist.ChecklistStatus;
import com.bbn.marti.excheck.checklist.ChecklistTask;
import com.bbn.marti.excheck.checklist.ChecklistTaskStatus;
import com.bbn.marti.excheck.checklist.ChecklistTasks;
import com.bbn.marti.excheck.checklist.Missions;
import com.bbn.marti.excheck.checklist.StatusCount;
import com.bbn.marti.excheck.checklist.Templates;
import com.bbn.marti.network.ContactManagerService;
import com.bbn.marti.remote.exception.ForbiddenException;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.exception.UnauthorizedException;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.Metadata;
import com.bbn.marti.sync.model.*;
import com.bbn.marti.sync.service.MissionTokenUtils;
import com.bbn.marti.sync.repository.MissionRepository;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.spring.SpringContextBeanForApi;

import tak.server.Constants;

public class ExCheckService {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.marti.excheck.ExCheckService.class);
    public final static String EXCHECK_TEMPLATES_MISSION = "exchecktemplates";
    public final static String EXCHECK_TOOL = "ExCheck";
    private final static String CSV_IGNORE_COMMAS_IN_DOUBLEQUOTES = ",(?=(?:(?:[^\"]*\"){2})*[^\"]*$)";

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private ContactManagerService contactManagerService;

    @Autowired
    private SubscriptionManagerLite subscriptionManager;

    @Autowired
    private DataSource ds;

    private ConcurrentHashMap<Class, JAXBContext> jaxbContextMap = new ConcurrentHashMap<>();

    private JAXBContext getJaxbContext(Class c) {
        try {
            JAXBContext jaxbContext = jaxbContextMap.get(c);
            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(c);
                jaxbContextMap.put(c, jaxbContext);
            }

            return jaxbContext;
        } catch (JAXBException e) {
            logger.error("Exception in getJaxbContext!", e);
            return null;
        }
    }

    public <T> String toXml(T object) {
        try {
            JAXBContext jc = getJaxbContext(object.getClass());
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(object, writer);
            return writer.toString();
        } catch (JAXBException e) {
            logger.error("Exception in toXml!", e);
            return null;
        }
    }

    public Checklist checklistFromXml(String xml) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(),
                    new InputSource(new StringReader(xml)));

            JAXBContext jaxbContext = getJaxbContext(Templates.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Checklist checklist = (Checklist) jaxbUnmarshaller.unmarshal(xmlSource);

            return checklist;
        } catch (Exception e) {
            logger.error("Exception in fromXml!", e);
            return null;
        }
    }

    public ChecklistTask checklistTaskFromXml(String xml) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(),
                    new InputSource(new StringReader(xml)));

            JAXBContext jaxbContext = getJaxbContext(ChecklistTask.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            ChecklistTask task = (ChecklistTask) jaxbUnmarshaller.unmarshal(xmlSource);

            return task;
        } catch (Exception e) {
            logger.error("Exception in fromXml!", e);
            return null;
        }
    }

    private List<ChecklistColumn> parseHeaderRow(String header) {
        List<ChecklistColumn> checklistColumns = new ArrayList<>();

        int ndx = 0;
        for (String columnName : Arrays.asList(header.split(CSV_IGNORE_COMMAS_IN_DOUBLEQUOTES, -1))) {
            if (ndx++ == 0) {
                continue;
            }

            ChecklistColumn col = new ChecklistColumn();
            columnName = columnName.replaceAll("\",\"", ",");
            col.setColumnName(columnName.trim());
            checklistColumns.add(col);
        }
        return checklistColumns;
    }

    private ChecklistTask parseChecklistTask(String row, int taskSize) {
        ChecklistTask checklistTask = new ChecklistTask();
        checklistTask.setUid(UUID.randomUUID().toString());

        // split the row on the commas, add the -1 to accept empty strings
        int ndx = 0;
        for (String task : Arrays.asList(row.split(CSV_IGNORE_COMMAS_IN_DOUBLEQUOTES, -1))) {
            if (ndx++ == 0) {
                for (String nextFormatKeyVal : Arrays.asList(task.split("\\|"))) {
                    String[] formatKeyVal = nextFormatKeyVal.split("=");
                    if (formatKeyVal[0].equalsIgnoreCase("break")) {
                        checklistTask.setLineBreak(Boolean.parseBoolean(formatKeyVal[1]));
                    } else if (formatKeyVal[0].equalsIgnoreCase("bgcolor")) {
                        checklistTask.setBgColor(formatKeyVal[1]);
                    }
                }
            } else {
                String value = task.trim();
                if (value.length() == 0) {
                    value = " ";
                } else {
                    value = value.replaceAll("\",\"", ",");
                }
                checklistTask.getValue().add(value);
                if (checklistTask.getValue().size() == taskSize) {
                    break;
                }
            }
        }

        int size = checklistTask.getValue().size();
        if (size < taskSize) {
            for (int i = 0; i < taskSize - size; i++) {
                checklistTask.getValue().add("");
            }
        }

        return checklistTask;
    }

    private List<ChecklistColumn> parseFormatRow(String formatRow, List<ChecklistColumn> header) {
        List<String> formatCols = new ArrayList(Arrays.asList(formatRow.split(CSV_IGNORE_COMMAS_IN_DOUBLEQUOTES, -1)));

        formatCols.remove(0);

        if (formatCols.size() != header.size()) {
            return null;
        }

        int ndx = 0;
        for (String nextFormatCol : formatCols) {
            ChecklistColumn nextCol = header.get(ndx);
            for (String nextFormatKeyVal : Arrays.asList(nextFormatCol.split("\\|"))) {
                String[] formatKeyVal = nextFormatKeyVal.split("=");
                if (formatKeyVal[0].equalsIgnoreCase("type")) {
                    nextCol.setColumnType(ChecklistColumnType.fromValue(formatKeyVal[1]));
                } else if (formatKeyVal[0].equalsIgnoreCase("width")) {
                    nextCol.setColumnWidth(Integer.parseInt(formatKeyVal[1]));
                } else if (formatKeyVal[0].equalsIgnoreCase("bgcolor")) {
                    nextCol.setColumnBgColor(formatKeyVal[1]);
                } else if (formatKeyVal[0].equalsIgnoreCase("textcolor")) {
                    nextCol.setColumnTextColor(formatKeyVal[1]);
                } else if (formatKeyVal[0].equalsIgnoreCase("editable")) {
                    nextCol.setColumnEditable(Boolean.valueOf(formatKeyVal[1]));
                }
            }
            ndx++;
        }

        return header;
    }

    public Checklist copyNotes(Checklist checklist) {

        // did the user include a Notes column?
        int notesNdx = -1;
        for (int i = 0; i < checklist.getChecklistColumns().getChecklistColumn().size(); i++) {
            if (checklist.getChecklistColumns().getChecklistColumn()
                    .get(i).getColumnName().compareToIgnoreCase("notes") == 0) {
                notesNdx = i;
                break;
            }
        }

        // bail if no notes to copy over
        if (notesNdx == -1) {
            return checklist;
        }

        // remove notes column added by the user
        checklist.getChecklistColumns().getChecklistColumn().remove(notesNdx);

        // iterate over the tasks, copying any preseeded notes into the notes field
        for (ChecklistTask task : checklist.getChecklistTasks().getChecklistTask()) {
            String notes = task.getValue().get(notesNdx);
            if (notes != null && notes.length() > 0) {
                task.setNotes(notes);
            }

            // remove the notes column added by the user
            task.getValue().remove(notesNdx);
        }

        return checklist;
    }

    public Checklist parseTemplate(String templateCsv) {
        Checklist checklist = new Checklist();

        // assign a uid for the template
        checklist.setChecklistDetails(new ChecklistDetails());
        checklist.getChecklistDetails().setUid(UUID.randomUUID().toString());

        templateCsv = templateCsv.replaceAll("\r", "");
        String[] rows = templateCsv.split("\n", -1);
        if (rows.length < 3) {
            return null;
        }

        // parse the labels from the header row
        List<ChecklistColumn> header = parseHeaderRow(rows[0]);

        // parse the format instructions, add them to the header row
        header = parseFormatRow(rows[1], header);

        // create the ChecklistColumns and add them to the header
        ChecklistColumns checklistColumns = new ChecklistColumns();
        checklistColumns.getChecklistColumn().addAll(header);
        checklist.setChecklistColumns(checklistColumns);

        // parse the remainder of the rows
        ChecklistTasks checklistTasks = new ChecklistTasks();
        for (int i = 2; i < rows.length; i++) {
            if (rows[i].length() == 0) {
                continue;
            }

            ChecklistTask checklistTask = parseChecklistTask(rows[i], header.size());
            checklistTasks.getChecklistTask().add(checklistTask);
        }
        checklist.setChecklistTasks(checklistTasks);

        checklist = copyNotes(checklist);

        return checklist;
    }

    public void addNewTemplate(Checklist checklist, String uid, String name, String description,
                               String clientUid, String callsign, String groupVector) {

        Checklist existing = getTemplateFromESync(uid, groupVector);
        if (existing != null) {
            throw new UnauthorizedException("attempt to add template that already exists!");
        }

        if (checklist.getChecklistDetails() == null) {
            checklist.setChecklistDetails(new ChecklistDetails());
        }

        checklist.getChecklistDetails().setCreatorUid(clientUid);
        checklist.getChecklistDetails().setCreatorUid(callsign);
        checklist.getChecklistDetails().setTemplateName(name);
        checklist.getChecklistDetails().setDescription(description);

        if (checklist.getChecklistTasks() != null) {
            for (ChecklistTask checklistTask : checklist.getChecklistTasks().getChecklistTask()) {
                if (isEmptyUid(checklistTask.getUid())) {
                    checklistTask.setUid(UUID.randomUUID().toString());
                }
            }
        }

        // add the template to esync
        String xml = toXml(checklist);

        List<String> keywords = new ArrayList<String>();
        keywords.add(name);
        keywords.add(description);
        keywords.add(callsign);
        Metadata metadata = addToEnterpriseSync(xml.getBytes(), groupVector, uid, keywords);

        // add the template to the excheck templates mission
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());
        missionService.addMissionContent(EXCHECK_TEMPLATES_MISSION, content, clientUid, groupVector);
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public Checklist getTemplateFromESync(String uid, String groupVector) {
        try {
            // pull the mission package from enterprise sync so we can update it
            com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);
            byte[] templateBytes = syncStore.getContentByUid(uid, groupVector);
            if (templateBytes != null) {
                return checklistFromXml(new String(templateBytes));
            }
        } catch (Exception e) {
            logger.error("Exception in getTemplatesFromESync!", e);
        }

        return null;
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public ChecklistTask getTaskFromESync(String uid, String groupVector) {
        try {
            // pull the mission package from enterprise sync so we can update it
            com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);
            byte[] taskBytes = syncStore.getContentByUid(uid, groupVector);
            if (taskBytes != null) {
                return checklistTaskFromXml(new String(taskBytes));
            }
        } catch (Exception e) {
            logger.error("Exception in getTaskFromESync!", e);
        }

        return null;
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public ChecklistTask getTaskAtTimeFromESync(String uid, Date date, String groupVector) {
        try {
            // pull the mission package from enterprise sync so we can update it
            com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);
            byte[] taskBytes = syncStore.getContentByUidAndMaxTime(uid, date.getTime(), groupVector);
            if (taskBytes != null) {
                return checklistTaskFromXml(new String(taskBytes));
            }
        } catch (Exception e) {
            logger.error("Exception in getTaskFromESync!", e);
        }

        return null;
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public ChecklistTask getTaskFromESyncByHash(String hash, String groupVector) {
        try {
            // pull the mission package from enterprise sync so we can update it
            com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);
            byte[] taskBytes = syncStore.getContentByOldHash(hash, groupVector);
            if (taskBytes != null) {
                return checklistTaskFromXml(new String(taskBytes));
            }
        } catch (Exception e) {
            logger.error("Exception in getTaskFromESyncByHash!", e);
        }

        return null;
    }

    public Metadata addToEnterpriseSync(byte[] content, String groupVector, String id, List<String> keywords, Date submissionTime) {
        try {
            //
            // build up the metadata for adding to enterprise sync
            //
            Metadata toStore = new Metadata();
            toStore.set(Metadata.Field.Keywords, keywords.toArray(new String[0]));
            toStore.set(Metadata.Field.DownloadPath, id + ".xml");
            toStore.set(Metadata.Field.Name, id);
            toStore.set(Metadata.Field.MIMEType, "application/xml");
            toStore.set(Metadata.Field.UID, new String[]{id});
            toStore.set(Metadata.Field.Tool, EXCHECK_TOOL);

            if (submissionTime != null) {
                SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
                sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
                toStore.set(Metadata.Field.SubmissionDateTime, sdf.format(submissionTime));
            }

            // Get the user name from the request
            String userName = SecurityContextHolder.getContext().getAuthentication().getName();
            if (userName != null) {
                toStore.set(Metadata.Field.SubmissionUser, userName);
            }

            //
            // add mission package to enterprise sync
            //
            com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(com.bbn.marti.sync.EnterpriseSyncService.class);

            Metadata metadata = syncStore.insertResource(toStore, content, groupVector);
            return metadata;

        } catch (Exception e) {
            logger.error("Exception in getTemplatesFromESync!", e);
            return null;
        }
    }

    public Metadata addToEnterpriseSync(byte[] content, String groupVector, String id, List<String> keywords) {
        return addToEnterpriseSync(content, groupVector, id, keywords, null);
    }


    public String startChecklist(String id, String clientUid, String callsign,
                                 String name, String description, String startTime, String groupVector) {

        Checklist checklist = getTemplateFromESync(id, groupVector);
        if (checklist == null) {
            throw new NotFoundException();
        }

        if (checklist.getChecklistDetails() == null) {
            checklist.setChecklistDetails(new ChecklistDetails());
        }

        // create a new uid for this instance of the checklist (can be multiple executing simultaneously)
        String checklistId = UUID.randomUUID().toString();
        checklist.getChecklistDetails().setUid(checklistId);
        checklist.getChecklistDetails().setName(name);
        checklist.getChecklistDetails().setDescription(description);
        checklist.getChecklistDetails().setStartTime(startTime);
        checklist.getChecklistDetails().setCreatorUid(clientUid);
        checklist.getChecklistDetails().setCreatorCallsign(callsign);

        // set new uids for each of the tasks
        for (ChecklistTask task : checklist.getChecklistTasks().getChecklistTask()) {
            task.setUid(UUID.randomUUID().toString());
        }

        createOrUpdateChecklistMission(checklist, clientUid, groupVector);

        return toXml(checklist);
    }

    public String addEditChecklistTask(
            ChecklistTask task, Checklist checklist, String clientUid, String groupVector) {

        // add the task to esync
        String xml = toXml(task);
        List<String> keywords = new ArrayList<String>();
        keywords.add("Task");
        Metadata metadata = addToEnterpriseSync(xml.getBytes(), groupVector, task.getUid(), keywords, new Date());
        String hash = metadata.getHash();

        // add the task doc to the checklist mission
        MissionContent content = new MissionContent();
        content.getHashes().add(hash);
        missionService.addMissionContent(checklist.getChecklistDetails().getUid(), content, clientUid, groupVector);

        return hash;
    }

    public ChecklistTask getTask(Checklist checklist, String taskUid) {
        for (ChecklistTask task : checklist.getChecklistTasks().getChecklistTask()) {
            if (task.getUid().compareTo(taskUid) == 0) {
                return task;
            }
        }
        return null;
    }


    public void createChecklistMission(Checklist checklist, String clientUid, String groupVector) {
        // add the new checklist to esync
        String xml = toXml(checklist);
        String checklistId = checklist.getChecklistDetails().getUid();

        List<String> keywords = new ArrayList<String>();
        keywords.add("Template");
        Metadata metadata = addToEnterpriseSync(xml.getBytes(), groupVector, checklistId, keywords);

        // create a new mission for the checklist
        Mission checklistMission = missionService.createMission(checklistId, EXCHECK_TOOL, groupVector,
                checklist.getChecklistDetails().getDescription(), null, EXCHECK_TOOL, null, null, null);

        // add the new checklist to the checklist mission
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());

        int taskNumber = 0;
        for (ChecklistTask task : checklist.getChecklistTasks().getChecklistTask()) {

            // set a new uid for this task if there isnt one already set
            if (isEmptyUid(task.getUid())) {
                task.setUid(UUID.randomUUID().toString());
            }

            task.setNumber(taskNumber++);

            // add the task to esync
            xml = toXml(task);
            keywords = new ArrayList<String>();
            keywords.add("Task");
            metadata = addToEnterpriseSync(xml.getBytes(), groupVector, task.getUid(), keywords, new Date());
            String hash = metadata.getHash();

            content.getHashes().add(hash);
        }

        missionService.addMissionContent(checklistId, content, clientUid, groupVector);

        // whoever starts the checklist gets automatically subscribed
        try {
            missionService.missionSubscribe(checklistId, clientUid, groupVector);
        } catch (JpaSystemException e) { } // DuplicateKeyException comes through as JpaSystemException due to transaction

        // add the checklist mission to the ExCheck mission
        missionService.setParent(checklistMission.getName(), EXCHECK_TEMPLATES_MISSION, groupVector);
    }


    public void updateChecklistMission(Checklist oldChecklist, Checklist newChecklist,
                                       String clientUid, String groupVector) {

        String checklistUid = oldChecklist.getChecklistDetails().getUid();

        // collect up task uids currently in the mission
        int maxTaskNumber = Integer.MIN_VALUE;
        Set<String> newTaskUids = new HashSet<>();
        for (ChecklistTask task : oldChecklist.getChecklistTasks().getChecklistTask()) {
            if (!task.isLineBreak()) {
                newTaskUids.add(task.getUid());
            }

            if (task.getNumber() > maxTaskNumber) {
                maxTaskNumber = task.getNumber();
            }
        }


        // add/update each task in the updatedChecklist
        for (ChecklistTask updatedTask : newChecklist.getChecklistTasks().getChecklistTask()) {
            newTaskUids.remove(updatedTask.getUid());

            String operation;

            // brand new task
            if (isEmptyUid(updatedTask.getUid())) {
                updatedTask.setUid(UUID.randomUUID().toString());
                updatedTask.setNumber(++maxTaskNumber);
                operation = "added";

                // check to see if the task was updated
            } else {
                ChecklistTask existingTask = getTask(oldChecklist, updatedTask.getUid());

                // don't bother updating an existing task if it hasn't changed
                if (existingTask != null) {
                    if (checklistTaskToString(existingTask).compareTo(checklistTaskToString(updatedTask)) == 0) {
                        continue;
                    } else if (existingTask.getStatus() == ChecklistTaskStatus.PENDING &&
                            (updatedTask.getStatus() == ChecklistTaskStatus.COMPLETE ||
                                    updatedTask.getStatus() == ChecklistTaskStatus.COMPLETE_LATE)) {
                        operation = "completed";
                    } else {
                        operation = "updated";
                    }
                } else {
                    operation = "added";
                    updatedTask.setNumber(++maxTaskNumber);
                }
            }

            String hash = addEditChecklistTask(updatedTask, oldChecklist, clientUid, groupVector);
            notifyMissionReferences(hash, operation, oldChecklist, updatedTask, clientUid, groupVector);
        }

        // remove any tasks remaining in currentTaskUids
        for (String taskUid : newTaskUids) {
            ChecklistTask removed = getTask(oldChecklist, taskUid);
            notifyMissionReferences(null, "removed", newChecklist, removed, clientUid, groupVector);
            deleteChecklistTask(checklistUid, taskUid, clientUid, groupVector);
        }

        subscriptionManager.announceMissionChange(
                checklistUid, SubscriptionManagerLite.ChangeType.METADATA, clientUid, EXCHECK_TOOL, null);
    }

    public void createOrUpdateChecklistMission(Checklist checklist, String clientUid, String groupVector) {
        try {
            Checklist existing = getChecklist(checklist.getChecklistDetails().getUid(),
                    -1L, groupVector,false);
            if (existing == null) {
                logger.error("getChecklist returned null!");
                return;
            }

            updateChecklistMission(existing, checklist, clientUid, groupVector);

        } catch (NotFoundException nfe) {
            createChecklistMission(checklist, clientUid, groupVector);
        }
    }

    public void stopChecklist(String id, String clientUid, String groupVector) {
        missionService.deleteMission(id, clientUid, groupVector, true);
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public String getChecklistXml(String checklistUid, Long secago, String groupVector, boolean onlyDetails) {
        Checklist checklist = getChecklist(checklistUid, secago, groupVector, onlyDetails);
        return toXml(checklist);
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public String getChecklistXml(Mission checklistMission, Long secago, String groupVector, boolean onlyDetails) {
        Checklist checklist = getChecklist(checklistMission, secago, groupVector, onlyDetails);
        return toXml(checklist);
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public Checklist getChecklist(String checklistUid, Long secago, String groupVector, boolean onlyDetails) {
        Mission checklistMission = missionService.getMissionByNameCheckGroups(checklistUid, groupVector);
        return getChecklist(checklistMission, secago, groupVector, onlyDetails);
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public Checklist getChecklist(Mission checklistMission, Long secago, String groupVector, boolean onlyDetails) {
        if (checklistMission == null) {
            throw new NotFoundException();
        }

        String checklistUid = checklistMission.getName();

        Checklist checklist = null;

        // collect up the template (for header info) and unique task ids
        Set<String> taskUids = new HashSet<String>();

        Set<Resource> resources = checklistMission.getContents();
        Map<Integer, List<String>> keywordMap = missionService.hydrate(resources);
        for (Resource resource : resources) {
            List<String> keywords = keywordMap.get(resource.getId());
            resource.setKeywords(keywords);
        }

        for (Resource resource : checklistMission.getContents()) {
            if (resource.getKeywords().contains("Template")) {
                if (checklist == null) {
                    checklist = getTemplateFromESync(resource.getUid(), groupVector);
                }
            } else if (!onlyDetails) {
                taskUids.add(resource.getUid());
            }
        }

        if (checklist == null) {
            throw new TakException();
        }

        // clear out the tasks from the template
        checklist.getChecklistTasks().getChecklistTask().clear();

        if (onlyDetails) {
            // clear out the columns from the template
            checklist.getChecklistColumns().getChecklistColumn().clear();
        } else {
            // get the latest version of each task
            for (String taskUid : taskUids) {
                ChecklistTask task = getTaskFromESync(taskUid, groupVector);
                if (task == null) {
                    logger.error("getTaskFromESync (in getChecklist) returned null for taskUid: " + taskUid);
                    continue;
                }

                checklist.getChecklistTasks().getChecklistTask().add(task);
            }

            checklist.getChecklistTasks().getChecklistTask().sort(new Comparator<ChecklistTask>() {
                public int compare(ChecklistTask checklistTask1, ChecklistTask checklistTask2) {
                    return checklistTask1.getNumber() - checklistTask2.getNumber();
                }
            });
        }

        return checklist;
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public Checklist recreateChecklistAtDate(String checklistUid, Date date, String groupVector) {

        Mission checklistMission = missionService.getMissionByNameCheckGroups(checklistUid, groupVector);
        if (checklistMission == null) {
            throw new NotFoundException();
        }

        Checklist checklist = null;

        //
        // collect up the template (for header info) and unique task ids.
        //
        Set<String> taskUids = new HashSet<String>();
        missionService.hydrate(checklistMission, true);
        for (Resource resource : checklistMission.getContents()) {
            if (resource.getKeywords().contains("Template")) {
                if (checklist == null) {
                    checklist = getTemplateFromESync(resource.getUid(), groupVector);
                }
            } else {
                taskUids.add(resource.getUid());
            }
        }

        if (checklist == null) {
            throw new TakException();
        }

        // clear out the tasks from the template
        checklist.getChecklistTasks().getChecklistTask().clear();

        // get the latest version of each task
        for (String taskUid : taskUids) {
            ChecklistTask task = getTaskAtTimeFromESync(taskUid, date, groupVector);
            if (task == null) {
                logger.error("getTaskFromESync (in recreateChecklistAtDate) returned null for taskUid: " + taskUid);
                continue;
            }

            checklist.getChecklistTasks().getChecklistTask().add(task);
        }

        checklist.getChecklistTasks().getChecklistTask().sort(new Comparator<ChecklistTask>() {
            public int compare(ChecklistTask checklistTask1, ChecklistTask checklistTask2) {
                return checklistTask1.getNumber() - checklistTask2.getNumber();
            }
        });

        return checklist;
    }

    @Cacheable(Constants.ALL_MISSION_CACHE)
    public String getActiveChecklistXml(String groupVector) {

        try {
            StringBuilder result = new StringBuilder();
            result.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><checklists>");

            String sql =
                    "   select array_to_string(xpath('/checklist/checklistDetails/*[not(self::unreadCount)]'::text, xmlparse(document encode(data, 'escape'))),'') as checklistDetails from resource where id in (" +
                    "       select max(resource_id) from mission_resource, resource where " +
                    "           resource_id = resource.id and " +
                    "           mission_id in ( select id from mission where parent_mission_id in ( select id from mission where name = 'exchecktemplates' ) and " +
                            RemoteUtil.getInstance().getGroupClause() +
                    "       ) group by resource.uid " +
                    "   ) and keywords && '{Template}'::character varying[] order by submissiontime desc";

            try (Connection connection = ds.getConnection(); PreparedStatement query = connection.prepareStatement(sql)) {
                query.setString(1, groupVector);
                try (ResultSet queryResults = query.executeQuery()) {
                    while (queryResults.next()) {
                        result.append("<checklist><checklistDetails>");
                        result.append(queryResults.getString(1));
                        result.append(" </checklistDetails><checklistColumns/><checklistTasks/>");
                        result.append("</checklist>");
                    }
                }
            }

            result.append("</checklists>");
            return result.toString();

        } catch (Exception e) {
            logger.error("exception in getActiveChecklistXml!", e);
            return null;
        }
    }

    public void deleteUidFromEnterpriseSync(String uid, String groupVector) {

        try {
            com.bbn.marti.sync.EnterpriseSyncService syncStore = SpringContextBeanForApi.getSpringContext().getBean(
                    com.bbn.marti.sync.EnterpriseSyncService.class);

            List<Metadata> metadata = syncStore.getMetadataByUid(uid, groupVector);
            if (metadata == null || metadata.size() == 0) {
                throw new NotFoundException();
            }

            List<Integer> pks = new LinkedList<>();
            for (Metadata nextMeta : metadata) {
                pks.add(nextMeta.getPrimaryKey());
            }

            syncStore.delete(pks, groupVector);
        } catch (Exception e) {
            logger.error("exception in deleteUidFromEnterpriseSync for " + uid);
        }
    }

    public void deleteChecklistTask(String checklistUid, String taskUid, String clientUid, String groupVector) {

        // get the checklist mission
        Mission checklistMission = missionService.getMission(checklistUid, groupVector);

        // get the esync resource for this task
        Resource taskResource = null;
        for (Resource resource : checklistMission.getContents()) {
            if (resource.getUid().compareTo(taskUid) == 0) {
                taskResource = resource;
                break;
            }
        }

        if (taskResource == null) {
            throw new NotFoundException("task not found! : " + taskUid);
        }

        // remove the task file from the mission
        missionService.deleteMissionContent(checklistUid, taskResource.getHash(), null, clientUid, groupVector);

        // delete the file from esync
        deleteUidFromEnterpriseSync(taskUid, groupVector);
    }

    public void deleteTemplate(String templateUid, String clientUid, String groupVector) {

        // get the exchecktemplates mission
        Mission templatesMission = missionService.getMission(EXCHECK_TEMPLATES_MISSION, groupVector);

        // get the esync resource for this template
        Resource templateResource = null;
        for (Resource resource : templatesMission.getContents()) {
            if (resource.getUid().compareTo(templateUid) == 0) {
                templateResource = resource;
                break;
            }
        }

        if (templateResource == null) {
            throw new NotFoundException("template not found! : " + templateUid);
        }

        // remove the task file from the mission
        missionService.deleteMissionContent(EXCHECK_TEMPLATES_MISSION, templateResource.getHash(), null, clientUid, groupVector);

        // delete the file from esync
        deleteUidFromEnterpriseSync(templateUid, groupVector);
    }

    public boolean addEditTemplateTask(
            String templateUid, ChecklistTask templateTask, String clientUid, String groupVector) {
        // get the template
        Checklist template = getTemplateFromESync(templateUid, groupVector);
        if (template == null) {
            throw new NotFoundException("template found found! uid : " + templateUid);
        }

        // find and remove any existing task uid
        boolean existing = false;
        for (ChecklistTask nextTask : template.getChecklistTasks().getChecklistTask()) {
            if (nextTask.getUid().compareTo(templateTask.getUid()) == 0) {
                template.getChecklistTasks().getChecklistTask().remove(nextTask);
                existing = true;
                break;
            }
        }

        // add the new task
        template.getChecklistTasks().getChecklistTask().add(templateTask);

        // get the xml for the updated template
        String xml = toXml(template);

        // push it back to esync
        List<String> keywords = new ArrayList<String>();
        keywords.add(template.getChecklistDetails().getName());
        keywords.add(template.getChecklistDetails().getDescription());
        Metadata metadata = addToEnterpriseSync(xml.getBytes(), groupVector, templateUid, keywords);

        // add the updated template doc to the templates mission
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());
        missionService.addMissionContent(EXCHECK_TEMPLATES_MISSION, content, clientUid, groupVector);

        return existing;
    }

    public void deleteTemplateTask(
            String templateUid, String taskUid, String clientUid, String groupVector) {
        // get the template
        Checklist template = getTemplateFromESync(templateUid, groupVector);
        if (template == null) {
            throw new NotFoundException("template found found! uid : " + templateUid);
        }

        // find and remove the task uid
        boolean found = false;
        for (ChecklistTask nextTask : template.getChecklistTasks().getChecklistTask()) {
            if (nextTask.getUid().compareTo(taskUid) == 0) {
                template.getChecklistTasks().getChecklistTask().remove(nextTask);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new NotFoundException("task found found! uid : " + taskUid);
        }

        // get the xml for the updated template
        String xml = toXml(template);

        // push it back to esync
        List<String> keywords = new ArrayList<String>();
        keywords.add(template.getChecklistDetails().getName());
        keywords.add(template.getChecklistDetails().getDescription());
        Metadata metadata = addToEnterpriseSync(xml.getBytes(), groupVector, templateUid, keywords);

        // add the updated template doc to the templates mission
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());
        missionService.addMissionContent(EXCHECK_TEMPLATES_MISSION, content, clientUid, groupVector);
    }

    public ChecklistStatus getChecklistStatus(String checklistUid, String groupVector) {

        //
        // count up each status
        //
        Integer count = null;
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        Checklist checklist = getChecklist(checklistUid, -1L, groupVector, false);
        for (ChecklistTask task : checklist.getChecklistTasks().getChecklistTask()) {
            if (task.getStatus() == null) {
                continue;
            }

            count = counts.get(task.getStatus().value());
            if (count == null) {
                count = new Integer(1);
            } else {
                count++;
            }
            counts.put(task.getStatus().value(), count);
        }

        //
        // convert results to ChecklistStatus format
        //
        StatusCount statusCount = null;
        ChecklistStatus status = new ChecklistStatus();
        status.setChecklistUid(checklistUid);
        status.setChecklistName(checklist.getChecklistDetails().getName());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            statusCount = new StatusCount();
            statusCount.setStatus(ChecklistTaskStatus.fromValue(entry.getKey()));
            statusCount.setCount(entry.getValue());
            status.getStatusCount().add(statusCount);
        }

        return status;
    }

    public void notifyMissionReferences(
            String token, String operation, Checklist checklist, ChecklistTask checklistTask, String clientUid, String groupVector) {

        // bail if the checklist doesnt have any mission references
        if (checklist.getChecklistDetails().getMissions() == null) {
            return;
        }

        // iterate over mission references
        for (String missionName : checklist.getChecklistDetails().getMissions().getMission()) {

            // get the mission
            Mission mission = missionService.getMission(missionService.trimName(missionName), groupVector);
            if (mission == null) {
                continue;
            }

            // build up the notes
            String callsign = contactManagerService.getCallsignForUid(clientUid, groupVector);
            String user = callsign != null ? callsign : clientUid;
            String notes = user + " " + operation + " " + checklist.getChecklistDetails().getName();
            if (checklist.getChecklistColumns().getChecklistColumn().size() > 0
                    && checklistTask.getValue().size() > 0) {
                notes += "; " + checklist.getChecklistColumns().getChecklistColumn().get(0).getColumnName()
                        + ": " + checklistTask.getValue().get(0);
            }

            //
            // find the external data source that conatins the checklist uid, and notify the mission that it changed
            //
            for (ExternalMissionData externalMissionData : mission.getExternalData()) {
                if (externalMissionData.getUrlData().contains(checklist.getChecklistDetails().getUid())) {
                    missionService.notifyExternalMissionDataChanged(
                            missionName, externalMissionData.getId(), token, notes, clientUid, groupVector);
                }
            }
        }
    }

    public void addMissionReferenceToChecklist(
            String checklistUid, String missionName,
            String clientUid, String baseUrl, String groupVector, String password, HttpServletRequest request)
            throws RemoteException {

        String checklistXml = getChecklistXml(checklistUid, -1L, groupVector, false);
        Checklist checklist = checklistFromXml(checklistXml);

        missionName = missionService.trimName(missionName);
        Mission dataSyncMission = missionService.getMission(missionName, groupVector);

        if (dataSyncMission.isPasswordProtected()) {
            if (!Strings.isNullOrEmpty(password)) {
                missionService.validatePassword(dataSyncMission, password);
            } else {
                MissionRole tokenRole = missionService.getRoleFromToken(dataSyncMission,
                        new MissionTokenUtils.TokenType[] {
                                MissionTokenUtils.TokenType.SUBSCRIPTION, MissionTokenUtils.TokenType.ACCESS }, request);
                if (tokenRole == null || !tokenRole.hasPermission(MissionPermission.Permission.MISSION_WRITE)) {
                    throw new ForbiddenException("Illegal attempt to add checklist reference to mission.");
                }
            }
        }

        // get the list of mission references, create if needed
        Missions missionList = checklist.getChecklistDetails().getMissions();
        if (missionList == null) {
            missionList = new Missions();
            checklist.getChecklistDetails().setMissions(missionList);
        }

        // add a reference to the mission if we dont already have one
        if (!missionList.getMission().contains(missionName)) {
            missionList.getMission().add(missionName);
            checklistXml = toXml(checklist);

            List<String> keywords = new ArrayList<String>();
            keywords.add("Template");
            Metadata metadata = addToEnterpriseSync(checklistXml.getBytes(), groupVector, checklistUid, keywords);

            // add the new checklist to the checklist mission
            MissionContent content = new MissionContent();
            content.getHashes().add(metadata.getHash());
            missionService.addMissionContent(checklistUid, content, clientUid, groupVector);
        }

        //
        // register the checklist as an external data source within the mission
        //

        String urlData = baseUrl + "/Marti/api/excheck/checklist/" + checklistUid;
        String urlView = baseUrl + "/Marti/api/excheck/checklist/" + checklistUid + "/status";

        // build up the notes
        String callsign = contactManagerService.getCallsignForUid(clientUid, groupVector);
        String user = callsign != null ? callsign : clientUid;
        String notes = user + " added " + checklist.getChecklistDetails().getName();

        ExternalMissionData externalMissionData = new ExternalMissionData(
                checklist.getChecklistDetails().getName(), EXCHECK_TOOL, urlData, urlView, notes);

        externalMissionData.setId(checklistUid);

        missionService.setExternalMissionData(missionName, clientUid, externalMissionData, groupVector);
    }

    public void removeMissionReferenceFromChecklist(
            String checklistUid, String missionName, String clientUid, String groupVector) throws RemoteException {

        String checklistXml = getChecklistXml(checklistUid, -1L, groupVector, false);
        Checklist checklist = checklistFromXml(checklistXml);

        Missions missionList = checklist.getChecklistDetails().getMissions();
        if (missionList == null) {
            return;
        }

        missionName = missionService.trimName(missionName);

        missionList.getMission().remove(missionName);
        checklistXml = toXml(checklist);

        List<String> keywords = new ArrayList<String>();
        keywords.add("Template");
        Metadata metadata = addToEnterpriseSync(checklistXml.getBytes(), groupVector, checklistUid, keywords);

        // add the new checklist to the checklist mission
        MissionContent content = new MissionContent();
        content.getHashes().add(metadata.getHash());
        missionService.addMissionContent(checklistUid, content, clientUid, groupVector);

        // build up the notes
        String callsign = contactManagerService.getCallsignForUid(clientUid, groupVector);
        String user = callsign != null ? callsign : clientUid;
        String notes = user + " removed " + checklist.getChecklistDetails().getName();

        // remove the checklist's external data from the mission
        Mission dataSyncMission = missionService.getMission(missionName, groupVector);
        for (ExternalMissionData externalMissionData : dataSyncMission.getExternalData()) {
            if (externalMissionData.getUrlData().contains(checklistUid)) {
                missionService.deleteExternalMissionData(
                        missionName, externalMissionData.getId(), notes, clientUid, groupVector);
            }
        }
    }

    public static boolean isEmptyUid(String uid) {
        return uid == null || uid.length() == 0 ||
                uid.compareTo("00000000-0000-0000-0000-000000000000") == 0;
    }

    public static String checklistTaskToString(ChecklistTask checklistTask) {
        StringBuilder sb = new StringBuilder();

        sb.append("number: " + checklistTask.getNumber() + ", ");
        sb.append("lineBreak: " + checklistTask.isLineBreak() + ", ");

        if (checklistTask.getUid() != null) {
            sb.append("uid: " + checklistTask.getUid().trim() + ", ");
        }

        if (checklistTask.getNotes() != null) {
            sb.append("notes: " + checklistTask.getNotes().trim() + ", ");
        }

        if (checklistTask.getDueDTG() != null) {
            sb.append("dueDTG: " + checklistTask.getDueDTG().trim() + ", ");
        }

        if (checklistTask.getStatus() != null) {
            sb.append("status: " + checklistTask.getStatus() + ", ");
        }

        if (checklistTask.getBgColor() != null) {
            sb.append("bgColor: " + checklistTask.getBgColor().trim() + ", ");
        }

        if (checklistTask.getCompleteBy() != null) {
            sb.append("completeBy: " + checklistTask.getCompleteBy().trim() + ", ");
        }

        if (checklistTask.getCompleteDTG() != null) {
            sb.append("completeDTG: " + checklistTask.getCompleteDTG().trim() + ", ");
        }

        if (checklistTask.getDueRelativeTime() != null) {
            sb.append("dueRelativeTime: " + checklistTask.getDueRelativeTime().trim() + ", ");
        }

        for (String value : checklistTask.getValue()) {
            if (value != null) {
                sb.append("value: " + value.trim() + ", ");
            }
        }

        return sb.toString();
    }
}