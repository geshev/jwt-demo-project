package com.example.demo.utils.security;

public class SecurityConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_AUTHORIZATION = "Bearer ";
    public static final String ROLES_CLAIM = "roles";

    public static final String TOKEN_REFRESH_ENDPOINT = "/auth/refresh";

    public static final String ACCESS_TYPE_CLAIM = "access";
    public static final String REFRESH_TYPE_CLAIM = "refresh";
    public static final String TYPE_CLAIM_VALUE = "jwt";
    public static final String INVALID_TOKEN_TYPE = "Invalid accessToken type";
}
