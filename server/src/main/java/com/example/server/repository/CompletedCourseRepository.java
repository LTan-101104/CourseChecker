package com.example.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.server.model.CompletedCourse;

@Repository
public interface CompletedCourseRepository extends JpaRepository<CompletedCourse, Long> {

    List<CompletedCourse> findByStudentId(String studentId);

    boolean existsByStudentIdAndCourseCode(String studentId, String courseCode);
}
