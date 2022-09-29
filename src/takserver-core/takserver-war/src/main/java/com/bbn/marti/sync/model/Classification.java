package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ComparisonChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


@XmlRootElement(name = "classification")
@Entity
@Table(name = "classification")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Classification implements Serializable, Comparable<Classification> {

    protected static final Logger logger = LoggerFactory.getLogger(Classification.class);

    protected Long id;
    protected String level;
    protected Set<Caveat> caveats;

    public Classification() {
        caveats = new ConcurrentSkipListSet<>();
    }

    public Classification(String level) {
        this();
        this.level = level;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    @JsonIgnore
    @XmlTransient
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "level", nullable = false, columnDefinition = "text")
    public String getLevel() {
        return level;
    }
    public void setLevel(String level) {
        this.level = level;
    }

    @ManyToMany(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
    @JoinTable(name="classification_caveat",
            joinColumns = {@JoinColumn(name="classification_id", referencedColumnName="id")},
            inverseJoinColumns = {@JoinColumn(name="caveat_id", referencedColumnName="id")}
    )
    public Set<Caveat> getCaveats() { return caveats; }
    public void setCaveats(Set<Caveat> caveats) { this.caveats = caveats; }

    @Override
    public int compareTo(Classification that) {
        return ComparisonChain.start()
                .compare(this.level, that.level)
                .result();
    }
}