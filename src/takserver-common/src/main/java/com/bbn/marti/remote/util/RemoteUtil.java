package com.bbn.marti.remote.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.Ignite;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atakmap.Tak.ROL;
import com.bbn.marti.remote.exception.TakException;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.bbn.marti.remote.sync.MissionChangeType;
import com.bbn.marti.remote.sync.MissionContent;
import com.bbn.marti.remote.sync.MissionUpdateDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/*
 * 
 * Constants and utility functions that are shared between TAK Server microservices
 * 
 * Can be used as a singleton in two ways - from this class directly (getInstance()) or by autowiring as a Spring bean.
 * 
 */
public class RemoteUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(RemoteUtil.class);
    
    private static RemoteUtil instance = new RemoteUtil();

    public static final int GROUPS_BIT_VECTOR_LEN = 32768;
    
    public static final String GROUP_CLAUSE = " :groupVector\\:\\:bit(" + GROUPS_BIT_VECTOR_LEN + ") & " +
            "lpad(groups\\:\\:character varying, " + GROUPS_BIT_VECTOR_LEN + ", '0')\\:\\:bit(" + GROUPS_BIT_VECTOR_LEN + ")\\:\\:bit varying " +
            "<> 0\\:\\:bit(" + GROUPS_BIT_VECTOR_LEN + ")\\:\\:bit varying ";

    public static final String GROUP_FUNCTION_CALL = " function( 'bitwiseAndGroups',  :groupVector, groups ) = TRUE ";

    public static final String GROUP_VECTOR = " cast(:groupVector as bit(" + GROUPS_BIT_VECTOR_LEN + "))";

    private String bitStringNoGroups = null;

    private Object unsafe;
    private Method putObjectVolatile;
    private Field valueField;
    private Long valueOffset;
    
    @Autowired
    private Ignite ignite;

    /*
     * Use this method to obtain a singleton instance of this class when Spring-less
     * 
     */
    public static RemoteUtil getInstance() {
        return instance;
    }

    public RemoteUtil() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = field.get(null);

            Method objectFieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
            putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);

            valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
            valueOffset = (Long) objectFieldOffset.invoke(unsafe, valueField);
        } catch (Exception e) {
            logger.error("exception in RemoteUtil constructor!", e);
        }
    }

    public boolean[] getBitVectorForGroups(Set<Group> groups, int direction) {
        // Bit vector for the message which represents what groups this message is associated with. Default to empty (no groups) for the moment.
        boolean[] groupsBitVector = new boolean[RemoteUtil.GROUPS_BIT_VECTOR_LEN];

        // set group bits in vector
        if (groups != null) {
            for (Group group : groups) {
                if (group == null) {
                    logger.error("null group in message - skipping");
                    continue;
                }

                if ((group.getDirection().getValue() & direction) == 0) {
                    continue;
                }

                if (group.getBitpos() == null) {
                    if (logger.isErrorEnabled()) {
                        logger.error("empty bit position in group - skipping: " + StringUtils.normalizeSpace(group.toString()));
                    }
                    continue;
                }

                if (group.getBitpos() >= RemoteUtil.GROUPS_BIT_VECTOR_LEN) {
                    logger.error("group bit vector offset too large: " + group.getBitpos() + " exceeds " + (RemoteUtil.GROUPS_BIT_VECTOR_LEN - 1));
                }

                groupsBitVector[group.getBitpos()] = true;
            }
        }

        return groupsBitVector;
    }

    public boolean[] getBitVectorForGroups(Set<Group> groups, Direction direction) {
        return getBitVectorForGroups(groups, direction.getValue());
    }

    public boolean[] getBitVectorForGroups(Set<Group> groups) {
        return getBitVectorForGroups(groups, Direction.IN.getValue() | Direction.OUT.getValue());
    }

    public NavigableSet<Group> getGroupsForBitVectorString(String groupsBitVector, Collection<Group> groupsToSearch) {
        NavigableSet<Group> results = new ConcurrentSkipListSet<>();

        groupsBitVector = new StringBuilder(groupsBitVector).reverse().toString();
        for (Group nextGroup : groupsToSearch) {
                if (groupsBitVector.charAt(nextGroup.getBitpos()) == '1') {
                    results.add(nextGroup);
            }
        }

        return results;
    }

    public NavigableSet<String> getGroupNamesForBitVectorString(String groupsBitVector, Collection<Group> groupsToSearch) {
        NavigableSet<String> results = new ConcurrentSkipListSet<>();
        NavigableSet<Group> groups = getGroupsForBitVectorString(groupsBitVector, groupsToSearch);
        for (Group group : groups) {
            results.add(group.getName());
        }

        return results;
    }

    public String bitVectorToString(boolean[] groupsBitVector) {
        try {
            byte[] bits = getBitStringNoGroups().getBytes().clone();

            for (int i = 1; i <= groupsBitVector.length; i++) {
                if (groupsBitVector[groupsBitVector.length - i]) {
                    bits[i - 1] = '1';
                }
            }

            String bitString = new String();
            putObjectVolatile.invoke(unsafe, bitString, valueOffset, bits);
            return bitString;

        } catch (Exception e) {
            logger.error("exception in bitVectorToString!", e);
            return null;
        }
    }

    public BigInteger bitVectorToInt(boolean[] groupsBitVector){
        BigInteger n = BigInteger.ZERO;
        for (int i = groupsBitVector.length - 1; i >= 0; i--) {
            n = (n.shiftLeft(1)).or(groupsBitVector[i] ? BigInteger.ONE : BigInteger.ZERO);
        }
        return n;
    }

    public BigInteger bitVectorStringToInt(String groupsBitVector){
        BigInteger n = BigInteger.ZERO;
        for (char c : groupsBitVector.toCharArray()) {
            n = (n.shiftLeft(1)).or(c == '1' ? BigInteger.ONE : BigInteger.ZERO);
        }
        return n;
    }

    // get a bit vector that indicates membership in all groups
    public String getBitStringAllGroups() {
        StringBuilder groupsString = new StringBuilder();
        
        for (int i = 0; i < GROUPS_BIT_VECTOR_LEN; i++) {
            groupsString.append('1');
        }
        
        return groupsString.toString();
    }
    
    // get a bit vector that indicates membership in no groups
    public String getBitStringNoGroups() {
        if (bitStringNoGroups == null) {
            StringBuilder groupsString = new StringBuilder();

            for (int i = 0; i < GROUPS_BIT_VECTOR_LEN; i++) {
                groupsString.append('0');
            }

            bitStringNoGroups = groupsString.toString();
        }

        return bitStringNoGroups;
    }
    
    public String getGroupAndClause() {
        return " and" + getGroupClause();
    }
    
    public String getGroupAndClause(String tableName) {
        return " and" + getGroupClause(tableName);
    }
    
    public String getGroupClause() {
        return getGroupClause("");
    }
    
    public String getGroupClause(String tableName) {
        return " ?::bit(" + GROUPS_BIT_VECTOR_LEN + ") & "+
                "lpad(" + (Strings.isNullOrEmpty(tableName) ? "" : tableName + ".") + "groups::character varying, " + GROUPS_BIT_VECTOR_LEN + ", '0')::bit(" + GROUPS_BIT_VECTOR_LEN + ")::bit varying " +
                "<> 0::bit(" + GROUPS_BIT_VECTOR_LEN + ")::bit varying ";
    }

    public String getGroupType() {
        return "::bit(" + GROUPS_BIT_VECTOR_LEN + ")::bit varying";
    }
    
    /*
     * Create a group vector bit string for a single group
     * 
     */
    public String getBitStringForGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("null group");
        }
        
        if (group.getBitpos() == null) {
            throw new IllegalStateException("null bit position for group " + group.getName() + " Group save ");
        }
        
        NavigableSet<Group> groups = new ConcurrentSkipListSet<>();
        groups.add(group);
        
        return bitVectorToString(getBitVectorForGroups(groups));
    }
    
    // Get the hash of the encoded cert bytes, using the provided hash function
    public String getCertFingerprint(@NotNull X509Certificate cert, @NotNull HashFunction hf, boolean withColons) {

        HashCode hash;
        try {
            hash = hf.newHasher().putBytes(cert.getEncoded()).hash();
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        
        hash.asBytes();
        
        String fingerprint = DatatypeConverter.printHexBinary(hash.asBytes());
        
        StringBuilder fpBuilder = new StringBuilder();
        
        for (int i = 0; i < fingerprint.length(); i++) {
            if (withColons && i > 0 && i % 2 == 0) {
                fpBuilder.append(':');
            }
            
            fpBuilder.append(fingerprint.charAt(i));
        }
        
        return fpBuilder.toString();
    }
    
    public String getCertFingerprint(@NotNull X509Certificate cert, @NotNull HashFunction hf) {

        return getCertFingerprint(cert, hf, true);
    }

    // Get the SHA-256 hash of the encoded cert bytes
    public String getCertSHA256Fingerprint(@NotNull X509Certificate cert) {
        return getCertFingerprint(cert, Hashing.sha256());
    }
    
    // Get the SHA-256 hash of the encoded cert bytes
    public String getCertSHA256Fingerprint(@NotNull X509Certificate cert, boolean withColons) {
        return getCertFingerprint(cert, Hashing.sha256(), withColons);
    }
    
    public ROL getROLforMissionChange(MissionContent content, String missionName, String creatorUid, String missionCreatorUid, String missionChatRoom, String missionTool, String missionDescription) {

    	try {

    		ObjectMapper mapper = new ObjectMapper();

    		String contentJson = mapper.writeValueAsString(content);
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("mission content json: " + contentJson);
    		}

    		MissionUpdateDetails contentDetails = new MissionUpdateDetails();

    		contentDetails.setContent(content);
    		contentDetails.setMissionName(missionName);
    		contentDetails.setCreatorUid(creatorUid);
    		contentDetails.setChangeType(MissionChangeType.ADD_CONTENT);

    		// include mission metadata in federated add content, so that the mission can be created on the federate, if it does not exist
    		contentDetails.setMissionCreatorUid(missionCreatorUid);
    		contentDetails.setMissionChatRoom(missionChatRoom);
    		contentDetails.setMissionTool(missionTool);
    		contentDetails.setMissionDescription(missionDescription);

    		String contentDetailsJson = mapper.writeValueAsString(contentDetails);

    		return ROL.newBuilder().setProgram("update mission\n" + contentDetailsJson + ";").build();

    	} catch (Exception e) {
    		throw new TakException("exception constructing or sending mission create federated ROL", e);
    	}

    }
    
    public boolean isGroupVectorAllowed(String requestGroupVectorString, String dataGroupVectorString) {

        try {
            if (Strings.isNullOrEmpty(requestGroupVectorString) || Strings.isNullOrEmpty(dataGroupVectorString)) {
                throw new IllegalArgumentException("empty group vector");
            }

            //
            // compare the two group vectors by walking them in reverse order. since we've increased group vector size
            // and may encounter smaller group vectors in the database, compare bits using common offset from end of
            // string since group's bitPos is indexed from the end of the groupVectorString
            //

            final int dataGroupVectorStringLength = dataGroupVectorString.length();
            final int requestGroupVectorStringLength = requestGroupVectorString.length();

            final byte[] requestGroupVectorBytes = (byte[]) valueField.get(requestGroupVectorString);
            final byte[] dataGroupVectorBytes = (byte[]) valueField.get(dataGroupVectorString);

            for (int offset = 1; offset <= dataGroupVectorStringLength; offset++) {

                int requestGroupNdx = requestGroupVectorStringLength - offset;
                if (requestGroupNdx < 0) {
                    return false;
                }

                char next = (char) requestGroupVectorBytes[requestGroupNdx];
                if (next == '1' && next ==
                        dataGroupVectorBytes[dataGroupVectorStringLength - offset]) {
                    return true;
                }
            }

        } catch (Exception e) {
            logger.error("exception in isGroupVectorAllowed!", e);
        }

    	return false;
    }
}
