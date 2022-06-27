package tak.server.federation.hub.ui;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom JsonRawValue deserializer. Jackson Json provides a JsonRaw serializer, but no deserializer.
 * This does the job. Found this approach via Google.
 *
 *
 */
public class JsonRawValueDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser jp, DeserializationContext context)
        throws IOException {
        return jp.readValueAsTree().toString();
    }
}
