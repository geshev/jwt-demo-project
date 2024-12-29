package com.example.demo.config.security;

import com.example.demo.filter.security.JwtTokenFilter;
import com.example.demo.service.security.PasswordService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.sql.DataSource;

import static com.example.demo.data.model.Authority.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String USERNAME_QUERY = "select username, password, enabled from account where username=?";
    private static final String AUTHORITIES_QUERY = "select account.username, account_roles.roles"
            + " from account_roles join account on account_roles.account_id = account.id where username=?";

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenFilter jwtTokenFilter;
    private final PasswordService passwordService;

    public SecurityConfig(DataSource dataSource, PasswordEncoder passwordEncoder, JwtTokenFilter jwtTokenFilter, PasswordService passwordService) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenFilter = jwtTokenFilter;
        this.passwordService = passwordService;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication()
                .dataSource(dataSource)
                .passwordEncoder(passwordEncoder)
                .userDetailsPasswordManager(passwordService)
                .usersByUsernameQuery(USERNAME_QUERY)
                .authoritiesByUsernameQuery(AUTHORITIES_QUERY);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(
                                (request, response, ex) -> response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler(
                                (request, response, ex) -> response.setStatus(HttpServletResponse.SC_FORBIDDEN))
                )
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/refresh").authenticated()
                        .requestMatchers(HttpMethod.GET, "/profile").hasAuthority(PROFILE_READ.name())
                        .requestMatchers(HttpMethod.PATCH, "/profile").hasAuthority(PROFILE_UPDATE.name())
                        .requestMatchers(HttpMethod.GET, "/accounts").hasAuthority(ACCOUNTS_READ.name())
                        .requestMatchers(HttpMethod.POST, "/accounts").hasAuthority(ACCOUNTS_CREATE.name())
                        .requestMatchers(HttpMethod.GET, "/accounts/**").hasAuthority(ACCOUNTS_READ.name())
                        .requestMatchers(HttpMethod.PATCH, "/accounts/**").hasAuthority(ACCOUNTS_UPDATE.name())
                        .requestMatchers(HttpMethod.DELETE, "/accounts/**").hasAnyAuthority(ACCOUNTS_DELETE.name())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
