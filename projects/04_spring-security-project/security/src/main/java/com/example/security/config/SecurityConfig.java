package com.example.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    // Here, we're creating our own SecurityFilterChain bean by overriding the default one.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                // disables CSRF (Cross Site Request Forgery) protection
                .csrf(customizer -> customizer.disable())

                // for every API endpoint, user must be authenticated (logged in)
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/register", "/login")
                        .permitAll()
                        .anyRequest().authenticated()
                )

                // enables HTTP Basic authentication
                .httpBasic(Customizer.withDefaults())

                // configures how Spring Security manages sessions
                // STATELESS means it'll not store or create HTTP sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // builds and returns the final SecurityFilterChain object
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider needs UserDetailsService by which it'll call userDetailsService.loadUserByUsername()
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        // password comparison logic
        provider.setPasswordEncoder(new BCryptPasswordEncoder(10));

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}