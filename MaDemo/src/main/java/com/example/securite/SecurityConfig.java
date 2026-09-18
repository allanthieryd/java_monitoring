package com.example.securite;

import com.example.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final boolean rateLimitEnabled;
    private final int rateLimitApiPerMinute;
    private final int rateLimitAuthPerMinute;

    public SecurityConfig(
            JwtUtil jwtUtil,
            @Value("${ratelimit.enabled:true}") boolean rateLimitEnabled,
            @Value("${ratelimit.api-per-minute:100}") int rateLimitApiPerMinute,
            @Value("${ratelimit.auth-per-minute:10}") int rateLimitAuthPerMinute) {
        this.jwtUtil = jwtUtil;
        this.rateLimitEnabled = rateLimitEnabled;
        this.rateLimitApiPerMinute = rateLimitApiPerMinute;
        this.rateLimitAuthPerMinute = rateLimitAuthPerMinute;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/profil.html", "/").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/moderation/reports/*/resolve").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/moderation/reports/*/reject").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().permitAll()
            )
            .addFilterBefore(
                new RateLimitFilter(rateLimitEnabled, rateLimitApiPerMinute, rateLimitAuthPerMinute),
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var user = User.withUsername("allan")
                .password(encoder.encode("7895"))
                .roles("USER")
                .build();

        var admin = User.withUsername("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
