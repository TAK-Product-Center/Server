

package com.bbn.marti.remote.groups;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Sets;

/*
 * 
 * Group value class
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Group implements Node, Comparable<Group>, Serializable {
  
    private static final long serialVersionUID = -3052292286675008800L;

    protected Set<Node> neighbors;
    
    public Group() {
        neighbors = Sets.newConcurrentHashSet();
        this.bitpos = null;
        this.created = new Date();
        this.type = Type.SYSTEM;
    }
    
    public Group(String name, @NotNull Direction direction) {
        this();
        this.name = name;
        this.direction = direction;
    }
    
    protected String name;
    
    protected Direction direction;
    
    protected Date created;
    
    protected Type type;
    
    // assigned bit position - which bit in the "groups" vector
    protected Integer bitpos = null;

    protected boolean active = true;

    protected String description = null;

    public Integer getBitpos() {
        return bitpos;
    }

    @Nullable
    public void setBitpos(Integer bitpos) {
        this.bitpos = bitpos;
    }

    public String getName() {
        return name;
    }
   
    public Direction getDirection() {
        return direction;
    }
    
    public Type getType() {
        return type;
    }

    public void setType(@NotNull Type type) {
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }
        
        this.type = type;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean getActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((direction == null) ? 0 : direction.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Group other = (Group) obj;
        if (direction != other.direction)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        else if (active != other.active)
            return false;
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isLeaf() {
        return getNeighbors() == null || getNeighbors().isEmpty();
    }
    
    // add a child (group member)
    public void addNeighbor(Node node) {
        if (getNeighbors() == null) {
            throw new IllegalStateException("null children set");
        }
        
        getNeighbors().add(node);
    }
    
    // remove a child (group member)
    public void removeMember(Node node) {
        if (getNeighbors() == null) {
            throw new IllegalStateException("null children set");
        }
        
        getNeighbors().remove(node);
    }

    @Override
    public String toString() {
        return "Group [id=" + name + ", direction=" + direction + ", created=" + created + ", size: " + (Integer) getNeighbors().size() + ", assigned bit position: " + bitpos + "]";
    }

    @Override
    @JsonIgnore
    public Set<Node> getNeighbors() {
        return neighbors;
    }

    @Override
    public int compareTo(Group o) {
        // base Group uniqueness on {id, direction}
        return ComparisonChain.start().compare(getName(), o.getName()).compare(getDirection(), o.getDirection()).result();
    }
    
    // Maps to table 
    public static enum Type {
        LDAP,   // 0
        SYSTEM;  // 1
    }
    
    // overwrite the fields of this obejct with those from the given object
    public Group update(@NotNull Group that) {
        if (that == null) {
            throw new IllegalArgumentException("null group pass to update");
        }
        
        if (that.getName() != null) {
            this.setName(that.getName());
        }
        
        // note: not touching direction field. This must be explicity set, and is not persisted in the database because it is not relevant to messages, only users.
        if (that.getBitpos() != null) {
            this.setBitpos(that.getBitpos());
        }
        
        if (that.getType() != null) {
            this.setType(that.getType());
        }
        
        if (that.getCreated() != null) {
            this.setCreated(that.getCreated());
        }
        
        return this;
    }
    
    @JsonIgnore
    public Group getCopy() {
    	Group cp = new Group(getName(), getDirection());
    	cp.setBitpos(getBitpos());
    	cp.setCreated(getCreated());
    	cp.setType(getType());
    	return cp;
    }
    
    /**
     * A set projection for a {@link ConcurrentHashMap}.
     *
     * @param <T> The element type of the set projection.
     */
    public static class ConcurrentHashSet<T> extends AbstractSet<T> {

        /**
         * The delegate map.
         */
        private final ConcurrentMap<T, Boolean> delegate;

        /**
         * Creates a concurrent hash set.
         */
        protected ConcurrentHashSet() {
            delegate = new ConcurrentHashMap<T, Boolean>();
        }

        @Override
        public boolean add(T value) {
            return delegate.put(value, Boolean.TRUE) == null;
        }

        @Override
        public boolean remove(Object value) {
            return delegate.remove(value) != null;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<T> iterator() {
            return delegate.keySet().iterator();
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return delegate.size();
        }
    }
}
