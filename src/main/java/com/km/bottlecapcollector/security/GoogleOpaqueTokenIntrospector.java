package com.km.bottlecapcollector.security;

import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoogleOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final UserEntityRepository userRepository;
    private RestTemplate restTemplate;
    private final AppProperties appProperties;

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        // Call Google's tokeninfo endpoint to validate the access token
        String url = "https://oauth2.googleapis.com/tokeninfo?access_token=" + token;
        restTemplate = new RestTemplate();
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null || response.containsKey("error")) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException("Invalid token");
        }

        // Extract user info from token response
        String googleId = (String) response.get("sub");
        String email = (String) response.get("email");

        // Get user's name from Google userinfo endpoint (tokeninfo doesn't include name)
        String name = fetchUserName(token, email);

        // Find or create user in Firestore (auto-registration)
        UserEntity user = userRepository.findById(googleId)
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .id(googleId)
                            .email(email)
                            .name(name)
                            .role(Role.USER)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .maxItems(appProperties.getMaxItemsPerUser())
                            .maxIdentificationsPerDay(appProperties.getMaxIdentificationsPerDay())
                            .build();
                    return userRepository.save(newUser);
                });

        // Build authorities based on user's role
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Build attributes map - convert String timestamps to Instant for Spring Security
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", googleId);
        attributes.put("email", email);
        attributes.put("name", user.getName());
        attributes.put("active", true);

        // Convert 'exp' (expires_in) from String to Instant
        if (response.get("expires_in") != null) {
            long expiresIn = Long.parseLong(response.get("expires_in").toString());
            attributes.put("exp", Instant.now().plusSeconds(expiresIn));
        }

        // Copy other safe attributes
        if (response.get("scope") != null) {
            attributes.put("scope", response.get("scope"));
        }

        return new OAuth2IntrospectionAuthenticatedPrincipal(email, attributes, authorities);
    }

    private String fetchUserName(String token, String fallbackEmail) {
        try {
            String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + token;
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = restTemplate.getForObject(url, Map.class);
            if (userInfo != null && userInfo.get("name") != null) {
                return (String) userInfo.get("name");
            }
        } catch (Exception e) {
            // Fall back to email if userinfo fails
        }
        return fallbackEmail;
    }
}
