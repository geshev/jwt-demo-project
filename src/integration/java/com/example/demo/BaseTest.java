package com.example.demo;

import com.example.demo.data.dto.security.LoginRequest;
import com.example.demo.data.dto.security.LoginResponse;
import com.example.demo.data.dto.security.RefreshResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.example.demo.util.TestConstants.*;
import static com.example.demo.utils.security.SecurityConstants.BEARER_AUTHORIZATION;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseTest {

    protected String accessToken;
    protected String refreshToken;

    @Autowired
    protected TestRestTemplate rest;

    protected void loginAsRoot() {
        login(ROOT_USERNAME, ROOT_PASSWORD);
    }

    protected void loginAsAdmin() {
        login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    protected void loginAsUser() {
        login(USER_USERNAME, USER_PASSWORD);
    }

    protected void login(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        ResponseEntity<LoginResponse> response = rest.postForEntity(LOGIN_ENDPOINT, request, LoginResponse.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BadCredentialsException("Login request for " + username + " failed with "
                    + response.getStatusCode());
        }

        accessToken = response.getBody().accessToken();
        refreshToken = response.getBody().refreshToken();
    }

    protected void refreshToken() {
        HttpEntity<Object> request = createRefreshRequest();
        ResponseEntity<RefreshResponse> response =
                rest.postForEntity(LOGIN_REFRESH_ENDPOINT, request, RefreshResponse.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new BadCredentialsException("Refresh request failed with " + response.getStatusCode());
        }

        accessToken = response.getBody().accessToken();
    }

    protected HttpEntity<Object> createRefreshRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_AUTHORIZATION + refreshToken);
        return new HttpEntity<>(headers);
    }

    @BeforeEach
    protected void logout() {
        accessToken = null;
        refreshToken = null;
    }

    protected <T> ResponseEntity<T> getRequest(String url, Class<T> responseType) {
        HttpEntity<?> request = createHttpRequest(null);
        return rest.exchange(url, HttpMethod.GET, request, responseType);
    }

    protected <T> ResponseEntity<T> getRequest(String url, ParameterizedTypeReference<T> responseType) {
        HttpEntity<?> request = createHttpRequest(null);
        return rest.exchange(url, HttpMethod.GET, request, responseType);
    }

    protected <T> ResponseEntity<T> postRequest(String url, Object body, Class<T> responseType) {
        HttpEntity<?> request = createHttpRequest(body);
        return rest.exchange(url, HttpMethod.POST, request, responseType);
    }

    protected <T> ResponseEntity<T> postRequest(String url, Object body, ParameterizedTypeReference<T> responseType) {
        HttpEntity<?> request = createHttpRequest(body);
        return rest.exchange(url, HttpMethod.POST, request, responseType);
    }

    protected <T> ResponseEntity<T> putRequest(String url, Object body, Class<T> responseType) {
        HttpEntity<?> request = createHttpRequest(body);
        return rest.exchange(url, HttpMethod.PUT, request, responseType);
    }

    protected <T> ResponseEntity<T> patchRequest(String url, Object body, Class<T> responseType) {
        HttpEntity<?> request = createHttpRequest(body);
        return rest.exchange(url, HttpMethod.PATCH, request, responseType);
    }

    protected <T> ResponseEntity<T> deleteRequest(String url, Class<T> responseType) {
        HttpEntity<?> request = createHttpRequest(null);
        return rest.exchange(url, HttpMethod.DELETE, request, responseType);
    }

    private HttpEntity<?> createHttpRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            headers.set(HttpHeaders.AUTHORIZATION, BEARER_AUTHORIZATION + accessToken);
        }
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
        return new HttpEntity<>(body, headers);
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder, SslBundles sslBundles) {
        return restTemplateBuilder.setSslBundle(sslBundles.getBundle("integration")).build();
    }

    @Container
    private static PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:17-alpine3.20")
                    .withDatabaseName("demo")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }
}
