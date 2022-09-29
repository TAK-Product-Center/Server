package mil.af.rl.rol.value;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/*
 * Supertype for ROL parameters value class implementations
 * 
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "type",
              defaultImpl=ResourceDetails.class)
@JsonSubTypes({ 
    @Type(value = ResourceDetails.class, name = "ResourceDetails"),
    @Type(value = MissionMetadata.class, name = "MissionMetadata"),
    @Type(value = DataFeedMetadata.class, name = "DataFeedMetadata")
})
public abstract class Parameters implements Serializable {

    private static final long serialVersionUID = 1973992082385716433L;

    public Parameters() { }

}
