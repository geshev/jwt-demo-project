package com.example.demo.data.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import static com.example.demo.data.model.Authority.*;

@Getter
@RequiredArgsConstructor
public enum Role {
    ROOT(Set.of(ACCOUNTS_DELETE)),
    ADMIN(Set.of(ACCOUNTS_CREATE, ACCOUNTS_READ, ACCOUNTS_UPDATE)),
    USER(Set.of(PROFILE_READ, PROFILE_UPDATE));

    private final Set<Authority> authorities;
}
