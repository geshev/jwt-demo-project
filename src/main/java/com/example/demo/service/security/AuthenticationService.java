package com.example.demo.service.security;

import com.example.demo.data.dto.security.LoginRequest;
import com.example.demo.data.dto.security.LoginResponse;
import com.example.demo.data.dto.security.RefreshResponse;
import com.example.demo.data.model.Account;
import com.example.demo.data.model.Role;
import com.example.demo.service.AccountService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.example.demo.utils.security.SecurityConstants.ROLES_CLAIM;

@Service
@Transactional
public class AuthenticationService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AccountService accountService;

    public AuthenticationService(JwtService jwtService, AuthenticationManager authenticationManager, AccountService accountService) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.accountService = accountService;
    }

    public LoginResponse authenticate(LoginRequest loginRequest) throws AccountNotFoundException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        Map<String, Set<Role>> accountClaims = getAccountClaims(loginRequest.username());
        String accessToken = jwtService.generateAccessToken(accountClaims, loginRequest.username());
        String refreshToken = jwtService.generateRefreshToken(accountClaims, loginRequest.username());

        return new LoginResponse(accessToken, refreshToken);
    }

    public RefreshResponse refreshAuthentication() throws AccountNotFoundException {
        String username =
                ((Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        Map<String, Set<Role>> accountClaims = getAccountClaims(username);
        String accessToken = jwtService.generateAccessToken(accountClaims, username);
        return new RefreshResponse(accessToken);
    }

    private Map<String, Set<Role>> getAccountClaims(String username) throws AccountNotFoundException {
        Map<String, Set<Role>> accountClaims = new HashMap<>();
        Set<Role> accountRoles = accountService.getAccountRoles(username);
        accountClaims.put(ROLES_CLAIM, accountRoles);
        return accountClaims;
    }
}
