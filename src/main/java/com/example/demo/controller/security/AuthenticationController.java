package com.example.demo.controller.security;

import com.example.demo.data.dto.security.LoginRequest;
import com.example.demo.data.dto.security.LoginResponse;
import com.example.demo.data.dto.security.RefreshResponse;
import com.example.demo.error.AccountNotFoundException;
import com.example.demo.service.security.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("token")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginRequest loginRequest) throws AccountNotFoundException {
        return ResponseEntity.ok(authenticationService.authenticate(loginRequest));
    }

    @PostMapping("refresh")
    public ResponseEntity<RefreshResponse> refreshAuthentication() throws AccountNotFoundException {
        return ResponseEntity.ok(authenticationService.refreshAuthentication());
    }
}
