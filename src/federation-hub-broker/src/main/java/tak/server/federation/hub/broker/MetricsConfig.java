
package tak.server.federation.hub.broker;

import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public ActuatorMetricsService actuatorMetricsService(MetricsEndpoint metricsEndpoint) {
        return new ActuatorMetricsService(metricsEndpoint);
    }

}