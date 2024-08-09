package com.bbn.marti.device.profile.api;


import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.marti.cot.search.model.ApiResponse;
import com.bbn.marti.device.profile.model.Profile;
import com.bbn.marti.device.profile.model.ProfileDirectory;
import com.bbn.marti.device.profile.model.ProfileFile;
import com.bbn.marti.device.profile.repository.ProfileDirectoryRepository;
import com.bbn.marti.device.profile.repository.ProfileFileRepository;
import com.bbn.marti.device.profile.repository.ProfileRepository;
import com.bbn.marti.device.profile.service.ProfileService;
import com.bbn.marti.network.BaseRestController;
import com.bbn.marti.remote.SubmissionInterface;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.api.MissionApi;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.remote.util.SpringContextBeanForApi;

import tak.server.Constants;


/**
 * Created on 5/8/2018.
 */
@RestController
public class ProfileAdminAPI extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileAPI.class);

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileFileRepository profileFileRepository;

    @Autowired
    private ProfileDirectoryRepository profileDirectoryRepository;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private RemoteUtil remoteUtil;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private CommonUtil martiUtil;

    private SubmissionInterface submission;

    @EventListener({ContextRefreshedEvent.class})
    protected void init() {
    	submission = SpringContextBeanForApi.getSpringContext().getBean(SubmissionInterface.class);
    }

    @RequestMapping(value = "/device/profile", method = RequestMethod.GET)
    public ApiResponse<List<Profile>> getAllProfile()
            throws ValidationException, IntrusionException, RemoteException {
        try {

            List<Profile> profileList = profileRepository.findAll(Sort.by("id"));

            for (Profile profile : profileList) {
                for (Group group : groupManager.groupVectorToGroupSet(profile.getGroupVector())) {
                    if (group.getDirection() == Direction.IN) {
                        profile.getGroupNames().add(group.getName());
                    }
                }

                profile.updateType();
            }

            return new ApiResponse<List<Profile>>(Constants.API_VERSION, Profile.class.getSimpleName(), profileList);

        } catch (Exception e) {
            throw new TakException("exception in getProfile", e);
        }
    }

    @RequestMapping(value = "/device/profile/{name}", method = RequestMethod.GET)
    public ApiResponse<Profile> getProfile(@PathVariable("name") @NotNull String name)
            throws ValidationException, IntrusionException, RemoteException {

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            throw new NotFoundException();
        }

        try {
            for (Group group : groupManager.groupVectorToGroupSet(profile.getGroupVector())) {
                if (group.getDirection() == Direction.IN) {
                    profile.getGroupNames().add(group.getName());
                }
            }

            profile.updateType();

            return new ApiResponse<Profile>(Constants.API_VERSION, Profile.class.getSimpleName(), profile);

        } catch (Exception e) {
            throw new TakException("exception in getProfile", e);
        }
    }

    @RequestMapping(value = "/device/profile/{name}/send", method = RequestMethod.POST)
    public ResponseEntity sendProfile(
            @PathVariable("name") @NotNull String name,
            @RequestBody String[] selected)
            throws RemoteException
    {
        try {
            if (selected.length == 0) {
                return new ResponseEntity(HttpStatus.OK);
            }

            // generate the mission package so we can pull some data from it
            Profile profile = profileRepository.findByName(name);
            List<ProfileFile> profileFiles = profileFileRepository.findAllByProfileIdOrderById(profile.getId());
            byte[] profileMP = profileService.createProfileMissionPackage(profile.getId(),
                    ProfileService.defaultFilename, name, profileFiles);

            String shaHash = CommonUtil.SHA256(profileMP);

            String uid = shaHash;
            String callsign = "TAK Server";

            String requestUrl = request.getRequestURL().toString();
            String url = requestUrl.substring(0, requestUrl.indexOf(request.getServletPath()))
                    + "/Marti/api/device/profile/" + name + "/missionpackage";

            // Generate the CoT message
            String cotMessage = CommonUtil.getFileTransferCotMessage(
                     /*String uid*/ uid, // yes: set the UID to be the hash, this makes it consistent with ATAK-generated mission packages
                     /*String shaHash*/ shaHash,
                     /*String callsign*/  callsign,
                     /*String filename*/ "profile.zip",
                     /*String url*/ url,
                     /*long sizeInBytes*/ profileMP.length,
                     /*String[] contacts*/ selected);

            submission.submitCot(cotMessage, martiUtil.getGroupsFromRequest(request));

            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in sendProfile!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @RequestMapping(value = "/device/profile/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteProfile(@PathVariable("id") @NotNull Long id)
            throws ValidationException, IntrusionException, RemoteException {
        try {
            profileDirectoryRepository.deleteByProfileId(id);
            profileFileRepository.deleteByProfileId(id);
            profileRepository.deleteById(id);
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in deleteProfile!", e);
            throw new TakException("exception in deleteProfile", e);
        }
    }

    @RequestMapping(value = "/device/profile/{name}", method = RequestMethod.POST)
    public ResponseEntity createProfile(
            @PathVariable("name") @NotNull String name,
            @RequestParam(value = "group", defaultValue = "") @MissionApi.ValidatedBy("MartiSafeString") String[] groupNames)
            throws RemoteException
    {
        Set<Group> groups = groupManager.findGroups(Arrays.asList(groupNames));
        String groupVector = remoteUtil.bitVectorToString(remoteUtil.getBitVectorForGroups(groups));

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            profileRepository.create(name, groupVector);
            return new ResponseEntity(HttpStatus.CREATED);
        }

        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/device/profile/{name}", method = RequestMethod.PUT)
    public ResponseEntity updateProfile(
            @PathVariable("name") @NotNull String name,
            @RequestBody Profile profile)
            throws RemoteException
    {
        try {
            if (profile.getGroupNames().contains("APPLY_TO_ALL_GROUPS")) {
                profileRepository.updateGroups(profile.getId(), RemoteUtil.getInstance().getBitStringAllGroups());
            } else {
                boolean[] groups = remoteUtil.getBitVectorForGroups(groupManager.findGroups(profile.getGroupNames()));
                profileRepository.updateGroups(profile.getId(), remoteUtil.bitVectorToString(groups));
            }

            profile.setUpdated(new Date());

            profile.setApplyOnConnect(profile.getType().compareTo(Profile.TYPE_CONNECTION) == 0);
            profile.setApplyOnEnrollment(profile.getType().compareTo(Profile.TYPE_ENROLLMENT) == 0);

            profileRepository.save(profile);
            return new ResponseEntity(HttpStatus.OK);
        } catch (Exception e) {
            logger.error("exception in updateProfile!", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/device/profile/{name}/files", method = RequestMethod.GET)
    public ApiResponse<List<ProfileFile>>  getFiles(
            @PathVariable("name") @NotNull String name) {

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            throw new NotFoundException();
        }

        List<ProfileFile> files = profileFileRepository.findAllByProfileIdOrderById(profile.getId());
        return new ApiResponse<List<ProfileFile>>(Constants.API_VERSION, ProfileFile.class.getSimpleName(), files);
    }

    @RequestMapping(value = "/device/profile/{name}/file", method = RequestMethod.PUT)
    public ApiResponse<ProfileFile> addFile(
            @PathVariable("name") @NotNull String name,
            @RequestParam(value = "filename") String filename,
            @RequestBody byte[] contents) {

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            throw new NotFoundException();
        }

        ProfileFile profileFile = new ProfileFile();
        profileFile.setProfileId(profile.getId());
        profileFile.setName(filename);
        profileFile.setData(contents);
        profileFileRepository.save(profileFile);

        profileRepository.updated(profile.getId());

        return new ApiResponse<ProfileFile>(Constants.API_VERSION, ProfileFile.class.getSimpleName(), profileFile);
    }

    @RequestMapping(value = "/device/profile/{name}/file/{id}", method = RequestMethod.GET)
    public byte[] getFile(
            @PathVariable("name") @NotNull String name,
            @PathVariable("id") @NotNull Long id) {

        ProfileFile profileFile = profileFileRepository.getOne(id);
        if (profileFile == null) {
            throw new NotFoundException();
        }

        response.addHeader(
                "Content-Disposition",
                "attachment; filename=" + profileFile.getName());

        return profileFile.getData();
    }

    @Transactional
    @RequestMapping(value = "/device/profile/{name}/file/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteFile(
            @PathVariable("name") @NotNull String name,
            @PathVariable("id") @NotNull Long id) {

        ProfileFile profileFile = profileFileRepository.getOne(id);
        profileRepository.updated(profileFile.getProfileId());
        profileFileRepository.deleteById(id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/device/profile/directories", method = RequestMethod.GET)
    public ApiResponse<List<String>> getValidDirectories() {
        List<String> directories = profileService.getValidDirectories();
        return new ApiResponse<List<String>>(Constants.API_VERSION, String.class.getSimpleName(), directories);
    }

    @Transactional
    @RequestMapping(value = "/device/profile/{name}/directories/{directories}", method = RequestMethod.PUT)
    public ResponseEntity updateDirectories(
            @PathVariable("name") @NotNull String name,
            @PathVariable("directories") @NotNull List<String> directories) {

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            throw new NotFoundException();
        }

        // validate inputs
        List<String> validDirectories = profileService.getValidDirectories();

        for (String directory : directories) {
            if (!validDirectories.contains(directory)) {
                logger.error("attempt to add illegal profile directory! : " + directory);
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                ProfileDirectory profileDirectory = new ProfileDirectory();
                profileDirectory.setProfileId(profile.getId());
                profileDirectory.setPath(directory);
                profileDirectoryRepository.save(profileDirectory);
            }
        }

        profileRepository.updated(profile.getId());
        return new ResponseEntity(HttpStatus.OK);
    }

    @RequestMapping(value = "/device/profile/{name}/directories", method = RequestMethod.GET)
    public ApiResponse<List<ProfileDirectory>> getDirectories(
            @PathVariable("name") @NotNull String name) {

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            throw new NotFoundException();
        }

        List<ProfileDirectory> files = profileDirectoryRepository.findAllByProfileIdOrderById(profile.getId());
        return new ApiResponse<List<ProfileDirectory>>(Constants.API_VERSION, ProfileDirectory.class.getSimpleName(), files);
    }

    @Transactional
    @RequestMapping(value = "/device/profile/{name}/directories", method = RequestMethod.DELETE)
    public ResponseEntity deleteDirectories(
            @PathVariable("name") @NotNull String name) {

        Profile profile = profileRepository.findByName(name);
        if (profile == null) {
            throw new NotFoundException();
        }

        // clear out current profile directories
        profileDirectoryRepository.deleteByProfileId(profile.getId());

        return new ResponseEntity(HttpStatus.OK);
    }
}

