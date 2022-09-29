package com.bbn.marti.sync.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bbn.marti.sync.model.DataFeedDao;

public interface DataFeedRepository extends JpaRepository<DataFeedDao, Integer> {

    @Query(value = "select * from data_feed where uuid = :uuid", nativeQuery = true)
    List<DataFeedDao> getDataFeedByUUID(@Param("uuid") String uuid);

    @Query(value = "select * from data_feed where name = :name", nativeQuery = true)
    List<DataFeedDao> getDataFeedByName(@Param("name") String name);

    @Query(value = "select id, uuid, name, type, auth, port, auth_required, protocol, feed_group, iface, archive, anongroup, archive_only, "
    		+ "core_version, core_version_tls_versions, sync from data_feed order by name", nativeQuery = true)
    List<DataFeedDao> getDataFeeds();

    @Query(value = "insert into data_feed (uuid, name, type, auth, port, auth_required, protocol, feed_group, iface, archive, anongroup, "
            + "archive_only, core_version, core_version_tls_versions, sync) values (:uuid, :name, :type, :auth, :port, :authRequired, :protocol, "
            + ":feedGroup, :iface, :archive, :anongroup, :archiveOnly, :coreVersion, :coreVersion2TlsVersions, :sync) returning id", nativeQuery = true)
    Long addDataFeed(@Param("uuid") String uuid, @Param("name") String name, @Param("type") int type,
                     @Param("auth") String auth, @Param("port") Integer port, @Param("authRequired") boolean authRequired,
                     @Param("protocol") String protocol, @Param("feedGroup") String feedGroup, @Param("iface") String iface,
                     @Param("archive") boolean archive, @Param("anongroup") boolean anongroup,
                     @Param("archiveOnly") boolean archiveOnly, @Param("coreVersion") int coreVersion,
                     @Param("coreVersion2TlsVersions") String coreVersion2TlsVersions,
                     @Param("sync") boolean sync);

    @Query(value = "update data_feed set name = :name, type = :type, auth = :auth, port = :port, auth_required = :authRequired, protocol = :protocol, "
            + "feed_group = :feedGroup, iface = :iface, archive = :archive, anongroup = :anongroup, archive_only = :archiveOnly, core_version = :coreVersion, "
            + "core_version_tls_versions = :coreVersion2TlsVersions, sync = :sync where uuid = :uuid returning id", nativeQuery = true)
    Long updateDataFeed(@Param("uuid") String uuid, @Param("name") String name, @Param("type") int type,
                        @Param("auth") String auth, @Param("port") Integer port, @Param("authRequired") boolean authRequired,
                        @Param("protocol") String protocol, @Param("feedGroup") String feedGroup, @Param("iface") String iface,
                        @Param("archive") boolean archive, @Param("anongroup") boolean anongroup,
                        @Param("archiveOnly") boolean archiveOnly, @Param("coreVersion") int coreVersion,
                        @Param("coreVersion2TlsVersions") String coreVersion2TlsVersions,
                        @Param("sync") boolean sync);

	@Query(value = "update data_feed set name = :name, type = :type, archive = :archive, archive_only = :archiveOnly, sync = :sync where uuid = :uuid returning id", nativeQuery = true)
	Long modifyDataFeed(@Param("uuid") String uuid, @Param("name") String name, @Param("type") int type,
			@Param("archive") boolean archive, @Param("archiveOnly") boolean archiveOnly, @Param("sync") boolean sync);

    @Query(value = "delete from data_feed where name = :name returning id", nativeQuery = true)
    Long deleteDataFeed(@Param("name") String name);
    
    @Query(value = "delete from data_feed where id = :id returning id", nativeQuery = true)
    Long deleteDataFeedById(@Param("id") Long id);

    @Query(value = "select tag from data_feed_tag where data_feed_id = :id", nativeQuery = true)
    List<String> getDataFeedTagsById(@Param("id") Long id);

    @Query(value = "insert into data_feed_tag (data_feed_id, tag) values (:id, :tag) returning data_feed_id", nativeQuery = true)
    Long addDataFeedTag(@Param("id") Long id, @Param("tag") String tag);

    @Query(value = "delete from data_feed_tag where data_feed_id = :data_feed_id returning data_feed_id", nativeQuery = true)
    List<Long> removeAllDataFeedTagsById(@Param("data_feed_id") Long id);

    @Query(value = "select filter_group from data_feed_filter_group where data_feed_id = :id", nativeQuery = true)
    List<String> getDataFeedFilterGroupsById(@Param("id") Long id);

    @Query(value = "insert into data_feed_filter_group (data_feed_id, filter_group) values (:id, :filterGroup) returning data_feed_id", nativeQuery = true)
    Long addDataFeedFilterGroup(@Param("id") Long id, @Param("filterGroup") String filterGroup);

    @Query(value = "delete from data_feed_filter_group where data_feed_id = :data_feed_id returning data_feed_id", nativeQuery = true)
    List<Long> removeAllDataFeedFilterGroupsById(@Param("data_feed_id") Long id);

    @Query(value = "select distinct uid from cot_router inner join data_feed_cot on cot_router.id = cot_router_id inner join data_feed on data_feed.id = data_feed_id where stale > now() and data_feed.uuid = :data_feed_uuid", nativeQuery = true)
    List<String> getFeedEventUids(@Param("data_feed_uuid") String dataFeedUuid);
}
