package com.example.server.exception;

public class CourseNotFoundException extends ResourceNotFoundException {

    public CourseNotFoundException(String courseCode) {
        super("Course not found: " + courseCode);
    }
}
