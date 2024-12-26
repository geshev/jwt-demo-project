package com.example.demo.controller;

import com.example.demo.data.dto.account.AccountCreateRequest;
import com.example.demo.data.dto.account.AccountInfo;
import com.example.demo.data.dto.account.AccountUpdate;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequestMapping("accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping()
    public ResponseEntity<List<AccountInfo>> getAccounts() {
        return ResponseEntity.ok(accountService.getAccounts());
    }

    @GetMapping("{username}")
    public ResponseEntity<AccountInfo> getAccount(@PathVariable String username) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getAccount(username));
    }

    @PostMapping
    public ResponseEntity<AccountInfo> createAccount(@RequestBody @Valid AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @PatchMapping("{username}")
    public ResponseEntity<AccountInfo> updateAccount(@PathVariable String username,
                                                     @RequestBody AccountUpdate request) throws AccountNotFoundException, IllegalRoleAssignmentException {
        return ResponseEntity.ok(accountService.updateAccount(username, request));
    }

    @DeleteMapping("{username}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String username) throws AccountNotFoundException {
        accountService.deleteAccount(username);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
