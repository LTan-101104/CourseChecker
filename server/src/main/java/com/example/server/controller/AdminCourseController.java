package com.example.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.dto.CourseResponse;
import com.example.server.dto.CreateCourseRequest;
import com.example.server.dto.UpdateCourseRequest;
import com.example.server.service.AdminCourseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/courses")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminCourseService.createCourse(request));
    }

    @PutMapping("/{courseCode}")
    public CourseResponse updateCourse(
        @PathVariable String courseCode,
        @Valid @RequestBody UpdateCourseRequest request
    ) {
        return adminCourseService.updateCourse(courseCode, request);
    }

    @DeleteMapping("/{courseCode}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String courseCode) {
        adminCourseService.deleteCourse(courseCode);
        return ResponseEntity.noContent().build();
    }
}
