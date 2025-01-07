package com.example.demo.controller.security;

import com.example.demo.data.dto.security.LoginRequest;
import com.example.demo.data.dto.security.LoginResponse;
import com.example.demo.data.dto.security.RefreshResponse;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.service.security.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    private static final LoginRequest TEST_LOGIN_REQUEST =
            new LoginRequest("test", "test");
    private static final LoginResponse TEST_LOGIN_RESPONSE =
            new LoginResponse("accToken", "refToken");
    private static final RefreshResponse TEST_REFRESH_RESPONSE = new RefreshResponse("accRefToken");

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @Test
    void testAuthenticate() throws AccountNotFoundException {
        when(authenticationService.authenticate(TEST_LOGIN_REQUEST)).thenReturn(TEST_LOGIN_RESPONSE);

        ResponseEntity<LoginResponse> result = authenticationController.authenticate(TEST_LOGIN_REQUEST);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(authenticationService, times(1)).authenticate(TEST_LOGIN_REQUEST);
    }

    @Test
    void testAuthenticateNotFound() throws AccountNotFoundException {
        when(authenticationService.authenticate(TEST_LOGIN_REQUEST)).thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class, () -> authenticationController.authenticate(TEST_LOGIN_REQUEST));

        verify(authenticationService, times(1)).authenticate(TEST_LOGIN_REQUEST);
    }

    @Test
    void testRefreshAuthentication() throws AccountNotFoundException {
        when(authenticationService.refreshAuthentication()).thenReturn(TEST_REFRESH_RESPONSE);

        ResponseEntity<RefreshResponse> result = authenticationController.refreshAuthentication();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(authenticationService, times(1)).refreshAuthentication();
    }

    @Test
    void testRefreshAuthenticationNotFound() throws AccountNotFoundException {
        when(authenticationService.refreshAuthentication()).thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class, () -> authenticationController.refreshAuthentication());

        verify(authenticationService, times(1)).refreshAuthentication();
    }
}
