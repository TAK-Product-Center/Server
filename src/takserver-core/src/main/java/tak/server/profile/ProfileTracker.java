package tak.server.profile;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import tak.server.Constants;

// Keep track of which profiles are currently active
// See Constants.java for list of supported profiles
public class ProfileTracker implements EnvironmentAware {
	
	private static final Logger logger = LoggerFactory.getLogger(ProfileTracker.class);
	
	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}
	
	@EventListener({ContextRefreshedEvent.class})
	private void init() {
		if (logger.isDebugEnabled()) {
			logger.debug("active profiles: " + Arrays.toString(environment.getActiveProfiles()));
		}
	}
	
	public boolean isMonolith() {
		return environment.acceptsProfiles(Profiles.of(Constants.MONOLITH_PROFILE_NAME));
	}
}
