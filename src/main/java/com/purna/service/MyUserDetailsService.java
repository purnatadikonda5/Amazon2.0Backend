package com.purna.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.purna.model.UserObj;
import com.purna.repository.UserRepository;

@Service
public class MyUserDetailsService implements UserDetailsService {
	
	@Autowired 
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//		System.out.println("here");
//		System.out.println(email);
		UserObj user= userRepository.findByEmail(email);
		if(user==null)throw new UsernameNotFoundException("no such user");
		System.out.println(user.getPassword());
		return User.builder()
		.username(email)
		.password(user.getPassword())
		.roles("USER").build();
	}


}
