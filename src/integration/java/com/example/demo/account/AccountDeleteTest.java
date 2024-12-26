package com.example.demo.account;

import com.example.demo.BaseTest;
import com.example.demo.data.dto.account.AccountInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.example.demo.util.TestConstants.*;
import static com.example.demo.util.TestConstants.USER_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountDeleteTest extends BaseTest {

    @Test
    void testDeleteAccount() {
        loginAsRoot();

        ResponseEntity<AccountInfo> getResponse = getRequest(ACCOUNT_ENDPOINT + USER_USERNAME, AccountInfo.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();

        ResponseEntity<Void> response = deleteRequest(ACCOUNT_ENDPOINT + USER_USERNAME, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        getResponse = getRequest(ACCOUNT_ENDPOINT + USER_USERNAME, AccountInfo.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getResponse.getBody()).isNull();
    }

    @Test
    void testDeleteAccountNotFound() {
        loginAsRoot();

        ResponseEntity<Void> response = deleteRequest(ACCOUNT_ENDPOINT + USER_USERNAME + USER_USERNAME, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testDeleteAccountAccess() {
        // not logged in - no access
        ResponseEntity<Void> response = deleteRequest(ACCOUNT_ENDPOINT + USER_USERNAME, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logged in with insufficient authority - no access
        loginAsAdmin();
        response = deleteRequest(ACCOUNT_ENDPOINT + USER_USERNAME, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
