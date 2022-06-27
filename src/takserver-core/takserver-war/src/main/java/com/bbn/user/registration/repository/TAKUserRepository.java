package com.bbn.user.registration.repository;

import com.bbn.marti.remote.util.RemoteUtil;
import com.bbn.user.registration.model.TAKUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface TAKUserRepository extends JpaRepository<TAKUser, Long> {

    @Query(value = "insert into tak_user ( token, user_name, email_address, first_name, last_name, phone_number, organization, state, activated, groups ) values " +
            " ( :token, :userName, :emailAddress, :firstName, :lastName, :phoneNumber, :organization, :state, :activated, " + RemoteUtil.GROUP_VECTOR + " )  " +
            " returning id", nativeQuery = true)
    Long create(@Param("token") String token, @Param("userName") String userName, @Param("emailAddress") String emailAddress,
                @Param("firstName") String firstName, @Param("lastName") String lastName, @Param("phoneNumber") String phoneNumber,
                @Param("organization") String organization, @Param("state") String state, @Param("activated") boolean activated,
                @Param("groupVector") String groupVector);

    @Query(value = "insert into tak_user ( token, user_name, email_address, first_name, last_name, phone_number, organization, state, activated ) values " +
            " ( :token, :userName, :emailAddress, :firstName, :lastName, :phoneNumber, :organization, :state, :activated )  " +
            " returning id", nativeQuery = true)
    Long create(@Param("token") String token, @Param("userName") String userName, @Param("emailAddress") String emailAddress,
                @Param("firstName") String firstName, @Param("lastName") String lastName, @Param("phoneNumber") String phoneNumber,
                @Param("organization") String organization, @Param("state") String state, @Param("activated") boolean activated);

    @Query(value = "update tak_user set token = :token, user_name = :userName, email_address = :emailAddress, first_name = :firstName, last_name = :lastName, " +
            " phone_number = :phoneNumber, organization = :organization, state = :state, activated = :activated, groups = " + RemoteUtil.GROUP_VECTOR +
            " where id = :id returning id ", nativeQuery = true)
    Long update(@Param("token") String token, @Param("userName") String userName, @Param("emailAddress") String emailAddress,
                @Param("firstName") String firstName, @Param("lastName") String lastName, @Param("phoneNumber") String phoneNumber,
                @Param("organization") String organization, @Param("state") String state, @Param("activated") boolean activated,
                @Param("groupVector") String groupVector, @Param("id") Long id);

    @Query(value = "update tak_user set token = :token, user_name = :userName, email_address = :emailAddress, first_name = :firstName, last_name = :lastName, " +
            " phone_number = :phoneNumber, organization = :organization, state = :state, activated = :activated " +
            " where id = :id returning id ", nativeQuery = true)
    Long update(@Param("token") String token, @Param("userName") String userName, @Param("emailAddress") String emailAddress,
                @Param("firstName") String firstName, @Param("lastName") String lastName, @Param("phoneNumber") String phoneNumber,
                @Param("organization") String organization, @Param("state") String state, @Param("activated") boolean activated,
                @Param("id") Long id);

    TAKUser findByToken(String token);
    TAKUser findByEmailAddress(String emailAddress);
};

