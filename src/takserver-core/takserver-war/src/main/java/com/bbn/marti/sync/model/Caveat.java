package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ComparisonChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

@Entity
@Table(name = "caveat")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Caveat implements Serializable, Comparable<Caveat> {

    protected static final Logger logger = LoggerFactory.getLogger(Caveat.class);

    protected Long id;
    protected String name;

    public Caveat(String namne) {
        this.name = namne;
    }

    public Caveat() {
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

    @Column(name = "name", unique = true, nullable = false, columnDefinition = "text")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(Caveat that) {
        return ComparisonChain.start()
                .compare(this.name, that.name)
                .result();
    }
}