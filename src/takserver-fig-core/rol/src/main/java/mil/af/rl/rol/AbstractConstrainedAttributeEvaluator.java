package mil.af.rl.rol;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public abstract class AbstractConstrainedAttributeEvaluator<CT> implements RoleAttributeEvaluator<CT> {
    
    Logger logger = Logger.getLogger(AbstractConstrainedAttributeEvaluator.class);
    
    private final AtomicInteger assignmentCounter;
    private final CT client;

    public AbstractConstrainedAttributeEvaluator(AtomicInteger roleAssignmentCounter, CT client) {
        this.assignmentCounter = roleAssignmentCounter;
        this.client = client;
    }
    
    @Override
    public void assign(String role) {
           assignmentCounter.incrementAndGet();
           
           logger.debug("role assignment for " + role + " and client " + client + " counter incremented: " + assignmentCounter.get());
    }

}
