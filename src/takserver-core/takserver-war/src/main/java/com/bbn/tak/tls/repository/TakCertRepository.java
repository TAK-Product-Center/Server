

package com.bbn.tak.tls.repository;

import com.bbn.tak.tls.TakCert;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tak.server.Constants;

import java.util.Date;
import java.util.List;

public interface TakCertRepository extends JpaRepository<TakCert, Long> {

    @CacheEvict(value = Constants.CERTIFICATE_CACHE, allEntries = true)
    TakCert save(TakCert entity);

    @CacheEvict(value = Constants.CERTIFICATE_CACHE, allEntries = true)
    @Query(value = "update certificate set revocation_date = now() where hash = :hash returning id", nativeQuery = true)
    Long revokeByHash(@Param("hash") String hash);


    @Cacheable(Constants.CERTIFICATE_CACHE)
    TakCert findOneById(Long id);

    @Cacheable(Constants.CERTIFICATE_CACHE)
    TakCert findOneByHash(String hash);

    @Cacheable(Constants.CERTIFICATE_CACHE)
    List<TakCert> findAllByUserDn(String userDn);

    @Cacheable(Constants.CERTIFICATE_CACHE)
    List<TakCert> findAllByExpirationDateIsLessThanEqual(Date start);

    @Cacheable(Constants.CERTIFICATE_CACHE)
    List<TakCert> findAllByRevocationDateIsLessThanEqual(Date start);

    @Cacheable(Constants.CERTIFICATE_CACHE)
    @Query(value = "delete from certificate where id in ( :idList ) returning id", nativeQuery = true)
    List<Long> deleteByIds(@Param("idList") List<Long> idList);

    @Cacheable(Constants.CERTIFICATE_CACHE)
    @Query(value = "select * from (select * from certificate) q1 inner join (select client_uid, user_dn, max(issuance_date) as issuance_date from certificate group by client_uid, user_dn) q2 on q1.issuance_date = q2.issuance_date and q1.client_uid = q2.client_uid and q1.user_dn = q2.user_dn ", nativeQuery = true)
    List<TakCert> getActive();

    @Cacheable(Constants.CERTIFICATE_CACHE)
    @Query(value = "select * from (select * from certificate) q1 inner join (select client_uid, user_dn, max(issuance_date) as issuance_date from certificate group by client_uid, user_dn) q2 on q1.issuance_date < q2.issuance_date and q1.client_uid=q2.client_uid and q1.user_dn=q2.user_dn order by q1.client_uid, q1.user_dn, q1.issuance_date", nativeQuery = true)
    List<TakCert> getReplaced();
}