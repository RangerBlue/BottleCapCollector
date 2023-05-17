package com.km.bottlecapcollector.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DataSource dataSource;
    public SecurityConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .cors().and()
                    .csrf().disable()
                    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                    .httpBasic().and()
                    .headers().frameOptions().sameOrigin().and()
                    .authorizeHttpRequests((requests) -> requests
                            .requestMatchers(HttpMethod.POST, "/caps").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.DELETE, "/caps/*").hasRole("ADMIN")
                            .requestMatchers(HttpMethod.PUT, "/caps/*").hasRole("ADMIN")
                            .requestMatchers("/admin/*").hasRole("ADMIN")
                            .requestMatchers("/management/*").hasRole("ADMIN")
                            .anyRequest().permitAll()
                    )
                    .formLogin().disable()
                    .logout();
            return http.build();
        }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource)
                .authoritiesByUsernameQuery("select USERNAME, AUTHORITY from AUTHORITIES where USERNAME=?")
                .usersByUsernameQuery("select USERNAME, PW, 1 as enabled from USERS where USERNAME=?");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
