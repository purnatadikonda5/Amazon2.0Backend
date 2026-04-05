package com.purna.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.purna.model.UserObj;

@Repository
public interface UserRepository extends JpaRepository<UserObj, Integer> {

	public UserObj findByEmail(String email);

}
