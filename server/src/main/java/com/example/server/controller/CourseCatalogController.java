package com.example.server.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.dto.CourseDetailDTO;
import com.example.server.dto.CourseSummaryDTO;
import com.example.server.service.CourseCatalogService;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class CourseCatalogController {

    private final CourseCatalogService courseCatalogService;

    public CourseCatalogController(CourseCatalogService courseCatalogService) {
        this.courseCatalogService = courseCatalogService;
    }

    @GetMapping("/search")
    public List<CourseSummaryDTO> searchCourses(@RequestParam("q") String query) {
        return courseCatalogService.searchCourses(query);
    }

    @GetMapping("/{courseCode}")
    public CourseDetailDTO getCourseByCode(@PathVariable String courseCode) {
        return courseCatalogService.getCourseByCode(courseCode);
    }
}
