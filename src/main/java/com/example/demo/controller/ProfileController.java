package com.example.demo.controller;

import com.example.demo.data.dto.account.PasswordUpdate;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.error.IllegalRoleAssignmentException;
import com.example.demo.error.InvalidPasswordUpdateException;
import com.example.demo.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("profile")
public class ProfileController {

    private final AccountService accountService;

    public ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<Profile> getProfile(@AuthenticationPrincipal UserDetails userDetails) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getProfile(userDetails.getUsername()));
    }

    @PatchMapping
    public ResponseEntity<Void> updateAccount(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody @Valid PasswordUpdate request) throws AccountNotFoundException, IllegalRoleAssignmentException, InvalidPasswordUpdateException {
        accountService.updateAccountPassword(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
