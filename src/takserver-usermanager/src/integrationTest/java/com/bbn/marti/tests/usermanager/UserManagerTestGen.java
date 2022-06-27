package com.bbn.marti.tests.usermanager;

import com.bbn.marti.takcl.AppModules.TAKCLConfigModule;
import com.bbn.marti.test.shared.data.users.MutableUser;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Used to generate user manager test scenarios
 * <p>
 * Created on 9/25/17.
 */
public class UserManagerTestGen {

    private static final String STRING_GEN_VALS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // The assigned fingerprints, group names, and cert paths are assigned from lists that 
    // will loop around when the end is reached.
    private int fingerprintArrayPointer = 0;
    private int groupNameArrayPointer = 0;
    private int certPathPointer = 0;

    private final Random rand;

    private final boolean randomize;

    private static final long DEFAULT_RANDOM_SEED = 2111884417934772713L;

    /**
     * It can either be consistently random with the default seed or inconsistently random
     * with a custom seed, take your pick
     *
     * @param shakeThingsUp Whether or not to "shake" the generated permutations up
     * @param randomSeed    The random seed to use to generate the scenarios
     */
    public UserManagerTestGen(boolean shakeThingsUp, @Nullable Long randomSeed) {
        randomize = shakeThingsUp;
        if (randomSeed == null) {
            rand = new Random(DEFAULT_RANDOM_SEED);
        } else {
            rand = new Random(randomSeed);
        }
    }


    /**
     * Gets the next fingerprint from the list of valid fingerprints. Goes back to start when the end is reached.
     *
     * @return A fingerprint
     */
    private String getFingerprint() {
        String rval = fingerprintArray[fingerprintArrayPointer];
        fingerprintArrayPointer++;
        if (fingerprintArrayPointer >= fingerprintArray.length) {
            fingerprintArrayPointer = 0;
        }
        return rval;
    }

    /**
     * Gets a certificate from a list of certificates stored as resources in takcl
     * They are written to disk if they do not exist so that they can be read in by the normal API
     *
     * @return A certificate filepath
     */
    private String getCertPath() {
        try {
            File f = new File(TAKCLConfigModule.getInstance().getTemporaryDirectory(), "user00" + certPathPointer + ".pem");

            if (!f.exists()) {
//                String inputPath = "/certpool/user00" + Integer.toString(certPathPointer) + ".pem";
//                FileInputStream certStream = new FileInputStream(new File("resources" + inputPath));
                InputStream certStream = UserManagerTestGen.class.getResourceAsStream("/certpool/user00" + Integer.toString(certPathPointer) + ".pem");
//                    InputStream certStream = UserManagementTests.class.getResourceAsStream(inputPath);

//                    FileUtils.copyInputStreamToFile(certStream, f);
                FileUtils.copyInputStreamToFile(certStream, new File(TAKCLConfigModule.getInstance().getTemporaryDirectory(), "user00" + certPathPointer + ".pem"));
            }

            if (certPathPointer >= 10) {
                certPathPointer = 0;
            }

            return f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a group from the list of groups, looping back to the start if the end is reached
     *
     * @return The group identifier
     */
    private String getGroup() {
        String rval = groupNameArray[groupNameArrayPointer];
        groupNameArrayPointer++;
        if (groupNameArrayPointer >= groupNameArray.length) {
            groupNameArrayPointer = 0;
        }
        return rval;
    }

    /**
     * Gets a list of groups from the list of default groups and randomly. This adds a little more
     * variability.
     *
     * @param defaultGroups The number of default groups to add
     * @param randomGroups  The number of randomly generated groups to add
     * @return The group list
     */
    private String[] getGroups(int defaultGroups, int randomGroups) {
        String[] groupNames = new String[defaultGroups + randomGroups];

        for (int i = 0; i < defaultGroups; i++) {
            groupNames[i] = getGroup();
        }
        for (int i = 0; i < randomGroups; i++) {
            groupNames[defaultGroups + i] = genRandomString(6);
        }
        return groupNames;
    }

    /**
     * Generates a random string of the given length
     *
     * @param length The length of the String
     * @return A string of the length
     */
    private String genRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(STRING_GEN_VALS.charAt(rand.nextInt(STRING_GEN_VALS.length())));
        }
        return sb.toString();
    }


    /**
     * Generates a set of test scenarios using all known APIs the provided {@link UserManagerTestEngine} as the backing and validation
     * store and the provided user.
     *
     * @param umte         The test engine
     * @param existingUser the user to test with
     * @return A list of test configuraitons
     */
    public List<UserCertModConfig> generateTestScenarios(@NotNull UserManagerTestEngine umte, @NotNull MutableUser existingUser) {
        List<UserCertModConfig> rval = new LinkedList<>();
        for (ModMode m : ModMode.values()) {
            rval.addAll(generateTestScenarios(umte, m, existingUser));
        }
        if (randomize) {
            Collections.shuffle(rval, rand);
        }
        return rval;
    }

    /**
     * Generates a set of test scenarios
     *
     * @param umte         The test engine
     * @param existingUser The user to test with
     * @param modMode      The api method to test
     * @return A lest of test configurations
     */
    private List<UserCertModConfig> generateTestScenarios(@NotNull UserManagerTestEngine umte, @NotNull ModMode modMode, @NotNull MutableUser existingUser) {
        List<UserCertModConfig> rval = new LinkedList<>();

        // The possible boolean state values
        boolean[] stateToggle = new boolean[]{true, false};

        // The possible Boolean state values
        Boolean[] stateNullToggle = new Boolean[]{null, true, false};

        // Group pairings ("number of default groups", "number of random groups") that offer decent coverage
        int[][] groupConfigs = {
                {2, 0},
                {1, 1},
                {0, 1}
        };

        // We want to test combinations of...

        // password state changes
        for (boolean changePassword : stateToggle) {
            String password = changePassword ? (genRandomString(14) + "^2aX") : null;

            // Administrator state changes
            for (Boolean setAdministrator : stateNullToggle) {

                // Fingerprint state changes
                for (boolean setFingerprint : stateToggle) {
                    String fingerprint = setFingerprint ? getFingerprint() : null;

                    // And group changes
                    for (int[] gc : groupConfigs) {
                        String[] groups = getGroups(gc[0], gc[1]);

                        // For the specified API endpoint
                        switch (modMode) {
                            case CERT_MOD:
                                List<String> certs = new LinkedList<>();
                                certs.add(getCertPath());
                                certs.add(getCertPath());

                                for (String certPath : certs) {

                                    rval.add(new UserCertModConfig(
                                            umte,
                                            existingUser.getServer(),
                                            modMode,
                                            null,
                                            null,
                                            password,
                                            certPath,
                                            setAdministrator,
                                            fingerprint,
                                            groups));
                                }
                                break;

                            case USER_MOD:
                                List<String> usernames = new LinkedList<>();

                                usernames.add(existingUser.getUserName());
                                usernames.add(genRandomString(12));

                                for (String username : usernames) {

                                    for (boolean updateCert : stateToggle) {
                                        String certPath = updateCert ? getCertPath() : null;

                                        rval.add(new UserCertModConfig(
                                                umte,
                                                existingUser.getServer(),
                                                modMode,
                                                username,
                                                null,
                                                password,
                                                certPath,
                                                setAdministrator,
                                                fingerprint,
                                                groups
                                        ));
                                    }
                                }
                                break;

                            default:
                                throw new RuntimeException("Unexpected ModMode '" + modMode.name() + "'!");
                        }

                    }
                }
            }
        }

        if (randomize) {
            Collections.shuffle(rval, rand);
        }
        return rval;
    }


    // The default groups
    private static final String[] groupNameArray = new String[]{
            "group 00",
            "group 01",
            "group 02",
            "group 03",
            "group 04",
            "group 05",
            "group 06",
            "group 07"
    };

