package com.example.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.server.model.Requirement;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {
}
