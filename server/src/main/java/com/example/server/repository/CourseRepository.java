package com.example.server.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.server.model.Course;
import com.example.server.model.CourseType;

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

    List<Course> findByCourseCodeIn(Collection<String> courseCodes);

    @Query("""
        SELECT c FROM Course c
        WHERE c.courseType = :type
          AND (LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY c.courseCode ASC
        """)
    List<Course> findByTypeAndQuery(
        @Param("type") CourseType type,
        @Param("q") String q,
        Pageable pageable
    );
}
