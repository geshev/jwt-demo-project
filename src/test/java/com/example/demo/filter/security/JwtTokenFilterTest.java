package com.example.demo.filter.security;

import com.example.demo.data.security.JwtToken;
import com.example.demo.service.security.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;

import static com.example.demo.utils.security.SecurityConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtTokenFilterTest {

    // No need for this to be an actual valid token
    private static final String TEST_JWT_TOKEN = "eyJhbGciOiJIUzM4NCJ9";
    private static final String TEST_AUTHORIZATION_HEADER = BEARER_AUTHORIZATION + TEST_JWT_TOKEN;
    private static final String TEST_USER = "user";
    private static final String TEST_ENDPOINT = "/endpoint";

    @Mock
    private JwtToken validatedToken;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtTokenFilter jwtTokenFilter;

    @Value("${security.jwt.refresh-expiration}")
    long refreshExpiration;

    @Test
    void testErrorDispatchFiltering() {
        assertThat(jwtTokenFilter.shouldNotFilterErrorDispatch()).isFalse();
    }

    @Test
    void testNullAuthorizationHeader() throws ServletException, IOException {
        testNoJwtValidation(null);
    }

    @Test
    void testEmptyAuthorizationHeader() throws ServletException, IOException {
        testNoJwtValidation("");
    }

    @Test
    void testNoBearerAuthorizationHeader() throws ServletException, IOException {
        testNoJwtValidation("null");
    }

    private void testNoJwtValidation(String authorizationHeader) throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);

        jwtTokenFilter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verify(jwtService, never()).validateAndGetToken(any());
    }

    @Test
    void testInvalidToken() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(TEST_AUTHORIZATION_HEADER);
        when(jwtService.validateAndGetToken(TEST_JWT_TOKEN)).thenThrow(JwtException.class);

        jwtTokenFilter.doFilterInternal(request, response, chain);

        verify(jwtService, times(1)).validateAndGetToken(TEST_JWT_TOKEN);
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testInvalidTokenTypeRefreshTokenForAnotherEndpoint() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(TEST_AUTHORIZATION_HEADER);
        when(jwtService.validateAndGetToken(TEST_JWT_TOKEN)).thenReturn(validatedToken);
        when(validatedToken.isRefreshToken()).thenReturn(true);
        when(request.getRequestURI()).thenReturn(TEST_ENDPOINT);

        jwtTokenFilter.doFilterInternal(request, response, chain);

        verify(jwtService, times(1)).validateAndGetToken(TEST_JWT_TOKEN);
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain, never()).doFilter(request, response);
        verify(validatedToken, never()).isAccessToken();
    }

    @Test
    void testInvalidTokenTypeAccessTokenForRefreshEndpoint() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(TEST_AUTHORIZATION_HEADER);
        when(jwtService.validateAndGetToken(TEST_JWT_TOKEN)).thenReturn(validatedToken);
        when(validatedToken.isRefreshToken()).thenReturn(false);
        when(validatedToken.isAccessToken()).thenReturn(true);
        when(request.getRequestURI()).thenReturn(TOKEN_REFRESH_ENDPOINT);

        jwtTokenFilter.doFilterInternal(request, response, chain);

        verify(jwtService, times(1)).validateAndGetToken(TEST_JWT_TOKEN);
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(chain, never()).doFilter(request, response);
        verify(validatedToken, times(1)).isRefreshToken();
        verify(validatedToken, times(1)).isAccessToken();
    }

    @Test
    void testUserNotFound() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(TEST_AUTHORIZATION_HEADER);
        when(jwtService.validateAndGetToken(TEST_JWT_TOKEN)).thenReturn(validatedToken);
        when(validatedToken.isRefreshToken()).thenReturn(false);
        when(validatedToken.isAccessToken()).thenReturn(true);
        when(request.getRequestURI()).thenReturn(TEST_ENDPOINT);
        when(validatedToken.subject()).thenReturn(TEST_USER);
        when(userDetailsService.loadUserByUsername(TEST_USER)).thenThrow(UsernameNotFoundException.class);

        jwtTokenFilter.doFilterInternal(request, response, chain);

        verify(jwtService, times(1)).validateAndGetToken(TEST_JWT_TOKEN);
        verify(response, times(1)).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(userDetailsService, times(1)).loadUserByUsername(TEST_USER);
        verify(chain, never()).doFilter(request, response);
        verify(validatedToken, times(1)).isRefreshToken();
        verify(validatedToken, times(1)).isAccessToken();
    }

    @Test
    void testSuccessfulValidation() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        UserDetails userDetails = mock(UserDetails.class);

        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(TEST_AUTHORIZATION_HEADER);
        when(jwtService.validateAndGetToken(TEST_JWT_TOKEN)).thenReturn(validatedToken);
        when(validatedToken.isRefreshToken()).thenReturn(false);
        when(validatedToken.isAccessToken()).thenReturn(true);
        when(request.getRequestURI()).thenReturn(TEST_ENDPOINT);
        when(validatedToken.subject()).thenReturn(TEST_USER);
        when(userDetailsService.loadUserByUsername(TEST_USER)).thenReturn(userDetails);

        jwtTokenFilter.doFilterInternal(request, response, chain);

        verify(jwtService, times(1)).validateAndGetToken(TEST_JWT_TOKEN);
        verify(userDetailsService, times(1)).loadUserByUsername(TEST_USER);
        verify(chain, times(1)).doFilter(request, response);
        verify(validatedToken, times(1)).isRefreshToken();
        verify(validatedToken, times(1)).isAccessToken();
        // This validates that the filter set the authentication token for the security context
        verify(userDetails, times(1)).getAuthorities();
    }
}
