package com.example.demo.data.dto.account;

import com.example.demo.data.model.Role;

import java.util.Set;

public record Profile(String username, Set<Role> roles) {
}
