package com.purna.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitingFilter (Targeted Anti-DDOS Bucket4j Security)
 * 
 * WHY USE THIS:
 * The user requested proper rate-limiting exclusively mapped to `/auth/login`, `/auth/signup`, and `/auth/refresh`.
 * Passwords can be brute-forced. By assigning a strict bucket (e.g. 5 attempts per 10 minutes) 
 * exclusively to the Auth routes, we protect endpoints without choking standard API queries like Product Listings.
 */
@Component
public class RateLimitingFilter implements Filter {

    // IP Cache for Auth Routes (Strict Limits)
    private final Map<String, Bucket> authCache = new ConcurrentHashMap<>();
    
    // IP Cache for Standard Routes (Loose Limits)
    private final Map<String, Bucket> standardCache = new ConcurrentHashMap<>();

    private Bucket createAuthBucket() {
        // STRICT: Only 5 login/signup tries allowed every 5 minutes per IP! 
        Refill refill = Refill.greedy(5, Duration.ofMinutes(5));
        Bandwidth limit = Bandwidth.classic(5, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createStandardBucket() {
        // STANDARD: 100 requests per minute
        Refill refill = Refill.greedy(100, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(100, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
            
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String ipAddress = httpRequest.getRemoteAddr();
        String uri = httpRequest.getRequestURI();
        
        Bucket bucket;
        if (uri.startsWith("/auth/login") || uri.startsWith("/auth/signup") || uri.startsWith("/auth/refresh")) {
            bucket = authCache.computeIfAbsent(ipAddress, k -> createAuthBucket());
        } else {
            bucket = standardCache.computeIfAbsent(ipAddress, k -> createStandardBucket());
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            // DDOS Detected or Rate Limit Exceeded!
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429); // HTTP 429 Too Many Requests
            httpResponse.getWriter().write("Security Policy: Rate Limit Exceeded for " + uri + ". Try again later.");
            return;
        }
    }
}
