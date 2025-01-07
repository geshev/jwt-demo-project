package com.example.demo.data.dto.account;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdate(@NotBlank String oldPassword, @NotBlank String newPassword) {
}
