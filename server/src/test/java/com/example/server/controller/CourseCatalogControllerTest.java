package com.example.server.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.server.dto.CourseDetailDTO;
import com.example.server.dto.CourseSummaryDTO;
import com.example.server.service.CourseCatalogService;

@WebMvcTest(CourseCatalogController.class)
class CourseCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseCatalogService courseCatalogService;

    @Test
    void searchCoursesReturnsSummaryJson() throws Exception {
        when(courseCatalogService.searchCourses("COMPSCI 1")).thenReturn(List.of(
            new CourseSummaryDTO("COMPSCI 187", "Programming with Data Structures", 4)
        ));

        mockMvc.perform(get("/api/v1/courses/search").param("q", "COMPSCI 1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseCode").value("COMPSCI 187"))
            .andExpect(jsonPath("$[0].title").value("Programming with Data Structures"))
            .andExpect(jsonPath("$[0].credits").value(4));
    }

    @Test
    void getCourseByCodeReturnsDetailJson() throws Exception {
        when(courseCatalogService.getCourseByCode("COMPSCI 220")).thenReturn(
            new CourseDetailDTO(
                "COMPSCI 220",
                "Programming Methodology",
                4,
                "Object-oriented design and software engineering.",
                "COMPSCI 187 AND (MATH 131 OR MATH 132)"
            )
        );

        mockMvc.perform(get("/api/v1/courses/{courseCode}", "COMPSCI 220"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseCode").value("COMPSCI 220"))
            .andExpect(jsonPath("$.title").value("Programming Methodology"))
            .andExpect(jsonPath("$.credits").value(4))
            .andExpect(jsonPath("$.description")
                .value("Object-oriented design and software engineering."))
            .andExpect(jsonPath("$.prerequisiteDescription")
                .value("COMPSCI 187 AND (MATH 131 OR MATH 132)"));
    }
}
