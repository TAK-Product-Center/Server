package tak.server.cache;

import java.util.List;

import org.springframework.cache.interceptor.CacheOperation;

public final class SpringCacheOperationUpdate {
	private CacheOperation operation;
	private List<String> caches;
	
	public SpringCacheOperationUpdate(CacheOperation operation, List<String> caches) {
		this.operation = operation;
		this.caches = caches;
	}

	public CacheOperation getOperation() {
		return operation;
	}

	public List<String> getCaches() {
		return caches;
	}

	@Override
	public String toString() {
		return "SpringCacheOperationUpdater [operation=" + operation + ", caches=" + caches + "]";
	}
}
