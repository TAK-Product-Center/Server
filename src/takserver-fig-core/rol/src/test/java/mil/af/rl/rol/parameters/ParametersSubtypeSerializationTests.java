package mil.af.rl.rol.parameters;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import mil.af.rl.rol.value.MissionMetadata;
import mil.af.rl.rol.value.Parameters;
import mil.af.rl.rol.value.ResourceDetails;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * 
 * Test deserialization of ROL Parameters value classes
 * 
 */
public class ParametersSubtypeSerializationTests {

    private ObjectMapper mapper = new ObjectMapper();
    
    public ParametersSubtypeSerializationTests() { }

    @Test
    public void resourceDetails() throws JsonParseException, JsonMappingException, IOException {
        Parameters p = mapper.readValue("{ \"sha256\" : \"7cd075a026fa2a21af8b11306abe329c9bfaf6c08b17405388bdce415f182518\",\n" + 
                "   \"senderUid\" : \"7cd075a026fa2a21af8b11306abe329c9bfaf6c08b17405388bdce415f182518\",\n" + 
                "   \"name\" : \"2014-07-28T02-00-00.0Z.kmz.zip\",\n" + 
                "   \"filename\" : \"2014-07-28T02-00-00.0Z.kmz.zip\",\n" + 
                "   \"senderUrl\" : \"https://marti:8443/Marti/sync/content?hash=7cd075a026fa2a21af8b11306abe329c9bfaf6c08b17405388bdce415f182518\",\n" + 
                "   \"sizeInBytes\" : 28144,\n" + 
                "   \"senderCallsign\" : \"test1\" };", Parameters.class);
                
        
        assertTrue(p instanceof ResourceDetails);
        
        ResourceDetails rd = (ResourceDetails) p;
        
        assertTrue(rd.getSenderUrl().equals("https://marti:8443/Marti/sync/content?hash=7cd075a026fa2a21af8b11306abe329c9bfaf6c08b17405388bdce415f182518"));
    }
    
    @Test
    public void missionGetAll() throws JsonParseException, JsonMappingException, IOException {
        Parameters p = mapper.readValue("{ \"type\": \"MissionMetadata\" };", Parameters.class);
        
        assertTrue(p instanceof MissionMetadata);
    }
}