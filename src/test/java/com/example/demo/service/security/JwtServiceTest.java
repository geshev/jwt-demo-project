package com.example.demo.service.security;

import com.example.demo.data.model.Role;
import com.example.demo.data.security.JwtToken;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.demo.utils.security.SecurityConstants.ROLES_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    private static final String SECRET_KEY = "da2e8af32b20a7dc1b19197d2c2ae6e4a5c059b37b64bbcb03eb038d1f1c04fe";
    private static final int TOKEN_EXPIRATION = 2000;
    private static final int REFRESH_TOKEN_EXPIRATION = 4000;
    private static final int TEST_EXPIRATION_MARGIN = 500;

    private static final String TEST_USER = "user";
    private static final Set<Role> TEST_ADMIN_ROLE = Set.of(Role.ADMIN);
    private static final Set<Role> TEST_USER_ROLE = Set.of(Role.USER);
    private static final Map<String, Set<Role>> TEST_ADMIN_CLAIMS = Map.of(ROLES_CLAIM, TEST_ADMIN_ROLE);
    private static final Map<String, Set<Role>> TEST_USER_CLAIMS = Map.of(ROLES_CLAIM, TEST_USER_ROLE);

    private static final String MALFORMED_TOKEN = "eyJhbGciOiJIUzM4NCJ9";
    private static final String INVALID_SIGNATURE_TOKEN = "eyJhbGciOiJIUzM4NCJ9."
            + "eyJyb2xlcyI6WyJBRE1JTiIsIlVTRVIiXSwiYWNjZXNzIjoiand0Iiwic3ViI"
            + "joiYWRtaW4iLCJpYXQiOjE3MzQ2MDU0ODMsImV4cCI6MTczNDYwNTkwM30."
            + "eyJhbGciOiJIUzM4NCJ9";

    private final JwtService jwtService = new JwtService(SECRET_KEY, TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

    @Test
    void testMalformedToken() {
        assertThrows(MalformedJwtException.class, () -> jwtService.validateAndGetToken(MALFORMED_TOKEN));
    }

    @Test
    void testInvalidSignature() {
        assertThrows(SignatureException.class, () -> jwtService.validateAndGetToken(INVALID_SIGNATURE_TOKEN));
    }

    @Test
    void testAccessToken() {
        String generatedToken = jwtService.generateAccessToken(TEST_ADMIN_CLAIMS, TEST_USER);
        JwtToken adminToken = jwtService.validateAndGetToken(generatedToken);
        assertThat(adminToken.isAccessToken()).isTrue();
        assertThat(adminToken.isRefreshToken()).isFalse();

        generatedToken = jwtService.generateAccessToken(TEST_USER_CLAIMS, TEST_USER);
        JwtToken userToken = jwtService.validateAndGetToken(generatedToken);
        assertThat(userToken.isAccessToken()).isTrue();
        assertThat(userToken.isRefreshToken()).isFalse();
    }

    @Test
    void testRefreshToken() {
        String generatedToken = jwtService.generateRefreshToken(TEST_ADMIN_CLAIMS, TEST_USER);
        JwtToken adminToken = jwtService.validateAndGetToken(generatedToken);
        assertThat(adminToken.isAccessToken()).isFalse();
        assertThat(adminToken.isRefreshToken()).isTrue();

        generatedToken = jwtService.generateRefreshToken(TEST_USER_CLAIMS, TEST_USER);
        JwtToken userToken = jwtService.validateAndGetToken(generatedToken);
        assertThat(userToken.isAccessToken()).isFalse();
        assertThat(userToken.isRefreshToken()).isTrue();
    }

    @Test
    void testTokenClaims() {
        String generatedToken = jwtService.generateAccessToken(TEST_ADMIN_CLAIMS, TEST_USER);
        JwtToken adminToken = jwtService.validateAndGetToken(generatedToken);

        assertThat(adminToken.subject()).isEqualTo(TEST_USER);
        assertThat(adminToken.claims().containsKey(ROLES_CLAIM)).isTrue();
        assertThat(((List<?>) adminToken.claims().get(ROLES_CLAIM)).contains(Role.ADMIN.name())).isTrue();
    }

    @Test
    void testExpiredAccessToken() throws InterruptedException {
        String generatedToken = jwtService.generateAccessToken(TEST_ADMIN_CLAIMS, TEST_USER);
        Thread.sleep(TOKEN_EXPIRATION + TEST_EXPIRATION_MARGIN);
        assertThrows(ExpiredJwtException.class, () -> jwtService.validateAndGetToken(generatedToken));
    }

    @Test
    void testExpiredRefreshToken() throws InterruptedException {
        String generatedToken = jwtService.generateRefreshToken(TEST_ADMIN_CLAIMS, TEST_USER);
        Thread.sleep(REFRESH_TOKEN_EXPIRATION + TEST_EXPIRATION_MARGIN);
        assertThrows(ExpiredJwtException.class, () -> jwtService.validateAndGetToken(generatedToken));
    }
}
