package com.example.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.server.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
