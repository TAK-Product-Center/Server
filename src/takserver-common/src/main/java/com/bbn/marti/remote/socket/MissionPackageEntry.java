package com.bbn.marti.remote.socket;

import java.util.Objects;
import java.util.StringJoiner;

public class MissionPackageEntry {

    private String filename;
    private String hash;
    private Long sizeInBytes;

    public String getName() {
        return filename;
    }

    public void setName(String name) {
        this.filename = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(Long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MissionPackageEntry that = (MissionPackageEntry) o;
        return Objects.equals(filename, that.filename) &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(sizeInBytes, that.sizeInBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, hash, sizeInBytes);
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", MissionPackageEntry.class.getSimpleName() + "[", "]")
                .add("filename='" + filename + "'")
                .add("hash='" + hash + "'")
                .add("sizeInBytes=" + sizeInBytes)
                .toString();
    }
}
