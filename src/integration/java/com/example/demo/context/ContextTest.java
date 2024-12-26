package com.example.demo.context;

import com.example.demo.controller.security.AuthenticationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ContextTest {

    @Autowired
    private AuthenticationController authenticationController;

    @Test
    void contextLoads() {
        assertThat(authenticationController).isNotNull();
    }
}
