package com.example.demo.service.security;

import com.example.demo.data.dto.security.LoginRequest;
import com.example.demo.data.dto.security.LoginResponse;
import com.example.demo.data.dto.security.RefreshResponse;
import com.example.demo.data.model.Account;
import com.example.demo.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import javax.security.auth.login.AccountNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    private static final String TEST_USER = "user";
    private static final String TEST_PASSWORD = "user";
    private static final LoginRequest TEST_LOGIN_REQUEST = new LoginRequest(TEST_USER, TEST_PASSWORD);
    private static final UsernamePasswordAuthenticationToken TEST_USER_PASS_AUTH_TOKEN =
            new UsernamePasswordAuthenticationToken(TEST_USER, TEST_PASSWORD);
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzM4NCJ9";
    private static final Account TEST_ACCOUNT = new Account();

    static {
        TEST_ACCOUNT.setUsername(TEST_USER);
    }

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void testBadCredentials() throws AccountNotFoundException {
        when(authenticationManager.authenticate(TEST_USER_PASS_AUTH_TOKEN)).thenThrow(BadCredentialsException.class);

        assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(TEST_LOGIN_REQUEST));

        verify(authenticationManager, times(1)).authenticate(TEST_USER_PASS_AUTH_TOKEN);
        verify(accountService, never()).getAccountRoles(any());
        verify(jwtService, never()).generateAccessToken(any(), any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
    }

    @Test
    void testAccountNotFound() throws AccountNotFoundException {
        when(accountService.getAccountRoles(TEST_USER)).thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class, () -> authenticationService.authenticate(TEST_LOGIN_REQUEST));

        verify(authenticationManager, times(1)).authenticate(TEST_USER_PASS_AUTH_TOKEN);
        verify(accountService, times(1)).getAccountRoles(TEST_USER);
        verify(jwtService, never()).generateAccessToken(any(), any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
    }

    @Test
    void testAuthentication() throws AccountNotFoundException {
        when(jwtService.generateAccessToken(any(), eq(TEST_USER))).thenReturn(TEST_TOKEN);
        when(jwtService.generateRefreshToken(any(), eq(TEST_USER))).thenReturn(TEST_TOKEN);

        LoginResponse authResponse = authenticationService.authenticate(TEST_LOGIN_REQUEST);

        assertThat(StringUtils.hasText(authResponse.accessToken())).isTrue();
        assertThat(authResponse.accessToken()).isEqualTo(TEST_TOKEN);
        assertThat(StringUtils.hasText(authResponse.refreshToken())).isTrue();
        assertThat(authResponse.refreshToken()).isEqualTo(TEST_TOKEN);

        verify(authenticationManager, times(1)).authenticate(TEST_USER_PASS_AUTH_TOKEN);
        verify(accountService, times(1)).getAccountRoles(TEST_USER);
        verify(jwtService, times(1)).generateAccessToken(any(), eq(TEST_USER));
        verify(jwtService, times(1)).generateRefreshToken(any(), eq(TEST_USER));
    }

    @Test
    void testAuthenticationRefresh() throws AccountNotFoundException {
        UsernamePasswordAuthenticationToken authToken = mock(UsernamePasswordAuthenticationToken.class);
        when(authToken.getPrincipal()).thenReturn(TEST_ACCOUNT);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(jwtService.generateAccessToken(any(), eq(TEST_USER))).thenReturn(TEST_TOKEN);

        RefreshResponse refreshResponse = authenticationService.refreshAuthentication();

        assertThat(StringUtils.hasText(refreshResponse.accessToken())).isTrue();
        assertThat(refreshResponse.accessToken()).isEqualTo(TEST_TOKEN);
        verify(accountService, times(1)).getAccountRoles(TEST_USER);
        verify(jwtService, times(1)).generateAccessToken(any(), eq(TEST_USER));
    }

    @Test
    void testAuthenticationRefreshAccountNotFound() throws AccountNotFoundException {
        UsernamePasswordAuthenticationToken authToken = mock(UsernamePasswordAuthenticationToken.class);
        when(authToken.getPrincipal()).thenReturn(TEST_ACCOUNT);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(accountService.getAccountRoles(TEST_USER)).thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class, () -> authenticationService.refreshAuthentication());

        verify(accountService, times(1)).getAccountRoles(TEST_USER);
        verify(jwtService, never()).generateAccessToken(any(), eq(TEST_USER));
    }
}
