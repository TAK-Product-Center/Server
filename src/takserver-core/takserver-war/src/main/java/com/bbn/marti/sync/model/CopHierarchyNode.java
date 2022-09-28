package com.bbn.marti.sync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CopHierarchyNode implements Comparable<CopHierarchyNode> {

    protected String name;

    protected CopHierarchyNode parent;
    protected Set<CopHierarchyNode> children = new ConcurrentSkipListSet<CopHierarchyNode>();

    public CopHierarchyNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public CopHierarchyNode getParent() { return parent; }

    public void setParent(CopHierarchyNode parent) { this.parent = parent; }

    public Set<CopHierarchyNode> getChildren() { return children; }

    public void setChildren(Set<CopHierarchyNode> children) { this.children = children; }

    public static String getPath(CopHierarchyNode node) {
        if (node == null) {
            return "";
        }

        return getPath(node.getParent()) + "/" + node.getName();
    }

    @Override
    public int compareTo(CopHierarchyNode that) {
        return getPath(this).compareTo(getPath(that));
    }

    public void addChild(CopHierarchyNode child) {
        children.add(child);
        child.setParent(this);
    }
}