package com.example.server.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;
import com.example.server.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCourseControllerIntegrationTest {

    private static final String ADMIN_SECRET = "test-admin-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompletedCourseRepository completedCourseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        completedCourseRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminSecretProtectedCrudWorks() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "COMPSCI 220",
                      "title": "Programming Methodology",
                      "credits": 4,
                      "description": "desc"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/courses")
                .header("X-Admin-Secret", ADMIN_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "COMPSCI 220",
                      "title": "Programming Methodology",
                      "credits": 4,
                      "description": "desc",
                      "prerequisite": {
                        "type": "AND",
                        "children": [
                          { "type": "COURSE", "courseCode": "COMPSCI 187" },
                          { "type": "COURSE", "courseCode": "MATH 131" }
                        ]
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.courseCode").value("COMPSCI 220"))
            .andExpect(jsonPath("$.prerequisite.type").value("AND"));

        mockMvc.perform(put("/api/v1/admin/courses/{courseCode}", "COMPSCI 220")
                .header("X-Admin-Secret", ADMIN_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "COMPSCI 220",
                      "title": "Programming Methodology",
                      "credits": 4,
                      "description": "updated desc",
                      "prerequisite": {
                        "type": "OR",
                        "children": [
                          { "type": "COURSE", "courseCode": "MATH 131" },
                          { "type": "COURSE", "courseCode": "MATH 132" }
                        ]
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prerequisite.type").value("OR"))
            .andExpect(jsonPath("$.prerequisiteDescription").value("MATH 131 OR MATH 132"));

        mockMvc.perform(delete("/api/v1/admin/courses/{courseCode}", "COMPSCI 220")
                .header("X-Admin-Secret", ADMIN_SECRET))
            .andExpect(status().isNoContent());
    }

    @Test
    void invalidPrerequisitePayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/courses")
                .header("X-Admin-Secret", ADMIN_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "COMPSCI 220",
                      "title": "Programming Methodology",
                      "credits": 4,
                      "prerequisite": {
                        "type": "COURSE",
                        "courseCode": "COMPSCI 220"
                      }
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("A course cannot require itself"));
    }
}
