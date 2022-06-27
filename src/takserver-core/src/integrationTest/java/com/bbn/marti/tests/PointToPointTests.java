package com.bbn.marti.tests;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.takcl.TestLogger;
import com.bbn.marti.takcl.Util;
import com.bbn.marti.test.shared.AbstractTestClass;
import com.bbn.marti.test.shared.data.generated.ImmutableUsers;
import com.bbn.marti.test.shared.data.servers.ImmutableServerProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.takcl.connectivity.AbstractRunnableServer;
import com.bbn.marti.test.shared.engines.TestEngine;
import com.bbn.marti.test.shared.engines.UserIdentificationData;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

/**
 * Created on 8/30/16.
 */
public class PointToPointTests extends AbstractTestClass {

    private static final ImmutableServerProfiles[] testServers = new ImmutableServerProfiles[]{ImmutableServerProfiles.SERVER_0, ImmutableServerProfiles.SERVER_1};

    private static final String className = "PointToPointTests";

    @BeforeClass
    public static void setup() {
    	try {
			SSLHelper.genCertsIfNecessary();
        } catch (IOException e) {
    	    throw new RuntimeException(e);
        }
        if (engine != null) {
            engine.engineFactoryReset();
        }
        AbstractRunnableServer.setLogDirectory(TEST_ARTIFACT_DIRECTORY);
        TestLogger.setFileLogging(TEST_ARTIFACT_DIRECTORY);
        engine = new TestEngine(testServers);
    }

    public static class UserMixer {

        private long randomSeed = 3245493834579L;
        private Random random;


        public static class ItemPool<T> {

            private final T[] itemArray;
            private final int length;
            private int idx = 0;

            public ItemPool(@NotNull T[] array) {
                itemArray = array;
                length = array.length;
            }

            public synchronized T next() {
                if (idx >= length) {
                    idx = 0;
                }
                return itemArray[idx++];
            }

            public int size() {
                return length;
            }

            public T[] getBackingArray() {
                return itemArray;
            }
        }

        private final List<ItemPool<AbstractUser>> userPools = new LinkedList<>();

        private List<List<AbstractUser>> getCombinations(List<ItemPool<AbstractUser>> userList, int currentIndex) {
            LinkedList<List<AbstractUser>> returnList = new LinkedList<>();
            if (currentIndex >= userList.size()) {
                return returnList;
            }

            List<List<AbstractUser>> combinations = getCombinations(userList, currentIndex + 1);

            returnList.addAll(combinations);

            AbstractUser currentUser = userList.get(currentIndex).next();

            for (List<AbstractUser> combination : combinations) {
                List<AbstractUser> newCombination = new LinkedList<>(combination);
                newCombination.add(currentUser);
                returnList.add(newCombination);
            }

            List<AbstractUser> singleCombination = new LinkedList<>();
            singleCombination.add(currentUser);
            returnList.add(singleCombination);

            return returnList;
        }

        private AbstractUser[] getAllUsers() {
            Set<AbstractUser> userSet = new HashSet<>();
            for (ItemPool<AbstractUser> ip : userPools) {
                for (AbstractUser user : ip.getBackingArray()) {
                    userSet.add(user);
                }
            }
            return userSet.toArray(new AbstractUser[0]);
        }

        public UserMixer() {
            random = new Random(randomSeed);
        }

        public void addUserList(AbstractUser... users) {
            userPools.add(new ItemPool<>(users));

        }

        public List<AbstractUser[]> produceUserSets(int maxCombinationSize) {
            List<List<AbstractUser>> itemPoolList = getCombinations(userPools, 0);

            List<AbstractUser[]> returnValue = new ArrayList<>(itemPoolList.size());

            AbstractUser[] arrayModel = new AbstractUser[0];

            for (int i = 0; i < itemPoolList.size(); i++) {
                List<AbstractUser> userSet = itemPoolList.get(i);
                if (userSet.size() <= maxCombinationSize) {
                    returnValue.add(itemPoolList.get(i).toArray(arrayModel));
                }
            }
            return returnValue;
        }
    }

