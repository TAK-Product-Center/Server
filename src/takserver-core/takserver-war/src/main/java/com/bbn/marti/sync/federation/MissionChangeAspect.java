package com.bbn.marti.sync.federation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

import com.bbn.marti.sync.model.MissionChange;

@Aspect
@Configurable
public class MissionChangeAspect {
	
    private static final Logger logger = LoggerFactory.getLogger(MissionChangeAspect.class);
	
	@Around("execution(* com.bbn.marti.sync.repository.MissionChangeRepository.save(..)) || execution(* com.bbn.marti.sync.repository.MissionChangeRepository.saveAndFlush(..))")
	public Object save(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
		Object[] args = proceedingJoinPoint.getArgs();
		try {
			if (args != null && args.length == 1 && args[0] instanceof MissionChange) {
				MissionChange change = (MissionChange) args[0];
				change.setIsFederatedChange(isFederatedMissionChange());
			}
			
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("exception executing create mission advice: " + e);
			}
		}		
		return proceedingJoinPoint.proceed(args); 
	}
	
	private boolean isFederatedMissionChange() {
    	StackTraceElement[] stack = new Exception().getStackTrace();
    	
    	for (StackTraceElement el : stack) {
    		
    		if (logger.isTraceEnabled()) {
    			logger.trace("stack element: " + el.getClassName());
    		}
    		
			if (el.getClassName().equals(FederationROLHandler.class.getName())) {
				return true;
			}
		} 

    	return false;
    }

}
