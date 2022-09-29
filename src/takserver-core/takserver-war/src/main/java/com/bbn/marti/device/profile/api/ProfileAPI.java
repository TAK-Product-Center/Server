package com.bbn.marti.device.profile.api;


import com.bbn.marti.device.profile.model.Profile;
import com.bbn.marti.device.profile.model.ProfileFile;
import com.bbn.marti.device.profile.repository.ProfileRepository;
import com.bbn.marti.device.profile.repository.ProfileFileRepository;
import com.bbn.marti.device.profile.service.ProfileService;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.SubscriptionManagerLite;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.util.CommonUtil;
import com.bbn.security.web.MartiValidator;
import com.bbn.security.web.MartiValidatorConstants;

import java.io.IOException;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.Validator;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class ProfileAPI extends BaseRestController {

    private final Validator validator = new MartiValidator();

    private static final Logger logger = LoggerFactory.getLogger(ProfileAPI.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileFileRepository profileFileRepository;

    @Autowired
    SubscriptionManagerLite subscriptionManager;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    CoreConfig coreConfig;


    private String getGroupVectorFromStreamingClient(String clientUid, String groupVector) {
        try {
            if (coreConfig.getRemoteConfiguration().getProfile() != null &&
                    coreConfig.getRemoteConfiguration().getProfile().isUseStreamingGroup()) {
                RemoteSubscription subscription = subscriptionManager.getRemoteSubscriptionByClientUid(clientUid);
                if (subscription != null) {
                    String groupVectorTmp = groupManager.getCachedOutboundGroupVectorByConnectionId(
                            subscription.getUser().getConnectionId());
                    if (groupVectorTmp != null && groupVectorTmp.length() > 0) {
                        groupVector = groupVectorTmp;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("exception getting groupVector from streaming user", e);
        }

        return groupVector;
    }

    @RequestMapping(value = "/tls/profile/enrollment", method = RequestMethod.GET)
    public byte[] getEnrollmentTimeProfiles(
            @RequestParam(value = "clientUid") String clientUid)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            String host = new URI(request.getRequestURL().toString()).getHost();

            groupVector = getGroupVectorFromStreamingClient(clientUid, groupVector);

            List<ProfileFile> files = profileService.getProfileFiles(host, groupVector,
                    true, false, -1);

            if (files.size() == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return null;
            }

            logger.info("Returning enrollment profile for " + clientUid);

            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + ProfileService.defaultFilename);

            return profileService.createProfileMissionPackage(ProfileService.defaultFilename, "Enrollment", files);

        } catch (Exception e) {
            throw new TakException("exception in getEnrollmentTimeProfiles", e);
        }
    }

    @RequestMapping(value = "/device/profile/connection", method = RequestMethod.GET)
    public byte[] getConnectionTimeProfiles(
            @RequestParam(value = "syncSecago") Long syncSecago,
            @RequestParam(value = "clientUid") String clientUid)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);
            String host = new URI(request.getRequestURL().toString()).getHost();

            groupVector = getGroupVectorFromStreamingClient(clientUid, groupVector);

            List<ProfileFile> files = profileService.getProfileFiles(host, groupVector,
                    false, true, syncSecago);

            if (files.size() == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return null;
            }

            logger.info("Returning connection profile for " + clientUid);

            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + ProfileService.defaultFilename);

            return profileService.createProfileMissionPackage(ProfileService.defaultFilename, "Connection", files);

        } catch (Exception e) {
            throw new TakException("exception in getConnectionTimeProfiles", e);
        }
    }

    @RequestMapping(value = "/device/profile/tool/{toolName}", method = RequestMethod.GET)
    public byte[] getToolProfiles(
            @PathVariable("toolName") @NotNull String toolName,
            @RequestParam(value = "syncSecago", defaultValue = "-1") Long syncSecago,
            @RequestParam(value = "clientUid") String clientUid)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            String groupVector = martiUtil.getGroupVectorBitString(request);

            groupVector = getGroupVectorFromStreamingClient(clientUid, groupVector);

            List<ProfileFile> files = profileService.getProfileFilesForTool(groupVector,
                    toolName, syncSecago);

            if (files.size() == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return null;
            }

            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + ProfileService.defaultFilename);

            return profileService.createProfileMissionPackage(ProfileService.defaultFilename, toolName, files);

        } catch (Exception e) {
            throw new TakException("exception in getToolProfiles", e);
        }
    }

    @RequestMapping(value = "/tls/profile/tool/{toolName}/file", method = RequestMethod.GET)
    public void tlsGetProfileDirectoryContent(
            @PathVariable("toolName") @NotNull String toolName,
            @RequestParam("relativePath") @NotNull String[] relativePaths,
            @RequestParam(value = "syncSecago", defaultValue = "-1") Long syncSecago,
            @RequestParam(value = "clientUid") String clientUid)
            throws ValidationException, IntrusionException, IOException {

        for (String relativePath : relativePaths) {
            validator.getValidInput(ProfileAPI.class.getName(), relativePath,
                    MartiValidatorConstants.Regex.PreventDirectoryTraversal.name(),
                    MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
        }

        byte[] content = profileService.getProfileDirectoryContent(toolName, relativePaths, syncSecago, clientUid);
        if (content != null) {
            response.getOutputStream().write(content);
        }
    }

    @RequestMapping(value = "/device/profile/tool/{toolName}/file", method = RequestMethod.GET)
    public void deviceGetProfileDirectoryContent(
            @PathVariable("toolName") @NotNull String toolName,
            @RequestParam("relativePath") @NotNull String[] relativePaths,
            @RequestParam(value = "syncSecago", defaultValue = "-1") Long syncSecago,
            @RequestParam(value = "clientUid") String clientUid)
            throws ValidationException, IntrusionException, IOException {

        for (String relativePath : relativePaths) {
            validator.getValidInput(ProfileAPI.class.getName(), relativePath,
            		MartiValidatorConstants.Regex.PreventDirectoryTraversal.name(),
            		MartiValidatorConstants.DEFAULT_STRING_CHARS, false);
        }

        byte[] content = profileService.getProfileDirectoryContent(toolName, relativePaths, syncSecago, clientUid);
        if (content != null) {
            response.getOutputStream().write(content);
        }
    }

    @RequestMapping(value = "/device/profile/{name}/missionpackage", method = RequestMethod.HEAD)
    public void headProfileMp(@PathVariable("name") @NotNull String name)
            throws ValidationException, IntrusionException, RemoteException {
        try {

        } catch (Exception e) {
            throw new TakException("exception in headProfileMp", e);
        }
    }

    @RequestMapping(value = "/device/profile/{name}/missionpackage", method = RequestMethod.GET)
    public byte[] getProfileMp(@PathVariable("name") @NotNull String name)
            throws ValidationException, IntrusionException, RemoteException {
        try {

            Profile profile = profileRepository.findByNameAndGroupVector(name,
                    martiUtil.getGroupVectorBitString(request));

            if (profile == null) {
                throw new NotFoundException();
            }

            List<ProfileFile> files = profileFileRepository.findAllByProfileIdOrderById(profile.getId());

            response.addHeader(
                    "Content-Disposition",
                    "attachment; filename=" + ProfileService.defaultFilename);

            return profileService.createProfileMissionPackage(profile.getId(),
                    ProfileService.defaultFilename, name, files);

        } catch (Exception e) {
            throw new TakException("exception in getProfileMp", e);
        }
    }
}

