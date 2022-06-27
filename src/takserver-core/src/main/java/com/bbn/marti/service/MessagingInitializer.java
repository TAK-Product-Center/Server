
package com.bbn.marti.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import com.bbn.marti.config.Buffer.Queue;
import com.bbn.marti.injector.InjectionManager;
import com.bbn.marti.injector.UidCotTagInjector;
import com.bbn.marti.remote.exception.DuplicateFederateException;
import com.bbn.marti.remote.groups.GroupManager;



public class MessagingInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MessagingInitializer.class);
    
    // creation of this bean will force a load of config files
    @Autowired
    private DistributedConfiguration config;
    
    @Autowired
    private SubmissionService submissionService;
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Autowired
    private BrokerService brokerService;
    
    @Autowired
    private InjectionManager injectionManager;

    @Autowired
    private RepeaterService repeaterService;
    
    @Autowired
    private GroupManager groupMgr;
    
    @Autowired
    private Environment environment;

    @EventListener({ContextRefreshedEvent.class})
    public void init() {

    	for (String profile : environment.getActiveProfiles()) {
    		logger.debug("active profile: " + profile);
    	}

    	// Use the first command-line argument that doesn't start with -- as the CoreConfig filename
        if (config.getRepository().isEnable()) {
            try {
                repositoryService.testDatabaseConnection();
            } catch (Exception e) {
                System.err.println("Repository service is enabled, but database is not available. " + e.getMessage());
            }
        }

        try {

            injectionManager.addInjector(UidCotTagInjector.getInstance());
            
            if (config.getAuth() == null) {
                throw new IllegalArgumentException("auth element is required in CoreConfig");
            }

            // Submission -> Repeater Service
            submissionService.addConsumer(repeaterService);

            // Submission -> Broker
            submissionService.addConsumer(brokerService);

            // Submission -> Repository
            submissionService.addConsumer(repositoryService);

            // Repository -> Broker
            repositoryService.addConsumer(brokerService);
            
            try {
                if (config.getRepository().isEnable()) {
                    repositoryService.closeOpenCallsignAudits();

                    // seed in-memory data structures with mission content (CoT) uids
                    repositoryService.initializeMissionData();
                }
            } catch (Exception e) {
                logger.error("Couldn't initialize missions: " + e.getLocalizedMessage());
            }

            submissionService.startService();
            brokerService.startService();
            repositoryService.startService();
            repeaterService.startService();
            
            
            Queue queue = config.getRemoteConfiguration().getBuffer().getQueue();
            
            // if old default queue capacity is set, use the default instead
            if (queue.getCapacity() == 10) {
            	queue.setCapacity(null);
            	config.saveChanges();
            }
           
            if (config.getNetwork().getAnnounce().isEnable()) {
            	logger.warn("Announce service enabled in CoreConfig, but the announce service was removed in TAK Server 1.3.10.");
            }
            
        } catch (DuplicateFederateException def) {
            logger.error(def.getMessage());
            System.exit(1);
        } catch (Exception e) {
        	logger.error("Problem starting server: " + e.getMessage(), e);
            System.exit(1);
        }
        
        if (logger.isDebugEnabled()) {
        	logger.debug("takserver-core init complete.");
        }
	}
}
