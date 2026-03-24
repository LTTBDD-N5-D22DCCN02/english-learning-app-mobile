package com.estudy.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.estudy.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByUsernameAndDeletedFalse(String username);

    Boolean existsByEmailAndDeletedFalse(String email);

    Boolean existsByPhoneAndDeletedFalse(String phone);

    Boolean existsByUsernameAndDeletedFalse(String username);
}
