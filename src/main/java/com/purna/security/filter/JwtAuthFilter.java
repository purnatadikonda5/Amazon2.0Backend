package com.purna.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.purna.utils.AuthUtil;

import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);
            
            // ⚡ ADVANCED SECURITY: JWT Redis Denylist (Instant Kill-Switch)
            // Mathematical Tokens cannot be naturally revoked inside the JVM cache before they expire.
            // When a user logs out cleanly, we stash the token inside AWS Redis. If a hacker intercepts 
            // the wire, the firewall intercepts it here instantly blocking the session globally!
            Boolean isBlacklisted = redisTemplate.hasKey("BLOCKED_JWT:" + token);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Security Violation: Token has been revoked/logged out.");
                return;
            }

            String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(token, username)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        chain.doFilter(request, response);
    }
}