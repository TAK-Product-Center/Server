package com.bbn.marti.excheck;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.bbn.marti.excheck.checklist.Checklist;
import com.bbn.marti.excheck.checklist.ChecklistColumn;
import com.bbn.marti.excheck.checklist.ChecklistTask;
import com.bbn.marti.excheck.checklist.ChecklistTaskStatus;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.sync.model.MissionRole;
import com.bbn.marti.sync.UploadServlet;
import com.bbn.marti.sync.model.MissionChange;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.cert.exception.NotFoundException;
import com.google.common.io.ByteStreams;

import tak.server.Constants;

@RestController
public class ExCheckAPI extends BaseRestController {
	
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.marti.excheck.ExCheckAPI.class);

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private ExCheckService exCheckService;
    
    @Autowired
    private CoreConfig coreConfig;

    @Autowired
    private MissionService missionService;

    //
    // create template via csv or xml upload
    //
    @RequestMapping(value = "/excheck/template", method = RequestMethod.POST)
    ResponseEntity postTemplate(
            @RequestParam(value = "clientUid", required = true) String clientUid,
            @RequestParam(value = "callsign", required = false) String callsign,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request,
            HttpServletResponse response) throws RemoteException, IOException, ServletException {

        // Get group vector for the user associated with this session
        String groupVector = martiUtil.getGroupBitVector(request);

        byte[] payload = null;
        String contentType = request.getHeader("Content-Type");

        Checklist checklist = null;

        if (contentType != null && contentType.compareToIgnoreCase("application/xml") == 0) {
            payload = readByteArray(request.getInputStream());
            String xml = new String(payload);

            checklist = exCheckService.checklistFromXml(xml);
            if (exCheckService.isEmptyUid(checklist.getChecklistDetails().getUid())) {
                checklist.getChecklistDetails().setUid(UUID.randomUUID().toString());
            }

            name = checklist.getChecklistDetails().getName();
            description = checklist.getChecklistDetails().getDescription();
            callsign = checklist.getChecklistDetails().getCreatorCallsign();
        } else {

            if (contentType == null || !contentType.contains("multipart/form-data")) {
                payload = readByteArray(request.getInputStream());
            } else {
                MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
                MultipartFile mpf = multipartRequest.getFile("assetfile");
                payload = mpf.getBytes();
            }

            if (payload.length > coreConfig.getRemoteConfiguration().getNetwork().
                    getEnterpriseSyncSizeLimitMB() * 1000000) {
                String message = "Uploaded file exceeds server's size limit of "
                        + coreConfig.getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB()
                        + " MB! (limit is set in CoreConfig)";
                logger.error(message);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            String csv = new String(payload);
            checklist = exCheckService.parseTemplate(csv);
        }

        exCheckService.addNewTemplate(checklist, checklist.getChecklistDetails().getUid(),
                name, description, clientUid, callsign, groupVector);

        response.getOutputStream().write(checklist.getChecklistDetails().getUid().getBytes());

        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // get template
    //
    @RequestMapping(value = "/excheck/template/{templateUid}", method = RequestMethod.GET)
    ResponseEntity getTemplate(
            @PathVariable("templateUid") String templateUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Checklist template = exCheckService.getTemplateFromESync(templateUid, martiUtil.getGroupBitVector(request));
        String xml = exCheckService.toXml(template);
        response.setContentType("application/xml");
        response.getOutputStream().write(xml.getBytes());
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // delete template
    //
    @RequestMapping(value = "/excheck/template/{templateUid}", method = RequestMethod.DELETE)
    ResponseEntity deleteTemplate(
            @PathVariable("templateUid") String templateUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        exCheckService.deleteTemplate(templateUid, clientUid, martiUtil.getGroupBitVector(request));

        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // start a checklist from a template
    //
    @RequestMapping(value = "/excheck/{templateUid}/start", method = RequestMethod.POST)
    ResponseEntity startChecklist(
            @PathVariable("templateUid") String templateUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            @RequestParam(value = "callsign", required = true) String callsign,
            @RequestParam(value = "name", required = true) String name,
            @RequestParam(value = "description", required = true) String description,
            @RequestParam(value = "startTime", required = true) String startTime,
            @RequestParam(value = "defaultRole", required = false) MissionRole.Role defaultRole,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String checklistXml = exCheckService.startChecklist(
                templateUid, clientUid, callsign, name, description, startTime, defaultRole, martiUtil.getGroupBitVector(request));
        response.setContentType("application/xml");
        response.getOutputStream().write(checklistXml.getBytes());
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // create a checklist from xml
    //
    @RequestMapping(value = "/excheck/checklist", method = RequestMethod.POST)
    ResponseEntity createChecklist(
            @RequestParam(value = "clientUid", required = true) String clientUid,
            @RequestBody String checklistXml,
            @RequestParam(value = "defaultRole", required = false) MissionRole.Role defaultRole,
            HttpServletRequest request,
            HttpServletResponse response) throws RemoteException, IOException {

        Checklist checklist = exCheckService.checklistFromXml(checklistXml);

        if (exCheckService.isEmptyUid(checklist.getChecklistDetails().getUid())) {
            checklist.getChecklistDetails().setUid(UUID.randomUUID().toString());
        }

        exCheckService.createOrUpdateChecklistMission(checklist, clientUid, defaultRole, martiUtil.getGroupBitVector(request));

        response.getOutputStream().write(checklist.getChecklistDetails().getUid().getBytes());

        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // get active checklists
    //
    @RequestMapping(value = "/excheck/checklist/active", method = RequestMethod.GET)
    ResponseEntity getChecklist(
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String checklistXml = exCheckService.getActiveChecklistXml(martiUtil.getGroupBitVector(request));
        response.setContentType("application/xml");
        response.getOutputStream().write(checklistXml.getBytes());
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // get checklist
    //
    @RequestMapping(value = "/excheck/checklist/{checklistUid}", method = RequestMethod.GET)
    ResponseEntity getChecklist(
            @PathVariable("checklistUid") String checklistUid,
            @RequestParam(value = "clientUid", required = false) String clientUid,
            @RequestParam(value = "secago", required = false) Long secago,
            @RequestParam(value = "token", required = false) String token,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String groupVector = martiUtil.getGroupBitVector(request);

        //
        // if we've got a token, find the corresponding missionChange and checklistTask, and recreate
        // the checklist at the time of the change
        //
        Checklist checklist;
        if (token != null && token.length() > 0) {
            MissionChange missionChange = missionService.getLatestMissionChangeForContentHash(
                    checklistUid, token);
            if (missionChange == null) {
                throw new NotFoundException("change not found for token " + token);
            }

            checklist = exCheckService.recreateChecklistAtDate(
                    checklistUid, missionChange.getTimestamp(), groupVector);
        } else {
            // otherwise just get the current checklist
            checklist = exCheckService.getChecklist(
                    checklistUid, -1L, groupVector, false);
        }

        if (checklist == null){
            throw new NotFoundException("checklist not found! : " + checklistUid);
        }

        response.setContentType("application/xml");
        response.getOutputStream().write(exCheckService.toXml(checklist).getBytes());
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // delete a checklist
    //
    @RequestMapping(value = "/excheck/{checklistUid}/stop", method = RequestMethod.POST)
    ResponseEntity stopChecklist(
            @PathVariable("checklistUid") String checklistUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws RemoteException {

        exCheckService.stopChecklist(checklistUid, clientUid, martiUtil.getGroupBitVector(request));
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/excheck/checklist/{checklistUid}", method = RequestMethod.DELETE)
    ResponseEntity deleteChecklist(
            @PathVariable("checklistUid") String checklistUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        exCheckService.stopChecklist(checklistUid, clientUid, martiUtil.getGroupBitVector(request));
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // CRUD ops for checklist tasks
    //

    @RequestMapping(value = "/excheck/checklist/{checklistUid}/task/{taskUid}", method = RequestMethod.PUT)
    ResponseEntity addEditChecklistTask(
            @PathVariable("checklistUid") String checklistUid,
            @PathVariable("taskUid") String taskUid,
            @RequestBody String checklistTaskXml,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ChecklistTask checklistTask = exCheckService.checklistTaskFromXml(checklistTaskXml);

        if (checklistTask.getUid().compareTo(taskUid) != 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        String groupVector = martiUtil.getGroupBitVector(request);

        ChecklistTask existing = exCheckService.getTaskFromESync(checklistTask.getUid(), groupVector);
        Checklist checklist = exCheckService.getChecklist(checklistUid, -1L, groupVector, false);

        String hash = exCheckService.addEditChecklistTask(checklistTask, checklist, clientUid, groupVector);

        String operation;
        if (existing == null) {
            operation = "added";
        } else if (existing.getStatus() == ChecklistTaskStatus.PENDING &&
            (checklistTask.getStatus() == ChecklistTaskStatus.COMPLETE ||
                    checklistTask.getStatus() == ChecklistTaskStatus.COMPLETE_LATE)) {
            operation = "completed";
        } else {
            operation = "updated";
        }

        exCheckService.notifyMissionReferences(hash, operation, checklist, checklistTask, clientUid, groupVector);

        HttpStatus status = existing != null ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity(status);
    }

    @RequestMapping(value = "/excheck/checklist/{checklistUid}/task/{taskUid}", method = RequestMethod.GET)
    ResponseEntity getChecklistTask(
            @PathVariable("checklistUid") String checklistUid,
            @PathVariable("taskUid") String taskUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ChecklistTask checklistTask = exCheckService.getTaskFromESync(
                taskUid, martiUtil.getGroupBitVector(request));
        if (checklistTask == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        String xml = exCheckService.toXml(checklistTask);
        response.getOutputStream().write(xml.getBytes());
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/excheck/checklist/{checklistUid}/task/{taskUid}", method = RequestMethod.DELETE)
    ResponseEntity deleteChecklistTask(
            @PathVariable("checklistUid") String checklistUid,
            @PathVariable("taskUid") String taskUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        exCheckService.deleteChecklistTask(
                checklistUid, taskUid, clientUid, martiUtil.getGroupBitVector(request));
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // CRUD ops template tasks
    //

    @RequestMapping(value = "/excheck/template/{templateUid}/task/{taskUid}", method = RequestMethod.PUT)
    ResponseEntity addEditTemplateTask(
            @PathVariable("templateUid") String templateUid,
            @PathVariable("taskUid") String taskUid,
            @RequestBody String templateTaskXml,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ChecklistTask templateTask = exCheckService.checklistTaskFromXml(templateTaskXml);

        if (templateTask.getUid().compareTo(taskUid) != 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        boolean existing = exCheckService.addEditTemplateTask(templateUid, templateTask, clientUid,
                martiUtil.getGroupBitVector(request));

        HttpStatus status = existing ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity(status);
    }

    @RequestMapping(value = "/excheck/template/{templateUid}/task/{taskUid}", method = RequestMethod.GET)
    ResponseEntity getTemplateTask(
            @PathVariable("templateUid") String templateUid,
            @PathVariable("taskUid") String taskUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Checklist template = exCheckService.getTemplateFromESync(templateUid, martiUtil.getGroupBitVector(request));
        if (template == null) {
            throw new NotFoundException("template found found! uid : " + templateUid);
        }

        // find the task
        for (ChecklistTask nextTask : template.getChecklistTasks().getChecklistTask()) {
            if (nextTask.getUid().compareTo(taskUid) == 0) {
                String xml = exCheckService.toXml(nextTask);
                response.getOutputStream().write(xml.getBytes());
                return new ResponseEntity(HttpStatus.OK);
            }
        }

        throw new NotFoundException("task found found! uid : " + taskUid);
    }

    @RequestMapping(value = "/excheck/template/{templateUid}/task/{taskUid}", method = RequestMethod.DELETE)
    ResponseEntity deleteTemplateTask(
            @PathVariable("templateUid") String templateUid,
            @PathVariable("taskUid") String taskUid,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        exCheckService.deleteTemplateTask(
                templateUid, taskUid, clientUid, martiUtil.getGroupBitVector(request));
        return new ResponseEntity(HttpStatus.OK);
    }

    //
    // status and mission integration
    //

    @RequestMapping(value = "/excheck/checklist/{checklistUid}/status", method = RequestMethod.GET)
    ResponseEntity getChecklistStatus(
            @PathVariable("checklistUid") String checklistUid,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "clientUid", required = false) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        try {
            Checklist checklist = null;
            ChecklistTask checklistTask = null;
            String timestamp = null;
            String creatorUid = null;
            String groupVector = martiUtil.getGroupBitVector(request);

            //
            // if we've got a token, find the corresponding missionChange and checklistTask, and recreate
            // the checklist at the time of the change
            //
            if (token != null && token.length() > 0) {
                MissionChange missionChange = missionService.getLatestMissionChangeForContentHash(
                        checklistUid, token);
                if (missionChange == null) {
                    throw new NotFoundException("change not found for token " + token);
                }

                checklist = exCheckService.recreateChecklistAtDate(
                        checklistUid, missionChange.getTimestamp(), groupVector);
                checklistTask = exCheckService.getTaskFromESyncByHash(token, groupVector);

                SimpleDateFormat sdf = new SimpleDateFormat(Constants.COT_DATE_FORMAT);
                sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
                timestamp = sdf.format(missionChange.getTimestamp());

                creatorUid = missionChange.getCreatorUid();
            } else {
                // otherwise just get the current checklist
                checklist = exCheckService.getChecklist(
                        checklistUid, -1L, groupVector, false);
            }

            if (checklist == null) {
                throw new NotFoundException("checklist not found for uid " + checklistUid);
            }

            //
            // tally up the total number of tasks and number of completed tasks
            //
            int complete = 0;
            int total = checklist.getChecklistTasks().getChecklistTask().size();
            for (ChecklistTask task : checklist.getChecklistTasks().getChecklistTask()) {
                if (task.isLineBreak()) {
                    total--;
                } else if (task.getStatus() == null) {
                    continue;
                } else if (task.getStatus().compareTo(ChecklistTaskStatus.COMPLETE) == 0
                        || task.getStatus().compareTo(ChecklistTaskStatus.COMPLETE_LATE) == 0) {
                    complete++;
                }
            }

            //
            // show the top level checklist progress info
            //
            StringBuilder sb = new StringBuilder();
            sb.append("<html><body>");
            sb.append("<h2>Checklist : " + checklist.getChecklistDetails().getName() + "</h2>");
            sb.append("<h3>Progress : " + complete + "/" + total + "</h3>");
                sb.append("<progress value=\"" + complete + "\" max=\"" + total + "\"></progress>");

            //
            // show task level info if we were given a token
            //
            if (checklistTask != null) {
                sb.append("<h4>The following task is now " + checklistTask.getStatus().value() + "</h4>");

                sb.append("<table>");
                sb.append("<tr>");
                for (ChecklistColumn checklistColumn : checklist.getChecklistColumns().getChecklistColumn()) {
                    sb.append("<th>" + checklistColumn.getColumnName() + "</th>");
                }
                sb.append("</tr>");

                sb.append("<tr>");
                for (String columnValue : checklistTask.getValue()) {

                    if (columnValue.length() > 8) {
                        columnValue = columnValue.substring(0, 8) + "...";
                    }
                    sb.append("<td align=center>" + columnValue + "</td>");
                }
                sb.append("</tr>");

                sb.append("</table>");
            }

            sb.append("</body></html>");

            response.setContentType("text/html");
            response.getOutputStream().write(sb.toString().getBytes());
            return new ResponseEntity(HttpStatus.OK);

        } catch (Exception e) {
            logger.error("exception in getChecklistStatus! : " + e.getMessage());
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/excheck/checklist/{checklistUid}/mission/{missionName:.+}", method = RequestMethod.PUT)
    ResponseEntity addMissionReferenceToChecklist(
            @PathVariable("checklistUid") String checklistUid,
            @PathVariable("missionName") String missionName,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            @RequestParam(value = "password", required = false) String password,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String requestUrl = URLDecoder.decode(request.getRequestURL().toString(),"UTF-8");
        String baseUrl = requestUrl.substring(0, requestUrl.indexOf(request.getServletPath()));

        exCheckService.addMissionReferenceToChecklist(
                checklistUid, missionService.trimName(missionName),
                clientUid, baseUrl, martiUtil.getGroupBitVector(request), password, request);

        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/excheck/checklist/{checklistUid}/mission/{missionName:.+}", method = RequestMethod.DELETE)
    ResponseEntity removeMissionReferenceFromChecklist(
            @PathVariable("checklistUid") String checklistUid,
            @PathVariable("missionName") String missionName,
            @RequestParam(value = "clientUid", required = true) String clientUid,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        exCheckService.removeMissionReferenceFromChecklist(
                checklistUid, missionService.trimName(missionName), clientUid, martiUtil.getGroupBitVector(request));
        return new ResponseEntity(HttpStatus.OK);
    }
    
    /**
	 * Utility method that reads a byte array from an input stream using Guava.
	 * 
	 * @param in
	 *            any InputStream containing some data
	 * @return the InputStream's contents as a byte array; may be size 0 but
	 *         will not be null.
	 * @throws IOException
	 *             if a read error occurs
	 */
	private byte[] readByteArray(InputStream in) throws IOException {
	    
	    return ByteStreams.toByteArray(in);
	}
}