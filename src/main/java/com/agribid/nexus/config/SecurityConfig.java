package com.agribid.nexus.config;

import com.agribid.nexus.security.JwtAuthFilter;
import com.agribid.nexus.security.KycAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Every rule here fails closed by default. Role checks happen at the
 * filter-chain layer (authorizeHttpRequests) rather than scattered
 * across service methods, and the KycAuthorizationManager is wired
 * in specifically for the bidding endpoint to reject unverified
 * distributors before DispatcherServlet routes the request anywhere.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final KycAuthorizationManager kycAuthorizationManager;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/listings/**").permitAll()
                        .requestMatchers("/api/v1/listings/*/bids").access(kycAuthorizationManager)
                        .requestMatchers("/api/v1/bids/**").hasRole("DISTRIBUTOR")
                        .requestMatchers("/h2-console/**").permitAll()
                        // NOTE: this used to be .hasRole("FARMER") for the whole
                        // /api/v1/crop-lots/** prefix, which silently blocked
                        // distributors from GET /{lotId} at the filter-chain layer —
                        // contradicting the intent that any authenticated user can
                        // view a graded lot. Fine-grained role + ownership checks
                        // already live on each controller method via @PreAuthorize
                        // (create/attachImage/grade/mine require FARMER + ownership
                        // where relevant; getLot() intentionally has none beyond
                        // "authenticated"), so this rule only needs to gate entry,
                        // not duplicate what @PreAuthorize already enforces correctly.
                        .requestMatchers("/api/v1/crop-lots/**").authenticated()
                        .requestMatchers("/api/v1/agronomist/**").hasRole("AGRONOMIST")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://127.0.0.1:5500",
                "http://localhost:5500"
        ));

        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposed for AuthServiceImpl.login(), which authenticates
     * email/password credentials via authenticationManager.authenticate(...)
     * rather than manually loading and comparing hashes — this keeps
     * BCrypt comparison logic inside Spring Security's own
     * DaoAuthenticationProvider rather than reimplemented in our service.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}