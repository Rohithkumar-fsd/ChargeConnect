package com.example.chargeconnect.user.repo;

import com.example.chargeconnect.user.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r = :role")
    long countByRole(@org.springframework.data.repository.query.Param("role") com.example.chargeconnect.user.model.Role role);
}

