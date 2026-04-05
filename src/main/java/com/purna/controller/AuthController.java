package com.purna.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.purna.dto.LoginRequestDTO;
import com.purna.dto.SignUpRequestDTO;
import com.purna.service.AuthServices;

@RestController
@RequestMapping("/auth")
public class AuthController {
	
	@Autowired
	public AuthServices authServices;
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
	    try {
	        String token = authServices.login(request);
	        return ResponseEntity.ok(token);
	    } catch (Exception e) {
	        return ResponseEntity.status(401).body("Invalid credentials");
	    }
	}
	
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody SignUpRequestDTO request) {
	    try {
	        authServices.signup(request);
	        return ResponseEntity.ok("User registered successfully");
	    } catch (Exception e) {
	        return ResponseEntity.status(400).body(e.getMessage());
	    }
	}
}
