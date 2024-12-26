package com.example.demo.data.dto.account;

import com.example.demo.data.model.Role;

import java.util.Set;

public record AccountInfo(String username, boolean enabled, Set<Role> roles) {
}
