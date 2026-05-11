package com.example.server.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.server.repository.CompletedCourseRepository;
import com.example.server.model.Course;
import com.example.server.repository.CourseRepository;
import com.example.server.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndScopedCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompletedCourseRepository completedCourseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        completedCourseRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void authFlowAndCompletedCourseCrudStayScopedToTheAuthenticatedUser() throws Exception {
        JsonNode registeredAlice = register("A10001", "Alice", "Alice@Example.com", "password123");
        String aliceToken = login("ALICE@EXAMPLE.COM", "password123");
        String bobToken = register("A10002", "Bob", "bob@example.com", "password456").get("token").asText();

        mockMvc.perform(get("/api/v1/auth/me").header(AUTHORIZATION, bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value("A10001"))
            .andExpect(jsonPath("$.displayName").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"));

        assertThat(registeredAlice.at("/user/email").asText()).isEqualTo("alice@example.com");

        MvcResult createdResult = mockMvc.perform(
            post("/api/v1/users/me/completed-courses")
                .header(AUTHORIZATION, bearer(aliceToken))
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "courseCode", " compsci 187 ",
                    "grade", " A ",
                    "semester", " Fall 2025 "
                )))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.courseCode").value("COMPSCI 187"))
            .andExpect(jsonPath("$.grade").value("A"))
            .andExpect(jsonPath("$.semester").value("Fall 2025"))
            .andReturn();

        long completedCourseId = readJson(createdResult).get("id").asLong();

        mockMvc.perform(get("/api/v1/users/me/completed-courses").header(AUTHORIZATION, bearer(bobToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(
            post("/api/v1/users/me/completed-courses")
                .header(AUTHORIZATION, bearer(aliceToken))
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "courseCode", "COMPSCI 187",
                    "grade", "B",
                    "semester", "Spring 2026"
                )))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Completed course already exists for this user"));

        mockMvc.perform(
            put("/api/v1/users/me/completed-courses/{completedCourseId}", completedCourseId)
                .header(AUTHORIZATION, bearer(bobToken))
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "courseCode", "math 132",
                    "grade", "A",
                    "semester", "Spring 2026"
                )))
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value("You do not have access to this completed course"));

        mockMvc.perform(
            put("/api/v1/users/me/completed-courses/{completedCourseId}", completedCourseId)
                .header(AUTHORIZATION, bearer(aliceToken))
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "courseCode", "math 132",
                    "grade", "A",
                    "semester", "Spring 2026"
                )))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseCode").value("MATH 132"))
            .andExpect(jsonPath("$.grade").value("A"))
            .andExpect(jsonPath("$.semester").value("Spring 2026"));

        mockMvc.perform(get("/api/v1/users/me/completed-courses").header(AUTHORIZATION, bearer(aliceToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseCode").value("MATH 132"))
            .andExpect(jsonPath("$[0].grade").value("A"));
    }

    @Test
    @Tag("integration")
    void completedCourseEndpointsIncludeCatalogMetadataWhenCourseExists() throws Exception {
        courseRepository.save(new Course(
            "COMPSCI 187",
            "Programming with Data Structures",
            4,
            "Use, design, and analysis of data structures."
        ));
        String token = register("A10003", "Carol", "carol@example.com", "password789")
            .get("token")
            .asText();

        mockMvc.perform(
            post("/api/v1/users/me/completed-courses")
                .header(AUTHORIZATION, bearer(token))
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "courseCode", "compsci 187",
                    "grade", "A",
                    "semester", "Fall 2025"
                )))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.courseCode").value("COMPSCI 187"))
            .andExpect(jsonPath("$.title").value("Programming with Data Structures"))
            .andExpect(jsonPath("$.credits").value(4));

        mockMvc.perform(get("/api/v1/users/me/completed-courses").header(AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].courseCode").value("COMPSCI 187"))
            .andExpect(jsonPath("$[0].title").value("Programming with Data Structures"))
            .andExpect(jsonPath("$[0].credits").value(4));
    }

    private JsonNode register(
        String studentId,
        String displayName,
        String email,
        String password
    ) throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "studentId", studentId,
                    "displayName", displayName,
                    "email", email,
                    "password", password
                )))
        )
            .andExpect(status().isCreated())
            .andReturn();

        return readJson(result);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of(
                    "email", email,
                    "password", password
                )))
        )
            .andExpect(status().isOk())
            .andReturn();

        return readJson(result).get("token").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
