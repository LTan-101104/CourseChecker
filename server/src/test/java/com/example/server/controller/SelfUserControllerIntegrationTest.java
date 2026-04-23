package com.example.server.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.server.model.CompletedCourse;
import com.example.server.model.User;
import com.example.server.repository.CompletedCourseRepository;
import com.example.server.repository.CourseRepository;
import com.example.server.repository.UserRepository;
import com.example.server.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SelfUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void completedCourseCrudIsScopedToCurrentUser() throws Exception {
        User owner = userRepository.save(new User(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            passwordEncoder.encode("password123!")
        ));
        User otherUser = userRepository.save(new User(
            "student-999",
            "Grace Hopper",
            "grace@umass.edu",
            passwordEncoder.encode("password123!")
        ));
        CompletedCourse otherUsersCourse = completedCourseRepository.save(
            new CompletedCourse(otherUser, "COMPSCI 187", "A", "Spring 2025")
        );

        String ownerToken = jwtService.generateToken(owner);

        mockMvc.perform(get("/api/v1/users/me/completed-courses"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/me/completed-courses")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "compsci 220",
                      "grade": "A-",
                      "semester": "Fall 2025"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.courseCode").value("COMPSCI 220"))
            .andExpect(jsonPath("$.grade").value("A-"));

        CompletedCourse ownersCourse = completedCourseRepository.findByUserIdOrderByCourseCodeAsc(owner.getId())
            .stream()
            .filter(course -> course.getCourseCode().equals("COMPSCI 220"))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(put("/api/v1/users/me/completed-courses/{completedCourseId}", ownersCourse.getId())
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "COMPSCI 230",
                      "grade": "A",
                      "semester": "Spring 2026"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.courseCode").value("COMPSCI 230"))
            .andExpect(jsonPath("$.semester").value("Spring 2026"));

        mockMvc.perform(delete("/api/v1/users/me/completed-courses/{completedCourseId}", otherUsersCourse.getId())
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/users/me/completed-courses/{completedCourseId}", ownersCourse.getId())
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void userCanUpdateAndDeleteOwnAccount() throws Exception {
        User owner = userRepository.save(new User(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            passwordEncoder.encode("password123!")
        ));

        String token = jwtService.generateToken(owner);

        mockMvc.perform(put("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "studentId": "student-123",
                      "displayName": "Ada King",
                      "email": "ada.king@umass.edu"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Ada King"))
            .andExpect(jsonPath("$.email").value("ada.king@umass.edu"));

        mockMvc.perform(delete("/api/v1/users/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    @Test
    void duplicateCompletedCourseReturnsConflict() throws Exception {
        User owner = userRepository.save(new User(
            "student-123",
            "Ada Lovelace",
            "ada@umass.edu",
            passwordEncoder.encode("password123!")
        ));
        completedCourseRepository.save(new CompletedCourse(owner, "COMPSCI 220", "A", "Fall 2025"));

        mockMvc.perform(post("/api/v1/users/me/completed-courses")
                .header("Authorization", "Bearer " + jwtService.generateToken(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "courseCode": "COMPSCI 220",
                      "grade": "A-",
                      "semester": "Spring 2026"
                    }
                    """))
            .andExpect(status().isConflict());
    }
}
