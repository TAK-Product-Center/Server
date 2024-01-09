package tak.server.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.bbn.marti.remote.ClientEndpoint;
import com.bbn.marti.remote.CoreConfig;
import com.bbn.marti.remote.exception.TakException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class ContactCacheHelper {

	private static final Logger log = Logger.getLogger(ContactCacheHelper.class);

	private Cache<String, List<ClientEndpoint>> contactCache;

	@PostConstruct
	public void init() {

		if (log.isDebugEnabled()) {
			log.debug("in init");
		}

		contactCache = Caffeine.newBuilder()
				.expireAfterWrite(CoreConfigFacade.getInstance().getCachedConfiguration().getBuffer().getQueue().getContactCacheUpdateRateLimitSeconds() * 4, TimeUnit.SECONDS)
				.build();
	}

	// invalidate the contact cache 
	public void clearContactCache() {

		// concurrently hitting this block can result in concurrent invalidations - this is intentional
		try {

			if (contactCache != null) {
				contactCache.invalidateAll();
			}
		} catch (Exception e) {
			throw new TakException(e);
		}
	}

	public Cache<String, List<ClientEndpoint>> getContactsCache() {
		return contactCache;
	}

	public String getKeyGetCachedClientEndpointData(boolean connected, boolean recent, long secAgo) {
		return "getCachedClientEndpointData_" + connected + "_" + recent + "_" + secAgo;
	}
}