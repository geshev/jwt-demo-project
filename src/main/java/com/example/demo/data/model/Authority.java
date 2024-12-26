package com.example.demo.data.model;

import org.springframework.security.core.GrantedAuthority;

public enum Authority implements GrantedAuthority {
    PROFILE_READ, PROFILE_UPDATE,
    ACCOUNTS_CREATE, ACCOUNTS_READ, ACCOUNTS_UPDATE, ACCOUNTS_DELETE;

    @Override
    public String getAuthority() {
        return name();
    }
}
