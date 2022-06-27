package tak.server.federation.hub.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import tak.server.federation.hub.FederationHubConstants;
import tak.server.federation.hub.FederationHubUtils;
import tak.server.federation.hub.broker.FederationHubBroker;
import tak.server.federation.hub.broker.FederationHubBrokerProxyFactory;

@SpringBootApplication
public class FederationHubPolicyManagerService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(FederationHubPolicyManagerService.class);

    private static Ignite ignite = null;

    @Autowired
    private FederationHubBroker fedHubBroker;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(FederationHubPolicyManagerService.class);

        ignite = Ignition.getOrStart(FederationHubUtils.getIgniteConfiguration(
           FederationHubConstants.FEDERATION_HUB_POLICY_IGNITE_PROFILE,
           false));
        if (ignite == null) {
            System.exit(1);
        }

        ApplicationContext context = application.run(args);
    }

    @Bean
    public Ignite getIgnite() {
        return ignite;
    }

    @Override
    public void run(String... args) throws Exception {
        FederationHubPolicyManagerImpl hpm = new FederationHubPolicyManagerImpl();
        ClusterGroup cg = ignite.cluster().forAttribute(
            FederationHubConstants.FEDERATION_HUB_IGNITE_PROFILE_KEY,
            FederationHubConstants.FEDERATION_HUB_POLICY_IGNITE_PROFILE);
        ignite.services(cg).deployClusterSingleton(
            FederationHubConstants.FED_HUB_POLICY_MANAGER_SERVICE, hpm);
    }

    @Bean
    public FederationHubBrokerProxyFactory fedHubBrokerProxyFactory() {
        return new FederationHubBrokerProxyFactory();
    }

    @Bean
    public FederationHubBroker fedHubBroker() throws Exception {
        return fedHubBrokerProxyFactory().getObject();
    }
}
