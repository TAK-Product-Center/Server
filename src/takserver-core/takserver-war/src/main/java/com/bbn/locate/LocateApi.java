package com.bbn.locate;

import java.util.Date;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import org.owasp.esapi.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.config.Locate;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.config.CoreConfigFacade;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.DateUtil;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.Mission;
import com.bbn.marti.sync.service.MissionService;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Validated
@RestController
public class LocateApi {

    Logger logger = LoggerFactory.getLogger(LocateApi.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private SubmissionInterface submission;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private MissionService missionService;

    @Autowired
    private Validator validator = new MartiValidator();


    // TODO: add API for guid
    @RequestMapping(value = "/locate/api", method = RequestMethod.POST)
    public void locate(
                @RequestParam(value = "latitude", required = true) Double latitude,
                @RequestParam(value = "longitude", required = true) Double longitude,
                @RequestParam(value = "name", required = true) String name,
                @RequestParam(value = "remarks", required = true) String remarks
    ) {
        boolean status = true;
        try {

            if (CoreConfigFacade.getInstance() == null || CoreConfigFacade.getInstance().getRemoteConfiguration() == null) {
                throw new TakException("Unable to find coreConfig");
            }

            if (CoreConfigFacade.getInstance().getRemoteConfiguration().getLocate() == null ||
                    !CoreConfigFacade.getInstance().getRemoteConfiguration().getLocate().isEnabled()) {
                throw new TakException("locate element is missing from coreConfig or is not enabled");
            }

            Locate locateConfig = CoreConfigFacade.getInstance().getRemoteConfiguration().getLocate();

            // validate inputs
            validator.getValidInput("LocateApi", Double.toString(latitude),
                    MartiValidatorConstants.Regex.Double.name(), MartiValidatorConstants.SHORT_STRING_CHARS, false);
            validator.getValidInput("LocateApi", Double.toString(longitude),
            		MartiValidatorConstants.Regex.Double.name(), MartiValidatorConstants.SHORT_STRING_CHARS, false);
            validator.getValidInput("LocateApi", name,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);
            validator.getValidInput("LocateApi", remarks,
            		MartiValidatorConstants.Regex.MartiSafeString.name(), MartiValidatorConstants.DEFAULT_STRING_CHARS, true);

            //
            // build up the cot marker
            //

            String uid = UUID.randomUUID().toString();
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            if (username != null && username.compareToIgnoreCase("UserROLE_NO_CLIENT_CERT") != 0) {
                name += " (" + username + ")";
            }

            long millis = System.currentTimeMillis();
            String startAndTime = DateUtil.toCotTime(millis);
            String stale = DateUtil.toCotTime(millis + 365 * 86400000);
            String creatorUid = "locate-service";

            String cotType = locateConfig.getCotType();
            String cot = "<event version=\"2.0\" uid=\"" + uid + "\"  type=\"" + cotType
                    + "\" time=\"" + startAndTime + "\" start=\"" + startAndTime + "\" stale=\"" + stale + "\" how=\"h-g-i-g-o\">" +
                    "<point lat=\"" + latitude + "\" lon=\"" + longitude + "\" hae=\"999999.0000000000\" ce=\"999999.0\" le=\"999999.0\"/>" +
                    "<detail>" +
                    "<contact callsign=\"" + name + "\"/>" +
                    "<takv device=\"Locator\" platform=\"Locator\" os=\"Locator\" version=\"1.0\"/>" +
                    "<archive/>" +
                    "<usericon iconsetpath=\"COT_MAPPING_2525B/\"/>" +
                    "<link relation=\"p-p\" type=\"a-f-G-U-C-I\" uid=\"" + uid + "\"/>" +
                    "<remarks>" + remarks + "</remarks>" +
                    "</detail>" +
                    "</event>";

            // build up the group used for this mission
            String groupName = locateConfig.getGroup();
            NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
            Group locateGroup = groupManager.hydrateGroup(new Group(groupName, Direction.IN));
            groups.add(locateGroup);
            String groupVector = RemoteUtil.getInstance().bitVectorToString(
                    RemoteUtil.getInstance().getBitVectorForGroups(groups));
            
            Mission mission = missionService.getMissionByNameCheckGroups(name, groupVector);
            
            missionService.validateMissionByGuid(mission);

            if (locateConfig.isAddToMission()) {
                String missionName = locateConfig.getMission().toLowerCase();

                if (missionService.getMission(missionName, false) == null) {
                    // create the mission and make sure that the we're subscribed
                    missionService.createMission(missionName, creatorUid, groupVector, null,
                            null, null, null, null, null, "public", null, null, null, null, false);
                }

                missionService.missionSubscribe(mission.getGuidAsUUID(), creatorUid, groupVector);

                // submit the marker to the mission
                submission.submitMissionPackageCotAtTime(cot, mission.getGuidAsUUID(), new Date(), groups, creatorUid);
            }

            if (locateConfig.isBroadcast()) {
                submission.submitCot(cot, groups);
            }

        } catch (Exception e) {
            logger.error("exception in LocateApi!", e);
            status = false;
        }

        response.setStatus(status ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
