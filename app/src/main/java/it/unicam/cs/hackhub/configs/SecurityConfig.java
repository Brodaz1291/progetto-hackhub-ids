package it.unicam.cs.hackhub.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security of the platform. Without this configuration Spring Security would apply its
 * defaults, which block every request of the project.
 *
 * HackHub is a stateless REST API: there are no cookie-based sessions, so CSRF protection is
 * disabled, and the endpoints are open because there is no real authentication mechanism yet
 * and the identity of the user travels as an explicit parameter (organizerId, userId, ...).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll());

        // the H2 console renders inside frames
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}
