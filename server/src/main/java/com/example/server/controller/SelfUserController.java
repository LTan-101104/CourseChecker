package com.example.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.server.dto.CompletedCourseResponse;
import com.example.server.dto.CreateCompletedCourseRequest;
import com.example.server.dto.UpdateCompletedCourseRequest;
import com.example.server.dto.UpdateSelfUserRequest;
import com.example.server.dto.UserResponse;
import com.example.server.security.AuthenticatedUser;
import com.example.server.service.CompletedCourseService;
import com.example.server.service.SelfUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/me")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SelfUserController {

    private final SelfUserService selfUserService;
    private final CompletedCourseService completedCourseService;

    public SelfUserController(
        SelfUserService selfUserService,
        CompletedCourseService completedCourseService
    ) {
        this.selfUserService = selfUserService;
        this.completedCourseService = completedCourseService;
    }

    @GetMapping
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        return selfUserService.getCurrentUser(principal.id());
    }

    @PutMapping
    public UserResponse updateCurrentUser(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody UpdateSelfUserRequest request
    ) {
        return selfUserService.updateCurrentUser(principal.id(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal AuthenticatedUser principal) {
        selfUserService.deleteCurrentUser(principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/completed-courses")
    public List<CompletedCourseResponse> listCompletedCourses(
        @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return completedCourseService.listForUser(principal.id());
    }

    @PostMapping("/completed-courses")
    public ResponseEntity<CompletedCourseResponse> createCompletedCourse(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateCompletedCourseRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(completedCourseService.createForUser(principal.id(), request));
    }

    @PutMapping("/completed-courses/{completedCourseId}")
    public CompletedCourseResponse updateCompletedCourse(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable Long completedCourseId,
        @Valid @RequestBody UpdateCompletedCourseRequest request
    ) {
        return completedCourseService.updateForUser(principal.id(), completedCourseId, request);
    }

    @DeleteMapping("/completed-courses/{completedCourseId}")
    public ResponseEntity<Void> deleteCompletedCourse(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable Long completedCourseId
    ) {
        completedCourseService.deleteForUser(principal.id(), completedCourseId);
        return ResponseEntity.noContent().build();
    }
}
