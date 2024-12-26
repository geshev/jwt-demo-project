package com.example.demo.account;

import com.example.demo.BaseTest;
import com.example.demo.data.dto.account.AccountCreateRequest;
import com.example.demo.data.dto.account.AccountInfo;
import com.example.demo.data.dto.account.AccountUpdate;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.data.dto.error.ValidationError;
import com.example.demo.data.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.example.demo.util.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class AccountCreateTest extends BaseTest {

    private static final String NEW_ACCOUNT_USERNAME = "new-account";
    private static final String NEW_ACCOUNT_PASSWORD = "new-account";
    private static final Set<Role> NEW_ACCOUNT_ROLES = Set.of(Role.ADMIN, Role.USER);

    @Test
    void testCreateAccount() {
        String testUsername = NEW_ACCOUNT_USERNAME + UUID.randomUUID();
        loginAsAdmin();

        AccountCreateRequest request = new AccountCreateRequest(testUsername, NEW_ACCOUNT_PASSWORD, NEW_ACCOUNT_ROLES);
        ResponseEntity<AccountInfo> response = postRequest(ACCOUNTS_ENDPOINT, request, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(testUsername);
        assertThat(account.enabled()).isFalse();
        assertThat(account.roles()).isEqualTo(NEW_ACCOUNT_ROLES);

        // verify that the account is not enabled
        assertThrows(BadCredentialsException.class, () -> login(testUsername, NEW_ACCOUNT_PASSWORD));
        // then enable the account
        AccountUpdate enableUpdate = new AccountUpdate(null, true, null);
        ResponseEntity<AccountInfo> enableResponse =
                patchRequest(ACCOUNT_ENDPOINT + testUsername, enableUpdate, AccountInfo.class);

        // verify that we can log in with the new account and that it is usable
        login(testUsername, NEW_ACCOUNT_PASSWORD);

        // test the profile functionality
        ResponseEntity<Profile> profileResponse = getRequest(PROFILE_ENDPOINT, Profile.class);

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody()).isNotNull();

        Profile profile = profileResponse.getBody();
        assertThat(profile.username()).isEqualTo(testUsername);
        assertThat(profile.roles()).isEqualTo(NEW_ACCOUNT_ROLES);

        // test an admin endpoint
        ResponseEntity<AccountInfo> accountResponse =
                getRequest(ACCOUNT_ENDPOINT + testUsername, AccountInfo.class);

        assertThat(accountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accountResponse.getBody()).isNotNull();

        AccountInfo accountInfo = accountResponse.getBody();
        assertThat(accountInfo.username()).isEqualTo(testUsername);
        assertThat(accountInfo.enabled()).isTrue();
        assertThat(accountInfo.roles()).isEqualTo(NEW_ACCOUNT_ROLES);
    }

    @Test
    void testCreateDuplicate() {
        String testUsername = NEW_ACCOUNT_USERNAME + UUID.randomUUID();
        loginAsAdmin();

        AccountCreateRequest request = new AccountCreateRequest(testUsername, NEW_ACCOUNT_PASSWORD, NEW_ACCOUNT_ROLES);
        ResponseEntity<AccountInfo> response = postRequest(ACCOUNTS_ENDPOINT, request, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        response = postRequest(ACCOUNTS_ENDPOINT, request, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testCreateAccountEmptyJson() {
        loginAsAdmin();
        String emptyJson = "{}";
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, emptyJson, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(3);
        errors.forEach(error -> {
            switch (error.field()) {
                case "username", "password" -> assertThat(error.error()).contains("blank");
                case "roles" -> assertThat(error.error()).contains("empty");
                // we are not expecting errors in other fields
                default -> fail();
            }
        });
    }

    @Test
    void testCreateAccountEmptyRequest() {
        loginAsAdmin();
        AccountCreateRequest emptyRequest =
                new AccountCreateRequest(null, null, null);
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, emptyRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(3);
        errors.forEach(error -> {
            switch (error.field()) {
                case "username", "password" -> assertThat(error.error()).contains("blank");
                case "roles" -> assertThat(error.error()).contains("empty");
                // we are not expecting errors in other fields
                default -> fail();
            }
        });
    }

    @Test
    void testCreateAccountNoUsernameRequest() {
        loginAsAdmin();
        AccountCreateRequest noUsernameRequest =
                new AccountCreateRequest(null, NEW_ACCOUNT_PASSWORD, NEW_ACCOUNT_ROLES);
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, noUsernameRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().field()).isEqualTo("username");
    }

    @Test
    void testCreateAccountEmptyUsernameRequest() {
        loginAsAdmin();
        AccountCreateRequest emptyUsernameRequest =
                new AccountCreateRequest("", NEW_ACCOUNT_PASSWORD, NEW_ACCOUNT_ROLES);
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, emptyUsernameRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().field()).isEqualTo("username");
    }

    @Test
    void testCreateAccountNoPasswordRequest() {
        loginAsAdmin();
        AccountCreateRequest noPasswordRequest =
                new AccountCreateRequest(NEW_ACCOUNT_USERNAME, null, NEW_ACCOUNT_ROLES);
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, noPasswordRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().field()).isEqualTo("password");
    }

    @Test
    void testCreateAccountEmptyPasswordRequest() {
        loginAsAdmin();
        AccountCreateRequest emptyPasswordRequest =
                new AccountCreateRequest(NEW_ACCOUNT_USERNAME, "  ", NEW_ACCOUNT_ROLES);
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, emptyPasswordRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().field()).isEqualTo("password");
    }

    @Test
    void testCreateAccountNoRolesRequest() {
        loginAsAdmin();
        AccountCreateRequest noRolesRequest =
                new AccountCreateRequest(NEW_ACCOUNT_USERNAME, NEW_ACCOUNT_PASSWORD, null);
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, noRolesRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().field()).isEqualTo("roles");
    }

    @Test
    void testCreateAccountEmptyRolesRequest() {
        loginAsAdmin();
        AccountCreateRequest emptyRolesRequest =
                new AccountCreateRequest(NEW_ACCOUNT_USERNAME, NEW_ACCOUNT_PASSWORD, Set.of());
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, emptyRolesRequest, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst().field()).isEqualTo("roles");
    }

    @Test
    void testCreateAccountInvalidRole() {
        loginAsAdmin();
        String invalidRolesJson = """
                {
                    "username": "username123",
                    "password": "username123",
                    "roles": [
                        "ADMIN",
                        "SOME_ROLE",
                        "USER"
                    ]
                }
                """;
        ResponseEntity<List<ValidationError>> response =
                postRequest(ACCOUNTS_ENDPOINT, invalidRolesJson, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testCreateAccountAccess() {
        AccountCreateRequest request =
                new AccountCreateRequest(NEW_ACCOUNT_USERNAME, NEW_ACCOUNT_PASSWORD, NEW_ACCOUNT_ROLES);
        // not logged in - no access
        ResponseEntity<AccountInfo> response = postRequest(ACCOUNTS_ENDPOINT, request, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logged in with insufficient authority - no access
        loginAsUser();
        response = postRequest(ACCOUNTS_ENDPOINT, request, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
