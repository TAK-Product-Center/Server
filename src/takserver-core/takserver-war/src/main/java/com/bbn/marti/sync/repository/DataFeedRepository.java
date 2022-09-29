package com.bbn.marti.sync.repository;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.marti.sync.model.DataFeedDao;

import tak.server.Constants;

public interface DataFeedRepository extends JpaRepository<DataFeedDao, Integer> {

    @Query(value = "select * from data_feed where uuid = :uuid", nativeQuery = true)
    List<DataFeedDao> getDataFeedByUUID(@Param("uuid") String uuid);

    @Query(value = "select * from data_feed where name = :name", nativeQuery = true)
    List<DataFeedDao> getDataFeedByName(@Param("name") String name);
    
    @Query(value = "select * from data_feed where id = :id", nativeQuery = true)
    List<DataFeedDao> getDataFeedById(@Param("id") Long id);

    @Query(value = "select * from data_feed where name = :name and " + RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<DataFeedDao> getDataFeedByGroup(@Param("name") String name, @Param("groupVector") String groupVector);    
    
    @Query(value = "select * from data_feed order by name", nativeQuery = true)
    List<DataFeedDao> getDataFeeds();
    
    @Query(value = "select * from data_feed where type = 3 order by name", nativeQuery = true)
    List<DataFeedDao> getFederationDataFeeds();

	@Query(value = "select * from data_feed where " + RemoteUtil.GROUP_CLAUSE + " order by name", nativeQuery = true)
    List<DataFeedDao> getDataFeedsByGroups(@Param("groupVector") String groupVector);

    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "insert into data_feed (uuid, name, type, auth, port, auth_required, protocol, feed_group, iface, archive, anongroup, "
            + "archive_only, core_version, core_version_tls_versions, sync, sync_cache_retention_seconds, groups) values (:uuid, :name, :type, :auth, :port, :authRequired, :protocol, :feedGroup, "
            + " :iface, :archive, :anongroup, :archiveOnly, :coreVersion, :coreVersion2TlsVersions, :sync, :syncCacheRetentionSeconds, " + RemoteUtil.GROUP_VECTOR + " ) returning id", nativeQuery = true)
    Long addDataFeed(@Param("uuid") String uuid, @Param("name") String name, @Param("type") int type,
                     @Param("auth") String auth, @Param("port") Integer port, @Param("authRequired") boolean authRequired,
                     @Param("protocol") String protocol, @Param("feedGroup") String feedGroup, @Param("iface") String iface,
                     @Param("archive") boolean archive, @Param("anongroup") boolean anongroup,
                     @Param("archiveOnly") boolean archiveOnly, @Param("coreVersion") int coreVersion,
                     @Param("coreVersion2TlsVersions") String coreVersion2TlsVersions,
                     @Param("sync") boolean sync, @Param("syncCacheRetentionSeconds") int syncCacheRetentionSeconds, @Param("groupVector") String groupVector);

    
    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "update data_feed set name = :name, type = :type, auth = :auth, port = :port, auth_required = :authRequired, protocol = :protocol, "
            + "feed_group = :feedGroup, iface = :iface, archive = :archive, anongroup = :anongroup, archive_only = :archiveOnly, core_version = :coreVersion, "
            + "core_version_tls_versions = :coreVersion2TlsVersions, sync = :sync, sync_cache_retention_seconds = :syncCacheRetentionSeconds where uuid = :uuid returning id", nativeQuery = true)
    Long updateDataFeed(@Param("uuid") String uuid, @Param("name") String name, @Param("type") int type,
                        @Param("auth") String auth, @Param("port") Integer port, @Param("authRequired") boolean authRequired,
                        @Param("protocol") String protocol, @Param("feedGroup") String feedGroup, @Param("iface") String iface,
                        @Param("archive") boolean archive, @Param("anongroup") boolean anongroup,
                        @Param("archiveOnly") boolean archiveOnly, @Param("coreVersion") int coreVersion,
                        @Param("coreVersion2TlsVersions") String coreVersion2TlsVersions,
                        @Param("sync") boolean sync, @Param("syncCacheRetentionSeconds") int syncCacheRetentionSeconds);

    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "update data_feed set name = :name, type = :type, auth = :auth, port = :port, auth_required = :authRequired, protocol = :protocol, "
            + "feed_group = :feedGroup, iface = :iface, archive = :archive, anongroup = :anongroup, archive_only = :archiveOnly, core_version = :coreVersion, "
            + "core_version_tls_versions = :coreVersion2TlsVersions, sync = :sync, sync_cache_retention_seconds = :syncCacheRetentionSeconds, groups =" + RemoteUtil.GROUP_VECTOR + " where uuid = :uuid and " + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long updateDataFeedWithGroupVector(@Param("uuid") String uuid, @Param("name") String name, @Param("type") int type,
                        @Param("auth") String auth, @Param("port") Integer port, @Param("authRequired") boolean authRequired,
                        @Param("protocol") String protocol, @Param("feedGroup") String feedGroup, @Param("iface") String iface,
                        @Param("archive") boolean archive, @Param("anongroup") boolean anongroup,
                        @Param("archiveOnly") boolean archiveOnly, @Param("coreVersion") int coreVersion,
                        @Param("coreVersion2TlsVersions") String coreVersion2TlsVersions,
                        @Param("sync") boolean sync, @Param("syncCacheRetentionSeconds") int syncCacheRetentionSeconds, @Param("groupVector") String groupVector);
    
    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "delete from data_feed where name = :name and " + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long deleteDataFeed(@Param("name") String name, @Param("groupVector") String groupVector);
    
    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "delete from data_feed where id = :id and " + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long deleteDataFeedById(@Param("id") Long id, @Param("groupVector") String groupVector);
    
    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "delete from data_feed where id = :id returning id", nativeQuery = true)
    Long deleteDataFeedById(@Param("id") Long id);

    @Query(value = "select tag from data_feed_tag where data_feed_id = :id", nativeQuery = true)
    List<String> getDataFeedTagsById(@Param("id") Long id);

    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "SELECT count(*) FROM insert_data_feed_tags(:id, :tags);", nativeQuery = true)
    Long addDataFeedTags(@Param("id") Long id, @Param("tags") List<String> tags);

    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "delete from data_feed_tag where data_feed_id = :data_feed_id returning data_feed_id", nativeQuery = true)
    List<Long> removeAllDataFeedTagsById(@Param("data_feed_id") Long id);

    @Query(value = "select filter_group from data_feed_filter_group where data_feed_id = :id", nativeQuery = true)
    List<String> getDataFeedFilterGroupsById(@Param("id") Long id);

    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "SELECT count(*) FROM insert_data_feed_filter_groups(:id, :filterGroups);", nativeQuery = true)
    Long addDataFeedFilterGroups(@Param("id") Long id, @Param("filterGroups") List<String> filterGroups);
    
    @CacheEvict(value = Constants.DATA_FEED_CACHE, allEntries = true)
    @Query(value = "delete from data_feed_filter_group where data_feed_id = :data_feed_id returning data_feed_id", nativeQuery = true)
    List<Long> removeAllDataFeedFilterGroupsById(@Param("data_feed_id") Long id);

    @Query(value = "select distinct uid from cot_router inner join data_feed_cot on cot_router.id = cot_router_id inner join data_feed on data_feed.id = data_feed_id where stale > now() and data_feed.uuid = :data_feed_uuid", nativeQuery = true)
    List<String> getFeedEventUids(@Param("data_feed_uuid") String dataFeedUuid);
}
