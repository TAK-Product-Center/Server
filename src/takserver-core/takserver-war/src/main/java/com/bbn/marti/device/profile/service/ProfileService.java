package com.bbn.marti.device.profile.service;

import com.bbn.marti.remote.config.CoreConfigFacade;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.zip.ZipException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.joda.time.DateTime;
import org.owasp.esapi.errors.IntrusionException;
import org.owasp.esapi.errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bbn.marti.config.Auth;
import com.bbn.marti.device.profile.model.Profile;
import com.bbn.marti.device.profile.model.ProfileDirectory;
import com.bbn.marti.device.profile.model.ProfileFile;
import com.bbn.marti.device.profile.model.PreferenceFile;
import com.bbn.marti.device.profile.repository.ProfileDirectoryRepository;
import com.bbn.marti.device.profile.repository.ProfileFileRepository;
import com.bbn.marti.device.profile.repository.ProfileRepository;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.NotFoundException;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.GroupManager;
import com.bbn.marti.remote.LdapUser;
import com.bbn.marti.util.CommonUtil;
import com.bbn.marti.util.missionpackage.MissionPackage;


public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    public static final String profilesRoot = "profiles";
    public static final String defaultFilename = "profile.zip";

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ProfileFileRepository profileFileRepository;

    @Autowired
    private ProfileDirectoryRepository profileDirectoryRepository;

    @Autowired
    private CommonUtil martiUtil;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Autowired
    private GroupManager groupManager;


    public List<ProfileFile> getProfileFiles(
            String host, String groupVector, boolean applyOnEnrollment, boolean applyOnConnect, long syncSecago) {

        List<Profile> profileList;
        if (syncSecago == -1) {
            profileList = profileRepository.getAllProfiles(
                    applyOnEnrollment, applyOnConnect, groupVector);
        } else {
            Date lastSyncTime = new DateTime().minusSeconds(new Long(syncSecago).intValue()).toDate();
            profileList = profileRepository.getLatestProfiles(
                    applyOnEnrollment, applyOnConnect, lastSyncTime, groupVector);
        }

        List<ProfileFile> files = new ArrayList<>();
        for (Profile profile : profileList) {
            files.addAll(profileFileRepository.findAllByProfileIdOrderById(profile.getId()));
        }

        if (applyOnEnrollment) {
            Auth.Ldap ldap = CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().getLdap();

            // if we've got any of the user attributes set, go ahead and build the user preferences
            if (ldap != null &&
                    (!Strings.isNullOrEmpty(ldap.getCallsignAttribute()) ||
                            !Strings.isNullOrEmpty(ldap.getColorAttribute()) ||
                            !Strings.isNullOrEmpty(ldap.getRoleAttribute()))) {

                PreferenceFile preferenceFile = getUserPreferences(
                        SecurityContextHolder.getContext().getAuthentication().getName());
                if (preferenceFile != null) {
                    files.add(preferenceFile);
                }
            }

            // is the X509GroupCache enabled on this server?
            if (CoreConfigFacade.getInstance().getRemoteConfiguration().getAuth().isX509UseGroupCache()) {
                files.add(getX509GroupCachePreference(host));
            }
        }

        return files;
    }

    public PreferenceFile getUserPreferences(String username) {

        LdapUser ldapUser = groupManager.searchUser(username);
        if (ldapUser == null) {
            logger.error("searchUser lookup failed for : " + username);
            return null;
        }

        PreferenceFile preferenceFile = new PreferenceFile("user-profile.pref");

        if (!Strings.isNullOrEmpty(ldapUser.getCallsign())) {
            preferenceFile.addPreference("locationCallsign", ldapUser.getCallsign());
        }

        if (!Strings.isNullOrEmpty(ldapUser.getColor())) {
            preferenceFile.addPreference("locationTeam", ldapUser.getColor());
        }

        if (!Strings.isNullOrEmpty(ldapUser.getRole())) {
            preferenceFile.addPreference("atakRoleType", ldapUser.getRole());
        }

        return preferenceFile;
    }

    public PreferenceFile getX509GroupCachePreference(String takServerHost) {
        PreferenceFile preferenceFile = new PreferenceFile("enable-channels.pref");
        preferenceFile.addPreference("prefs_enable_channels", "true");
        preferenceFile.addPreference("prefs_enable_channels_host-" + takServerHost, "true");
        return preferenceFile;
    }

    public List<ProfileFile> getProfileFilesForTool(
            String groupVector, String tool, long syncSecago) {

        List<Profile> profileList;
        if (syncSecago == -1) {
            profileList = profileRepository.getAllProfilesForTool(
                    tool, groupVector);
        } else {
            Date lastSyncTime = new DateTime().minusSeconds(new Long(syncSecago).intValue()).toDate();
            profileList = profileRepository.getLatestProfilesForTool(
                    tool, lastSyncTime, groupVector);
        }

        List<ProfileFile> files = new ArrayList<>();
        for (Profile profile : profileList) {
            files.addAll(profileFileRepository.findAllByProfileIdOrderById(profile.getId()));
        }

        return files;
    }

    public byte[] createProfileMissionPackage(String filename, String name, List<ProfileFile> files) {
        return createProfileMissionPackage(UUID.randomUUID().toString(), filename, name, files);
    }

    public byte[] createProfileMissionPackage(Long id, String filename, String name, List<ProfileFile> files) {
        String uid = "ProfileMissionPackage-" + Long.toString(id);
        return createProfileMissionPackage(uid, filename, name, files);
    }

    private byte[] createProfileMissionPackage(String uid, String filename, String name, List<ProfileFile> files) {
        try {
            
            MissionPackage mp = new MissionPackage(filename);
            mp.addParameter("uid", uid);
            mp.addParameter("name", name);
            mp.addParameter("onReceiveImport", "true");
            mp.addParameter("onReceiveDelete", "true");

            int ndx = 0;
            for (ProfileFile profileFile : files) {
                mp.addDirectory("file" + ndx + "/");
                mp.addFile("file" + ndx + "/" + profileFile.getName(), profileFile.getData());
                ndx++;
            }

            return mp.save();
        } catch (Exception e) {
            logger.error("exception in createProfileMissionPackage!", e);
            return null;
        }
    }

    private class ProfileDirectoryWalker extends DirectoryWalker {
        public ProfileDirectoryWalker() {
            super(DirectoryFileFilter.INSTANCE, 1);
        }

        public void walk(File file, ArrayList<File> subdirectories) throws IOException {
            super.walk(file, subdirectories);
        }

        @Override
        public boolean handleDirectory(
                final File directory, final int depth, final Collection results) throws IOException {

            // dont include root
            if (depth == 0) {
                return true;
            }

            results.add(directory);
            return true;
        }
    }

    public List<String> getValidDirectories() {
        try {
            List<String> results = new LinkedList<>();
            File profileDirectory = new File("profiles");
            ArrayList<File> subdirectories = new ArrayList<>();

            ProfileDirectoryWalker profileDirectoryWalker = new ProfileDirectoryWalker();
            profileDirectoryWalker.walk(profileDirectory, subdirectories);

            for (File file : subdirectories) {
                if (file.isDirectory()) {
                    results.add(file.getName());
                }
            }

            return results;
        } catch (Exception e) {
            logger.error("exception in getValidDirectories!", e);
            return new LinkedList<>();
        }
    }

    public List<ProfileDirectory> getProfileDirectoriesForTool(
            String groupVector, String tool, long syncSecago) {

        List<Profile> profileList;
        if (syncSecago == -1) {
            profileList = profileRepository.getAllProfilesForTool(
                    tool, groupVector);
        } else {
            Date lastSyncTime = new DateTime().minusSeconds(new Long(syncSecago).intValue()).toDate();
            profileList = profileRepository.getLatestProfilesForTool(
                    tool, lastSyncTime, groupVector);
        }

        // validate paths coming out of the database
        List<String> validDirectories = getValidDirectories();

        List<ProfileDirectory> directories = new ArrayList<>();
        for (Profile profile : profileList) {
            for (ProfileDirectory profileDirectory :
                    profileDirectoryRepository.findAllByProfileIdOrderById(profile.getId())) {
                if (!validDirectories.contains(profileDirectory.getPath())) {
                    logger.error("attempt to add illegal profile directory! : " + profileDirectory.getPath());
                    return null;
                } else {
                    directories.add(profileDirectory);
                }
            }
        }

        return directories;
    }

    private class ProfileDirectoryContent {
        public File file;
        public byte[] contents;
        public ProfileDirectory profileDirectory;
    }

    private void loadDirectoryContents(
            File file, ProfileDirectory profileDirectory, List<ProfileDirectoryContent> profileDirectoryContents)
            throws IOException {
        if (file.isFile()) {

            if (!checkFile(file)) {
                logger.error("directory traversal attempt detected!!!");
                profileDirectoryContents.clear();
                return;
            }

            byte[] contents = Files.readAllBytes(Paths.get(file.getCanonicalPath()));
            ProfileDirectoryContent profileDirectoryContent = new ProfileDirectoryContent();
            profileDirectoryContent.file = file;
            profileDirectoryContent.contents = contents;
            profileDirectoryContent.profileDirectory = profileDirectory;
            profileDirectoryContents.add(profileDirectoryContent);
        } else {
            for (File child : file.listFiles()) {
                loadDirectoryContents(child, profileDirectory, profileDirectoryContents);
            }
        }
    }

    private boolean checkFile(File file) throws IOException {

        final String profilesDirectory = new File(profilesRoot).getCanonicalPath();

        if (file.getCanonicalPath().compareTo(file.getAbsolutePath()) != 0) {
            logger.error("attempt to manipulate relative path! canonicalPath != absolutePath : "
                    + file.getCanonicalPath() + ", " + file.getAbsolutePath());
            return false;
        }

        // double check that the root of the canonical path is our profiles directory
        if (!file.getCanonicalPath().startsWith(profilesDirectory)) {
            logger.error("attempt to manipulate relative path! canonicalPath has bad root: "
                    + file.getCanonicalPath());
            return false;
        }

        return true;
   }

    private List<ProfileDirectoryContent> getProfileDirectoryContent(
            String relativePath, List<ProfileDirectory> directories)
            throws IOException, NotFoundException {

        List<ProfileDirectoryContent> results = new LinkedList<>();
        final int uploadSizeLimitMB = CoreConfigFacade.getInstance().getRemoteConfiguration().getNetwork().getEnterpriseSyncSizeLimitMB();

        for (ProfileDirectory profileDirectory : directories) {

            // build up an absolute path using the relativePath
            String absolutePath = profilesRoot + "/" + profileDirectory.getPath() + relativePath;

            // make sure that the resolved canonical path matches the absolute path
            File file = new File(absolutePath);

            if (!checkFile(file)) {
                logger.error("directory traversal attempt detected!!! : " + relativePath);
                return null;
            }

            // keep searching if we didn't find the file in this profileDirectory
            if (!file.exists()) {
                continue;
            }

            if (file.isDirectory()) {

                loadDirectoryContents(file, profileDirectory, results);
                return results;

            } else {

                // make sure the file with under our max size before loading into memory
                if (file.length() > uploadSizeLimitMB * 1e6) {
                    logger.error("attempt to load file past size limit! : " + relativePath);
                    return null;
                }

                byte[] contents = Files.readAllBytes(Paths.get(file.getCanonicalPath()));

                ProfileDirectoryContent profileDirectoryContent = new ProfileDirectoryContent();
                profileDirectoryContent.file = file;
                profileDirectoryContent.contents = contents;
                profileDirectoryContent.profileDirectory = profileDirectory;

                results.add(profileDirectoryContent);
                return results;
            }
        }

        throw new NotFoundException();
    }

    private byte[] createProfileFileMissionPackage(ArrayList<ProfileDirectoryContent> contents) {
        try {

            MissionPackage mp = new MissionPackage("multiFile");
            mp.addParameter("uid", UUID.randomUUID().toString());
            mp.addParameter("name", "multiFile");
            mp.addParameter("onReceiveImport", "true");
            mp.addParameter("onReceiveDelete", "true");

            for (ProfileDirectoryContent content : contents) {
                File file = content.file;

                //
                // walk the parent file up the chain to get the directory hierarchy
                //
                ArrayList<String> directories = new ArrayList<>();
                while (file.getParentFile() != null) {
                    file = file.getParentFile();
                    // bail when we come to the profile directory
                    if (file.getName().compareTo(content.profileDirectory.getPath()) == 0) {
                        break;
                    }
                    directories.add(file.getName());
                }

                //
                // recreate the directory hierarchy in the zip file
                //
                String directory = "";
                ListIterator<String> it = directories.listIterator(directories.size());
                while (it.hasPrevious()) {
                    directory += it.previous() + "/";
                    try {
                        mp.addDirectory(directory);
                    } catch (ZipException ze) {
                        // ignore duplicate directories
                    }
                }

                // add the file
                mp.addFile(directory + content.file.getName(), content.contents);
            }

            return mp.save();
        } catch (Exception e) {
            logger.error("exception in createProfileFileMissionPackage!", e);
            return null;
        }
    }

    private void setContentType(String contentType) {
        if (response.containsHeader("Content-Type")) {
            response.setHeader("Content-Type", contentType);
        } else {
            response.addHeader("Content-Type", contentType);
        }
    }

    private void setContentLength(int contentLength) {
        if (response.containsHeader("Content-Length")) {
            response.setHeader("Content-Length", Integer.toString(contentLength));
        } else {
            response.addHeader("Content-Length", Integer.toString(contentLength));
        }
    }

    public byte[] getProfileDirectoryContent(
            String toolName, String[] relativePaths, Long syncSecago, String clientUid)
            throws ValidationException, IntrusionException, RemoteException {
        try {

            //
            // validate each relativePath and collect up contents, keep track of the most recent modified time
            //
            long lastModified = Long.MIN_VALUE;
            ArrayList<ProfileDirectoryContent> contents = new ArrayList<>();
            for (String relativePath : relativePaths) {

                String groupVector = martiUtil.getGroupVectorBitString(request);
                List<ProfileDirectory> directories = getProfileDirectoriesForTool(groupVector,
                        toolName, syncSecago);

                if (directories.size() == 0) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return null;
                }

                List<ProfileDirectoryContent> profileDirectoryContents = getProfileDirectoryContent(
                        relativePath, directories);
                if (profileDirectoryContents == null) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return null;
                }

                contents.addAll(profileDirectoryContents);

                for (ProfileDirectoryContent profileDirectoryContent : profileDirectoryContents) {
                    if (profileDirectoryContent.file.lastModified() > lastModified) {
                        lastModified = profileDirectoryContent.file.lastModified();
                    }
                }
            }

            if (contents.size() == 0) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }

            //
            // set the Last-Modified header with a RFC 1123 formatted date
            //
            ZonedDateTime lastModifiedZDT = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(lastModified / 1000), ZoneOffset.UTC);
            response.addHeader("Last-Modified", lastModifiedZDT.format(DateTimeFormatter.RFC_1123_DATE_TIME));

            //
            // if the caller supplied an If-Modified-Since header, remove any older content
            //
            String ifModifiedSince = request.getHeader("If-Modified-Since");
            ListIterator<ProfileDirectoryContent> contentsIterator = contents.listIterator();
            while (contentsIterator.hasNext()) {
                ProfileDirectoryContent nextContent = contentsIterator.next();

                ZonedDateTime nextLastModifiedZDT = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(nextContent.file.lastModified() / 1000), ZoneOffset.UTC);

                if (ifModifiedSince != null) {
                    ZonedDateTime ifModifiedSinceZDT = ZonedDateTime.parse(
                            ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
                    if (!nextLastModifiedZDT.isAfter(ifModifiedSinceZDT)) {
                        contentsIterator.remove();
                        continue;
                    }
                }
            }

            //
            // if we have more than one file, return as a mission package
            //
            if (contents.size() > 1) {
                response.addHeader(
                        "Content-Disposition",
                        "attachment; filename=" + defaultFilename);
                setContentType("application/zip");
                byte[] missionPackage = createProfileFileMissionPackage(contents);
                setContentLength(missionPackage.length);
                return missionPackage;

            //
            // single files get returned on their own
            //
            } else if (contents.size() == 1) {
                ProfileDirectoryContent content = contents.get(0);
                response.addHeader(
                        "Content-Disposition",
                        "attachment; filename=" + content.file.getName());
                String contentType = URLConnection.guessContentTypeFromName(content.file.getName());
                setContentType(contentType);
                setContentLength(content.contents.length);
                return content.contents;

            //
            // no files left after If-Modified-Since checks so return a 304
            //
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return null;
            }

        } catch (NotFoundException nfe) {
            throw nfe;
        } catch (Exception e) {
            throw new TakException("exception in getProfileDirectoryContent", e);
        }
    }
}
