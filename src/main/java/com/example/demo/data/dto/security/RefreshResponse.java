package com.example.demo.data.dto.security;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(@JsonProperty("access_token") String accessToken) {
}
