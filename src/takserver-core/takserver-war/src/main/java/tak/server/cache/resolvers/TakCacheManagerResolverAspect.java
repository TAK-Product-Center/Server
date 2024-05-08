package tak.server.cache.resolvers;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

import tak.server.cache.SpringCacheOperationUpdater;

@Aspect
@Configurable
public class TakCacheManagerResolverAspect {
	private static final Logger logger = LoggerFactory.getLogger(TakCacheManagerResolverAspect.class);
	
	@Autowired
    private SpringCacheOperationUpdater springCacheOperationUpdater;
	
	@AfterReturning(value = "execution(* tak.server.cache.resolvers.TakCacheManagerResolver.resolveCaches(..))", returning="returnValue")
	public void resolveCaches(JoinPoint jp, Object returnValue) throws RemoteException {
		try {
			Collection<? extends Cache> caches = (Collection<? extends Cache>) returnValue;
			CacheOperationInvocationContext<?> context = (CacheOperationInvocationContext<?>) jp.getArgs()[0];
			
			if (caches != null && context != null) {
				if ((CacheOperation) context.getOperation() instanceof CacheEvictOperation) {
					// notify that there was an eviction
					CacheOperation operation = (CacheOperation) context.getOperation();
					List<String> cacheNames = caches.stream().map(c -> c.getName()).collect(Collectors.toList());
					springCacheOperationUpdater.publishCacheUpdate(operation, cacheNames);
				}
			}
		} catch (Exception e) {
			logger.error("exception executing resolveCaches advice: ", e);
		}
	}

}
