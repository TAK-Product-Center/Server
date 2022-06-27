package com.bbn.marti.remote.socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MissionPackageMessage extends TakMessage {

    private static final String className = MissionPackageMessage.class.getSimpleName();

    public static String getClassName() {
        return className;
    }

    private static final long serialVersionUID = 1713269827745328148L;


    public String getMessageType() {
        return className;
    }

    private String cotType;
    private String filename;
    private String senderUrl;
    private long sizeInBytes;
    private String senderCallSign;
    private String sha256Hash;
    private String destCallSign;
    private List<MissionPackageEntry> missionPackageEntries;
    private boolean realMissionPackage = true;
    private boolean unPacked = true;

    @JsonProperty("innerCot")
    @JsonRawValue
    private String innerCotJson;


    public String getCotType() {
        return cotType;
    }

    public void setCotType(String cotType) {
        this.cotType = cotType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSenderUrl() {
        return senderUrl;
    }

    public void setSenderUrl(String senderUrl) {
        this.senderUrl = senderUrl;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getSenderCallSign() {
        return senderCallSign;
    }

    public void setSenderCallSign(String senderCallSign) {
        this.senderCallSign = senderCallSign;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getDestCallSign() {
        return destCallSign;
    }

    public void setDestCallSign(String destCallSign) {
        this.destCallSign = destCallSign;
    }

    public List<MissionPackageEntry> getMissionPackageEntries() {
        return missionPackageEntries;
    }

    public void addMissionPackageEntry(MissionPackageEntry missionPackageEntry) {
        if (missionPackageEntries == null) {
            missionPackageEntries = new ArrayList<>();
        }
        missionPackageEntries.add(missionPackageEntry);
    }

    public String getInnerCotJson() {
        return innerCotJson;
    }
    public void setInnerCotJson(JsonNode innerCotJson) {
        this.innerCotJson = innerCotJson.toString();
    }

    public boolean isRealMissionPackage() {
        return realMissionPackage;
    }

    public void setRealMissionPackage(boolean realMissionPackage) {
        this.realMissionPackage = realMissionPackage;
    }

    public boolean isUnPacked() {
        return unPacked;
    }

    public void setUnPacked(boolean unPacked) {
        this.unPacked = unPacked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissionPackageMessage that = (MissionPackageMessage) o;
        return sizeInBytes == that.sizeInBytes &&
                realMissionPackage == that.realMissionPackage &&
                unPacked == that.unPacked &&
                Objects.equals(cotType, that.cotType) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(senderUrl, that.senderUrl) &&
                Objects.equals(sha256Hash, that.sha256Hash) &&
                Objects.equals(destCallSign, that.destCallSign) &&
                Objects.equals(missionPackageEntries, that.missionPackageEntries) &&
                Objects.equals(innerCotJson, that.innerCotJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cotType, filename, senderUrl, sizeInBytes, senderCallSign, sha256Hash, destCallSign,
                missionPackageEntries, realMissionPackage, unPacked, innerCotJson);
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", MissionPackageMessage.class.getSimpleName() + "[", "]")
                .add("cotType='" + cotType + "'")
                .add("filename='" + filename + "'")
                .add("senderUrl='" + senderUrl + "'")
                .add("sizeInBytes=" + sizeInBytes)
                .add("senderCallSign='" + senderCallSign + "'")
                .add("sha256Hash='" + sha256Hash + "'")
                .add("destCallSign='" + destCallSign + "'")
                .add("missionPackageEntries=" + missionPackageEntries)
                .add("realMissionPackage=" + realMissionPackage)
                .add("unPacked=" + unPacked)
                .add("innerCotJson='" + innerCotJson + "'")
                .toString();
    }
}
