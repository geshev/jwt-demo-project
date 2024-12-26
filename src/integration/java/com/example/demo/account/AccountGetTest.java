package com.example.demo.account;

import com.example.demo.BaseTest;
import com.example.demo.data.dto.account.AccountInfo;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static com.example.demo.util.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class AccountGetTest extends BaseTest {

    private static final int EXPECTED_DEFAULT_ACCOUNTS = 3;

    @Test
    void testGetAccounts() {
        loginAsAdmin();

        ResponseEntity<List<AccountInfo>> response =
                getRequest(ACCOUNTS_ENDPOINT, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        List<AccountInfo> accounts = response.getBody();
        assertThat(accounts.size()).isEqualTo(EXPECTED_DEFAULT_ACCOUNTS);
        accounts.forEach(acc -> {
            assertThat(acc).isNotNull();
            assertThat(acc.enabled()).isTrue();
            switch (acc.username()) {
                case ROOT_USERNAME -> assertThat(acc.roles()).isEqualTo(ROOT_ROLES);
                case ADMIN_USERNAME -> assertThat(acc.roles()).isEqualTo(ADMIN_ROLES);
                case USER_USERNAME -> assertThat(acc.roles()).isEqualTo(USER_ROLES);
                // we are expecting just the default accounts to be present
                default -> fail();
            }
        });
    }

    @Test
    void testGetAccountsAccess() {
        // not logged in - no access
        ResponseEntity<List<AccountInfo>> response =
                getRequest(ACCOUNTS_ENDPOINT, new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logged in with insufficient authority - no access
        loginAsUser();
        response = getRequest(ACCOUNTS_ENDPOINT, new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void testGetAccount() {
        loginAsAdmin();

        ResponseEntity<AccountInfo> response = getRequest(ACCOUNT_ENDPOINT + USER_USERNAME, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(USER_USERNAME);
        assertThat(account.enabled()).isTrue();
        assertThat(account.roles()).isEqualTo(USER_ROLES);
    }

    @Test
    void testGetAccountNotFound() {
        loginAsAdmin();

        ResponseEntity<AccountInfo> response = getRequest(ACCOUNT_ENDPOINT + "USER_USERNAME", AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testGetAccountAccess() {
        // not logged in - no access
        ResponseEntity<AccountInfo> response = getRequest(ACCOUNT_ENDPOINT + USER_USERNAME, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logged in with insufficient authority - no access
        loginAsUser();
        response = getRequest(ACCOUNT_ENDPOINT + USER_USERNAME, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
