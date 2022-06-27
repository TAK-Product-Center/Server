package com.bbn.marti.injector;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.marti.config.Filter;
import com.bbn.marti.groups.GroupFederationUtil;
import com.bbn.marti.remote.RemoteSubscription;
import com.bbn.marti.remote.injector.Injector;
import com.bbn.marti.service.DistributedConfiguration;
import com.bbn.marti.service.Resources;
import com.bbn.marti.service.Subscription;
import com.bbn.marti.util.MessagingDependencyInjectionProxy;

import tak.server.cot.CotEventContainer;

/*
 * 
 */
public class InjectionManager {

    private final Filter config = DistributedConfiguration.getInstance().getRemoteConfiguration().getFilter();

    private static final Logger logger = LoggerFactory.getLogger(InjectionManager.class);

    private final GroupFederationUtil groupFederationUtil;

    public InjectionManager(GroupFederationUtil gfu) {
        this.groupFederationUtil = gfu;
    }

    // an (ordered) queue of injectors
    private final Queue<Injector<RemoteSubscription, CotEventContainer>> injectors = new ConcurrentLinkedQueue<>();

    public void addInjector(@NotNull Injector<RemoteSubscription, CotEventContainer> injector) {
        injectors.add(injector);
    }

    // using the configured thread pool, sequentially apply all configured injectors to this message, and ultimately send it
    public void process(final Subscription sub, final CotEventContainer cot) {
        
        // if injection is disabled in CoreConfig, just submit the event for processing in the calling thread
        if (config == null || config.getInjectionfilter() == null || !config.getInjectionfilter().isEnable()) {
        	if (logger.isTraceEnabled()) {
        		logger.trace("not applying injection filter - disabled in config");
        	}
            
            // submit message to marti core for processing
            MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(cot);
        } else {
            
            // if injection is enabled, run in the injection thread, and determine if injector are configured at all, and then if there are any set up for a particular UID.
        	Resources.injectionProcessor.execute(new Runnable() {
                public void run() {
                    try {

                        CotEventContainer cotEvent = cot;

                        // take a pass over this message with each injector in turn 
                        for (Injector<RemoteSubscription, CotEventContainer> injector : injectors) {
                        	if (logger.isTraceEnabled()) {
                        		logger.trace("applying injector " + injector.getName());
                        	}

                            cotEvent = injector.process(sub, cotEvent);
                        }

                        if (sub != null && sub.clientUid != null &&
                                cotEvent != null && cotEvent.getUid() != null &&
                                    sub.clientUid.compareTo(cotEvent.getUid()) == 0) {
                            groupFederationUtil.trackLatestSA(sub, cotEvent, true);
                        }

                        // submit the message for processing
                        MessagingDependencyInjectionProxy.getInstance().cotMessenger().send(cotEvent);

                    } catch (Exception e) {
                        logger.warn("exception during message injection or write " + e.getMessage(), e);
                    }
                }
            });
        }
    }
}