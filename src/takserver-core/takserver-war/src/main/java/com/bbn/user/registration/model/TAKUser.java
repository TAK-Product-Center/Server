package com.bbn.user.registration.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;

@Entity
@Table(name = "tak_user")
@Cacheable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TAKUser {

    protected Long id;
    protected String userName;
    protected String emailAddress;
    protected String firstName;
    protected String lastName;
    protected String phoneNumber;
    protected String organization;
    protected String token;
    protected String state;
    protected String groupVector;
    protected boolean activated;

    public TAKUser() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "user_name", nullable = false, columnDefinition = "TEXT")
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "email_address", nullable = false, columnDefinition = "TEXT")
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Column(name = "first_name", nullable = false, columnDefinition = "TEXT")
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(name = "last_name", nullable = false, columnDefinition = "TEXT")
    public String getLastName() {
        return firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Column(name = "phone_number", nullable = false, columnDefinition = "TEXT")
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Column(name = "organization", nullable = false, columnDefinition = "TEXT")
    public String getOrganization() {
        return organization;
    }
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    @Column(name = "state", nullable = false, columnDefinition = "TEXT")
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }

    @Column(name = "groups", columnDefinition = "bit varying")
    public String getGroupVector() {
        return groupVector;
    }
    public void setGroupVector(String groupVector) {
        this.groupVector = groupVector;
    }

    @Column(name = "activated", nullable = false, columnDefinition = "TEXT")
    public boolean getActivated() {
        return activated;
    }
    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}
