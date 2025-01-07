package com.example.demo.service;

import com.example.demo.data.dto.account.*;
import com.example.demo.data.mapper.AccountMapperImpl;
import com.example.demo.data.model.Account;
import com.example.demo.data.model.Role;
import com.example.demo.data.repo.AccountRepository;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.error.InvalidPasswordUpdateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "test";
    private static final Set<Role> TEST_ROLES = Set.of(Role.ADMIN, Role.USER);
    private static final Account TEST_ACCOUNT =
            new Account(null, TEST_USERNAME, TEST_PASSWORD, true, TEST_ROLES);
    private static final List<Account> TEST_ACCOUNTS = List.of(TEST_ACCOUNT, TEST_ACCOUNT, TEST_ACCOUNT);
    private static final AccountCreateRequest TEST_CREATE =
            new AccountCreateRequest(TEST_USERNAME, TEST_PASSWORD, TEST_ROLES);

    private static final String TEST_USERNAME_UPDATE = "test-update";
    private static final String TEST_PASSWORD_UPDATE = "test-update";
    private static final Set<Role> TEST_ROLES_UPDATE = Set.of(Role.USER);
    private static final AccountUpdate TEST_UPDATE_ALL = new AccountUpdate(TEST_PASSWORD, true, TEST_ROLES);
    private static final AccountUpdate TEST_UPDATE_PASSWORD = new AccountUpdate(TEST_PASSWORD, null, null);
    private static final AccountUpdate TEST_UPDATE_ENABLED = new AccountUpdate(null, true, null);
    private static final AccountUpdate TEST_UPDATE_ROLES = new AccountUpdate(null, null, TEST_ROLES);

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @Spy
    private final AccountMapperImpl accountMapper = new AccountMapperImpl();

    private AccountService accountService;

    @BeforeEach
    void serviceSetup() {
        accountService = new AccountService(passwordEncoder, accountRepository, accountMapper, false);
    }

    @Test
    void testGetAccount() throws AccountNotFoundException {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(TEST_ACCOUNT));

        AccountInfo result = accountService.getAccount(TEST_USERNAME);

        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).isEqualTo(TEST_ROLES);

        verify(accountRepository, times(1)).findByUsername(TEST_USERNAME);
        verify(accountMapper, times(1)).toInfo(TEST_ACCOUNT);
    }

    @Test
    void testGetAccountNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(TEST_USERNAME));

        verify(accountRepository, times(1)).findByUsername(TEST_USERNAME);
        verify(accountMapper, never()).toInfo(any());
    }

    @Test
    void testGetAccounts() {
        when(accountRepository.findAll()).thenReturn(TEST_ACCOUNTS);

        List<AccountInfo> results = accountService.getAccounts();
        results.forEach(info -> {
            assertThat(info.username()).isEqualTo(TEST_USERNAME);
            assertThat(info.enabled()).isTrue();
            assertThat(info.roles()).isEqualTo(TEST_ROLES);
        });

        verify(accountRepository, times(1)).findAll();
        verify(accountMapper, times(TEST_ACCOUNTS.size())).toInfo(TEST_ACCOUNT);
    }

    @Test
    void testGetAccountsEmpty() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<AccountInfo> results = accountService.getAccounts();

        assertThat(results.isEmpty()).isTrue();

        verify(accountRepository, times(1)).findAll();
        verify(accountMapper, never()).toInfo(any());
    }

    @Test
    void testGetRoles() throws AccountNotFoundException {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(TEST_ACCOUNT));

        Set<Role> result = accountService.getAccountRoles(TEST_USERNAME);

        assertThat(result).isEqualTo(TEST_ROLES);
    }

    @Test
    void testGetRolesNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountRoles(TEST_USERNAME));
    }

    @Test
    void testGetProfile() throws AccountNotFoundException {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(TEST_ACCOUNT));

        Profile result = accountService.getProfile(TEST_USERNAME);

        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.roles()).isEqualTo(TEST_ROLES);

        verify(accountMapper, times(1)).toProfile(TEST_ACCOUNT);
    }

    @Test
    void testGetProfileNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getProfile(TEST_USERNAME));

        verify(accountMapper, never()).toProfile(any());
    }

    @Test
    void testCreateAccount() {
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD);

        AccountInfo result = accountService.createAccount(TEST_CREATE);

        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.enabled()).isFalse();
        assertThat(result.roles()).isEqualTo(TEST_ROLES);

        verify(accountMapper, times(1)).fromCreateRequest(TEST_CREATE);
        verify(accountMapper, times(1)).toInfo(TEST_ACCOUNT);
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(accountRepository, times(1)).save(TEST_ACCOUNT);
    }

    @Test
    // Mockito has trouble detecting why mocking an authenticated admin is necessary
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateAccountAll() throws AccountNotFoundException, IllegalRoleAssignmentException {
        mockAuthenticatedAdmin();
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES_UPDATE);
        Account updatedAccount = new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD, true, TEST_ROLES);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD);

        AccountInfo result = accountService.updateAccount(TEST_USERNAME_UPDATE, TEST_UPDATE_ALL);
        assertThat(result.username()).isEqualTo(TEST_USERNAME_UPDATE);
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).isEqualTo(TEST_ROLES);

        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(accountRepository, times(1)).save(updatedAccount);
        verify(accountMapper, times(1)).toInfo(updatedAccount);
    }

    @Test
    void testUpdateAccountPassword() throws AccountNotFoundException, IllegalRoleAssignmentException {
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES_UPDATE);
        Account updatedAccount =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD, false, TEST_ROLES_UPDATE);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD);

        AccountInfo result = accountService.updateAccount(TEST_USERNAME_UPDATE, TEST_UPDATE_PASSWORD);
        assertThat(result.username()).isEqualTo(TEST_USERNAME_UPDATE);
        assertThat(result.enabled()).isFalse();
        assertThat(result.roles()).isEqualTo(TEST_ROLES_UPDATE);

        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(accountRepository, times(1)).save(updatedAccount);
        verify(accountMapper, times(1)).toInfo(updatedAccount);
    }

    @Test
    void testUpdateAccountEnabled() throws AccountNotFoundException, IllegalRoleAssignmentException {
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES_UPDATE);
        Account updatedAccount =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, TEST_ROLES_UPDATE);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountInfo result = accountService.updateAccount(TEST_USERNAME_UPDATE, TEST_UPDATE_ENABLED);
        assertThat(result.username()).isEqualTo(TEST_USERNAME_UPDATE);
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).isEqualTo(TEST_ROLES_UPDATE);

        verify(passwordEncoder, never()).encode(any());
        verify(accountRepository, times(1)).save(updatedAccount);
        verify(accountMapper, times(1)).toInfo(updatedAccount);
    }

    @Test
    // Mockito has trouble detecting why mocking an authenticated admin is necessary
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateAccountRoles() throws AccountNotFoundException, IllegalRoleAssignmentException {
        mockAuthenticatedAdmin();
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES_UPDATE);
        Account updatedAccount =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountInfo result = accountService.updateAccount(TEST_USERNAME_UPDATE, TEST_UPDATE_ROLES);
        assertThat(result.username()).isEqualTo(TEST_USERNAME_UPDATE);
        assertThat(result.enabled()).isFalse();
        assertThat(result.roles()).isEqualTo(TEST_ROLES);

        verify(passwordEncoder, never()).encode(any());
        verify(accountRepository, times(1)).save(updatedAccount);
        verify(accountMapper, times(1)).toInfo(updatedAccount);
    }

    @Test
    // Mockito has trouble detecting why mocking an authenticated admin is necessary
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateAccountAddRoot() {
        mockAuthenticatedAdmin();
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, TEST_ROLES);
        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountUpdate addRootUpdate = new AccountUpdate(null, null, Set.of(Role.ROOT));
        assertThrows(IllegalRoleAssignmentException.class,
                () -> accountService.updateAccount(TEST_USERNAME_UPDATE, addRootUpdate));
    }

    @Test
    // Mockito has trouble detecting why mocking an authenticated admin is necessary
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testUpdateAccountRemoveRoot() {
        mockAuthenticatedAdmin();
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, Set.of(Role.ROOT));
        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountUpdate removeRootUpdate = new AccountUpdate(null, null, TEST_ROLES);
        assertThrows(IllegalRoleAssignmentException.class,
                () -> accountService.updateAccount(TEST_USERNAME_UPDATE, removeRootUpdate));
    }

    @Test
    void testUpdateAccountNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateAccount(TEST_USERNAME, TEST_UPDATE_ALL));
    }

    @Test
    void testUpdatePassword() throws InvalidPasswordUpdateException, AccountNotFoundException {
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES_UPDATE);
        Account updatedAccount =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD, false, TEST_ROLES_UPDATE);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));
        when(passwordEncoder.matches(TEST_PASSWORD_UPDATE, TEST_PASSWORD_UPDATE)).thenReturn(true);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD);

        PasswordUpdate passwordUpdate = new PasswordUpdate(TEST_PASSWORD_UPDATE, TEST_PASSWORD);
        accountService.updateAccountPassword(TEST_USERNAME_UPDATE, passwordUpdate);

        verify(passwordEncoder, times(1)).matches(TEST_PASSWORD_UPDATE, TEST_PASSWORD_UPDATE);
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(accountRepository, times(1)).save(updatedAccount);
    }

    @Test
    void testUpdatePasswordInvalid() {
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, false, TEST_ROLES_UPDATE);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));
        // here the password mismatch is set up
        when(passwordEncoder.matches(TEST_PASSWORD_UPDATE, TEST_PASSWORD_UPDATE)).thenReturn(false);

        PasswordUpdate passwordUpdate = new PasswordUpdate(TEST_PASSWORD_UPDATE, TEST_PASSWORD);
        assertThrows(InvalidPasswordUpdateException.class,
                () -> accountService.updateAccountPassword(TEST_USERNAME_UPDATE, passwordUpdate));

        verify(passwordEncoder, times(1)).matches(TEST_PASSWORD_UPDATE, TEST_PASSWORD_UPDATE);
        verify(passwordEncoder, never()).encode(TEST_PASSWORD);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testUpdatePasswordNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.empty());

        PasswordUpdate passwordUpdate = new PasswordUpdate(TEST_PASSWORD_UPDATE, TEST_PASSWORD);
        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateAccountPassword(TEST_USERNAME_UPDATE, passwordUpdate));

        verify(passwordEncoder, never()).encode(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void testDeleteAccount() throws AccountNotFoundException {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(TEST_ACCOUNT));

        accountService.deleteAccount(TEST_USERNAME);

        verify(accountRepository, times(1)).delete(TEST_ACCOUNT);
    }

    @Test
    void testDeleteAccountNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.deleteAccount(TEST_USERNAME));

        verify(accountRepository, never()).delete(any());
    }

    private void mockAuthenticatedAdmin() {
        // TEST_ACCOUNT does not have ROOT role but HAS ADMIN
        UsernamePasswordAuthenticationToken authToken = mock(UsernamePasswordAuthenticationToken.class);
        when(authToken.getPrincipal()).thenReturn(TEST_ACCOUNT);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
