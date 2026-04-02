package com.smarthire.repository;

import java.util.List;
import java.util.Optional;

import com.smarthire.entity.User;
import com.smarthire.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByRoleAndVerifiedTrue(UserRole role);
}
