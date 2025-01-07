package com.example.demo.service;

import com.example.demo.data.dto.account.*;
import com.example.demo.data.mapper.AccountMapper;
import com.example.demo.data.model.Account;
import com.example.demo.data.model.Role;
import com.example.demo.data.repo.AccountRepository;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.error.InvalidPasswordUpdateException;
import com.example.demo.utils.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.example.demo.data.model.Role.*;

@Service
@Transactional
public class AccountService {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountService(PasswordEncoder passwordEncoder, AccountRepository accountRepository,
                          AccountMapper accountMapper,
                          @Value("${demo.create.default.accounts:false}") boolean createDefault) {
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;

        if (createDefault) {
            Account root = Account.builder().username("root").password(passwordEncoder.encode("root"))
                    .roles(Set.of(ROOT, ADMIN, USER)).enabled(true).build();
            Account admin = Account.builder().username("admin").password(passwordEncoder.encode("admin"))
                    .roles(Set.of(ADMIN, USER)).enabled(true).build();
            Account user = Account.builder().username("user").password(passwordEncoder.encode("user"))
                    .roles(Set.of(USER)).enabled(true).build();
            accountRepository.save(root);
            accountRepository.save(admin);
            accountRepository.save(user);
        }
    }

    public AccountInfo getAccount(String username) throws AccountNotFoundException {
        return accountMapper.toInfo(findAccount(username));
    }

    public List<AccountInfo> getAccounts() {
        return accountRepository.findAll().stream().map(accountMapper::toInfo).toList();
    }

    public Set<Role> getAccountRoles(String username) throws AccountNotFoundException {
        return findAccount(username).getRoles();
    }

    public Profile getProfile(String username) throws AccountNotFoundException {
        return accountMapper.toProfile(findAccount(username));
    }

    public AccountInfo createAccount(AccountCreateRequest request) {
        Account newAccount = accountMapper.fromCreateRequest(request);
        newAccount.setPassword(passwordEncoder.encode(newAccount.getPassword()));
        accountRepository.save(newAccount);
        return accountMapper.toInfo(newAccount);
    }

    public AccountInfo updateAccount(String username, AccountUpdate request) throws AccountNotFoundException, IllegalRoleAssignmentException {
        Account account = findAccount(username);
        if (request.password() != null) {
            account.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.enabled() != null) {
            account.setEnabled(request.enabled());
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            updateAccountRoles(account, request.roles());
        }
        accountRepository.save(account);
        return accountMapper.toInfo(account);
    }

    public void updateAccountPassword(String username, PasswordUpdate request) throws AccountNotFoundException, InvalidPasswordUpdateException {
        Account account = findAccount(username);

        if (!passwordEncoder.matches(request.oldPassword(), account.getPassword())) {
            throw new InvalidPasswordUpdateException();
        }

        account.setPassword(passwordEncoder.encode(request.newPassword()));
        accountRepository.save(account);
    }

    private void updateAccountRoles(Account account, Set<Role> rolesUpdate) throws IllegalRoleAssignmentException {
        // Detect if there is an update that involves the ROOT role using XOR
        if (rolesUpdate.contains(ROOT) ^ account.getRoles().contains(ROOT)) {
            // ROOT role can only be changed by users with ROOT role
            Account requester = SecurityUtils.getAuthenticatedAccount();
            if (requester == null || !requester.getRoles().contains(ROOT)) {
                throw new IllegalRoleAssignmentException();
            }
        }
        account.setRoles(rolesUpdate);
    }

    public void deleteAccount(String username) throws AccountNotFoundException {
        accountRepository.delete(findAccount(username));
    }

    private Account findAccount(String username) throws AccountNotFoundException {
        return accountRepository.findByUsername(username).orElseThrow(AccountNotFoundException::new);
    }
}
