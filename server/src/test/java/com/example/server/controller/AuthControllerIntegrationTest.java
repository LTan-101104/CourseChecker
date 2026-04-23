package com.example.server.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;
import com.example.server.repository.UserRepository;
import com.example.server.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompletedCourseRepository completedCourseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        completedCourseRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsJwtAndCreatedUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "studentId": "student-123",
                      "displayName": "Ada Lovelace",
                      "email": "ada@umass.edu",
                      "password": "password123!"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.user.email").value("ada@umass.edu"))
            .andExpect(jsonPath("$.user.studentId").value("student-123"));
    }

    @Test
    void loginRejectsBadPassword() throws Exception {
        userRepository.save(new User(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            passwordEncoder.encode("password123!")
        ));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "ada@umass.edu",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Bad credentials"));
    }

    @Test
    void meRequiresJwtAndReturnsCurrentUser() throws Exception {
        User savedUser = userRepository.save(new User(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            passwordEncoder.encode("password123!")
        ));

        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + jwtService.generateToken(savedUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ada@umass.edu"))
            .andExpect(jsonPath("$.displayName").value("Ada Lovelace"));
    }
}
