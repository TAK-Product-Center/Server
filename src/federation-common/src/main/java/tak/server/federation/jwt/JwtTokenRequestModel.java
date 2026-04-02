package tak.server.federation.jwt;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class JwtTokenRequestModel {
	private long expiration;
    private Map<String, Object> attributes = new HashMap<String, Object>();
    
    public long getExpiration() {
		return expiration;
	}
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	@JsonAnySetter
    public void addAttribute(String key, Object value) {
		attributes.put(key, value);
    }

	@JsonAnyGetter
    public Map<String,Object> getAttributes() {
        return attributes;
    }
}
