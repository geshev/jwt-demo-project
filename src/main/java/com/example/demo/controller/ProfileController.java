package com.example.demo.controller;

import com.example.demo.data.dto.account.AccountUpdate;
import com.example.demo.data.dto.account.PasswordUpdate;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.AccountNotFoundException;

@RestController
public class ProfileController {

    private final AccountService accountService;

    public ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("profile")
    public ResponseEntity<Profile> getProfile(@AuthenticationPrincipal UserDetails userDetails) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getProfile(userDetails.getUsername()));
    }

    @PatchMapping("profile")
    public ResponseEntity<Void> updateAccount(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody PasswordUpdate request) throws AccountNotFoundException, IllegalRoleAssignmentException {
        accountService.updateAccount(userDetails.getUsername(),
                new AccountUpdate(request.password(), null, null));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
