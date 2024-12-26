package com.example.demo.data.dto.account;

import com.example.demo.data.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AccountCreateRequest(@NotBlank String username, @NotBlank String password, @NotEmpty Set<Role> roles) {
}
