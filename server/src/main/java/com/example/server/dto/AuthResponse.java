package com.example.server.dto;

public record AuthResponse(
    String token,
    CurrentUserResponse user
) {}
