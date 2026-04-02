package tak.server.federation.hub.broker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import tak.server.federation.FederateIdentity;

/*
 * An addressable entity is a wrapper that holds information
 * about a remote entity on a network. This entity may be
 * identified by a URI, or something else, like a UID or a callsign.
 */
public class AddressableEntity implements Serializable, Comparable<AddressableEntity> {
    private static final long serialVersionUID = 1124681280265508465L;
    protected final FederateIdentity entity;
    protected Collection<String> entityGroups = new ArrayList<>();


    public AddressableEntity(FederateIdentity entity) {
        this.entity = entity;
    }
    
    public AddressableEntity(FederateIdentity entity, Collection<String> entityGroups) {
        this.entity = entity;
        this.entityGroups = entityGroups;
    }

    public FederateIdentity getEntity() {
        return entity;
    }

    public String getType() {
        return entity.getClass().getName();
    }

    public Collection<String> getEntityGroups() {
		return entityGroups;
	}

	public void setEntityGroups(Collection<String> entityGroups) {
		this.entityGroups = entityGroups;
	}

	@Override
    public String toString() {
        return "AddressableEntity{" +
                "entity=" + entity + ", type=" + getType() +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        AddressableEntity that = (AddressableEntity) object;

        return entity.equals(that.entity);

    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(AddressableEntity other) {
        String thisType = this.getType();
        String otherType = other.getType();

        int typeComparison = thisType.compareTo(otherType);

        if (typeComparison != 0) {
            return typeComparison;
        }

        /* Types are the same, entities should have the same class. */
        if (Comparable.class.isAssignableFrom(this.entity.getClass())){
            Comparable<FederateIdentity> thisEntity = (Comparable<FederateIdentity>) entity;
            FederateIdentity oEntity = other.entity;
            return thisEntity.compareTo(oEntity);
        } else {
            return this.entity.toString().compareTo(other.entity.toString());
        }
    }
}
