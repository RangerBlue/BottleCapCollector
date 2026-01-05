package com.km.bottlecapcollector.security;

import com.km.bottlecapcollector.gcp.repository.FirestoreUserRepository;
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

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final FirestoreUserRepository userRepository;

    @Value("${bcc.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, FirestoreUserRepository userRepository) {
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
                        // OpenAPI / Swagger UI
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Admin endpoints
                        .requestMatchers(HttpMethod.POST, "/caps").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/caps").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/caps/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/caps/*").hasRole("ADMIN")
                        .requestMatchers("/admin/*").hasRole("ADMIN")
                        .requestMatchers("/management/*").hasRole("ADMIN")
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
