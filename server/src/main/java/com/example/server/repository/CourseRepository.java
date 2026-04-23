package com.example.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.server.model.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    Optional<Course> findByCourseCodeIgnoreCase(String courseCode);

    List<Course> findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
        String courseCodeQuery,
        String titleQuery,
        Pageable pageable
    );

    boolean existsByCourseCode(String courseCode);

    boolean existsByCourseCodeIgnoreCase(String courseCode);
}