    @Test(timeout = 1500000)
    public void basicPointToPointTest() {
        try {
			String sessionIdentifier = initTestMethod();

            AbstractUser[] userSet = {
                    ImmutableUsers.s0_stcp0_anonuser_0f,
                    ImmutableUsers.s0_stcp0_anonuser_0f_A,
                    ImmutableUsers.s0_stcp12_anonuser_12f,
                    ImmutableUsers.s0_authssl_authuser01_01f,
                    ImmutableUsers.s0_stcp_anonuser_t_A,
                    ImmutableUsers.s0_stcp01t_anonuser_01t
            };

            engine.offlineAddUsersAndConnectionsIfNecessary(userSet);

            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

            engine.connectClientsAndVerify(true, userSet);

            UserMixer mixer = new UserMixer();

            for (AbstractUser user : userSet) {
                engine.attemptSendFromUserAndVerify(user);
                mixer.addUserList(user);
            }

            List<AbstractUser[]> sendSets = mixer.produceUserSets(3);

            for (AbstractUser sender : userSet) {
                for (AbstractUser[] receivers : sendSets) {
                    engine.attemptSendFromUserAndVerify(sender, receivers);
                }
            }

        } finally {
            engine.stopServers(ImmutableServerProfiles.SERVER_0);
        }
    }

    @Test(timeout = 1500000)
    public void uidIdentificationTest() {
        try {
			String sessionIdentifier = initTestMethod();

            AbstractUser[] userSet = {
                    ImmutableUsers.s0_stcp01t_anonuser_01t,
                    ImmutableUsers.s0_stcp0_anonuser_0f_A,
                    ImmutableUsers.s0_stcp12_anonuser_12f,
                    ImmutableUsers.s0_authssl_authuser01_01f,
                    ImmutableUsers.s0_stcp_anonuser_t_A,

                    ImmutableUsers.s0_stcp0_anonuser_0f
            };

            engine.offlineAddUsersAndConnectionsIfNecessary(userSet);

            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

            engine.connectClientsAndVerify(true, userSet);

            UserMixer mixer = new UserMixer();

            for (AbstractUser user : userSet) {
                engine.attemptSendFromUserAndVerify(UserIdentificationData.UID, user);
                mixer.addUserList(user);
            }

            List<AbstractUser[]> sendSets = mixer.produceUserSets(3);

            for (AbstractUser sender : userSet) {
                for (AbstractUser[] receivers : sendSets) {
                    engine.attemptSendFromUserAndVerify(UserIdentificationData.UID, sender, UserIdentificationData.UID, receivers);
                }
            }

        } finally {
            engine.stopServers(ImmutableServerProfiles.SERVER_0);
        }
    }

    @Test(timeout = 1500000)
    public void callsignIdentificationTest() {
        try {
			String sessionIdentifier = initTestMethod();

            AbstractUser[] userSet = {
                    ImmutableUsers.s0_stcp0_anonuser_0f,
                    ImmutableUsers.s0_stcp0_anonuser_0f_A,
                    ImmutableUsers.s0_stcp12_anonuser_12f,
                    ImmutableUsers.s0_authssl_authuser01_01f,
                    ImmutableUsers.s0_stcp_anonuser_t_A,
                    ImmutableUsers.s0_stcp01t_anonuser_01t
            };

            engine.offlineAddUsersAndConnectionsIfNecessary(userSet);

            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

            engine.connectClientsAndVerify(true, userSet);

            UserMixer mixer = new UserMixer();

            for (AbstractUser user : userSet) {
                engine.attemptSendFromUserAndVerify(UserIdentificationData.UID_AND_CALLSIGN, user);
                mixer.addUserList(user);
            }

            List<AbstractUser[]> sendSets = mixer.produceUserSets(3);

            for (AbstractUser sender : userSet) {
                for (AbstractUser[] receivers : sendSets) {
                    engine.attemptSendFromUserAndVerify(UserIdentificationData.UID_AND_CALLSIGN, sender, UserIdentificationData.CALLSIGN, receivers);
                }
            }

        } finally {
            engine.stopServers(ImmutableServerProfiles.SERVER_0);
        }
    }


