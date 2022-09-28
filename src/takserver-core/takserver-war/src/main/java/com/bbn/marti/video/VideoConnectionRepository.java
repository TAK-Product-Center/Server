package com.bbn.marti.video;

import com.bbn.marti.remote.util.RemoteUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tak.server.Constants;

import java.util.List;

public interface VideoConnectionRepository extends JpaRepository<VideoConnection, Long> {

    @CacheEvict(value = Constants.VIDEO_CACHE, allEntries = true)
    @Query(value = "insert into video_connections_v2 (uid, active, alias, thumbnail, classification, xml, groups) values " +
            " (:uid, :active, :alias, :thumbnail, :classification, :xml, " + RemoteUtil.GROUP_VECTOR +  " ) returning id", nativeQuery = true)
    Long create(@Param("uid") String uid,
                @Param("active") boolean active,
                @Param("alias") String alias,
                @Param("thumbnail") String thumbnail,
                @Param("classification") String classification,
                @Param("xml") String xml,
                @Param("groupVector") String groupVector);

    @CacheEvict(value = Constants.VIDEO_CACHE, allEntries = true)
    @Query(value = "update video_connections_v2 set uid = :uid, active = :active, alias = :alias, " +
            " thumbnail = :thumbnail, classification = :classification, xml = :xml " +
            " where uid = :uid and " + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long update(@Param("uid") String uid,
                @Param("active") boolean active,
                @Param("alias") String alias,
                @Param("thumbnail") String thumbnail,
                @Param("classification") String classification,
                @Param("xml") String xml,
                @Param("groupVector") String groupVector);

    @CacheEvict(value = Constants.VIDEO_CACHE, allEntries = true)
    @Query(value = "delete from  video_connections_v2 where uid = :uid and " + RemoteUtil.GROUP_CLAUSE + " returning id", nativeQuery = true)
    Long delete(@Param("uid") String uid, @Param("groupVector") String groupVector);

    @Cacheable(Constants.VIDEO_CACHE)
    @Query(value = "select id, uid, active, alias, thumbnail, classification, xml, groups from video_connections_v2" +
            " where " + RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    List<VideoConnection> get(@Param("groupVector") String groupVector);

    @Cacheable(Constants.VIDEO_CACHE)
    @Query(value = "select id, uid, active, alias, thumbnail, classification, xml, groups from video_connections_v2" +
            " where uid = :uid and " + RemoteUtil.GROUP_CLAUSE, nativeQuery = true)
    VideoConnection getByUid(@Param("uid") String uid, @Param("groupVector") String groupVector);
}