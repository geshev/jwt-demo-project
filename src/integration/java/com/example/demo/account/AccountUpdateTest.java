package com.example.demo.account;

import com.example.demo.BaseTest;
import com.example.demo.data.dto.account.AccountCreateRequest;
import com.example.demo.data.dto.account.AccountInfo;
import com.example.demo.data.dto.account.AccountUpdate;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.data.model.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static com.example.demo.util.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountUpdateTest extends BaseTest {

    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_PASSWORD = "test-user";
    private static final Set<Role> TEST_ROLES = Set.of(Role.USER);

    @BeforeEach
    void setup() {
        loginAsAdmin();
        AccountCreateRequest request = new AccountCreateRequest(TEST_USERNAME, TEST_PASSWORD, TEST_ROLES);
        postRequest(ACCOUNTS_ENDPOINT, request, AccountInfo.class);
        logout();
    }

    @AfterEach
    void clean() {
        loginAsRoot();
        deleteRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, Void.class);
        logout();
    }

    @Test
    void testUpdateAccountEmpty() {
        loginAsAdmin();
        AccountUpdate enableUpdate = new AccountUpdate(null, null, null);
        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, enableUpdate, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(TEST_USERNAME);
        assertThat(account.enabled()).isFalse();
        assertThat(account.roles()).isEqualTo(TEST_ROLES);
    }

    @Test
    void testUpdateAccountPassword() {
        loginAsAdmin();
        AccountUpdate update = new AccountUpdate(USER_PASSWORD, true, null);
        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, update, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(TEST_USERNAME);
        assertThat(account.enabled()).isTrue();
        assertThat(account.roles()).isEqualTo(TEST_ROLES);

        login(TEST_USERNAME, USER_PASSWORD);
        ResponseEntity<Profile> profileResponse = getRequest(PROFILE_ENDPOINT, Profile.class);

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody()).isNotNull();

        Profile profile = profileResponse.getBody();
        assertThat(profile.username()).isEqualTo(TEST_USERNAME);
        assertThat(profile.roles()).isEqualTo(TEST_ROLES);
    }

    @Test
    void testUpdateAccountEnabled() {
        loginAsAdmin();
        AccountUpdate update = new AccountUpdate(null, true, null);
        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, update, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(TEST_USERNAME);
        assertThat(account.enabled()).isTrue();
        assertThat(account.roles()).isEqualTo(TEST_ROLES);
    }

    @Test
    void testUpdateAccountRoles() {
        loginAsAdmin();
        Set<Role> updatedRoles = Set.of(Role.ADMIN, Role.USER);
        AccountUpdate update = new AccountUpdate(null, null, updatedRoles);
        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, update, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(TEST_USERNAME);
        assertThat(account.enabled()).isFalse();
        assertThat(account.roles()).isEqualTo(updatedRoles);
    }

    @Test
    void testUpdateAccountInvalidRole() {
        loginAsAdmin();
        String invalidRolesJson = """
                {
                    "roles": [
                        "ADMIN",
                        "SOME_ROLE",
                        "USER"
                    ]
                }
                """;
        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, invalidRolesJson, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testUpdateAccountRootAddRemoveRoot() {
        loginAsRoot();

        // test add ROOT role
        Set<Role> rolesWithRoot = Set.of(Role.ROOT, Role.ADMIN, Role.USER);
        AccountUpdate addRootRequest =
                new AccountUpdate(null, null, rolesWithRoot);

        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, addRootRequest, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        AccountInfo account = response.getBody();
        assertThat(account.username()).isEqualTo(TEST_USERNAME);
        assertThat(account.enabled()).isFalse();
        assertThat(account.roles()).isEqualTo(rolesWithRoot);

        // test remove ROOT role
        AccountUpdate restoreRequest =
                new AccountUpdate(null, null, TEST_ROLES);
        response = patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, restoreRequest, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        account = response.getBody();
        assertThat(account.username()).isEqualTo(TEST_USERNAME);
        assertThat(account.enabled()).isFalse();
        assertThat(account.roles()).isEqualTo(TEST_ROLES);
    }

    @ParameterizedTest
    @ValueSource(strings = {ADMIN_USERNAME, USER_USERNAME})
    void testUpdateAccountAdminAddRoot(String testUser) {
        loginAsAdmin();

        AccountUpdate addRootRequest =
                new AccountUpdate(null, null, Set.of(Role.ROOT, Role.ADMIN, Role.USER));

        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + testUser, addRootRequest, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testUpdateAccountAdminRemoveRoot() {
        loginAsAdmin();

        AccountUpdate removeRootRequest =
                new AccountUpdate(null, null, Set.of(Role.ADMIN, Role.USER));

        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + ROOT_USERNAME, removeRootRequest, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testUpdateAccountNotFound() {
        loginAsAdmin();
        String emptyUpdate = "{}";

        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME + TEST_USERNAME, emptyUpdate, AccountInfo.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testUpdateAccountAccess() {
        String emptyUpdate = "{}";
        // not logged in - no access
        ResponseEntity<AccountInfo> response =
                patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, emptyUpdate, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // logged in with insufficient authority - no access
        loginAsUser();
        response = patchRequest(ACCOUNT_ENDPOINT + TEST_USERNAME, emptyUpdate, AccountInfo.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