    // The possible fingerprints to use
    private static final String[] fingerprintArray = new String[]{
            "9C:66:67:A1:24:BD:C9:EC:4E:1E:17:F2:46:7A:BC:EA:1C:2A:80:10:32:AF:0B:7E:DA:6C:06:E7:BC:AB:C4:B8",
            "AD:07:6F:B1:9B:C9:62:B2:C6:4F:AE:7D:5F:19:FB:CC:2A:3E:30:68:95:CF:17:0C:0D:A0:5D:94:23:9D:D3:78",
            "43:C3:D6:67:C9:89:C7:F8:EC:88:85:2E:4A:57:36:F0:8D:CB:BE:F4:50:85:26:83:F0:5E:60:32:A0:47:C7:C1",
            "68:9E:83:F6:C4:AF:4D:67:54:9A:31:E3:F0:73:86:08:54:8A:85:D1:49:A2:0C:2E:0B:3A:83:D7:E1:41:7C:2E",
            "EF:1E:55:BE:96:CF:9B:98:18:26:DE:AA:5B:4F:A9:F0:60:BA:BA:BB:9F:24:A0:5D:1E:65:80:06:BA:A3:45:C8",
            "BD:11:EC:B2:2C:F4:94:B8:10:6A:50:3F:23:20:18:02:D4:AD:71:81:32:56:1E:53:BE:58:97:D1:F4:D5:27:0D",
            "2F:BD:A8:9C:71:81:A4:3E:8A:23:4A:5A:1E:A2:C3:07:9C:8A:D9:80:EA:C8:8C:1D:59:FC:95:BC:8D:50:2B:9D",
            "5D:34:C1:56:90:68:E5:D9:D3:D3:47:41:09:D7:1F:9D:AF:33:B6:77:BD:7A:ED:EF:13:F4:D6:A2:A9:93:D0:0F",
            "28:EE:BA:02:31:06:2B:FA:83:03:44:DB:92:FB:09:E6:CC:BD:2E:41:04:A8:0B:0B:FA:D7:0E:2F:0E:68:41:11",
            "BE:19:9C:A1:45:96:73:76:55:31:C9:A6:FD:90:CF:4C:56:03:46:C8:75:30:E5:1F:23:5C:BB:2C:9E:FD:9D:26",
            "58:20:5E:31:1C:0F:BD:D2:D2:53:C0:A1:42:54:57:F0:AC:BE:A0:A4:23:78:8C:5D:98:2C:72:18:AE:08:B4:7D",
            "E7:C0:3A:69:6C:3A:25:17:BF:01:B8:57:60:95:D5:F4:9B:28:AB:E7:6E:D0:A7:68:E7:E3:76:9D:74:92:D2:53",
            "1A:BC:0B:11:6C:5B:C8:DB:EE:32:2F:BE:CB:DA:F0:D8:70:10:09:A4:94:FF:75:AE:74:E2:ED:AD:9C:C8:60:8C",
            "8D:64:9A:AD:A1:90:74:41:8C:B1:10:5B:F6:70:87:BF:BE:B6:6F:96:34:C5:00:73:11:E9:DB:D4:D4:92:FB:48",
            "5A:D1:B5:09:AB:EC:08:F8:4B:82:63:E1:EB:D1:EB:2F:06:58:1B:28:5B:AC:32:17:B6:0B:05:C5:01:01:7A:FB",
            "44:E9:2C:C2:81:54:22:1C:3B:37:5F:F7:D0:20:07:5B:47:C0:F5:7A:84:7D:1C:5F:AF:EC:C0:F9:9B:86:3D:AB",
            "C4:0F:70:6A:35:E7:BF:96:D9:21:CA:A6:F3:EA:0E:08:AC:E8:3C:97:F0:67:2F:A6:28:85:3D:5F:94:25:B3:96",
            "4C:A9:C9:47:0C:38:89:ED:6B:43:DC:68:E6:43:AF:A3:5F:3F:AC:6F:D9:4F:6B:18:1E:2E:89:43:7A:46:9D:A4",
            "DC:50:31:D6:9D:70:42:1A:7A:AE:BB:83:C0:99:8E:0F:85:F5:E3:53:3A:0B:9B:AE:D7:C9:95:D2:72:54:AD:97",
            "27:E8:60:71:4A:03:6A:7C:AB:C3:61:CC:23:8D:B6:4D:AD:2D:3D:3B:09:3C:E2:12:13:B8:0A:D1:07:49:28:50",
            "BB:7C:E8:EC:DB:5D:46:1B:37:E5:22:96:98:D0:70:96:17:4C:D2:E5:CF:0B:06:98:99:24:91:20:30:A2:D2:8C",
            "FD:25:F3:BF:E1:AD:71:88:F7:CC:B5:84:B1:F8:20:51:C1:9D:7A:09:81:14:F5:CE:1C:3F:95:49:E3:98:36:F4",
            "88:B9:53:EE:6D:DE:CD:F6:4C:4F:45:EC:DA:2D:8B:21:35:F8:A4:47:39:2C:9B:AA:D7:C9:6C:EA:7F:16:D1:C0",
            "DD:23:1B:FD:E9:F3:9F:5C:43:03:F3:C7:E7:55:0F:19:90:6D:42:0A:09:82:FF:DB:04:CE:49:4F:ED:17:41:AC",
            "7E:8A:54:01:03:67:44:D2:AF:C2:39:3A:D9:93:6F:40:5B:87:4C:D4:57:09:96:44:DC:74:22:E8:5F:C3:F4:F7",
            "F7:CF:7B:08:4D:B8:AF:95:F8:88:0F:D8:0C:CE:D3:09:0D:59:9C:98:F9:61:9F:89:F7:CD:F1:75:E6:F6:78:3D",
            "EE:24:54:2B:39:7C:50:27:AA:8D:F6:9B:17:3B:9E:EC:81:8C:9E:60:2A:56:9C:59:C6:3A:D3:7F:57:54:90:B1",
            "8B:94:7B:4F:00:74:19:F1:93:3C:45:C0:D3:60:C4:AF:CA:F1:40:DB:8E:26:D9:49:F5:87:57:4B:69:AE:53:C5",
            "F9:CF:85:D7:06:56:16:BB:42:7F:D2:11:A4:E0:58:00:2B:8A:D5:8A:BA:84:1C:32:61:B9:B4:EB:E8:4B:3D:5C",
            "81:F7:70:8D:3B:BC:C8:84:C5:3C:F2:5B:A2:0F:3F:15:FA:12:F4:55:FC:3B:23:7F:97:1A:D8:DE:A0:77:9C:35",
            "CA:05:F2:70:81:1D:3A:D0:F5:A9:0D:42:49:84:66:D1:53:7B:3F:E7:8A:AA:00:9A:44:CD:77:FB:92:49:EA:45",
            "D5:4E:8E:A0:E9:8D:E8:C5:E6:95:5A:A2:A4:40:66:A3:E2:1A:AA:0E:10:1C:E6:13:E0:75:6E:2C:48:05:C8:6F",
            "FC:AB:D7:CA:27:0C:A1:23:10:73:61:AD:A3:97:F8:27:79:0A:A0:97:DC:01:7E:CD:4D:5E:62:7B:08:BB:1F:92",
            "D6:93:AA:3E:6D:DB:1C:D3:FE:64:AC:62:64:AD:45:69:69:15:8A:CE:15:25:B4:95:A2:88:E7:EF:62:39:8D:05",
            "1F:9C:DF:B2:51:22:3F:D1:E5:22:7C:11:C8:62:FE:EC:6D:BC:D0:AB:01:62:BB:DD:0D:F5:7B:8D:FC:D4:66:CF",
            "36:FE:9E:64:81:CB:18:12:A3:3E:4D:9F:52:07:FB:97:6B:33:26:38:F3:7A:7F:CC:06:45:BC:A4:AA:26:83:6A",
            "A0:1E:A9:45:AD:EC:19:EE:27:51:4F:92:BA:69:BD:CB:26:4E:22:D5:96:B9:93:A7:91:2D:E9:5A:95:C5:8D:36",
            "E1:A2:1F:94:62:B1:E3:76:0B:68:E9:2C:70:E7:44:4D:45:AB:F5:F9:E7:74:06:8C:5A:CF:51:26:15:8C:DC:76",
            "53:A1:D2:FC:86:8C:08:A0:4A:94:D1:B8:9E:80:F0:5D:76:70:A4:65:05:7A:AA:48:08:AE:DA:3F:FA:80:7E:86",
            "B5:B8:D1:EF:51:2B:74:D1:04:F5:9D:C2:A6:9A:02:AA:79:25:8E:EE:DD:FE:8B:48:A4:36:F0:50:08:9B:35:C4",
            "DB:B1:F2:A7:5E:F7:B5:4F:CC:C5:45:D9:80:31:B4:91:4A:02:8E:13:A7:54:A3:FD:26:43:6A:F2:5B:C1:CF:BE",
            "CD:7C:2E:24:7D:6A:C6:C7:CA:01:10:42:F8:65:6F:72:71:31:0F:94:42:8D:50:D3:5E:EA:CC:66:12:99:8B:B7",
            "72:17:A5:46:D7:7A:15:4D:C2:96:EC:9C:6E:05:9C:E9:95:E3:D1:B0:6A:BA:AA:EE:F6:F1:D8:0A:54:1E:6B:94",
            "6B:2B:CF:FA:64:B8:AE:5B:13:DE:62:74:C4:FE:84:DE:F6:9C:C4:0F:78:5F:E5:53:8D:3C:17:7D:B6:40:94:5F",
            "D0:F5:73:06:34:29:0A:D9:83:AA:DC:BF:46:DD:E2:D2:95:B4:03:5D:81:39:62:7E:F5:DF:0E:07:8A:F0:0F:19",
            "4B:4C:E5:72:2F:7F:A8:67:20:BC:93:D5:AA:EF:09:6C:69:7E:3C:1D:45:B2:A5:E2:20:57:EC:CA:A8:2D:71:EC",
            "CA:3D:68:AE:BA:0D:27:DA:2F:A4:2F:ED:0A:AA:44:5B:6A:17:44:24:5C:18:86:7E:92:03:60:F8:F0:11:70:D6",
            "65:09:27:61:70:6F:61:EA:8E:2D:79:73:2E:EF:88:5F:93:8A:62:B0:3F:64:79:22:30:FE:65:A7:CA:38:09:B6",
            "62:9E:2E:BD:96:BD:3F:07:07:6A:AE:C2:D1:E1:9D:16:D2:78:43:8A:BE:59:86:FC:C2:57:54:1E:D0:00:D0:18",
            "85:5B:F8:1F:3E:68:EF:A5:72:98:BC:9C:3C:E7:52:50:AE:1A:7C:E3:7D:90:DA:74:20:C4:1D:6A:8E:0D:EF:2C",
            "A3:D5:88:A0:50:5A:8E:60:D1:6C:0D:6C:12:1C:CE:71:91:14:5A:4E:84:3F:DB:BC:D6:0E:AE:F2:57:15:4A:E9",
            "11:40:49:51:18:27:BB:C6:E7:43:7A:83:3D:F9:20:7B:35:73:9E:69:62:58:A9:9E:3E:93:3F:F7:B4:B6:74:29",
            "D1:D0:85:FD:1A:D6:48:87:80:6D:03:00:22:FA:03:22:17:98:61:06:DA:08:1E:78:13:4C:DF:CA:10:EA:EB:6E",
            "67:29:26:28:94:97:68:F6:04:A4:E4:2B:CE:5A:AA:BC:33:15:F4:82:3B:E5:0A:ED:51:2D:9B:86:10:6C:59:07",
            "C6:17:A6:07:F7:11:46:2B:14:BF:E3:6A:F3:79:92:AE:BA:02:79:0D:15:85:C9:F1:77:4F:C3:13:FC:04:90:56",
            "E8:27:11:EA:4F:D7:D1:53:C1:5A:A8:15:51:1E:45:24:5E:67:BF:4A:6D:67:F4:11:68:DF:2B:97:8F:CF:78:07",
            "3E:BD:C9:DA:76:92:CA:95:ED:7D:AF:E7:6D:B6:8C:C3:53:9C:23:1A:E6:9F:D3:A6:07:11:69:CC:76:AE:53:13",
            "AA:C5:CC:84:C4:00:D6:27:4B:C2:25:58:D8:0B:D4:62:32:71:45:56:2B:1C:27:1C:00:65:D7:15:49:70:9D:37",
            "2D:D0:FA:49:4F:6C:2C:C7:B6:D0:FB:5C:B1:73:05:63:B4:4E:9A:EB:1C:65:52:45:2D:68:73:FA:85:6E:08:B4",
            "62:0E:2B:83:20:EF:42:D0:26:9C:A0:AF:6B:27:B9:66:B5:AC:E0:D4:EF:B8:FD:90:7B:E1:35:66:04:54:27:1D",
            "44:45:34:A9:54:18:72:1B:17:2C:A1:F2:2E:37:F3:33:A6:E3:BB:D9:61:11:78:08:82:2C:27:27:71:C2:CD:FD",
            "52:9A:75:FE:83:09:47:A4:99:25:20:EE:B1:04:D7:15:6E:DF:06:81:01:45:70:2C:78:A4:F4:A3:1B:39:7B:CE",
            "2E:4B:91:01:BE:00:37:B2:BB:DD:12:37:18:EF:D6:34:11:09:CB:85:AE:8D:29:97:A7:83:03:52:88:86:53:7E",
            "64:D4:E3:97:4D:64:13:34:32:43:1B:78:7A:44:8F:4B:C3:C1:54:63:20:AA:43:CB:48:E0:03:D5:5A:6C:A6:BE",
            "C4:05:37:F1:51:D1:72:E7:D0:E2:36:A6:1F:AF:8B:14:00:A3:BF:9D:78:41:DE:1A:E4:04:C8:E7:51:25:8A:3B",
            "4E:8E:C9:DD:5C:A3:08:9E:09:ED:CE:10:6A:CA:01:FB:ED:8B:D8:44:60:C7:84:21:32:7B:B2:01:19:B2:2D:82",
            "49:2D:E1:AF:2D:53:F4:DD:35:E5:21:E4:6E:AD:0B:89:E5:F2:11:33:1B:7F:86:74:F2:32:7B:12:90:52:8E:54",
            "93:6C:9C:1A:B1:99:CE:F8:99:C0:7B:CB:BF:86:97:D4:34:83:A4:B6:05:31:2D:FF:A8:0B:D1:EE:4B:91:A2:85",
            "93:0E:AD:C7:C9:75:CE:8B:53:24:A7:7E:39:8D:B1:77:DF:C6:B6:5E:71:64:DA:6C:C5:95:39:FA:0D:BC:7F:A5",
            "DD:9A:DB:6F:FA:40:E2:02:D9:C2:5E:93:F0:9A:59:CB:D2:85:19:67:17:8F:2B:04:DF:2B:15:53:1B:F7:FB:2F",
            "8A:D5:1F:72:1B:2D:A6:2E:DF:18:28:8C:05:9B:0D:EF:4A:47:41:46:7C:BF:D7:83:86:51:07:0F:F4:A1:98:FD",
            "53:FC:EF:18:14:27:92:67:45:55:A4:D6:5C:03:8B:D0:AC:E0:EB:31:F0:AA:F1:8D:FD:8A:81:85:49:09:3F:87",
            "E8:46:F6:3A:7D:2C:05:6B:D1:AB:DB:0F:4F:BB:02:58:D0:5F:64:F8:C3:DC:2B:AC:C2:09:F7:41:41:13:38:92",
            "A3:A0:B4:88:62:E5:FE:BA:3E:EC:91:A6:9F:53:3C:05:45:96:AB:44:83:C8:E8:39:46:2A:8C:38:DC:74:00:92",
            "7C:90:46:2E:8D:57:8B:EC:AA:8B:F1:9F:C0:5D:21:EB:FB:45:CE:CA:0D:4A:09:15:7B:C4:EC:44:AA:72:96:4E",
            "F9:71:94:D2:E1:A6:1C:9B:6D:51:9D:8B:E1:85:EE:6B:BD:00:08:27:25:74:CB:2E:FF:87:F2:4E:A6:BD:4E:16",
            "9F:C0:BE:3C:04:A0:4F:2D:8D:A1:BB:A1:9E:B4:74:4A:72:6E:7D:A8:89:48:0D:53:B9:9A:23:94:D9:8C:BD:07",
            "FC:6D:C1:A2:E0:0A:29:0A:B8:6D:95:FB:E5:CD:C4:46:CA:41:8A:7F:54:B9:20:3B:EE:09:55:59:82:D0:64:A7",
            "3E:AE:6C:91:3B:45:A2:D2:64:E9:57:AD:36:5A:FD:25:27:35:7C:AE:66:B5:47:86:88:83:B2:0C:54:CC:62:76",
            "20:C9:3A:93:2B:DA:DE:3B:7D:EC:8B:1A:3D:EB:3B:FA:6A:34:90:53:71:70:E9:E6:78:93:7E:96:BD:CB:1D:FC",
            "28:BE:41:60:ED:9E:BE:94:95:E2:58:72:88:55:52:C2:72:80:41:2C:D3:0A:33:F4:3B:2E:59:62:F2:86:21:17",
            "C8:8C:EE:1C:39:D8:86:FE:A4:5F:BD:54:26:6D:66:EF:CB:53:9E:F3:F3:1F:44:9C:7C:93:39:4E:95:FC:F5:74",
            "84:B5:0C:B6:FE:1A:8B:49:78:F0:51:9A:8E:36:BB:87:F6:4E:D1:BE:5F:0A:2A:C6:AD:D8:2D:0E:CC:F4:FE:05",
            "4A:39:BE:74:E6:D9:61:D0:15:1D:96:FF:71:70:70:FB:84:50:32:02:E1:A3:56:45:39:49:AA:69:1A:78:CD:F4",
            "8D:D3:AF:C6:90:1C:E2:50:1E:8E:8C:3C:E5:28:F7:93:B5:03:E4:D9:92:05:EC:E9:12:7E:8D:46:6D:19:FE:EA",
            "2E:8A:5A:2C:20:F8:E5:92:92:8D:12:01:66:62:D1:B5:C0:0E:88:2C:8C:AE:71:BE:0C:B1:16:F7:FB:65:3F:F6",
            "5F:96:A6:44:26:B9:0C:D1:99:53:13:15:9F:E2:98:CA:03:AB:50:DE:53:7D:36:35:66:32:DE:16:5E:C5:3E:9F",
            "FF:2D:BC:E2:2F:D0:9A:BB:E9:20:01:4F:57:61:84:A3:50:05:B7:B3:3C:D7:6B:DC:F8:F0:72:8F:B7:8C:EB:40",
            "9F:4E:3F:A4:8D:EE:8B:2D:6F:E5:FC:12:47:DE:DD:69:89:74:39:58:EE:25:BE:D3:E3:DF:CA:0F:FC:0E:F1:91",
            "DE:4C:CC:35:D7:0D:DE:F4:33:A1:2E:9D:97:A5:E0:62:E8:A7:F8:8D:92:65:E0:BD:4C:F0:F1:E0:49:71:A6:EE",
            "A1:96:BC:48:1C:A0:D8:4D:BF:0F:EB:41:91:35:74:AD:32:E7:11:C8:F0:5C:C9:CB:8E:5E:69:A5:A1:D1:52:DE",
            "EE:71:6E:77:74:13:55:55:82:69:58:C2:48:21:D0:0E:34:13:D8:87:FA:2C:C1:32:87:DE:04:EB:26:9F:C7:07",
            "88:77:CB:5A:40:B4:FF:90:CE:65:59:63:83:17:28:4F:6B:52:0E:E5:CE:01:54:D6:EC:AB:B6:D7:16:8E:F1:BF",
            "DB:05:75:B7:35:3E:AA:C0:AD:B4:71:09:CB:F1:C5:14:51:11:61:14:58:BF:18:A9:A2:98:DC:5C:6B:35:67:FD",
            "6B:66:AA:BE:47:15:39:07:DF:5C:23:A8:17:38:FC:EA:9C:90:2D:81:F0:68:20:D4:F3:FC:F8:93:9A:03:E3:71",
            "76:2D:7F:40:F3:EC:AF:5F:DB:0C:6D:57:BD:75:14:CC:D6:B5:34:8B:16:19:2A:1B:D4:4E:E4:7A:1A:F8:68:A0",
            "18:4C:F1:76:30:9E:05:D2:58:B0:A0:33:91:08:BD:9E:4A:A2:56:F9:D6:99:82:80:9D:5D:06:34:F6:62:02:76",
            "C4:9B:84:65:59:2C:EB:2E:57:4C:ED:C3:23:AA:43:C4:F2:34:9F:D0:DC:76:92:CE:67:3E:8C:00:54:5A:38:25",
            "11:85:33:0C:5E:72:6F:58:CB:28:02:2D:99:9C:5F:1F:90:47:98:66:9E:F9:42:6E:05:E5:9D:F4:DB:9D:1B:D8",
            "50:13:42:67:83:F2:D8:F0:C3:1A:C6:5E:3D:FA:09:53:75:DE:B0:46:03:20:3E:69:7A:F5:8C:38:7C:AE:10:DB",
            "35:44:B2:7E:2F:0E:58:C6:C7:57:E9:EC:E9:76:52:D5:84:88:84:22:B5:B2:AF:99:64:9F:44:F2:C2:18:AB:70",
            "61:F5:F9:F3:44:29:A2:7D:3C:9C:15:53:8B:0B:FF:B5:F9:30:44:C9:11:92:16:80:19:1B:6E:06:D7:B2:FF:C0",
            "85:D8:34:8E:2B:5F:20:61:AD:25:B9:CC:1F:8A:A5:B2:F1:3B:51:DC:93:64:06:01:1F:E4:EC:41:95:B0:DE:E6",
            "4D:71:9C:43:86:D2:05:13:90:B7:03:B2:1D:2D:6C:43:22:07:DD:0A:4A:F9:56:DB:46:19:10:29:A1:60:1D:A5",
            "7F:69:95:D8:E2:92:01:C0:D1:FF:C1:F2:D8:AC:85:AA:52:F0:EA:23:52:34:34:AE:C9:2C:B4:7D:06:6F:EF:68",
            "21:A7:CC:02:53:CB:C2:19:4A:C6:E9:C2:05:68:81:92:4B:40:C0:82:92:15:B4:11:A5:4D:6B:A8:60:81:10:C8",
            "44:C1:EC:79:BB:AA:19:B4:E2:F7:CA:EB:18:2E:04:2C:74:40:D9:DD:8F:DE:7F:6C:91:19:F4:DD:F3:B1:8C:31",
            "74:AA:1F:2E:4B:FC:91:36:6F:08:01:29:40:C9:86:24:16:E1:2F:3F:E1:25:B7:68:6A:C2:8A:46:9B:AD:2F:A4",
            "78:AF:0F:3A:E7:8A:A6:21:06:92:F9:3C:B1:9A:19:65:79:B7:66:85:BC:F7:56:6D:80:73:95:F4:8B:87:29:94",
            "A8:2C:73:C9:8D:81:88:DB:A3:A0:1B:CE:4D:F2:7B:B3:D9:AA:8A:94:E3:8B:8D:17:1D:24:BC:DF:92:55:CF:AD",
            "9E:F4:25:F6:89:06:A8:05:22:09:E0:B0:C8:7E:38:F6:03:29:4D:63:F3:E6:4C:B2:D3:A9:53:D5:93:AE:A9:25",
            "2A:CA:A9:EC:33:24:E3:F7:A8:13:55:70:BB:88:55:83:78:27:0E:2E:61:FF:CB:9D:53:E9:4C:D8:AF:36:99:38",
            "DE:B9:B4:0C:14:E3:91:12:D7:A9:D6:62:5C:08:F7:E4:96:93:F6:D9:47:1B:6F:9D:1B:AA:BB:16:EC:7D:2A:B0",
            "81:1E:91:21:FA:93:39:56:22:FA:03:1D:B8:8C:CA:1B:64:12:D8:8C:77:59:59:87:8C:95:7D:C8:A3:FF:07:77",
            "AA:3D:E4:C9:09:6E:FA:AF:88:F7:50:4D:0B:65:6F:53:D9:C2:71:30:75:EB:98:2B:F9:01:45:35:11:E4:04:33",
            "2C:6C:DE:14:18:43:A2:CD:B4:7F:76:90:B9:63:65:54:B0:A5:4E:4B:DA:FE:54:FE:EC:91:FD:88:26:CE:B9:20",
            "84:2E:3B:27:2A:2D:1D:4E:BE:F4:42:B4:C3:01:5E:0A:02:A4:A3:5C:C7:88:84:F7:A0:53:FB:89:6E:19:95:CB",
            "AC:0C:DF:14:8B:FC:14:AC:0C:E6:0F:5C:F8:D3:42:F1:B7:E7:CD:69:C0:C0:44:C5:BC:A3:5C:E6:AF:E4:24:D2",
            "F0:E2:B9:C7:8B:44:25:D1:61:90:F0:BD:03:CB:AB:D3:EF:FD:01:35:44:A1:92:07:FA:0F:53:13:EE:FC:1C:44",
            "CE:5B:0E:CE:97:B0:E0:76:39:41:0D:13:FE:B8:3F:1F:56:66:24:FB:F4:FB:DD:AB:1D:F5:94:49:55:9A:5C:0D",
            "FC:49:E8:F1:7F:90:DC:6F:0D:E8:66:4B:AE:88:B3:6F:C4:04:A3:AD:10:20:A0:9F:F1:DA:1E:4E:E2:5B:1A:12",
            "0A:7A:A5:95:91:1F:19:92:BC:A9:6E:CE:80:8F:60:7F:18:D3:60:DE:46:FB:C5:EE:13:19:22:5A:79:0D:B3:C9",
            "63:BD:AD:45:C5:4C:71:A2:DD:67:A3:2F:86:4B:76:4A:CF:FD:F4:78:5E:46:CE:EE:71:ED:F7:A4:54:90:EA:A9",
            "06:E2:76:74:FF:25:8E:C9:3F:49:F5:AB:8D:09:C1:26:39:47:6B:3A:C7:FC:32:DB:73:D7:63:DE:DE:5F:52:10",
            "3A:48:C1:E9:DB:60:84:DC:1D:BC:73:60:25:F3:62:EA:D2:C6:76:B0:B4:33:53:B3:78:B2:E9:16:58:2B:AB:25",
            "F8:9C:5F:C1:EA:36:B9:C2:6B:B8:5F:69:D5:E0:63:BB:77:D0:85:5E:A9:E5:5D:69:2E:46:0D:D2:37:47:C4:30",
            "73:79:28:D0:73:18:53:6F:18:50:8A:F0:74:D6:5B:8A:EB:61:EC:DB:6E:C6:5A:7A:8E:36:08:D7:1E:6E:B5:09",
            "24:19:8D:18:62:52:75:3F:9F:BF:31:E0:31:F3:DF:84:C6:B5:03:9A:DE:1F:A4:13:D7:C1:DC:6A:9A:3E:19:16",
            "58:F1:D5:FC:03:53:6A:94:17:A3:E0:12:71:2E:91:6D:8F:94:FD:11:4D:0C:B4:AE:64:6C:FD:FF:FD:11:B3:4D",
            "A6:8B:17:88:96:8E:8A:69:27:0E:41:73:4B:2C:31:92:22:90:C0:9B:27:C0:88:A2:91:BE:25:91:37:D7:FF:5F",
            "BC:5B:1C:6D:DC:90:84:EC:38:08:54:0C:BA:A3:96:92:88:67:5A:00:92:47:8E:13:66:11:1F:C8:22:0C:7E:5C",
            "52:57:7C:C1:C8:0F:E5:D2:5C:1D:67:83:58:A9:51:71:1D:86:12:EB:EA:F3:D2:8C:E0:45:99:36:EC:CD:65:D0",
            "60:8E:16:92:9E:6B:33:68:89:BD:81:61:CF:EC:AA:7F:92:18:B7:7E:22:99:19:E0:09:E1:09:DC:C7:B3:0F:FE",
            "DE:62:94:FE:05:1E:72:BE:BE:52:20:DC:5C:61:9F:BB:62:25:79:C9:BA:00:42:A6:84:66:17:25:67:FD:9D:51",
            "DA:1C:74:ED:B4:4D:2E:8A:25:A9:3B:7A:3F:D1:67:6C:96:B2:6D:0B:B8:2C:EB:2F:39:12:FF:44:F3:DE:56:A4",
            "3B:1D:07:EC:DF:7E:5B:05:77:1B:28:F3:76:CB:BE:81:07:B2:FC:DD:C6:9A:4A:13:9A:1A:F1:A0:EC:22:8C:B9",
            "B7:11:83:5B:39:FC:32:D3:E3:E4:6C:6A:07:41:E1:2A:F1:6D:BC:AA:E1:8F:F4:F3:C7:F2:59:00:50:55:9F:CE",
            "9D:1B:BF:A6:7A:CE:29:CD:8A:2D:6F:5B:63:0C:A0:64:71:11:79:76:B1:3A:93:BD:29:03:12:D9:F4:46:2B:D2",
            "FC:40:00:46:E6:C7:97:B7:06:F8:15:4A:A3:FD:61:08:5A:50:5C:6E:B0:90:96:0B:2C:10:ED:E0:89:AA:76:03",
            "D8:DC:E9:39:A7:80:CE:49:31:0C:EC:4F:DE:57:84:E9:19:64:D6:AD:5E:36:C5:9A:CF:18:6C:60:CE:F9:81:68",
            "BB:5C:05:47:FD:D9:3B:1F:7C:29:2F:EE:70:C7:BB:9C:D7:48:14:50:86:F5:74:89:82:A3:C0:EE:5F:3D:6A:AA",
            "8E:8A:54:72:99:75:1D:E0:4D:1C:B2:61:37:DF:FB:FE:CE:A4:99:6D:6C:F9:06:73:A9:83:32:73:C6:5D:54:8A",
            "68:75:CC:40:D4:93:2B:9F:11:44:DB:88:92:26:3C:46:B8:B8:60:1B:9C:D3:5A:3B:14:9C:35:C8:4F:79:4E:A6",
            "F1:9E:93:F1:92:EC:EC:AD:E5:34:9E:17:94:AC:A5:89:94:5F:12:37:89:CE:F4:AB:0D:DF:2A:56:DC:5F:01:90",
            "0C:74:6D:5A:B6:DE:74:E3:A5:21:24:3A:DB:22:4C:7D:81:61:0D:ED:80:75:71:A1:E6:F5:86:DA:4E:4F:EF:9B",
            "D6:79:B5:60:89:5D:15:A1:95:26:92:D3:EC:26:C7:92:D9:F8:E1:D1:BA:19:24:81:9C:7B:4C:87:FB:CA:F0:9E",
            "60:7B:D1:83:B7:97:D3:82:8D:B5:CD:DD:0B:F5:C8:41:F3:21:BB:1F:6D:83:0D:39:70:64:1F:3B:18:E2:8E:6B",
            "F1:A7:CE:17:4A:50:E0:11:79:67:6A:17:54:16:F4:BC:E0:1F:39:A5:93:A4:29:A0:29:6C:04:AB:DD:AF:3B:4D",
            "8C:6C:59:F2:FF:9D:D5:47:D9:EE:37:86:AD:23:AE:73:4E:93:64:5B:08:04:1A:7E:CE:6B:B8:43:57:2E:31:9E",
            "2D:9B:82:E1:E5:83:2C:33:69:C4:83:C1:D0:2A:E7:57:06:58:61:D9:D7:49:26:58:2A:6F:FE:52:7E:3B:2F:B7",
            "F5:B9:55:F3:78:D2:AC:3C:52:B9:3D:C9:43:74:97:62:F9:BE:62:8A:10:7D:AC:0C:5B:53:96:EC:B9:E1:F4:D2",
            "E8:37:39:33:E4:72:33:D5:2B:10:2E:CF:94:D9:C8:74:A8:FF:BF:21:85:FF:3F:E2:34:F1:C9:1F:99:90:CC:4B",
            "C2:A5:B0:C5:AA:30:62:AB:CE:07:25:8C:4A:07:1F:AC:87:F8:B2:9A:BA:2A:D0:F9:08:D4:5B:F5:93:8B:97:DC",
            "81:BD:4D:D8:72:6D:32:18:FB:2E:91:90:5C:B1:D2:F7:09:3C:B3:A4:B4:AE:D9:48:E0:26:4A:BC:E6:FE:C3:D5",
            "70:2C:C8:43:23:7E:76:C2:C5:21:6B:22:BE:93:AC:B5:4B:CD:9B:AC:D2:FE:4A:F4:4C:6D:C5:A5:A9:A3:35:EA",
            "B2:3D:9F:EB:66:8F:C1:06:10:29:38:19:3C:48:A2:1B:2F:C3:7D:75:C4:18:45:19:C3:D0:66:09:95:8A:5F:85",
            "04:B8:01:6F:1B:53:DE:A6:6D:50:3F:4C:9D:59:D9:C6:17:0F:80:04:B1:10:B4:7A:E8:8F:B4:1F:E1:43:57:E6",
            "18:CC:94:C3:5E:93:C7:89:21:72:B3:9B:EB:B3:47:6E:AE:69:A9:D6:1B:AB:D0:BA:D3:A8:3B:E8:2A:15:C6:26",
            "5C:2B:57:D5:35:D7:32:5C:98:BB:1A:06:5D:B4:A2:86:DB:CF:6C:65:40:6D:EB:A7:6A:7D:21:B3:63:8A:26:DB",
            "E6:21:31:1A:45:1A:A8:23:07:29:BF:69:A4:C5:D6:9B:74:EE:47:66:0E:82:17:93:5B:49:8C:22:D2:DF:B6:B6",
            "09:CF:0B:13:28:99:99:57:96:E8:C3:62:19:78:C1:49:F7:A7:88:1D:A3:26:40:75:EE:F0:FC:DA:B4:75:91:82",
            "5E:39:B3:6A:11:AE:8C:08:67:FE:69:76:30:80:D3:B5:5B:6C:6C:74:49:DF:FB:7F:ED:D5:55:B8:EE:DF:96:F7",
            "E4:3A:F3:0B:8E:04:50:FA:1B:00:8D:43:F3:E9:D8:F6:58:92:50:4A:75:D6:E7:25:02:3E:A0:99:AB:CF:82:65",
            "02:4B:B6:50:99:7C:62:E9:03:10:6B:AC:E4:AE:D4:BB:92:B2:D4:30:75:DF:8B:07:52:29:43:16:05:C8:AF:A1",
            "19:9F:83:27:8B:32:2E:60:FE:86:E2:A0:1D:35:A8:B7:1A:66:8A:83:F7:E4:EB:6D:74:E0:C9:A0:92:6F:72:D5",
            "54:6D:13:D5:F3:26:29:6F:02:30:8B:52:DE:4C:21:86:75:19:F6:24:FE:C5:24:6A:90:DE:F2:05:2A:E4:7C:B1",
            "78:0D:AA:10:40:7E:E8:DA:8E:7B:03:8F:9E:4D:F3:FC:38:28:D2:EA:7B:1B:6E:79:02:EE:0C:92:C6:66:3A:4B",
            "9C:33:C1:48:C1:15:05:1C:7E:99:96:17:09:E8:C2:13:AE:46:92:B3:A7:BB:C1:7E:4D:F0:9E:FD:9B:01:43:E3",
            "68:C1:67:FD:C1:2A:B3:14:63:05:0C:AE:EE:F5:20:22:D3:82:2A:E7:FD:E7:61:1E:CC:47:26:52:E1:1E:47:DE",
            "42:E4:EA:D5:B4:2D:A3:B1:B6:86:A2:AE:0E:2F:F0:E9:32:C9:E3:49:0E:24:C1:4E:3B:33:EF:86:F8:16:E9:DF",
            "42:24:84:87:DB:7C:BB:94:9E:25:4D:50:77:3D:27:7D:C3:BF:9D:6F:99:EE:13:D4:02:BC:78:5A:FA:64:F3:72",
            "CF:5F:FC:DB:FB:B4:76:43:33:37:E0:E1:93:C7:CE:FE:07:28:98:E7:8B:67:B3:76:00:99:2C:FD:B4:43:81:32",
            "87:29:F4:6C:05:4B:59:2D:AF:95:96:A3:72:70:CF:B5:EE:E6:17:5B:6E:C5:B9:D9:66:E7:97:E0:7D:78:79:46",
            "84:2B:54:25:DD:33:1B:95:27:1A:E8:3B:08:70:A5:79:C6:BB:60:15:92:81:76:B8:4D:77:00:38:9D:1E:5F:47",
            "DF:F9:C4:73:B5:10:E2:87:A1:79:46:23:11:AF:A4:47:27:DF:8A:B8:3C:D9:55:6B:23:B6:B8:47:91:A1:54:39",
            "0E:B2:73:7E:87:10:D4:0B:EF:BD:56:07:04:ED:66:15:E4:13:32:A2:A3:F2:16:AF:E1:F4:E9:3B:F4:1D:03:16",
            "5C:8A:DA:B3:72:A9:D1:EA:16:37:8C:27:2A:9A:EC:FB:87:93:D7:04:77:E0:20:CA:3C:00:FC:B1:D5:69:75:C6",
            "2A:15:4D:D2:3A:80:80:DD:A9:99:D7:4F:70:0E:25:C0:5E:00:45:8F:E9:2A:3C:B8:82:E7:7B:C0:FB:58:0A:6D",
            "06:E0:35:DD:38:FF:5B:26:2E:33:0F:5E:19:D1:71:AC:43:37:E1:AD:8B:3A:90:0A:8D:FA:55:D7:F0:91:BC:0A",
            "67:60:07:5E:3F:F4:E9:73:58:0B:37:98:1C:12:5B:3D:41:0B:56:B4:ED:C5:A3:CE:F2:CD:7F:8F:DF:EE:A2:7E",
            "AD:41:B2:D1:8B:B3:C0:DD:03:C5:E9:20:02:9A:49:12:49:71:86:F7:45:62:9A:38:AE:88:B7:6F:0B:6B:41:BD",
            "8F:13:C5:F7:C6:DD:35:8E:9E:EA:AC:EE:74:2F:30:FA:7B:7D:17:EC:02:96:7C:EE:9A:40:24:87:89:95:D1:C4",
            "67:81:F5:61:3D:7A:5C:17:72:7A:4D:4C:4B:DB:EE:07:CF:46:73:DB:25:60:65:C0:17:F3:26:CD:A8:91:F4:86",
            "64:17:1B:AD:E3:7A:FE:4B:B1:16:C8:33:98:C1:3B:FE:EE:D3:74:B9:F8:D9:3A:0E:C4:9C:28:2D:50:46:F2:C6",
            "1B:31:24:C6:A8:F1:5E:EB:84:54:DD:80:0E:E1:2F:C4:B8:28:29:AF:FE:DC:FC:90:6C:17:E2:28:41:B9:47:B8",
            "2A:67:EC:23:49:E5:5E:9F:8A:42:D2:44:C0:8E:E5:8B:15:10:C4:F4:42:DC:A6:3A:BC:F4:D7:0B:53:C9:7E:F0",
            "64:48:BE:12:24:4E:00:E1:80:61:89:DD:EC:3C:CF:79:1C:96:CD:79:DE:46:4F:E2:C2:F0:EB:2E:12:A3:F7:9D",
            "86:B0:45:82:3D:2A:F3:F8:B6:D6:78:0A:CE:3E:89:C3:2A:8C:D7:B9:A5:9F:1D:8D:AE:60:9B:1D:E2:35:11:57",
            "97:AD:D6:5E:D1:70:0F:CD:52:9E:99:E2:A3:7F:5B:85:40:2B:CD:DC:F6:EF:0F:20:25:4E:75:81:65:B7:58:9F",
            "78:96:51:A8:5F:AC:38:BA:DD:32:D0:39:64:D3:9B:6C:0B:2C:49:6A:33:87:D7:20:58:03:69:DE:67:5D:05:28",
            "0A:2D:E3:D5:5F:00:82:48:B3:8A:55:C4:BC:93:BE:24:A7:75:53:34:74:65:D0:5F:A4:3B:D6:4B:18:67:10:53",
            "70:78:F2:45:6B:9E:5A:39:C7:34:16:C2:75:DB:58:4C:60:57:CE:43:65:35:C7:F7:3E:70:20:AE:3B:36:D1:14",
            "57:55:82:9D:39:16:51:24:F9:9D:63:7A:56:C1:28:CB:E0:27:94:02:4F:BB:73:D1:3B:BF:E1:B6:CF:2D:93:AE",
            "02:D5:E1:CA:47:31:A9:92:D9:7A:63:C7:56:A5:E8:51:95:75:63:57:FC:CA:F2:71:38:6C:09:94:A3:94:08:44",
            "6E:53:27:3C:EC:2C:B6:E9:76:94:BE:03:7C:D2:DC:DA:FE:15:53:90:CF:5E:82:58:1D:85:9F:7A:5B:3B:52:FC",
            "66:BF:3F:F9:12:4D:C0:98:03:5D:15:EC:61:E5:D8:03:68:98:CD:70:BE:16:BB:E7:23:10:94:C6:8F:D9:5F:05",
            "B0:6F:8B:77:A9:9F:E1:FB:04:2B:73:CF:40:06:EC:C2:6F:1C:77:0C:C1:C4:56:F8:87:9E:A7:29:4E:81:8C:3C",
            "B4:9D:07:92:1C:3E:9B:A4:45:7F:5F:1B:E9:FB:C6:66:21:4B:44:CE:5E:56:72:C2:1E:4E:66:98:ED:57:DA:E2",
            "26:54:6F:35:F6:FB:2C:20:90:12:05:DB:76:90:59:BE:08:3E:93:7D:64:96:22:D2:44:18:A6:3F:B3:F2:0A:ED",
            "4B:C6:9F:C4:F7:43:7D:3E:31:AA:D5:2B:74:57:57:B9:3A:32:45:41:4B:AF:0F:0B:FA:94:C6:0B:C9:45:04:56",
            "10:9A:86:0D:95:CD:09:39:FF:B0:E7:AD:B9:13:5B:7C:52:A6:6A:26:CA:71:82:18:70:5D:97:F2:C9:BE:88:F9",
            "3B:2B:55:15:8C:A5:6D:8E:FC:A1:74:84:61:42:A8:6A:FA:D1:FF:D2:B9:56:26:A4:11:F5:31:E1:26:1F:80:EA",
            "F3:D7:91:E8:9F:90:23:64:15:A0:43:64:13:60:EC:F4:DF:55:A3:B4:A4:6F:22:97:1B:BF:CB:95:65:79:D4:14",
            "22:A5:8C:8B:B6:95:CA:F7:D6:54:C1:0C:29:F9:5F:72:91:84:52:35:EF:2C:EF:A6:2C:76:54:E8:8E:42:E6:94",
            "55:55:51:26:2B:76:71:70:0E:1E:73:B7:C2:B8:CF:27:F6:45:57:DD:1A:55:3D:BA:8A:29:7C:21:57:4B:EC:C1",
            "B3:85:88:60:51:B4:3A:1D:D9:9A:9E:60:C1:C2:64:97:30:41:ED:DE:73:0C:29:85:53:8E:A5:30:71:F9:68:7E",
            "A4:B0:96:99:16:45:AB:11:52:38:E5:CA:9C:1F:24:F7:A7:F8:3A:68:6E:DF:F0:44:52:A9:48:6E:98:E1:37:59",
            "60:32:99:35:65:CF:3C:76:2B:94:A7:E5:69:49:D9:A3:03:BA:ED:EA:1D:4E:24:8E:8E:A3:52:54:C4:DE:CD:DE",
            "37:7F:44:9D:AB:36:DC:C1:48:6E:E5:A4:53:AE:85:94:CD:A3:19:1D:1A:93:5D:91:B6:E1:16:BF:17:95:D4:C6",
            "DB:C8:30:02:C7:77:E9:87:B0:B6:EC:EC:B5:90:BB:2B:F7:94:35:C3:18:F9:88:D0:8E:F8:8E:A4:1C:8E:F9:36",
            "A9:5D:E3:57:DC:97:DE:53:20:72:AB:21:5E:B0:50:52:D1:E8:3D:EC:41:C7:90:D2:86:9B:A4:9A:9B:39:89:6D",
            "88:F2:9D:53:F6:32:A3:D2:CE:7E:FC:7E:6C:0C:96:79:1B:E5:AD:41:DF:67:B6:EE:46:17:B7:B9:35:E3:A1:DD",
            "BD:AD:4B:2B:6F:85:84:41:57:B5:F6:C1:D0:1B:EC:B1:40:9F:29:52:65:69:6F:1E:88:90:4A:D7:BA:61:ED:24",
            "9F:E3:B2:18:CB:47:17:2B:06:DC:78:87:00:22:72:42:61:D9:0F:B6:7E:0E:2E:13:8C:47:9C:A8:04:A9:54:58",
            "A8:51:B6:61:AC:F1:58:6A:51:BC:3C:8D:63:EE:03:5C:40:65:98:F8:7F:EC:44:6A:37:B2:95:FD:F8:3F:AD:D3",
            "30:BE:44:83:3E:78:6D:B3:A6:78:44:4E:F5:24:27:32:6C:99:6E:55:D4:B3:71:FE:29:A2:5A:10:AF:92:FF:EE",
            "E1:46:1B:6F:06:3F:8E:91:EC:C6:BE:7A:15:E9:BD:02:69:78:6F:65:68:D3:A1:D9:5D:1E:85:73:EE:89:05:D5",
            "77:2B:DD:FA:F0:87:11:B9:3F:B9:D7:E5:51:4D:14:91:63:29:99:1E:72:A3:F6:C1:10:72:7C:61:D5:04:31:09",
            "58:3B:02:24:B1:BC:FD:C4:FC:26:F8:1A:8C:64:EA:8F:DD:56:12:B1:F6:E7:7E:08:43:23:3A:86:39:82:68:3A",
            "0D:38:EA:5A:B9:30:12:83:A0:FA:C9:F3:B2:C1:50:52:93:79:C2:6A:46:10:40:61:43:DD:5F:71:85:F4:80:C6",
            "87:63:85:71:5C:CD:FC:17:1E:66:23:CB:B6:9B:A4:BC:88:4A:AD:1A:6A:AA:B0:2E:97:3D:12:18:75:7B:22:EB",
            "E6:99:A0:D4:ED:43:CC:DD:E5:4D:59:B7:A6:F3:9A:44:0E:97:26:C9:2E:B4:5E:5F:58:E4:15:A2:37:17:7B:C1",
            "1D:26:2A:F4:54:69:C9:47:BD:96:E4:C1:5D:93:0D:2D:0E:99:97:27:5A:03:BF:DA:42:14:74:09:D8:C8:6E:3C",
            "96:3A:EF:D1:C1:41:7F:42:A9:91:E4:31:44:28:8F:C1:47:C7:B5:FC:9D:62:BF:DA:27:A3:34:51:D8:9E:15:90",
            "0E:5D:38:61:BB:72:41:9E:CF:A9:EC:45:5E:FD:98:A3:E1:C5:BE:9E:D8:C6:66:40:1A:9F:19:D4:66:96:38:8C",
            "D2:62:71:30:86:46:07:21:50:4E:A7:4F:B5:B9:8A:86:59:FD:C4:82:C4:99:D9:96:CC:B3:CE:47:6D:FB:50:58",
            "15:A3:F4:3B:AB:5F:1F:F1:D2:A3:0E:68:83:5A:76:47:21:FC:3F:D4:4D:FC:BC:ED:4E:1F:D8:43:A7:79:60:0F",
            "40:4C:0C:CB:43:BB:9D:21:2B:95:24:CA:86:E0:84:28:5C:19:B1:AC:2D:7B:5E:92:D5:7F:35:29:EE:54:35:34",
            "2D:35:41:DA:E3:C4:24:30:60:F7:F0:74:3C:FA:D2:F5:4D:C9:34:FE:F4:A6:AB:8B:60:11:24:08:2B:23:17:EA",
            "6B:7D:26:98:72:DC:21:EC:6B:42:FD:95:F2:45:CE:2C:7A:37:2B:13:F9:AD:EC:53:A4:0B:44:FF:CB:A4:27:DC",
            "37:06:02:C5:AC:4F:1E:D6:78:7A:D4:3A:75:F6:46:B2:C6:FF:B8:FF:8E:7F:19:48:50:E7:BE:AD:5D:CC:84:B2",
            "46:E3:98:F9:1B:09:A8:8C:8C:80:46:54:B3:FC:B0:81:60:ED:E6:A3:A0:77:A5:A7:AB:0E:E5:01:85:50:74:4A",
            "70:C9:34:87:A3:86:E0:12:22:D9:A1:A3:99:A2:CE:E9:88:1E:F5:EB:8B:A5:8E:3F:6C:DA:0F:59:13:F7:89:7F",
            "DC:4C:AB:BC:21:AE:D4:7B:13:35:20:1F:35:95:BA:33:DB:A3:60:7E:F0:CC:09:5E:49:92:41:8A:08:26:51:1E",
            "64:68:70:95:55:E1:41:2D:6F:AF:C6:77:CF:8B:A9:5C:90:D4:11:A5:5D:67:16:FA:A9:8E:3E:E7:E1:1C:23:24",
            "CE:DA:34:AC:CD:45:2D:73:5B:02:92:42:45:63:A6:58:D9:91:82:29:6A:ED:43:59:8C:ED:77:E5:42:46:4F:CA",
            "94:E8:91:8A:80:CE:2E:C3:2F:B9:F0:AA:CB:02:DC:C3:C7:53:16:95:03:2C:9B:87:AC:09:C9:79:FA:55:08:E5",
            "E0:3A:49:B9:76:E3:65:0E:AC:16:41:FE:BA:CB:12:FD:B3:F9:55:D9:F3:AD:05:03:B1:8A:21:1D:5C:47:05:8C",
            "6B:44:F8:4C:89:99:41:E3:F3:88:F8:17:DE:9C:96:35:66:6E:27:99:14:73:7C:68:1B:10:E0:0B:0A:81:01:7D",
            "F2:D9:51:7D:9A:B9:79:54:9A:F5:54:5A:DA:F1:16:D3:E0:6E:98:89:C5:D0:D9:0A:FA:64:E2:08:0D:31:77:A6",
            "73:6E:D0:97:15:E8:9C:A1:CF:B5:52:74:9B:E9:CF:BE:3E:FE:C3:67:51:15:91:DE:B5:9B:FF:DC:80:A4:75:73",
            "5A:27:A1:36:A8:D2:49:3D:0E:F1:DD:9E:72:29:26:BE:71:BD:78:CA:88:4D:29:49:E4:C6:AF:BE:53:31:47:83",
            "D0:6F:57:10:82:62:ED:95:02:73:48:0B:13:80:D8:69:C9:56:6F:6C:45:3E:AA:A8:86:48:B0:08:E7:C0:E6:FF",
            "3F:3D:7A:4C:D4:EC:DC:3D:A1:92:7A:DA:CA:07:84:F9:EE:34:F2:29:B0:3B:C3:32:B0:6D:0A:96:C1:F1:C4:D6",
            "C9:A6:E3:8E:EF:DF:0C:BE:D4:F5:DA:44:B9:89:4C:E9:23:19:7E:92:70:3F:CA:D9:3F:63:CE:CE:41:C4:70:D8",
            "03:3B:E3:DA:47:1F:BE:AA:6B:6E:8F:06:16:10:00:EE:65:08:43:6E:67:26:87:31:1D:55:A0:5C:A7:66:66:BD",
            "D0:7E:6F:7A:0B:9A:44:0D:C9:C4:00:32:E0:6D:B5:1B:22:5D:FB:4E:2D:A0:CB:EE:5B:ED:E9:0F:4D:63:9F:B4",
            "D3:D8:39:99:B8:08:85:92:1D:11:45:CB:87:3E:2E:04:CA:CA:FA:3D:31:87:75:84:91:3D:96:93:46:AC:09:04",
            "00:43:42:0D:85:09:AE:FF:BC:51:80:E0:8B:7E:20:00:BD:03:04:D6:64:1E:B8:85:48:90:E3:E9:C4:40:60:F2",
            "52:04:FA:90:3D:E6:65:B3:52:7C:A0:0B:3A:2C:1D:F7:8A:CB:92:DA:B1:BE:AF:75:38:39:D5:E0:7D:0E:57:12",
            "42:0B:AF:2B:BB:52:BE:8B:13:8A:7F:1A:3C:A6:8B:35:CC:1F:1D:68:41:A9:B4:D0:DE:42:6E:AF:98:5E:6F:85",
            "D0:84:C6:07:89:D8:2D:D8:47:60:A5:F9:FF:78:FC:66:55:49:F6:99:7D:01:5A:61:F3:EF:69:52:18:38:FB:4D",
            "5F:C1:4D:8A:05:71:2D:BA:B1:83:80:AF:C6:4A:53:24:3C:5A:60:B2:E8:AD:B4:21:37:80:B6:9F:85:AF:44:5D",
            "27:80:A0:14:F7:58:2C:31:9A:2E:62:D2:B6:2E:D6:BD:A2:60:DB:0F:57:0F:15:72:38:30:9F:77:DA:80:D8:C6",
            "E5:D6:70:16:B2:68:67:70:58:C3:B6:53:46:52:60:C8:43:1F:2F:43:D5:25:6F:FB:7F:09:4E:BF:94:EF:56:C1",
            "D1:A3:17:1F:BF:F4:43:86:56:28:5A:55:04:1D:3A:B9:C3:AB:CE:BE:8F:A8:D2:73:C4:F0:22:F6:82:81:A6:88"
    };
}
