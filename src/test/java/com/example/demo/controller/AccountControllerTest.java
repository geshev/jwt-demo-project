package com.example.demo.controller;

import com.example.demo.data.dto.account.AccountCreateRequest;
import com.example.demo.data.dto.account.AccountInfo;
import com.example.demo.data.dto.account.AccountUpdate;
import com.example.demo.data.model.Role;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "test";
    private static final Set<Role> TEST_ROLES = Set.of(Role.ADMIN, Role.USER);
    private static final AccountInfo TEST_ACCOUNT =
            new AccountInfo(TEST_USERNAME, true, TEST_ROLES);
    private static final List<AccountInfo> TEST_ACCOUNTS = List.of(TEST_ACCOUNT, TEST_ACCOUNT, TEST_ACCOUNT);
    private static final AccountCreateRequest TEST_ACCOUNT_CREATE =
            new AccountCreateRequest(TEST_USERNAME, TEST_PASSWORD, TEST_ROLES);
    private static final AccountUpdate TEST_ACCOUNT_UPDATE =
            new AccountUpdate(TEST_PASSWORD, null, null);

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    void testGetAccounts() {
        when(accountService.getAccounts()).thenReturn(TEST_ACCOUNTS);

        ResponseEntity<List<AccountInfo>> result = accountController.getAccounts();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(accountService, times(1)).getAccounts();
    }

    @Test
    void testGetAccount() throws AccountNotFoundException {
        when(accountService.getAccount(TEST_USERNAME)).thenReturn(TEST_ACCOUNT);

        ResponseEntity<AccountInfo> result = accountController.getAccount(TEST_USERNAME);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(accountService, times(1)).getAccount(TEST_USERNAME);
    }

    @Test
    void testGetAccountNotFound() throws AccountNotFoundException {
        when(accountService.getAccount(TEST_USERNAME)).thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class, () -> accountController.getAccount(TEST_USERNAME));

        verify(accountService, times(1)).getAccount(TEST_USERNAME);
    }

    @Test
    void testCreateAccount() {
        ResponseEntity<AccountInfo> result = accountController.createAccount(TEST_ACCOUNT_CREATE);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        verify(accountService, times(1)).createAccount(TEST_ACCOUNT_CREATE);
    }

    @Test
    void testUpdateAccount() throws AccountNotFoundException, IllegalRoleAssignmentException {
        when(accountService.updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE)).thenReturn(TEST_ACCOUNT);

        ResponseEntity<AccountInfo> result = accountController.updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        verify(accountService, times(1)).updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE);
    }

    @Test
    void testUpdateAccountIllegalRoleAssignment() throws AccountNotFoundException, IllegalRoleAssignmentException {
        when(accountService.updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE))
                .thenThrow(IllegalRoleAssignmentException.class);

        assertThrows(IllegalRoleAssignmentException.class,
                () -> accountController.updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE));

        verify(accountService, times(1)).updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE);
    }

    @Test
    void testUpdateAccountNotFound() throws AccountNotFoundException, IllegalRoleAssignmentException {
        when(accountService.updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE))
                .thenThrow(AccountNotFoundException.class);

        assertThrows(AccountNotFoundException.class,
                () -> accountController.updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE));

        verify(accountService, times(1)).updateAccount(TEST_USERNAME, TEST_ACCOUNT_UPDATE);
    }

    @Test
    void testDeleteAccount() throws AccountNotFoundException {
        ResponseEntity<Void> result = accountController.deleteAccount(TEST_USERNAME);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(accountService, times(1)).deleteAccount(TEST_USERNAME);
    }

    @Test
    void testDeleteAccountNotFound() throws AccountNotFoundException {
        doThrow(AccountNotFoundException.class).when(accountService).deleteAccount(TEST_USERNAME);

        assertThrows(AccountNotFoundException.class, () -> accountController.deleteAccount(TEST_USERNAME));

        verify(accountService, times(1)).deleteAccount(TEST_USERNAME);
    }
}
