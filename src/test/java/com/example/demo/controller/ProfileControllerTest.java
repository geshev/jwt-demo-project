package com.example.demo.controller;

import com.example.demo.data.dto.account.AccountUpdate;
import com.example.demo.data.dto.account.PasswordUpdate;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.data.model.Account;
import com.example.demo.data.model.Role;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.error.InvalidPasswordUpdateException;
import com.example.demo.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.security.auth.login.AccountNotFoundException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "test";
    private static final Set<Role> TEST_ROLES = Set.of(Role.ADMIN, Role.USER);
    private static final Account TEST_ACCOUNT =
            new Account(null, TEST_USERNAME, null, true, TEST_ROLES);
    private static final Profile TEST_PROFILE = new Profile(TEST_USERNAME, TEST_ROLES);
    private static final PasswordUpdate TEST_PASSWORD_UPDATE = new PasswordUpdate(TEST_USERNAME, TEST_PASSWORD);
    private static final AccountUpdate TEST_ACCOUNT_UPDATE =
            new AccountUpdate(TEST_PASSWORD, null, null);

    @Mock
    private AccountService accountService;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void testGetProfile() throws AccountNotFoundException {
        when(accountService.getProfile(TEST_USERNAME)).thenReturn(TEST_PROFILE);

        ResponseEntity<Profile> result = profileController.getProfile(TEST_ACCOUNT);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(accountService, times(1)).getProfile(TEST_USERNAME);
    }

    @Test
    void testGetProfileNotFound() throws AccountNotFoundException {
        when(accountService.getProfile(TEST_USERNAME)).thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class, () -> profileController.getProfile(TEST_ACCOUNT));

        verify(accountService, times(1)).getProfile(TEST_USERNAME);
    }

    @Test
    void testUpdateProfile() throws AccountNotFoundException, IllegalRoleAssignmentException, InvalidPasswordUpdateException {
        ResponseEntity<Void> result = profileController.updateAccount(TEST_ACCOUNT, TEST_PASSWORD_UPDATE);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(accountService, times(1)).updateAccountPassword(TEST_USERNAME, TEST_PASSWORD_UPDATE);
    }

    @Test
    void testUpdateProfileNotFound() throws AccountNotFoundException, IllegalRoleAssignmentException, InvalidPasswordUpdateException {
        doThrow(AccountNotFoundException.class)
                .when(accountService).updateAccountPassword(TEST_USERNAME, TEST_PASSWORD_UPDATE);

        assertThrows(AccountNotFoundException.class,
                () -> profileController.updateAccount(TEST_ACCOUNT, TEST_PASSWORD_UPDATE));

        verify(accountService, times(1)).updateAccountPassword(TEST_USERNAME, TEST_PASSWORD_UPDATE);
    }
}
