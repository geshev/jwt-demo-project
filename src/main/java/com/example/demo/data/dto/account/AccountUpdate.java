package com.example.demo.data.dto.account;

import com.example.demo.data.model.Role;

import java.util.Set;

public record AccountUpdate(String password, Boolean enabled, Set<Role> roles) {
}
