package com.purna.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.purna.dto.AuthResponseDTO;
import com.purna.dto.LoginRequestDTO;
import com.purna.dto.RefreshTokenRequestDTO;
import com.purna.dto.SignUpRequestDTO;
import com.purna.exception.InvalidCredentialsException;
import com.purna.exception.UserAlreadyExistsException;
import com.purna.model.UserObj;
import com.purna.repository.UserRepository;
import com.purna.utils.AuthUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthServices
 * 
 * WHY USE REDIS FOR REFRESH TOKENS:
 * Access tokens expire fast (e.g. 15 mins). Refresh tokens live long (e.g. 7 days). 
 * If a user's phone is stolen, we MUST be able to immediately revoke their session!
 * Storing Refresh Tokens in a SQL database is slow. Saving them inside AWS Redis Cluster Native Key-Values 
 * allows instant O(1) mathematical validation and automatic TTL cleanup without database rot!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServices {

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final AuthUtil authUtil;
	private final PasswordEncoder passwordEncoder;
    
    // Natively binds directly to your AWS Redis Labs Cloud Instance via application.properties!
    private final StringRedisTemplate redisTemplate;

	public AuthResponseDTO login(LoginRequestDTO request) {
	    try {
	        authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(
	                        request.getEmail(),
	                        request.getPassword()
	                )
	        );
	    } catch (Exception e) {
	        throw new InvalidCredentialsException("Invalid username or password");
	    }

        // 1. Issue short-lived fast Access JWT Token
        java.util.List<String> roles = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )).getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toList());
	    String accessToken = authUtil.generateToken(request.getEmail(), roles);
        
        // 2. Issue mathematical UUID for Refresh Token
        String refreshToken = UUID.randomUUID().toString();
        
        // 3. Connect to AWS Redis Labs and save the session instantly with a 7-day automated explosive Death (TTL)
        String redisKey = "REFRESH_TOKEN:" + request.getEmail();
        redisTemplate.opsForValue().set(redisKey, refreshToken, Duration.ofDays(7));
        log.info("Redis Memory Injection: Successfully mapped Refresh Token for User {}", request.getEmail());

        UserObj user = userRepository.findByEmail(request.getEmail());

	    return new AuthResponseDTO(accessToken, refreshToken, Long.valueOf(user.getId()), user.getName(), user.getEmail());
	}

    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String redisKey = "REFRESH_TOKEN:" + request.getEmail();
        
        // Query AWS Redis Labs globally via 1 millisecond TCP leap
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        
        if (storedToken == null || !storedToken.equals(request.getRefreshToken())) {
            log.warn("Security Alert: User {} attempted to utilize forged/expired refresh token.", request.getEmail());
            throw new InvalidCredentialsException("Refresh Token Expired or Invalid. Please log in again.");
        }
        
        // The token proved to match our secure Redis Node, rotate the access token!
        // During refresh, we can safely just grant the standard roles (or fetch from DB if roles change dynamically. We will fallback to default).
        String newAccessToken = authUtil.generateToken(request.getEmail(), java.util.Arrays.asList("ROLE_USER"));
        
        // Security Standard: Rotate the refresh token so it cannot be abused statically
        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(redisKey, newRefreshToken, Duration.ofDays(7));
        
        UserObj user = userRepository.findByEmail(request.getEmail());
        return new AuthResponseDTO(newAccessToken, newRefreshToken, Long.valueOf(user.getId()), user.getName(), user.getEmail());
    }

    public void signup(SignUpRequestDTO request) {
        UserObj user = userRepository.findByEmail(request.getEmail());
        if(user != null) {
            throw new UserAlreadyExistsException("Email already Exists");
        }
        
        user = UserObj.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
                
        userRepository.save(user);
        
        // Push to Redis for O(1) Instant Email Lookups
        redisTemplate.opsForSet().add("ALL_USER_EMAILS", request.getEmail());
    }

    /**
     * ⚡ ADVANCED SECURITY: Complete Remote Token Eradication
     * WHY USE THIS:
     * We don't just "tell the frontend to wipe local storage". We forcefully wipe their persistent 
     * refresh token immediately from the cluster, AND we drop the Mathematical Access Token into the Denylist
     * preventing thieves from hijacking the remaining 15 minutes of JWT validation window!
     */
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = authUtil.extractUsername(token);
                // 1. Destroy Long-Lived Refresh Token in Cluster!
                String redisRefreshKey = "REFRESH_TOKEN:" + username;
                redisTemplate.delete(redisRefreshKey);
                
                // 2. Blacklist the mathematical Short-Lived Access Token globally!
                // Instead of hoarding garbage indefinitely, we dynamically calculate exact token death.
                java.util.Date expiration = authUtil.extractExpiration(token);
                long ttlMillis = expiration.getTime() - System.currentTimeMillis();
                
                if (ttlMillis > 0) {
                    redisTemplate.opsForValue().set("BLOCKED_JWT:" + token, "revoked", Duration.ofMillis(ttlMillis));
                    log.info("Security Success: Force-revoked Access Token for {} across entire global cluster.", username);
                }
            } catch (Exception e) {
                log.warn("Attempting to logout with already mathematically expired / malformed token.");
            }
        }
    }
    
    public boolean isEmailTaken(String email) {
        // Fast O(1) Redis Set check
        Boolean existsInRedis = redisTemplate.opsForSet().isMember("ALL_USER_EMAILS", email);
        if (Boolean.TRUE.equals(existsInRedis)) {
            return true;
        }
        // Fallback for older entries not yet in Redis
        UserObj user = userRepository.findByEmail(email);
        if (user != null) {
            redisTemplate.opsForSet().add("ALL_USER_EMAILS", email);
            return true;
        }
        return false;
    }
}
