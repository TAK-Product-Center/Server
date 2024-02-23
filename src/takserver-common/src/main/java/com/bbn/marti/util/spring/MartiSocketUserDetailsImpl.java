package com.bbn.marti.util.spring;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import com.bbn.marti.remote.groups.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;


public class MartiSocketUserDetailsImpl implements MartiSocketUserDetails {

    private static final long serialVersionUID = 76876876781L;

    private User user;

    public MartiSocketUserDetailsImpl(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        @SuppressWarnings("unchecked")
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) Lists.newArrayList(Collections.EMPTY_LIST);

        // Use the authorities from the wrapped core user object
        for (String authorityName : user.getAuthorities()) {
            if (!Strings.isNullOrEmpty(authorityName)) {
                authorities.add(new SimpleGrantedAuthority(authorityName));
            }
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        // don't need the password, so hide it
        return "";
    }

    @Override
    public String getUsername() {
        return user.getId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public X509Certificate getCert() {
        return user.getCert();
    }

    @Override
    public String getToken() {
        return user.getToken();
    }

    @Override
    public String toString() {
        return "TakServerUserDetails " + getUsername() + " " + getCert();
    }

};