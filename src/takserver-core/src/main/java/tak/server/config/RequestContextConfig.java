package tak.server.config;

import jakarta.servlet.annotation.WebListener;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.RequestContextListener;

import tak.server.Constants;

// This configuration populates the HttpServletRequest
@Configuration
@WebListener
@Profile({Constants.API_PROFILE_NAME, Constants.MONOLITH_PROFILE_NAME})
public class RequestContextConfig extends RequestContextListener { }
