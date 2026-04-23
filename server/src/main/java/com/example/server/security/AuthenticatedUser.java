package com.example.server.security;

import com.example.server.model.User;

public record AuthenticatedUser(
    Long id,
    String studentId,
    String displayName,
    String email
) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
            user.getId(),
            user.getStudentId(),
            user.getDisplayName(),
            user.getEmail()
        );
    }
}
