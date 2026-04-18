package com.purna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.purna.security.filter.JwtAuthFilter;
import com.purna.service.MyUserDetailsService;

import lombok.RequiredArgsConstructor;

/**
 * WebSecurityConfig
 * 
 * WHY USE THIS:
 * The backbone of application security. We disable CSRF (Cross-Site Request Forgery) protections 
 * largely because we are using stateless JWT tokens instead of Session/Cookies. It defines exactly 
 * which URLs are public (like `/auth/**` and Swagger) and violently blocks unauthenticated traffic 
 * from touching critical endpoints like Payment and Ordering.
 * 
 * The `addFilterBefore(jwtfilter)` guarantees the JWT Token is mathematically decrypted and verified 
 * before Spring even processes the Request body, preventing payload attacks on secure endpoints.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final MyUserDetailsService myUserDetailsService;
    private final JwtAuthFilter jwtfilter;

    // 🔹 Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 🔹 Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 🔹 Security Filter Chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    	
        http
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/api/auth/**").permitAll()   // login/signup open under both mappings 
                .requestMatchers("/", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll() // swagger ui open
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products", "/api/products/**").permitAll() // allow public browsing
                .requestMatchers("/ws/**").permitAll()     // allow websocket handshakes natively
                .anyRequest().authenticated()              // others protected
            )
            
            .userDetailsService(myUserDetailsService) 
            .formLogin(form -> form.disable()) // we use API, not form
            .addFilterBefore(jwtfilter,UsernamePasswordAuthenticationFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}
