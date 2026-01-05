package com.km.bottlecapcollector.security;

import com.km.bottlecapcollector.gcp.document.FirestoreUser;
import com.km.bottlecapcollector.gcp.repository.FirestoreUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final FirestoreUserRepository userRepository;

    public CustomOAuth2UserService(FirestoreUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        FirestoreUser user = userRepository.findById(googleId)
                .orElseGet(() -> {
                    FirestoreUser newUser = FirestoreUser.builder()
                            .id(googleId)
                            .email(email)
                            .name(name)
                            .role(Role.USER)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return userRepository.save(newUser);
                });

        Set<GrantedAuthority> authorities = new HashSet<>(oauth2User.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "sub");
    }
}
