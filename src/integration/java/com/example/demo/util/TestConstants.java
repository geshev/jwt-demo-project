package com.example.demo.util;

import com.example.demo.data.model.Role;

import java.util.Set;

public class TestConstants {

    public static final String LOGIN_ENDPOINT = "/auth/token";
    public static final String LOGIN_REFRESH_ENDPOINT = "/auth/refresh";
    public static final String PROFILE_ENDPOINT = "/profile";
    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String ACCOUNT_ENDPOINT = "/accounts/";

    public static final String ROOT_USERNAME = "root";
    public static final String ROOT_PASSWORD = "root";
    public static final Set<Role> ROOT_ROLES = Set.of(Role.ROOT, Role.ADMIN, Role.USER);

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final Set<Role> ADMIN_ROLES = Set.of(Role.ADMIN, Role.USER);

    public static final String USER_USERNAME = "user";
    public static final String USER_PASSWORD = "user";
    public static final Set<Role> USER_ROLES = Set.of(Role.USER);
}
