package com.example.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.server.model.ImportJob;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
}
