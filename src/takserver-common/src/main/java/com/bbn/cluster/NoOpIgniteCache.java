package com.bbn.cluster;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheEntry;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.QueryMetrics;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.mxbean.CacheMetricsMXBean;
import org.apache.ignite.transactions.TransactionException;
import org.jetbrains.annotations.Nullable;

public class NoOpIgniteCache<K,V> implements IgniteCache<K, V> {

	@Override
	public void loadAll(Set keys, boolean replaceExistingValues, CompletionListener completionListener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheManager getCacheManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object unwrap(Class clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerCacheEntryListener(CacheEntryListenerConfiguration cacheEntryListenerConfiguration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deregisterCacheEntryListener(CacheEntryListenerConfiguration cacheEntryListenerConfiguration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Iterator iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsync() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <R> IgniteFuture<R> future() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configuration getConfiguration(Class clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withExpiryPolicy(ExpiryPolicy plc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withSkipStore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withNoRetries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withPartitionRecover() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withReadRepair() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withKeepBinary() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteCache withAllowAtomicOpsInTx() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadCache(@Nullable IgniteBiPredicate p, @Nullable Object... args) throws CacheException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture loadCacheAsync(@Nullable IgniteBiPredicate p, @Nullable Object... args) throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void localLoadCache(@Nullable IgniteBiPredicate p, @Nullable Object... args) throws CacheException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture localLoadCacheAsync(@Nullable IgniteBiPredicate p, @Nullable Object... args)
			throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAndPutIfAbsent(Object key, Object val) throws CacheException, TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAndPutIfAbsentAsync(Object key, Object val) throws CacheException, TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Lock lock(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Lock lockAll(Collection keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isLocalLocked(Object key, boolean byCurrThread) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public QueryCursor query(Query qry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FieldsQueryCursor query(SqlFieldsQuery qry) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryCursor query(Query qry, IgniteClosure transformer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable localEntries(CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueryMetrics queryMetrics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetQueryMetrics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection queryDetailMetrics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetQueryDetailMetrics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void localEvict(Collection keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object localPeek(Object key, CachePeekMode... peekModes) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size(CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IgniteFuture sizeAsync(CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long sizeLong(CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IgniteFuture sizeLongAsync(CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long sizeLong(int partition, CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IgniteFuture sizeLongAsync(int partition, CachePeekMode... peekModes) throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int localSize(CachePeekMode... peekModes) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long localSizeLong(CachePeekMode... peekModes) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long localSizeLong(int partition, CachePeekMode... peekModes) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map invokeAll(Map map, Object... args) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture invokeAllAsync(Map map, Object... args) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAsync(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheEntry getEntry(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getEntryAsync(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map getAll(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAllAsync(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection getEntries(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getEntriesAsync(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map getAllOutTx(Set keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAllOutTxAsync(Set keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsKey(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture containsKeyAsync(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsKeys(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture containsKeysAsync(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture putAsync(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAndPut(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAndPutAsync(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map map) throws TransactionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture putAllAsync(Map map) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean putIfAbsent(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture putIfAbsentAsync(Object key, Object val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture removeAsync(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object key, Object oldVal) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture removeAsync(Object key, Object oldVal) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAndRemove(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAndRemoveAsync(Object key) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean replace(Object key, Object oldVal, Object newVal) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture replaceAsync(Object key, Object oldVal, Object newVal) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean replace(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IgniteFuture replaceAsync(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAndReplace(Object key, Object val) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture getAndReplaceAsync(Object key, Object val) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAll(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture removeAllAsync(Set keys) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeAll() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture removeAllAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture clearAsync() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear(Object key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture clearAsync(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearAll(Set keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture clearAllAsync(Set keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void localClear(Object key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void localClearAll(Set keys) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object invoke(Object key, EntryProcessor entryProcessor, Object... arguments) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture invokeAsync(Object key, EntryProcessor entryProcessor, Object... arguments)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object invoke(Object key, CacheEntryProcessor entryProcessor, Object... arguments)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture invokeAsync(Object key, CacheEntryProcessor entryProcessor, Object... arguments)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map invokeAll(Set keys, EntryProcessor entryProcessor, Object... args) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture invokeAllAsync(Set keys, EntryProcessor entryProcessor, Object... args)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map invokeAll(Set keys, CacheEntryProcessor entryProcessor, Object... args) throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture invokeAllAsync(Set keys, CacheEntryProcessor entryProcessor, Object... args)
			throws TransactionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture rebalance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IgniteFuture indexReadyFuture() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheMetrics metrics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheMetrics metrics(ClusterGroup grp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheMetrics localMetrics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheMetricsMXBean mxBean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheMetricsMXBean localMxBean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection lostPartitions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void enableStatistics(boolean enabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearStatistics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preloadPartition(int partition) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IgniteFuture preloadPartitionAsync(int partition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean localPreloadPartition(int partition) {
		// TODO Auto-generated method stub
		return false;
	}

}
