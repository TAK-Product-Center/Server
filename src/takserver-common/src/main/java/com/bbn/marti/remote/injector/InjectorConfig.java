package com.bbn.marti.remote.injector;

import java.io.Serializable;

import com.google.common.collect.ComparisonChain;

// Injector value class
public final class InjectorConfig implements Comparable<InjectorConfig>, Serializable {
    
    private static final long serialVersionUID = 3579628652877709555L;

    private String uid;
    
    private String toInject;
    
    public InjectorConfig() { }
    
    public InjectorConfig(String uid, String toInject) {
        this.uid = uid;
        this.toInject = toInject;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getToInject() {
        return toInject;
    }

    public void setToInject(String toInject) {
        this.toInject = toInject;
    }

    @Override
    public String toString() {
        return "InjectorConfig [uid=" + uid + ", toInject=" + toInject + "]";
    }

    @Override
    public int compareTo(InjectorConfig that) {
        return ComparisonChain.start().compare(this.uid, that.getUid()).result();
    }
}