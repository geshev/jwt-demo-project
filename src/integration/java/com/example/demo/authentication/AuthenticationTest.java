package com.example.demo.authentication;

import com.example.demo.BaseTest;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.data.dto.security.LoginRequest;
import com.example.demo.data.dto.security.LoginResponse;
import com.example.demo.data.dto.security.RefreshResponse;
import com.example.demo.service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import static com.example.demo.util.TestConstants.*;
import static com.example.demo.utils.security.SecurityConstants.BEARER_AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationTest extends BaseTest {

    private static final int EXPIRATION_MARGIN = 1000;

    @Value("${security.jwt.expiration}")
    private int accessTokenExpiration;

    @Value("${security.jwt.refresh-expiration}")
    private int refreshTokenExpiration;

    @Autowired
    private JwtService jwtService;

    @Test
    void testLogin() {
        LoginRequest request = new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD);
        ResponseEntity<LoginResponse> response = postRequest(LOGIN_ENDPOINT, request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // validate the received tokens
        LoginResponse tokens = response.getBody();
        jwtService.validateAndGetToken(tokens.accessToken());
        jwtService.validateAndGetToken(tokens.refreshToken());
    }

    @Test
    void testLoginBadCredentials() {
        LoginRequest request = new LoginRequest(ADMIN_USERNAME, USER_PASSWORD);
        ResponseEntity<LoginResponse> response = postRequest(LOGIN_ENDPOINT, request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();

        request = new LoginRequest("", "");
        response = postRequest(LOGIN_ENDPOINT, request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();

        request = new LoginRequest("ADMIN_USERNAME", "USER_PASSWORD");
        response = postRequest(LOGIN_ENDPOINT, request, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testLoginEmpty() {
        ResponseEntity<LoginResponse> response = postRequest(LOGIN_ENDPOINT, null, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testRefresh() throws InterruptedException {
        loginAsAdmin();
        ResponseEntity<Profile> profileResponse = getRequest(PROFILE_ENDPOINT, Profile.class);
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // wait for the access token to expire and refresh it
        Thread.sleep(accessTokenExpiration + EXPIRATION_MARGIN);
        profileResponse = getRequest(PROFILE_ENDPOINT, Profile.class);
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        HttpEntity<Object> request = createRefreshRequest();
        ResponseEntity<RefreshResponse> response =
                rest.postForEntity(LOGIN_REFRESH_ENDPOINT, request, RefreshResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // validate the token from the response
        RefreshResponse refreshResponse = response.getBody();
        jwtService.validateAndGetToken(refreshResponse.accessToken());
        accessToken = response.getBody().accessToken();

        profileResponse = getRequest(PROFILE_ENDPOINT, Profile.class);
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testRefreshExpired() throws InterruptedException {
        loginAsAdmin();
        ResponseEntity<Profile> profileResponse = getRequest(PROFILE_ENDPOINT, Profile.class);
        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // wait for the refresh token to expire and try to use it
        Thread.sleep(refreshTokenExpiration + EXPIRATION_MARGIN);

        HttpEntity<Object> request = createRefreshRequest();
        ResponseEntity<RefreshResponse> response =
                rest.postForEntity(LOGIN_REFRESH_ENDPOINT, request, RefreshResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testRefreshInvalidHeader() {
        // without logging in the value of the refresh token is null
        HttpEntity<Object> request = createRefreshRequest();
        ResponseEntity<RefreshResponse> response =
                rest.postForEntity(LOGIN_REFRESH_ENDPOINT, request, RefreshResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testRefreshTokenAsAccessToken() {
        loginAsAdmin();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_AUTHORIZATION + refreshToken);
        HttpEntity<Object> request = new HttpEntity<>(headers);
        ResponseEntity<Profile> response = rest.exchange(PROFILE_ENDPOINT, HttpMethod.GET, request, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testAccessTokenAsRefreshToken() {
        loginAsAdmin();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_AUTHORIZATION + accessToken);
        HttpEntity<Object> request = new HttpEntity<>(headers);
        ResponseEntity<Profile> response =
                rest.exchange(LOGIN_REFRESH_ENDPOINT, HttpMethod.POST, request, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }
}
