package com.km.bottlecapcollector.security;

import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserEntityRepository userRepository;

    @Value("${bcc.cors.allowed-origins:}")
    private String allowedOrigins;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, UserEntityRepository userRepository) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .authorizeHttpRequests(requests -> requests
                        // Allow preflight OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // OpenAPI / Swagger UI
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Public collection API
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        // Admin endpoints
                        .requestMatchers(HttpMethod.POST, "/caps").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/caps").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/caps/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/caps/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/validateCap").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/whatCapAreYou").hasRole("ADMIN")
                        .requestMatchers("/admin/*").hasRole("ADMIN")
                        .requestMatchers("/management/*").hasRole("ADMIN")
                        // Collection API - requires authentication
                        .requestMatchers("/api/v1/collections/**").authenticated()
                        .anyRequest().permitAll()
                )
                // Browser-based OAuth2 login (for web frontend)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )
                // Bearer token authentication (for Postman/API clients)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .opaqueToken(opaque -> opaque
                                .introspector(googleOpaqueTokenIntrospector())
                        )
                );
        return http.build();
    }

    @Bean
    public OpaqueTokenIntrospector googleOpaqueTokenIntrospector() {
        return new GoogleOpaqueTokenIntrospector(userRepository);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins;
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            origins = List.of("http://localhost:3000");
            log.warn("CORS allowed origins not configured, using default: {}", origins);
        } else {
            origins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            log.info("CORS allowed origins configured: {}", origins);
        }

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins;
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            origins = List.of("http://localhost:3000");
        } else {
            origins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
