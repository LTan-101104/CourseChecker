package com.example.server.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.server.model.ImportCourseResult;

@Repository
public interface ImportCourseResultRepository extends JpaRepository<ImportCourseResult, Long> {
    List<ImportCourseResult> findByJob_IdOrderByIdAsc(UUID jobId);
}
