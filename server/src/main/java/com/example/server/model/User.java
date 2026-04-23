package com.example.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;

/**
 * Represents an application user identified by a student ID.
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private String studentId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompletedCourse> completedCourses = new ArrayList<>();

    public User() {}

    public User(String studentId, String displayName, String email, String passwordHash) {
        this.studentId = studentId;
        this.displayName = displayName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public List<CompletedCourse> getCompletedCourses() { return completedCourses; }
    public void setCompletedCourses(List<CompletedCourse> completedCourses) {
        this.completedCourses = completedCourses;
    }

    public void addCompletedCourse(CompletedCourse completedCourse) {
        completedCourses.add(completedCourse);
        completedCourse.setUser(this);
    }

    public void removeCompletedCourse(CompletedCourse completedCourse) {
        completedCourses.remove(completedCourse);
        completedCourse.setUser(null);
    }
}
