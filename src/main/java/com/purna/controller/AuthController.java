package com.purna.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import com.purna.dto.AuthResponseDTO;
import com.purna.dto.LoginRequestDTO;
import com.purna.dto.RefreshTokenRequestDTO;
import com.purna.dto.SignUpRequestDTO;
import com.purna.service.AuthServices;

import lombok.RequiredArgsConstructor;

/**
 * AuthController
 * 
 * WHY USE THIS:
 * Exposes securely mapped authentication channels. Notice how Rate Limiting is strictly configured 
 * uniquely for these endpoints (Bucket4j intercepting prior to this logic).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthServices authServices;
	
	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
	    AuthResponseDTO response = authServices.login(request);
	    return ResponseEntity.ok(response);
	}
	
	@PostMapping("/signup")
	public ResponseEntity<String> signup(@RequestBody @Valid SignUpRequestDTO request) {
	    authServices.signup(request);
	    return ResponseEntity.ok("User registered successfully");
	}

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@RequestBody @Valid RefreshTokenRequestDTO request) {
        AuthResponseDTO response = authServices.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        authServices.logout(authHeader);
        return ResponseEntity.ok("Successfully eradicated Active Session from the central cluster.");
    }
    
    @org.springframework.web.bind.annotation.GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@org.springframework.web.bind.annotation.RequestParam String email) {
        return ResponseEntity.ok(authServices.isEmailTaken(email));
    }
}