    @Test(timeout = 1500000)
    public void mixedIdentificationTest() {
        try {
			String sessionIdentifier = initTestMethod();

            AbstractUser[] userSet0 = {
                    ImmutableUsers.s0_stcp0_anonuser_0f,
                    ImmutableUsers.s0_stcp12_anonuser_12f,
                    ImmutableUsers.s0_authssl_authuser01_01f,
                    ImmutableUsers.s0_stcp_anonuser_t,
                    ImmutableUsers.s0_stcp01t_anonuser_01t
            };

//            AbstractUser[] userSet1 = {
//                    ImmutableUsers.s0_stcp01t_anonuser_01t_A,
//                    ImmutableUsers.s0_stcp_anonuser_t_A,
//                    ImmutableUsers.s0_authtls_authuser01_01f,
//                    ImmutableUsers.s0_stcp12_anonuser_12f_A,
//                    ImmutableUsers.s0_stcp0_anonuser_0f_A
//            };
//
//            AbstractUser[] userSet2 = {
//                    ImmutableUsers.s0_stcp01t_anonuser_01t_B,
//                    ImmutableUsers.s0_stcp_anonuser_t_B,
//                    ImmutableUsers.s0_authtls_authuser12_012f,
//                    ImmutableUsers.s0_stcp12_anonuser_12f_B,
//                    ImmutableUsers.s0_stcp0_anonuser_0f_B
//            };

            engine.offlineAddUsersAndConnectionsIfNecessary(userSet0);

            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);

            engine.connectClientsAndVerify(true, userSet0);

            UserMixer mixer = new UserMixer();

            boolean toggle = false;
            for (AbstractUser user : userSet0) {
                engine.attemptSendFromUserAndVerify(
                        (toggle == true ? UserIdentificationData.UID : UserIdentificationData.UID_AND_CALLSIGN),
                        user
                );
                toggle = !toggle;
                mixer.addUserList(user);
            }

            List<AbstractUser[]> sendSets = mixer.produceUserSets(3);

            for (AbstractUser sender : userSet0) {
                for (AbstractUser[] receivers : sendSets) {
                    engine.attemptSendFromUserAndVerify(UserIdentificationData.UID, sender, UserIdentificationData.UID, receivers);
                }
            }

        } finally {
            engine.stopServers(ImmutableServerProfiles.SERVER_0);
        }
    }

//
//
//    @Test
//    public void quirksTest() {
//        try {
//            String sessionIdentifier = className + ".quirksTest";
//            engine.engineFactoryReset();
//            TestLogger.startTestWithIdentifier(sessionIdentifier);
//
//            MutableServerProfile server = ImmutableServerProfiles.SERVER_0.getMutableInstance();
//
//            MutableConnection stcp0 = server.generateConnection(ImmutableConnections.s0_stcp0);
//            MutableConnection stcp01 = server.generateConnection(ImmutableConnections.s0_stcp01);
//
//            MutableUser u0A = stcp0.generateConnectionUser(BaseUsers.anonuser, true, "A");
//            u0A.overrideCotUid("UID_A");
//            u0A.overrideCotCallsign("CS_A");
//
//            MutableUser u0B = stcp0.generateConnectionUser(BaseUsers.anonuser, true, "B");
//            u0B.overrideCotUid("UID_0_B");
//            u0B.overrideCotCallsign("CS_B");
//
//            MutableUser u0C = stcp0.generateConnectionUser(BaseUsers.anonuser, true, "C");
//            u0C.overrideCotUid("UID_C");
//            u0C.overrideCotCallsign("CS_0_C");
//
//            MutableUser u0D = stcp0.generateConnectionUser(BaseUsers.anonuser, true, "D");
//            u0D.overrideCotUid("UID_D");
//            u0D.overrideCotCallsign("CS_D");
//
//            MutableUser u0E = stcp0.generateConnectionUser(BaseUsers.anonuser, true, "E");
//            u0E.overrideCotUid("UID_0_E");
//            u0E.overrideCotCallsign("CS_0_E");
//
//
//            AbstractUser callsignA = stcp0.generateConnectionUser()
//
//            AbstractUser[] userSet = {
//                    stcp0.generateConnectionUser(BaseUsers.anonuser, true, "A"),
//                    stcp0.generateConnectionUser(BaseUsers.anonuser, true, "B"),
//                    stcp0.generateConnectionUser(BaseUsers.anonuser, true, "C"),
//                    stcp01.generateConnectionUser(BaseUsers.anonuser, true, "D"),
//                    stcp01.generateConnectionUser(BaseUsers.anonuser, true, "E"),
//                    stcp01.generateConnectionUser(BaseUsers.anonuser, true, "F")
//            };
//
//            engine.offlineAddUsersAndConnectionsIfNecessary(userSet);
//            engine.startServer(ImmutableServerProfiles.SERVER_0, sessionIdentifier);
//            engine.connectClientsAndVerify(true, userSet);
//
//            for (AbstractUser user : userSet) {
//                engine.attemptSendFromUserAndVerify(user, userSet[5]);
//            }
//
//        } finally {
//            engine.stopServers(ImmutableServerProfiles.SERVER_0);
//        }
//    }

}
