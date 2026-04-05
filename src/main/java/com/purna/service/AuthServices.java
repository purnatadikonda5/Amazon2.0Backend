package com.purna.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.purna.dto.LoginRequestDTO;
import com.purna.dto.SignUpRequestDTO;
import com.purna.model.UserObj;
import com.purna.repository.UserRepository;
import com.purna.utils.AuthUtil;

@Service
public class AuthServices {
	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AuthUtil authUtil;
	@Autowired
	private PasswordEncoder passwordEncoder;
	public String login(LoginRequestDTO request) {
	    try {
	        authenticationManager.authenticate(
	                new UsernamePasswordAuthenticationToken(
	                        request.getEmail(),
	                        request.getPassword()
	                )
	        );
	    } catch (Exception e) {
	        throw new RuntimeException("Invalid username or password");
	    }

	    return authUtil.generateToken(request.getEmail());
	}

	public void signup(SignUpRequestDTO request) throws Exception {
		UserObj user= userRepository.findByEmail(request.getEmail());
		if(user!=null) {
			throw new Exception("Email already Exists");
		}
		user= new UserObj(request.getName(),request.getEmail(),passwordEncoder.encode(request.getPassword()));
		userRepository.save(user);
		return;
	}
	
}
