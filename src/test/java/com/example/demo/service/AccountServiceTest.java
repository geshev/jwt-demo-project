package com.example.demo.service;

import com.example.demo.data.dto.account.*;
import com.example.demo.data.mapper.AccountMapperImpl;
import com.example.demo.data.model.Account;
import com.example.demo.data.model.Role;
import com.example.demo.data.repo.AccountRepository;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.error.InvalidPasswordUpdateException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Objects;
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
    private static final Account TEST_ACCOUNT_CREATE =
            new Account(null, TEST_USERNAME, TEST_PASSWORD, false, TEST_ROLES);
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
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(TEST_ACCOUNT)));
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
        verify(accountMapper, times(TEST_ACCOUNTS.size()))
                .toInfo(argThat(new AccountArgumentMatcher(TEST_ACCOUNT)));
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

        verify(accountMapper, times(1))
                .toProfile(argThat(new AccountArgumentMatcher(TEST_ACCOUNT)));
    }

    @Test
    void testGetProfileNotFound() {
        when(accountRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getProfile(TEST_USERNAME));

        verify(accountMapper, never()).toProfile(any());
    }

    @Test
    void testCreateAccount() throws IllegalRoleAssignmentException {
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD);

        AccountInfo result = accountService.createAccount(TEST_CREATE);

        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.enabled()).isFalse();
        assertThat(result.roles()).isEqualTo(TEST_ROLES);

        verify(accountMapper, times(1)).fromCreateRequest(TEST_CREATE);
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(TEST_ACCOUNT_CREATE)));
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(TEST_ACCOUNT_CREATE)));
    }

    @Test
    void testCreateAccountRoot() throws IllegalRoleAssignmentException {
        mockAuthenticatedRoot();
        Set<Role> rootRoles = Set.of(Role.USER, Role.ROOT);
        Account testRootAccount = new Account(null, TEST_USERNAME, TEST_PASSWORD, false, rootRoles);

        AccountCreateRequest request =
                new AccountCreateRequest(TEST_USERNAME, TEST_PASSWORD, rootRoles);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(TEST_PASSWORD);

        AccountInfo result = accountService.createAccount(request);

        assertThat(result.username()).isEqualTo(TEST_USERNAME);
        assertThat(result.enabled()).isFalse();
        assertThat(result.roles()).isEqualTo(rootRoles);

        verify(accountMapper, times(1)).fromCreateRequest(request);
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(testRootAccount)));
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(testRootAccount)));
    }

    @Test
    void testCreateAccountRootFromAdmin() {
        mockAuthenticatedAdmin();
        AccountCreateRequest request =
                new AccountCreateRequest(TEST_USERNAME, TEST_PASSWORD, Set.of(Role.USER, Role.ROOT));

        assertThrows(IllegalRoleAssignmentException.class, () -> accountService.createAccount(request));
    }

    @Test
    void testUpdateAccountAll() throws AccountNotFoundException, IllegalRoleAssignmentException {
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
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(updatedAccount)));
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
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(updatedAccount)));
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
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(updatedAccount)));
    }

    @Test
    void testUpdateAccountRoles() throws AccountNotFoundException, IllegalRoleAssignmentException {
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
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(updatedAccount)));
    }

    @Test
    void testUpdateAccountAddRoot() throws IllegalRoleAssignmentException, AccountNotFoundException {
        mockAuthenticatedRoot();

        Set<Role> rootRole = Set.of(Role.ROOT);
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, TEST_ROLES);
        Account updatedAccount =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, rootRole);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountUpdate addRootUpdate = new AccountUpdate(null, null, rootRole);
        AccountInfo result = accountService.updateAccount(TEST_USERNAME_UPDATE, addRootUpdate);
        assertThat(result.username()).isEqualTo(TEST_USERNAME_UPDATE);
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).isEqualTo(rootRole);

        verify(passwordEncoder, never()).encode(any());
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(updatedAccount)));
    }

    @Test
    void testUpdateAccountAddRootFromAdmin() {
        mockAuthenticatedAdmin();
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, TEST_ROLES);
        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountUpdate addRootUpdate = new AccountUpdate(null, null, Set.of(Role.ROOT));
        assertThrows(IllegalRoleAssignmentException.class,
                () -> accountService.updateAccount(TEST_USERNAME_UPDATE, addRootUpdate));
    }

    @Test
    void testUpdateAccountRemoveRoot() throws IllegalRoleAssignmentException, AccountNotFoundException {
        mockAuthenticatedRoot();

        Set<Role> adminRole = Set.of(Role.ADMIN);
        Account accountToUpdate =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, Set.of(Role.ROOT));
        Account updatedAccount =
                new Account(null, TEST_USERNAME_UPDATE, TEST_PASSWORD_UPDATE, true, adminRole);

        when(accountRepository.findByUsername(TEST_USERNAME_UPDATE)).thenReturn(Optional.of(accountToUpdate));

        AccountUpdate addRootUpdate = new AccountUpdate(null, null, adminRole);
        AccountInfo result = accountService.updateAccount(TEST_USERNAME_UPDATE, addRootUpdate);
        assertThat(result.username()).isEqualTo(TEST_USERNAME_UPDATE);
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).isEqualTo(adminRole);

        verify(passwordEncoder, never()).encode(any());
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
        verify(accountMapper, times(1))
                .toInfo(argThat(new AccountArgumentMatcher(updatedAccount)));
    }

    @Test
    void testUpdateAccountRemoveRootFromAdmin() {
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
        verify(accountRepository, times(1))
                .save(argThat(new AccountArgumentMatcher(updatedAccount)));
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

        verify(accountRepository, times(1))
                .delete(argThat(new AccountArgumentMatcher(TEST_ACCOUNT)));
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
        when(authToken.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void mockAuthenticatedRoot() {
        UsernamePasswordAuthenticationToken authToken = mock(UsernamePasswordAuthenticationToken.class);
        when(authToken.getPrincipal())
                .thenReturn(new Account(null, null, null, false, Set.of(Role.ROOT)));
        when(authToken.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @RequiredArgsConstructor
    private static class AccountArgumentMatcher implements ArgumentMatcher<Account> {

        private final Account value;

        @Override
        public boolean matches(Account account) {
            return Objects.equals(account.getUsername(), value.getUsername())
                    && Objects.equals(account.getPassword(), value.getPassword())
                    && Objects.equals(account.isEnabled(), value.isEnabled())
                    && Objects.equals(account.getRoles(), value.getRoles());
        }
    }
}
