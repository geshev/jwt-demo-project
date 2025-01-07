package com.example.demo.profile;

import com.example.demo.BaseTest;
import com.example.demo.data.dto.account.PasswordUpdate;
import com.example.demo.data.dto.account.Profile;
import com.example.demo.data.dto.error.ValidationError;
import com.example.demo.data.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.example.demo.util.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ProfileTest extends BaseTest {

    private static final String ADMIN_UPDATED_PASSWORD = "admin-update";
    private static final String ADMIN_INVALID_PASSWORD = "invalid";

    @ParameterizedTest
    @MethodSource("allProfiles")
    void testGetProfile(String username, String password, Set<Role> roles) {
        login(username, password);
        ResponseEntity<Profile> response = getRequest(PROFILE_ENDPOINT, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Profile profile = response.getBody();
        assertThat(profile.username()).isEqualTo(username);
        assertThat(profile.roles()).isEqualTo(roles);
    }

    @Test
    void testGetProfileUnauthorized() {
        ResponseEntity<Profile> response = getRequest(PROFILE_ENDPOINT, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testUpdateProfile() {
        loginAsAdmin();
        PasswordUpdate passwordUpdate = new PasswordUpdate(ADMIN_PASSWORD, ADMIN_UPDATED_PASSWORD);
        ResponseEntity<Profile> response = patchRequest(PROFILE_ENDPOINT, passwordUpdate, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        // Verify that we can log in with the new password and revert the change
        logout();
        login(ADMIN_USERNAME, ADMIN_UPDATED_PASSWORD);

        passwordUpdate = new PasswordUpdate(ADMIN_UPDATED_PASSWORD, ADMIN_PASSWORD);
        response = patchRequest(PROFILE_ENDPOINT, passwordUpdate, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void testUpdateProfileInvalidUpdate() {
        loginAsAdmin();
        ResponseEntity<Profile> response = patchRequest(PROFILE_ENDPOINT, null, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateProfileInvalidOldPassword() {
        loginAsAdmin();
        PasswordUpdate passwordUpdate = new PasswordUpdate(ADMIN_INVALID_PASSWORD, ADMIN_UPDATED_PASSWORD);
        ResponseEntity<Profile> response = patchRequest(PROFILE_ENDPOINT, passwordUpdate, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUpdateProfileInvalidFields() {
        loginAsAdmin();
        PasswordUpdate passwordUpdate = new PasswordUpdate(null, null);
        ResponseEntity<List<ValidationError>> response =
                patchRequest(PROFILE_ENDPOINT, passwordUpdate, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(2);
        errors.forEach(error -> {
            switch (error.field()) {
                case "oldPassword", "newPassword" -> assertThat(error.error()).contains("blank");
                // we are not expecting errors in other fields
                default -> fail();
            }
        });
    }

    @Test
    void testUpdateProfileInvalidOldPasswordFieldNull() {
        testUpdateProfileInvalidFields(null, ADMIN_UPDATED_PASSWORD);
    }

    @Test
    void testUpdateProfileInvalidOldPasswordFieldEmpty() {
        testUpdateProfileInvalidFields("", ADMIN_UPDATED_PASSWORD);
    }

    @Test
    void testUpdateProfileInvalidNewPasswordFieldNull() {
        testUpdateProfileInvalidFields(ADMIN_PASSWORD, null);
    }

    @Test
    void testUpdateProfileInvalidNewPasswordFieldEmpty() {
        testUpdateProfileInvalidFields(ADMIN_PASSWORD, "");
    }

    void testUpdateProfileInvalidFields(String oldPassword, String newPassword) {
        loginAsAdmin();
        PasswordUpdate passwordUpdate = new PasswordUpdate(oldPassword, newPassword);
        ResponseEntity<List<ValidationError>> response =
                patchRequest(PROFILE_ENDPOINT, passwordUpdate, new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        List<ValidationError> errors = response.getBody();
        assertThat(errors.size()).isEqualTo(1);
        if (!StringUtils.hasText(oldPassword)) {
            assertThat(errors.getFirst().field()).isEqualTo("oldPassword");
        }
        if (!StringUtils.hasText(newPassword)) {
            assertThat(errors.getFirst().field()).isEqualTo("newPassword");
        }
    }

    @Test
    void testUpdateProfileUnauthorized() {
        PasswordUpdate passwordUpdate = new PasswordUpdate(ADMIN_PASSWORD, ADMIN_UPDATED_PASSWORD);
        ResponseEntity<Profile> response = patchRequest(PROFILE_ENDPOINT, passwordUpdate, Profile.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    static Stream<Arguments> allProfiles() {
        return Stream.of(
                Arguments.of(ROOT_USERNAME, ROOT_PASSWORD, ROOT_ROLES),
                Arguments.of(ADMIN_USERNAME, ADMIN_PASSWORD, ADMIN_ROLES),
                Arguments.of(USER_USERNAME, USER_PASSWORD, USER_ROLES)
        );
    }
}
